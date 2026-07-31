/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.resource.Delay;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An extension to {@link ConnectionWatchdog} that intercepts maintenance events.
 *
 * @author Tihomir Mateev
 * @since 7.0
 * @see ClientOptions#getMaintNotificationsConfig()
 */
@ChannelHandler.Sharable
public class MaintenanceAwareConnectionWatchdog extends ConnectionWatchdog implements PushListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MaintenanceAwareConnectionWatchdog.class);

    public static final AttributeKey<RebindState> REBIND_ATTRIBUTE = AttributeKey.newInstance("rebindAddress");

    /**
     * How long reconnects stay redirected to the slot-migration target. {@code SMIGRATED} carries no time-to-live on the wire,
     * unlike {@code MOVING}, so a fixed window is applied. It must outlast the reconnect (including reconnect backoff),
     * otherwise a later attempt falls back to the endpoint the slots migrated away from. Matches the default other Redis
     * clients apply to this notification.
     */
    static final Duration SLOT_HANDOFF_REDIRECT_WINDOW = Duration.ofSeconds(120);

    private Channel channel;

    private final Set<MaintenanceAwareComponent> componentListeners = new HashSet<>();

    private final Set<MaintenanceAwareClusterComponent> clusterComponentListeners = new HashSet<>();

    private RebindAwareAddressSupplier rebindAwareAddressSupplier;

    public MaintenanceAwareConnectionWatchdog(Delay reconnectDelay, ClientOptions clientOptions, Bootstrap bootstrap,
            Timer timer, EventExecutorGroup reconnectWorkers, Mono<SocketAddress> socketAddressSupplier,
            ReconnectionListener reconnectionListener, ConnectionFacade connectionFacade, EventBus eventBus,
            Endpoint endpoint) {

        super(reconnectDelay, clientOptions, bootstrap, timer, reconnectWorkers, socketAddressSupplier, reconnectionListener,
                connectionFacade, eventBus, endpoint);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.channel = ctx.channel();

        ChannelPipeline pipeline = ctx.channel().pipeline();
        CommandHandler commandHandler = pipeline.get(CommandHandler.class);

        if (!commandHandler.getEndpoint().getPushListeners().contains(this)) {
            commandHandler.getEndpoint().addListener(this);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel() != null && ctx.channel().isActive() && ctx.channel().hasAttr(REBIND_ATTRIBUTE)
                && ctx.channel().attr(REBIND_ATTRIBUTE).get() == RebindState.COMPLETED) {
            logger.debug("[{}]  Disconnecting at {}", ChannelLogDescriptor.logDescriptor(channel), LocalTime.now());
            ctx.channel().close().awaitUninterruptibly();
            notifyRebindCompleted();
        }

        super.channelReadComplete(ctx);
    }

    @Override
    protected Mono<SocketAddress> wrapSocketAddressSupplier(Mono<SocketAddress> socketAddressSupplier) {
        Mono<SocketAddress> source = super.wrapSocketAddressSupplier(socketAddressSupplier);
        rebindAwareAddressSupplier = new RebindAwareAddressSupplier();
        return rebindAwareAddressSupplier.wrappedSupplier(source);
    }

    @Override
    public void onPushMessage(PushMessage message) {

        MaintenanceNotification notification = MaintenanceNotification.from(message);

        if (notification != null) {

            switch (notification.getType()) {
                case MOVING:
                    logger.debug("Rebind requested");
                    MaintenanceNotification.MovingNotification moving = (MaintenanceNotification.MovingNotification) notification;

                    if (null == moving.getEndpoint()) {
                        logger.debug("[channel={}] Deferred Rebind requested. Rebinding to current endpoint after '{}'",
                                channel.id(), moving.getTime());
                        channel.eventLoop().schedule(() -> rebind(moving), moving.getTime().toMillis() / 2,
                                TimeUnit.MILLISECONDS);
                    } else {
                        rebind(moving);
                    }
                    break;
                case MIGRATING:
                    logger.debug("[{}] Shard migration started", ChannelLogDescriptor.logDescriptor(channel));
                    MaintenanceNotification.MigrationStartedNotification migrationStarted = (MaintenanceNotification.MigrationStartedNotification) notification;
                    notifyMigrateStarted(migrationStarted.getShards());
                    break;
                case MIGRATED:
                    logger.debug("[{}] Shard migration completed", ChannelLogDescriptor.logDescriptor(channel));
                    MaintenanceNotification.MigrationCompletedNotification migrationCompleted = (MaintenanceNotification.MigrationCompletedNotification) notification;
                    notifyMigrateCompleted(migrationCompleted.getShards());
                    break;
                case FAILING_OVER:
                    logger.debug("[{}] Failover started", ChannelLogDescriptor.logDescriptor(channel));
                    MaintenanceNotification.FailoverStartedNotification failoverStarted = (MaintenanceNotification.FailoverStartedNotification) notification;
                    notifyFailoverStarted(failoverStarted.getShards());
                    break;
                case FAILED_OVER:
                    logger.debug("[{}] Failover completed", ChannelLogDescriptor.logDescriptor(channel));
                    MaintenanceNotification.FailoverCompletedNotification failoverCompleted = (MaintenanceNotification.FailoverCompletedNotification) notification;
                    notifyFailoverCompleted(failoverCompleted.getShards());
                    break;
                case SMIGRATING:
                    logger.debug("[{}] Slot migration started", ChannelLogDescriptor.logDescriptor(channel));
                    MaintenanceNotification.SlotsMigrationStartedNotification slotMigrationStarted = (MaintenanceNotification.SlotsMigrationStartedNotification) notification;
                    // TODO: Check why slots are accessed as a String here and if this causes issues later
                    notifySlotMigrateStarted(slotMigrationStarted.getShards());
                    break;
                case SMIGRATED:
                    logger.debug("[{}] Slot migration completed", ChannelLogDescriptor.logDescriptor(channel));
                    MaintenanceNotification.SlotsMigrationCompletedNotification slotMigrationCompleted = (MaintenanceNotification.SlotsMigrationCompletedNotification) notification;

                    // Notify before rebinding so relaxed timeouts stay enabled for the duration of the handoff
                    notifySlotMigrateCompleted(slotMigrationCompleted.getShards());
                    rebind(slotMigrationCompleted);
                    break;

            }
        }
    }

    /**
     * Rebind after time seconds to the given endpoint
     *
     * @param time
     * @param endpoint
     */
    private void rebind(Duration time, InetSocketAddress endpoint) {
        logger.debug("[{}] Rebind to '{}'", ChannelLogDescriptor.logDescriptor(channel), endpoint);
        channel.attr(REBIND_ATTRIBUTE).set(RebindState.STARTED);
        rebindAwareAddressSupplier.rebind(time, endpoint);

        ChannelPipeline pipeline = channel.pipeline();
        CommandHandler commandHandler = pipeline.get(CommandHandler.class);
        if (commandHandler.getStack().isEmpty()) {
            logger.debug("[{}] Closing channel as part of rebind", ChannelLogDescriptor.logDescriptor(channel));
            channel.close().awaitUninterruptibly();
            channel.attr(REBIND_ATTRIBUTE).set(RebindState.COMPLETED);
        } else {
            notifyRebindStarted(time, endpoint);
        }
    }

    /**
     * Rebind based on MOVED
     *
     * @param movingEvent
     */
    private void rebind(MaintenanceNotification.MovingNotification movingEvent) {
        rebind(movingEvent.getTime(), movingEvent.getEndpoint());
    }

    /**
     * Rebind based on SMIGRATED
     *
     * @param sMigratedEvent
     */
    private void rebind(MaintenanceNotification.SlotsMigrationCompletedNotification sMigratedEvent) {

        // TODO: What if there is more than one target address?
        // E.g., slot 0-4095 got moved to node A, but 4096-8191 to node B
        // In such a case this channel needs to be rebound, but a second one needs to be created on a higher level
        boolean isRebound = false;
        List<MaintenanceNotification.SlotMigration> slots = sMigratedEvent.getSlotMigrations();

        for (MaintenanceNotification.SlotMigration s : slots) {
            if (this.channel.remoteAddress() instanceof InetSocketAddress) {
                InetSocketAddress sourceSocketAddr = (InetSocketAddress) this.channel.remoteAddress();
                if (MaintenanceNotification.matches(s.getSource(), sourceSocketAddr)) {
                    logger.debug("Found matching source endpoint '{}'", s.getSource());

                    if (!isRebound) {
                        InetSocketAddress destSocketAddr = MaintenanceNotification.getEndpoint(s.getDestination());
                        rebind(SLOT_HANDOFF_REDIRECT_WINDOW, destSocketAddr);
                        isRebound = true;
                    }
                }
            }
        }
    }

    /**
     * Register a component that is aware of re-bind events. Such a component is going to be notified of re-bind events by
     * calling their {@code onRebindStarted} and {@code onRebindCompleted} methods.
     *
     * @param component the component to register
     */
    public void setMaintenanceEventListener(MaintenanceAwareComponent component) {
        this.componentListeners.add(component);
    }

    /**
     * Register a component that is aware of slot migration events. Such a component is going to be notified of slot migrations
     * by calling their {@code onSlotMigrateStarted} and {@code onSlotMigrateCompleted} methods.
     *
     * @param component the component to register
     * @since 7.7
     */
    public void setMaintenanceClusterEventListener(MaintenanceAwareClusterComponent component) {
        this.clusterComponentListeners.add(component);
    }

    private void notifyRebindCompleted() {
        this.componentListeners.forEach(MaintenanceAwareComponent::onRebindCompleted);
    }

    /**
     * Called whenever a re-bind has been initiated by the remote server
     * <p>
     * A specific endpoint is going to move to another node within <time> seconds
     * </p>
     * 
     * @param endpoint address of the target endpoint
     * @param time estimated time for the re-bind to complete
     */
    private void notifyRebindStarted(Duration time, SocketAddress endpoint) {
        this.componentListeners.forEach(e -> e.onRebindStarted(time, endpoint));
    }

    private void notifyMigrateStarted(String shards) {
        this.componentListeners.forEach(component -> component.onMigrateStarted(shards));
    }

    private void notifyMigrateCompleted(String shards) {
        this.componentListeners.forEach(component -> component.onMigrateCompleted(shards));
    }

    private void notifyFailoverStarted(String shards) {
        this.componentListeners.forEach(component -> component.onFailoverStarted(shards));
    }

    private void notifyFailoverCompleted(String shards) {
        this.componentListeners.forEach(component -> component.onFailoverCompleted(shards));
    }

    private void notifySlotMigrateStarted(String slots) {
        this.clusterComponentListeners.forEach(component -> component.onSlotMigrateStarted(slots));
    }

    private void notifySlotMigrateCompleted(String slots) {
        this.clusterComponentListeners.forEach(component -> component.onSlotMigrateCompleted(slots));
    }

    /**
     * A supplier that is aware of re-bind events and can provide the appropriate address based on the current state.
     * <p>
     * During a re-bind, the supplier will return the rebind address for a certain period of time. After that period, it will
     * return the original address.
     * </p>
     */
    static class RebindAwareAddressSupplier {

        private static final class State {

            // Cutoff time for the current rebind
            // If the current time is before the cutoff time, the rebind address should be returned
            final Instant cutoff;

            // Address to which the connection should be re-bound
            // If null, the original address should be returned
            final SocketAddress rebindAddress;

            State(Instant cutoff, SocketAddress rebindAddress) {
                this.cutoff = cutoff;
                this.rebindAddress = rebindAddress;
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();

                return sb.append("State [cutoff=").append(cutoff).append(", rebindAddress=").append(rebindAddress).append("]")
                        .toString();
            }

        }

        private final AtomicReference<State> state = new AtomicReference<>();

        private final Clock clock;

        public RebindAwareAddressSupplier() {
            this(Clock.systemUTC());
        }

        public RebindAwareAddressSupplier(Clock clock) {
            this.clock = clock;
        }

        /**
         * Set a new rebind address for the specified duration.
         *
         * @param duration the duration for which the rebind address should be used
         * @param rebindAddress the address to which the connection should be re-bound
         */
        public void rebind(Duration duration, SocketAddress rebindAddress) {
            Instant newCutoff = clock.instant().plus(duration);
            state.set(new State(newCutoff, rebindAddress));
        }

        /**
         * Wrap the original supplier with a rebind-aware supplier.
         *
         * <p>
         * The returned supplier will return the rebind address if a rebind is in progress and the current time is before the
         * cutoff time set by the last call to {@link #rebind(Duration, SocketAddress)}. Otherwise, it will return the original
         * address.
         * </p>
         *
         * @param original the original supplier
         * @return a new supplier that is aware of re-bind events
         */
        public Mono<SocketAddress> wrappedSupplier(Mono<SocketAddress> original) {
            return Mono.defer(() -> {
                State current = state.get();
                logger.debug("RebindAwareAddressSupplier rebind state: {}", state.get());
                if (current != null && current.rebindAddress != null && clock.instant().isBefore(current.cutoff)) {
                    logger.debug("RebindAwareAddressSupplier using rebind address: {}", state.get());
                    return Mono.just(current.rebindAddress)
                            .doOnSubscribe(s -> logger.debug("RebindAwareAddressSupplier subscribed to rebind address"))
                            .doOnNext(address -> logger.debug("RebindAwareAddressSupplier rebind address: {}", address));
                } else {
                    logger.debug("RebindAwareAddressSupplier falling back to original.");
                    state.compareAndSet(current, null);
                    return original.doOnSubscribe(s -> logger.debug("RebindAwareAddressSupplier original to rebind address"))
                            .doOnNext(address -> logger.debug("RebindAwareAddressSupplier original address: {}", address));
                }
            });
        }

    }

}
