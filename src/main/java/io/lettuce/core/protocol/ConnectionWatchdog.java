/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.resource.Delay.StatefulDelay;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for monitoring the channel and reconnecting when the connection is lost.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Koji Lin
 */
@ChannelHandler.Sharable
public class ConnectionWatchdog extends ChannelInboundHandlerAdapter {

    private static final long LOGGING_QUIET_TIME_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionWatchdog.class);

    private final Delay reconnectDelay;

    private final Bootstrap bootstrap;

    private final EventExecutorGroup reconnectWorkers;

    private final ReconnectionHandler reconnectionHandler;

    private final ReconnectionListener reconnectionListener;

    private final Timer timer;

    private final EventBus eventBus;

    private Channel channel;

    private SocketAddress remoteAddress;

    private long lastReconnectionLogging = -1;

    private String logPrefix;

    private final AtomicBoolean reconnectSchedulerSync;

    private volatile int attempts;

    private volatile boolean armed;

    private volatile boolean listenOnChannelInactive;

    private volatile Timeout reconnectScheduleTimeout;

    /**
     * Create a new watchdog that adds to new connections to the supplied {@link ChannelGroup} and establishes a new
     * {@link Channel} when disconnected, while reconnect is true. The socketAddressSupplier can supply the reconnect address.
     *
     * @param reconnectDelay reconnect delay, must not be {@code null}
     * @param clientOptions client options for the current connection, must not be {@code null}
     * @param bootstrap Configuration for new channels, must not be {@code null}
     * @param timer Timer used for delayed reconnect, must not be {@code null}
     * @param reconnectWorkers executor group for reconnect tasks, must not be {@code null}
     * @param socketAddressSupplier the socket address supplier to obtain an address for reconnection, may be {@code null}
     * @param reconnectionListener the reconnection listener, must not be {@code null}
     * @param connectionFacade the connection facade, must not be {@code null}
     * @param eventBus Event bus to emit reconnect events.
     */
    public ConnectionWatchdog(Delay reconnectDelay, ClientOptions clientOptions, Bootstrap bootstrap, Timer timer,
            EventExecutorGroup reconnectWorkers, Mono<SocketAddress> socketAddressSupplier,
            ReconnectionListener reconnectionListener, ConnectionFacade connectionFacade, EventBus eventBus) {

        LettuceAssert.notNull(reconnectDelay, "Delay must not be null");
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(bootstrap, "Bootstrap must not be null");
        LettuceAssert.notNull(timer, "Timer must not be null");
        LettuceAssert.notNull(reconnectWorkers, "ReconnectWorkers must not be null");
        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");
        LettuceAssert.notNull(reconnectionListener, "ReconnectionListener must not be null");
        LettuceAssert.notNull(connectionFacade, "ConnectionFacade must not be null");
        LettuceAssert.notNull(eventBus, "EventBus must not be null");

        this.reconnectDelay = reconnectDelay;
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.reconnectWorkers = reconnectWorkers;
        this.reconnectionListener = reconnectionListener;
        this.reconnectSchedulerSync = new AtomicBoolean(false);
        this.eventBus = eventBus;

        Mono<SocketAddress> wrappedSocketAddressSupplier = socketAddressSupplier.doOnNext(addr -> remoteAddress = addr)
                .onErrorResume(t -> {

                    if (logger.isDebugEnabled()) {
                        logger.warn("Cannot retrieve current address from socketAddressSupplier: " + t.toString()
                                + ", reusing cached address " + remoteAddress, t);
                    } else {
                        logger.warn("Cannot retrieve current address from socketAddressSupplier: " + t.toString()
                                + ", reusing cached address " + remoteAddress);
                    }

                    return Mono.just(remoteAddress);
                });

        this.reconnectionHandler = new ReconnectionHandler(clientOptions, bootstrap, wrappedSocketAddressSupplier, timer,
                reconnectWorkers, connectionFacade);

        resetReconnectDelay();
    }

    void prepareClose() {

        setListenOnChannelInactive(false);
        setReconnectSuspended(true);

        Timeout reconnectScheduleTimeout = this.reconnectScheduleTimeout;
        if (reconnectScheduleTimeout != null && !reconnectScheduleTimeout.isCancelled()) {
            reconnectScheduleTimeout.cancel();
        }

        reconnectionHandler.prepareClose();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        reconnectSchedulerSync.set(false);
        channel = ctx.channel();
        reconnectScheduleTimeout = null;
        logPrefix = null;
        remoteAddress = channel.remoteAddress();
        attempts = 0;
        resetReconnectDelay();
        logPrefix = null;
        logger.debug("{} channelActive()", logPrefix());

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        logger.debug("{} channelInactive()", logPrefix());
        if (!armed) {
            logger.debug("{} ConnectionWatchdog not armed", logPrefix());
            return;
        }

        channel = null;

        if (listenOnChannelInactive && !reconnectionHandler.isReconnectSuspended()) {
            scheduleReconnect();
        } else {
            logger.debug("{} Reconnect scheduling disabled", logPrefix(), ctx);
        }

        super.channelInactive(ctx);
    }

    /**
     * Enable {@link ConnectionWatchdog} to listen for disconnected events.
     */
    void arm() {
        this.armed = true;
        setListenOnChannelInactive(true);
    }

    /**
     * Schedule reconnect if channel is not available/not active.
     */
    public void scheduleReconnect() {

        logger.debug("{} scheduleReconnect()", logPrefix());

        if (!isEventLoopGroupActive()) {
            logger.debug("isEventLoopGroupActive() == false");
            return;
        }

        if (!isListenOnChannelInactive()) {
            logger.debug("Skip reconnect scheduling, listener disabled");
            return;
        }

        if ((channel == null || !channel.isActive()) && reconnectSchedulerSync.compareAndSet(false, true)) {

            attempts++;
            final int attempt = attempts;
            int timeout = (int) reconnectDelay.createDelay(attempt).toMillis();
            logger.debug("{} Reconnect attempt {}, delay {}ms", logPrefix(), attempt, timeout);

            this.reconnectScheduleTimeout = timer.newTimeout(it -> {

                reconnectScheduleTimeout = null;

                if (!isEventLoopGroupActive()) {
                    logger.warn("Cannot execute scheduled reconnect timer, reconnect workers are terminated");
                    return;
                }

                reconnectWorkers.submit(() -> {
                    ConnectionWatchdog.this.run(attempt);
                    return null;
                });
            }, timeout, TimeUnit.MILLISECONDS);

            // Set back to null when ConnectionWatchdog#run runs earlier than reconnectScheduleTimeout's assignment.
            if (!reconnectSchedulerSync.get()) {
                reconnectScheduleTimeout = null;
            }
        } else {
            logger.debug("{} Skipping scheduleReconnect() because I have an active channel", logPrefix());
        }
    }

    /**
     * Reconnect to the remote address that the closed channel was connected to. This creates a new {@link ChannelPipeline} with
     * the same handler instances contained in the old channel's pipeline.
     *
     * @param attempt attempt counter
     *
     * @throws Exception when reconnection fails.
     */
    public void run(int attempt) throws Exception {

        reconnectSchedulerSync.set(false);
        reconnectScheduleTimeout = null;

        if (!isEventLoopGroupActive()) {
            logger.debug("isEventLoopGroupActive() == false");
            return;
        }

        if (!isListenOnChannelInactive()) {
            logger.debug("Skip reconnect scheduling, listener disabled");
            return;
        }

        if (isReconnectSuspended()) {
            logger.debug("Skip reconnect scheduling, reconnect is suspended");
            return;
        }

        boolean shouldLog = shouldLog();

        InternalLogLevel infoLevel = InternalLogLevel.INFO;
        InternalLogLevel warnLevel = InternalLogLevel.WARN;

        if (shouldLog) {
            lastReconnectionLogging = System.currentTimeMillis();
        } else {
            warnLevel = InternalLogLevel.DEBUG;
            infoLevel = InternalLogLevel.DEBUG;
        }

        InternalLogLevel warnLevelToUse = warnLevel;

        try {
            reconnectionListener.onReconnectAttempt(new ConnectionEvents.Reconnect(attempt));
            logger.log(infoLevel, "Reconnecting, last destination was {}", remoteAddress);

            Tuple2<CompletableFuture<Channel>, CompletableFuture<SocketAddress>> tuple = reconnectionHandler.reconnect();
            CompletableFuture<Channel> future = tuple.getT1();

            future.whenComplete((c, t) -> {

                if (c != null && t == null) {
                    return;
                }

                CompletableFuture<SocketAddress> remoteAddressFuture = tuple.getT2();
                SocketAddress remote = remoteAddress;
                if (remoteAddressFuture.isDone() && !remoteAddressFuture.isCompletedExceptionally()
                        && !remoteAddressFuture.isCancelled()) {
                    remote = remoteAddressFuture.join();
                }

                String message = String.format("Cannot reconnect to [%s]: %s", remote,
                        t.getMessage() != null ? t.getMessage() : t.toString());

                if (ReconnectionHandler.isExecutionException(t)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(message, t);
                    } else {
                        logger.log(warnLevelToUse, message);
                    }
                } else {
                    logger.log(warnLevelToUse, message, t);
                }

                eventBus.publish(new ReconnectFailedEvent(LocalAddress.ANY, remote, t, attempt));

                if (!isReconnectSuspended()) {
                    scheduleReconnect();
                }
            });
        } catch (Exception e) {
            logger.log(warnLevel, "Cannot reconnect: {}", e.toString());
            eventBus.publish(new ReconnectFailedEvent(LocalAddress.ANY, remoteAddress, e, attempt));
        }
    }

    private boolean isEventLoopGroupActive() {

        if (!isEventLoopGroupActive(bootstrap.group()) || !isEventLoopGroupActive(reconnectWorkers)) {
            return false;
        }

        return true;
    }

    private static boolean isEventLoopGroupActive(EventExecutorGroup executorService) {
        return !(executorService.isShuttingDown());
    }

    private boolean shouldLog() {

        long quietUntil = lastReconnectionLogging + LOGGING_QUIET_TIME_MS;
        return quietUntil <= System.currentTimeMillis();
    }

    /**
     * Enable event listener for disconnected events.
     *
     * @param listenOnChannelInactive {@code true} to listen for disconnected events.
     */
    public void setListenOnChannelInactive(boolean listenOnChannelInactive) {
        this.listenOnChannelInactive = listenOnChannelInactive;
    }

    public boolean isListenOnChannelInactive() {
        return listenOnChannelInactive;
    }

    /**
     * Suspend reconnection temporarily. Reconnect suspension will interrupt reconnection attempts.
     *
     * @param reconnectSuspended {@code true} to suspend reconnection
     */
    public void setReconnectSuspended(boolean reconnectSuspended) {
        reconnectionHandler.setReconnectSuspended(reconnectSuspended);
    }

    public boolean isReconnectSuspended() {
        return reconnectionHandler.isReconnectSuspended();
    }

    ReconnectionHandler getReconnectionHandler() {
        return reconnectionHandler;
    }

    private void resetReconnectDelay() {
        if (reconnectDelay instanceof StatefulDelay) {
            ((StatefulDelay) reconnectDelay).reset();
        }
    }

    private String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        String buffer = "[" + ChannelLogDescriptor.logDescriptor(channel) + ", last known addr=" + remoteAddress + ']';
        return logPrefix = buffer;
    }

}
