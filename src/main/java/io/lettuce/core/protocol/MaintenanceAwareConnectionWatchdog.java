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
import io.lettuce.core.codec.StringCodec;
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
import java.nio.ByteBuffer;
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
 * @see ClientOptions#getMaintenanceEventsOptions()
 */
@ChannelHandler.Sharable
public class MaintenanceAwareConnectionWatchdog extends ConnectionWatchdog implements PushListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MaintenanceAwareConnectionWatchdog.class);

    private static final String REBIND_MESSAGE_TYPE = "MOVING";

    private static final String MIGRATING_MESSAGE_TYPE = "MIGRATING";

    private static final String MIGRATED_MESSAGE_TYPE = "MIGRATED";

    private static final String FAILING_OVER_MESSAGE_TYPE = "FAILING_OVER";

    private static final String FAILED_OVER_MESSAGE_TYPE = "FAILED_OVER";

    public static final AttributeKey<RebindState> REBIND_ATTRIBUTE = AttributeKey.newInstance("rebindAddress");

    private static final int MIGRATING_SHARDS_INDEX = 3;

    private static final int MIGRATED_SHARDS_INDEX = 2;

    private static final int FAILING_OVER_SHARDS_INDEX = 3;

    private static final int FAILED_OVER_SHARDS_INDEX = 2;

    private Channel channel;

    private final Set<MaintenanceAwareComponent> componentListeners = new HashSet<>();

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
            logger.debug("Disconnecting at {}", LocalTime.now());
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
        String mType = message.getType();

        if (REBIND_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Rebind requested");
            final MovingEvent movingEvent = MovingEvent.from(message);
            if (movingEvent != null) {
                if (null == movingEvent.getEndpoint()) {
                    logger.debug("Deferred Rebind requested. Rebinding to current endpoint after '{}'", movingEvent.getTime());
                    channel.eventLoop().schedule(() -> rebind(movingEvent), movingEvent.getTime().toMillis() / 2,
                            TimeUnit.MILLISECONDS);
                } else {
                    rebind(movingEvent);
                }
            }
        } else if (MIGRATING_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Shard migration started");
            notifyMigrateStarted(getMigratingShards(message));
        } else if (MIGRATED_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Shard migration completed");
            notifyMigrateCompleted(getMigratedShards(message));
        } else if (FAILING_OVER_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Failover started");
            notifyFailoverStarted(getFailingOverShards(message));
        } else if (FAILED_OVER_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Failover completed");
            notifyFailoverCompleted(getFailedOverShards(message));
        }
    }

    private void rebind(MovingEvent movingEvent) {
        logger.debug("Attempting to rebind to new endpoint '{}'", movingEvent.getEndpoint());
        channel.attr(REBIND_ATTRIBUTE).set(RebindState.STARTED);
        rebindAwareAddressSupplier.rebind(movingEvent.getTime(), movingEvent.getEndpoint());

        ChannelPipeline pipeline = channel.pipeline();
        CommandHandler commandHandler = pipeline.get(CommandHandler.class);
        if (commandHandler.getStack().isEmpty()) {
            channel.close().awaitUninterruptibly();
            channel.attr(REBIND_ATTRIBUTE).set(RebindState.COMPLETED);
        } else {
            notifyRebindStarted(movingEvent.getTime(), movingEvent.getEndpoint());
        }
    }

    private String getMigratingShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 4))
            return null;

        return getShards(content, MIGRATING_SHARDS_INDEX, MIGRATING_MESSAGE_TYPE);
    }

    private String getMigratedShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 3))
            return null;

        return getShards(content, MIGRATED_SHARDS_INDEX, MIGRATED_MESSAGE_TYPE);
    }

    private String getFailingOverShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 4))
            return null;

        return getShards(content, FAILING_OVER_SHARDS_INDEX, FAILING_OVER_MESSAGE_TYPE);
    }

    private String getFailedOverShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 3))
            return null;

        return getShards(content, FAILED_OVER_SHARDS_INDEX, FAILED_OVER_MESSAGE_TYPE);
    }

    private static boolean isInvalidMaintenanceEvent(List<Object> content, int expectedSize) {
        if (content.size() < expectedSize) {
            logger.warn("Invalid maintenance message format, expected at least {} elements, got {}", expectedSize,
                    content.size());
            return true;
        }

        return false;
    }

    private static String getShards(List<Object> content, int shardsIndex, String maintenanceEvent) {
        Object shardsObject = content.get(shardsIndex);

        if (!(shardsObject instanceof ByteBuffer)) {
            logger.warn("Invalid shards format, expected ByteBuffer, got {} for {} maintenance event",
                    shardsObject != null ? shardsObject.getClass() : "null", maintenanceEvent);
            return null;
        }

        return StringCodec.UTF8.decodeKey((ByteBuffer) shardsObject);
    }

    static class MovingEvent {

        private static final int EVENT_ID_INDEX = 1;

        private static final int TIME_INDEX = 2;

        private static final int ADDRESS_INDEX = 3;

        private final Long eventId;

        private final InetSocketAddress endpoint;

        private final Duration time;

        private MovingEvent(Long eventId, Duration time, InetSocketAddress endpoint) {
            this.eventId = eventId;
            this.endpoint = endpoint;
            this.time = time;
        }

        static MovingEvent from(PushMessage message) {
            if (!REBIND_MESSAGE_TYPE.equals(message.getType())) {
                return null;
            }

            List<Object> content = message.getContent();

            if (content.size() != 4) {
                logger.warn("Invalid re-bind message format, expected 4 elements, got {}", content.size());
                return null;
            }

            try {
                Long eventId = (Long) content.get(EVENT_ID_INDEX);
                Long timeInSec = (Long) content.get(TIME_INDEX);
                ByteBuffer addressBuffer = (ByteBuffer) content.get(ADDRESS_INDEX);

                InetSocketAddress endpoint = null;
                if (addressBuffer != null) {
                    String addressAndPort = StringCodec.UTF8.decodeKey(addressBuffer);

                    // Handle "none" option where endpoint is null
                    if (addressAndPort != null && !"null".equals(addressAndPort)) {
                        String[] parts = addressAndPort.split(":");
                        String address = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        endpoint = new InetSocketAddress(address, port);
                    }
                }

                return new MovingEvent(eventId, Duration.ofSeconds(timeInSec), endpoint);
            } catch (Exception e) {
                logger.error("Invalid re-bind message format", e);
                return null;
            }
        }

        public Long getEventId() {
            return eventId;
        }

        public InetSocketAddress getEndpoint() {
            return endpoint;
        }

        public Duration getTime() {
            return time;
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
                if (current != null && current.rebindAddress != null && clock.instant().isBefore(current.cutoff)) {
                    return Mono.just(current.rebindAddress);
                } else {
                    state.compareAndSet(current, null);
                    return original;
                }
            });
        }

    }

}
