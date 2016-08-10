// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.resource.Delay;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for monitoring the channel and reconnecting when the connection is lost.
 * 
 * @author Will Glozer
 * @author Mark Paluch
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

    private Channel channel;
    private SocketAddress remoteAddress;
    private long lastReconnectionLogging = -1;
    private String logPrefix;

    private volatile int attempts;
    private volatile boolean armed;
    private volatile boolean listenOnChannelInactive;
    private volatile Timeout reconnectScheduleTimeout;

    /**
     * Create a new watchdog that adds to new connections to the supplied {@link ChannelGroup} and establishes a new
     * {@link Channel} when disconnected, while reconnect is true. The socketAddressSupplier can supply the reconnect address.
     * 
     * @param reconnectDelay reconnect delay, must not be {@literal null}
     * @param clientOptions client options for the current connection, must not be {@literal null}
     * @param bootstrap Configuration for new channels, must not be {@literal null}
     * @param timer Timer used for delayed reconnect, must not be {@literal null}
     * @param reconnectWorkers executor group for reconnect tasks, must not be {@literal null}
     * @param socketAddressSupplier the socket address supplier to obtain an address for reconnection, may be {@literal null}
     * @param reconnectionListener the reconnection listener, must not be {@literal null}
     * @param connectionFacade the connection facade, must not be {@literal null}
     */
    public ConnectionWatchdog(Delay reconnectDelay, ClientOptions clientOptions, Bootstrap bootstrap, Timer timer,
            EventExecutorGroup reconnectWorkers, Supplier<SocketAddress> socketAddressSupplier,
            ReconnectionListener reconnectionListener, ConnectionFacade connectionFacade) {

        LettuceAssert.notNull(reconnectDelay, "Delay must not be null");
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(bootstrap, "Bootstrap must not be null");
        LettuceAssert.notNull(timer, "Timer must not be null");
        LettuceAssert.notNull(reconnectWorkers, "ReconnectWorkers must not be null");
        LettuceAssert.notNull(reconnectionListener, "ReconnectionListener must not be null");
        LettuceAssert.notNull(connectionFacade, "ConnectionFacade must not be null");

        this.reconnectDelay = reconnectDelay;
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.reconnectWorkers = reconnectWorkers;
        this.reconnectionListener = reconnectionListener;

        Supplier<SocketAddress> wrappedSocketAddressSupplier = new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {

                if (socketAddressSupplier != null) {
                    try {
                        remoteAddress = socketAddressSupplier.get();
                    } catch (RuntimeException e) {
                        logger.warn("Cannot retrieve the current address from socketAddressSupplier: " + e.toString()
                                + ", reusing old address " + remoteAddress);
                    }
                }

                return remoteAddress;
            }
        };

        this.reconnectionHandler = new ReconnectionHandler(clientOptions, bootstrap, wrappedSocketAddressSupplier,
                connectionFacade);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        logger.debug("{} userEventTriggered(ctx, {})", logPrefix(), evt);

        if (evt instanceof ConnectionEvents.Activated) {
            attempts = 0;
        }

        super.userEventTriggered(ctx, evt);
    }

    void prepareClose() {

        setListenOnChannelInactive(false);
        setReconnectSuspended(true);

        reconnectionHandler.prepareClose();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        channel = ctx.channel();
        reconnectScheduleTimeout = null;
        remoteAddress = channel.remoteAddress();
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
    public synchronized void scheduleReconnect() {

        logger.debug("{} scheduleReconnect()", logPrefix());

        if (!isEventLoopGroupActive()) {
            logger.debug("isEventLoopGroupActive() == false");
            return;
        }

        if (!isListenOnChannelInactive()) {
            logger.debug("Skip reconnect scheduling, listener disabled");
            return;
        }

        if ((channel == null || !channel.isActive()) && reconnectScheduleTimeout == null) {
            attempts++;

            final int attempt = attempts;
            int timeout = (int) reconnectDelay.getTimeUnit().toMillis(reconnectDelay.createDelay(attempt));
            logger.debug("{} Reconnect attempt {}, delay {}ms", logPrefix(), attempt, timeout);
            this.reconnectScheduleTimeout = timer.newTimeout(new TimerTask() {
                @Override
                public void run(final Timeout timeout) throws Exception {

                    if (!isEventLoopGroupActive()) {
                        logger.debug("isEventLoopGroupActive() == false");
                        return;
                    }

                    reconnectWorkers.submit(() -> {
                        ConnectionWatchdog.this.run(attempt);
                        return null;
                    });
                }
            }, timeout, TimeUnit.MILLISECONDS);
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

        reconnectScheduleTimeout = null;

        if (!isEventLoopGroupActive()) {
            logger.debug("isEventLoopGroupActive() == false");
            return;
        }

        if (!isListenOnChannelInactive()) {
            logger.debug("Skip reconnect scheduling, listener disabled");
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

        try {
            reconnectionListener.onReconnect(new ConnectionEvents.Reconnect(attempt));
            reconnect(infoLevel, warnLevel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            logger.log(warnLevel, "Cannot connect: {}", e.toString());
            if (!isReconnectSuspended()) {
                scheduleReconnect();
            }
        }
    }

    protected void reconnect(InternalLogLevel infoLevel, InternalLogLevel warnLevel) throws Exception {

        logger.log(infoLevel, "Reconnecting, last destination was {}", remoteAddress);
        reconnectionHandler.reconnect(infoLevel);
    }

    private boolean isEventLoopGroupActive() {

        if (!isEventLoopGroupActive(bootstrap.group()) || !isEventLoopGroupActive(reconnectWorkers)) {
            return false;
        }

        return true;
    }

    private boolean isEventLoopGroupActive(EventExecutorGroup executorService) {
        return !(executorService.isShutdown() || executorService.isTerminated() || executorService.isShuttingDown());
    }

    private boolean shouldLog() {

        long quietUntil = lastReconnectionLogging + LOGGING_QUIET_TIME_MS;
        return quietUntil <= System.currentTimeMillis();
    }

    /**
     * Enable event listener for disconnected events.
     *
     * @param listenOnChannelInactive {@literal true} to listen for disconnected events.
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
     * @param reconnectSuspended {@literal true} to suspend reconnection
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

    private String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        StringBuilder buffer = new StringBuilder(64);
        buffer.append('[').append("Last known addr=").append(remoteAddress).append(", ")
                .append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return logPrefix = buffer.toString();
    }

}
