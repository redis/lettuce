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
import io.lettuce.core.protocol.MaintenanceNotification.Moving;
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

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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

    private Channel channel;

    // the MOVING notification of the re-bind in progress; accessed on the event loop only
    private Moving currentRebind;

    private final List<MaintenanceNotificationListener> listeners = new CopyOnWriteArrayList<>();

    private RebindAwareAddressSupplier rebindAwareAddressSupplier;

    public MaintenanceAwareConnectionWatchdog(Delay reconnectDelay, ClientOptions clientOptions, Bootstrap bootstrap,
            Timer timer, EventExecutorGroup reconnectWorkers, Supplier<CompletionStage<SocketAddress>> socketAddressSupplier,
            ReconnectionListener reconnectionListener, EventBus eventBus, Endpoint endpoint) {

        super(reconnectDelay, clientOptions, bootstrap, timer, reconnectWorkers, socketAddressSupplier, reconnectionListener,
                eventBus, endpoint);
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
    protected Supplier<CompletionStage<SocketAddress>> wrapSocketAddressSupplier(
            Supplier<CompletionStage<SocketAddress>> socketAddressSupplier) {
        Supplier<CompletionStage<SocketAddress>> source = super.wrapSocketAddressSupplier(socketAddressSupplier);
        rebindAwareAddressSupplier = new RebindAwareAddressSupplier();
        return rebindAwareAddressSupplier.wrappedSupplier(source);
    }

    @Override
    public void onPushMessage(PushMessage message) {

        MaintenanceNotification notification = MaintenanceNotifications.from(message);
        if (notification == null) {
            return;
        }

        logger.debug("[{}] Received {}", ChannelLogDescriptor.logDescriptor(channel), notification.getType());

        if (notification.getType() == MaintenanceNotification.Type.MOVING) {
            Moving moving = (Moving) notification;
            if (null == moving.getEndpoint()) {
                logger.debug("[channel={}] Deferred Rebind requested. Rebinding to current endpoint after '{}'", channel.id(),
                        moving.getTime());
                channel.eventLoop().schedule(() -> rebind(moving), moving.getTime().toMillis() / 2, TimeUnit.MILLISECONDS);
            } else {
                rebind(moving);
            }
        }

        listeners.forEach(listener -> listener.onMaintenanceNotification(notification));
    }

    private void rebind(Moving movingEvent) {
        logger.debug("[{}] Rebind to '{}'", ChannelLogDescriptor.logDescriptor(channel), movingEvent.getEndpoint());
        channel.attr(REBIND_ATTRIBUTE).set(RebindState.STARTED);
        rebindAwareAddressSupplier.rebind(movingEvent.getTime(), movingEvent.getEndpoint());
        currentRebind = movingEvent;

        // Relax the timeouts for the whole re-bind window, not only while commands are still in flight. While the
        // re-bind is in progress the endpoint reports itself as disconnected (see DefaultEndpoint#isConnected), so
        // every command issued after the MOVING notification is buffered until the connection to the new endpoint has
        // been established. Those commands need the relaxed timeout just as much as the ones already on the wire.
        listeners.forEach(listener -> listener.onRebindStarted(movingEvent));

        ChannelPipeline pipeline = channel.pipeline();
        CommandHandler commandHandler = pipeline.get(CommandHandler.class);
        if (commandHandler.getStack().isEmpty()) {
            logger.debug("[{}] Closing channel as part of rebind", ChannelLogDescriptor.logDescriptor(channel));
            channel.close().awaitUninterruptibly();
            channel.attr(REBIND_ATTRIBUTE).set(RebindState.COMPLETED);

            // The channel is closed already, so channelReadComplete() is not going to fire for it again and cannot
            // report the completion. Report it here, which lifts the relaxed timeouts after the grace period.
            notifyRebindCompleted();
        }

        // Otherwise CommandHandler#decode flips the state to COMPLETED once the stack has been drained and
        // channelReadComplete() closes the channel and reports the completion of the re-bind.
    }

    private void notifyRebindCompleted() {

        Moving moving = currentRebind;
        currentRebind = null;

        listeners.forEach(listener -> listener.onRebindCompleted(moving));
    }

    /**
     * Register a listener for the maintenance notifications and re-bind steps of this connection.
     *
     * @param listener the listener to register
     * @since 8.0
     */
    public void addMaintenanceNotificationListener(MaintenanceNotificationListener listener) {
        this.listeners.add(listener);
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
        public Supplier<CompletionStage<SocketAddress>> wrappedSupplier(Supplier<CompletionStage<SocketAddress>> original) {
            return () -> {
                State current = state.get();
                logger.debug("RebindAwareAddressSupplier rebind state: {}", current);
                if (current != null && current.rebindAddress != null && clock.instant().isBefore(current.cutoff)) {
                    logger.debug("RebindAwareAddressSupplier using rebind address: {}", current);
                    logger.debug("RebindAwareAddressSupplier rebind address: {}", current.rebindAddress);
                    return CompletableFuture.completedFuture(current.rebindAddress);
                } else {
                    logger.debug("RebindAwareAddressSupplier falling back to original.");
                    state.compareAndSet(current, null);
                    return original.get().thenApply(address -> {
                        logger.debug("RebindAwareAddressSupplier original address: {}", address);
                        return address;
                    });
                }
            };
        }

    }

}
