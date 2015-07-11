// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Supplier;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
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
 */
@ChannelHandler.Sharable
public class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask {

    public static final long LOGGING_QUIET_TIME_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);
    public static final int RETRY_TIMEOUT_MAX = 14;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionWatchdog.class);

    private final EventExecutorGroup reconnectWorkers;
    private final ClientOptions clientOptions;
    private final Bootstrap bootstrap;
    private boolean listenOnChannelInactive;
    private boolean reconnectSuspended;

    private Channel channel;
    private final Timer timer;

    private final Supplier<SocketAddress> socketAddressSupplier;
    private SocketAddress remoteAddress;
    private int attempts;
    private long lastReconnectionLogging = -1;
    private String logPrefix;

    private TimeUnit timeoutUnit = TimeUnit.SECONDS;
    private long timeout = 60;

    private volatile ChannelFuture currentFuture;

    /**
     * Create a new watchdog that adds to new connections to the supplied {@link ChannelGroup} and establishes a new
     * {@link Channel} when disconnected, while reconnect is true.
     * 
     * @param clientOptions
     * @param bootstrap Configuration for new channels.
     * @param timer Timer used for delayed reconnect.
     */
    public ConnectionWatchdog(ClientOptions clientOptions, Bootstrap bootstrap, EventExecutorGroup reconnectWorkers, Timer timer) {
        this(clientOptions, bootstrap, timer, reconnectWorkers, null);
    }

    /**
     * Create a new watchdog that adds to new connections to the supplied {@link ChannelGroup} and establishes a new
     * {@link Channel} when disconnected, while reconnect is true. The socketAddressSupplier can supply the reconnect address.
     *
     * @param clientOptions
     * @param bootstrap Configuration for new channels.
     * @param timer Timer used for delayed reconnect.
     * @param socketAddressSupplier the socket address suplier for gaining an address to reconnect to
     */
    public ConnectionWatchdog(ClientOptions clientOptions, Bootstrap bootstrap, Timer timer,
            EventExecutorGroup reconnectWorkers, Supplier<SocketAddress> socketAddressSupplier) {
        this.clientOptions = clientOptions;
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.reconnectWorkers = reconnectWorkers;
        this.socketAddressSupplier = socketAddressSupplier;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        logger.debug("{} userEventTriggered({}, {})", logPrefix, ctx, evt);
        if (evt instanceof ConnectionEvents.PrepareClose) {
            if (currentFuture != null && !currentFuture.isDone()) {
                currentFuture.cancel(true);
            }
            ConnectionEvents.PrepareClose prepareClose = (ConnectionEvents.PrepareClose) evt;
            setListenOnChannelInactive(false);
            prepareClose.getPrepareCloseFuture().set(true);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        logger.debug("{} channelActive({})", logPrefix, ctx);
        channel = ctx.channel();
        attempts = 0;
        remoteAddress = channel.remoteAddress();

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        logger.debug("{} channelInactive({})", logPrefix, ctx);
        channel = null;
        if (listenOnChannelInactive && !reconnectSuspended) {
            RedisChannelHandler channelHandler = ctx.pipeline().get(RedisChannelHandler.class);
            if (channelHandler != null) {
                timeout = channelHandler.getTimeout();
                timeoutUnit = channelHandler.getTimeoutUnit();
            }

            scheduleReconnect();
        } else {
            logger.debug("{} Reconnect scheduling disabled", logPrefix(), ctx);
            logger.debug("");
        }
        super.channelInactive(ctx);
    }

    /**
     * Schedule reconnect if channel is not available/not active.
     */
    public void scheduleReconnect() {
        logger.debug("{} scheduleReconnect()", logPrefix);

        if (!isEventLoopGroupActive()) {
            logger.debug("isEventLoopGroupActive() == false");
            return;
        }

        if (channel == null || !channel.isActive()) {
            if (attempts < RETRY_TIMEOUT_MAX) {
                attempts++;
            }
            int timeout = 2 << attempts;
            timer.newTimeout(new TimerTask() {
                @Override
                public void run(final Timeout timeout) throws Exception {

                    if (!isEventLoopGroupActive()) {
                        logger.debug("isEventLoopGroupActive() == false");
                        return;
                    }

                    if (reconnectWorkers != null) {
                        ConnectionWatchdog.this.run(timeout);
                        return;
                    }

                    reconnectWorkers.submit(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            ConnectionWatchdog.this.run(timeout);
                            return null;
                        }
                    });
                }
            }, timeout, TimeUnit.MILLISECONDS);
        } else {
            logger.debug("{} Skipping scheduleReconnect() because I have an active channel", logPrefix);
        }
    }

    /**
     * Reconnect to the remote address that the closed channel was connected to. This creates a new {@link ChannelPipeline} with
     * the same handler instances contained in the old channel's pipeline.
     * 
     * @param timeout Timer task handle.
     * 
     * @throws Exception when reconnection fails.
     */
    @Override
    public void run(Timeout timeout) throws Exception {

        if (!isEventLoopGroupActive()) {
            logger.debug("isEventLoopGroupActive() == false");
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
            reconnect(infoLevel, warnLevel);
        } catch (InterruptedException e) {
            return;
        } catch (Exception e) {
            logger.log(warnLevel, "Cannot connect: {}", e.toString());
            scheduleReconnect();
        }
    }

    private void reconnect(InternalLogLevel infoLevel, InternalLogLevel warnLevel) throws Exception {

        logger.log(infoLevel, "Reconnecting, last destination was " + remoteAddress);

        if (socketAddressSupplier != null) {
            try {
                remoteAddress = socketAddressSupplier.get();
            } catch (RuntimeException e) {
                logger.log(warnLevel, "Cannot retrieve the current address from socketAddressSupplier: " + e.toString()
                        + ", reusing old address " + remoteAddress);
            }
        }

        try {
            long timeLeft = timeoutUnit.toNanos(timeout);
            long start = System.nanoTime();
            currentFuture = bootstrap.connect(remoteAddress);
            if (!currentFuture.await(timeLeft, TimeUnit.NANOSECONDS)) {
                if (currentFuture.isCancellable()) {
                    currentFuture.cancel(true);
                }

                throw new TimeoutException("Reconnection attempt exceeded timeout of " + timeout + " " + timeoutUnit);
            }
            currentFuture.sync();

            RedisChannelInitializer channelInitializer = currentFuture.channel().pipeline().get(RedisChannelInitializer.class);
            CommandHandler commandHandler = currentFuture.channel().pipeline().get(CommandHandler.class);

            try {
                timeLeft -= System.nanoTime() - start;
                channelInitializer.channelInitialized().get(Math.max(0, timeLeft), TimeUnit.NANOSECONDS);
                logger.log(infoLevel, "Reconnected to " + remoteAddress);
            } catch (Exception e) {
                if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                    commandHandler.reset();
                }

                if (clientOptions.isSuspendReconnectOnProtocolFailure()) {
                    logger.error("Cannot initialize channel. Disabling autoReconnect", e);
                    setReconnectSuspended(true);
                } else {
                    logger.error("Cannot initialize channel.", e);
                    throw e;
                }
            }
        } finally {
            currentFuture = null;
        }
    }

    private boolean isEventLoopGroupActive() {
        if (bootstrap.group().isShutdown() || bootstrap.group().isTerminated() || bootstrap.group().isShuttingDown()) {
            return false;
        }

        if (reconnectWorkers != null
                && (reconnectWorkers.isShutdown() || reconnectWorkers.isTerminated() || reconnectWorkers.isShuttingDown())) {
            return false;
        }

        return true;
    }

    private boolean shouldLog() {

        long quietUntil = lastReconnectionLogging + LOGGING_QUIET_TIME_MS;

        if (quietUntil > System.currentTimeMillis()) {
            return false;
        }

        return true;
    }

    /**
     * @deprecated use {@link #setListenOnChannelInactive(boolean)}
     * @param reconnect
     */
    @Deprecated
    public void setReconnect(boolean reconnect) {
        setListenOnChannelInactive(reconnect);
    }

    public void setListenOnChannelInactive(boolean listenOnChannelInactive) {
        this.listenOnChannelInactive = listenOnChannelInactive;
    }

    public boolean isListenOnChannelInactive() {
        return listenOnChannelInactive;
    }

    public boolean isReconnectSuspended() {
        return reconnectSuspended;
    }

    public void setReconnectSuspended(boolean reconnectSuspended) {

        this.reconnectSuspended = reconnectSuspended;
    }

    private String logPrefix() {
        if (logPrefix != null) {
            return logPrefix;
        }
        StringBuffer buffer = new StringBuffer(64);
        buffer.append('[').append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return logPrefix = buffer.toString();
    }

}
