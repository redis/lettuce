package com.lambdaworks.redis.protocol;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisChannelInitializer;
import com.lambdaworks.redis.internal.LettuceAssert;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author Mark Paluch
 */
class ReconnectionHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReconnectionHandler.class);

    private final Supplier<SocketAddress> socketAddressSupplier;
    private final ClientOptions clientOptions;
    private final Bootstrap bootstrap;
    private final ConnectionFacade connectionFacade;

    private TimeUnit timeoutUnit = TimeUnit.SECONDS;
    private long timeout = 60;

    private volatile ChannelFuture currentFuture;
    private volatile boolean reconnectSuspended;

    ReconnectionHandler(ClientOptions clientOptions, Bootstrap bootstrap, Supplier<SocketAddress> socketAddressSupplier,
            ConnectionFacade connectionFacade) {

        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(bootstrap, "Bootstrap must not be null");
        LettuceAssert.notNull(connectionFacade, "ConnectionFacade must not be null");

        this.socketAddressSupplier = socketAddressSupplier;
        this.bootstrap = bootstrap;
        this.clientOptions = clientOptions;
        this.connectionFacade = connectionFacade;
    }

    protected boolean reconnect(InternalLogLevel infoLevel) throws Exception {

        SocketAddress remoteAddress = socketAddressSupplier.get();

        try {
            long timeLeft = timeoutUnit.toNanos(timeout);
            long start = System.nanoTime();

            logger.debug("Reconnecting to Redis at {}", remoteAddress);
            currentFuture = bootstrap.connect(remoteAddress);
            if (!currentFuture.await(timeLeft, TimeUnit.NANOSECONDS)) {
                if (currentFuture.isCancellable()) {
                    currentFuture.cancel(true);
                }

                throw new TimeoutException("Reconnection attempt exceeded timeout of " + timeout + " " + timeoutUnit);
            }

            currentFuture.sync();

            Channel channel = currentFuture.channel();

            RedisChannelInitializer channelInitializer = channel.pipeline().get(RedisChannelInitializer.class);

            if (channelInitializer == null) {
                logger.warn("Reconnection attempt without a RedisChannelInitializer in the channel pipeline");
                close(channel);
                return false;
            }

            try {
                timeLeft -= System.nanoTime() - start;
                channelInitializer.channelInitialized().get(Math.max(0, timeLeft), TimeUnit.NANOSECONDS);
                if (logger.isDebugEnabled()) {
                    logger.log(infoLevel, "Reconnected to {}, Channel {}", remoteAddress,
                            ChannelLogDescriptor.logDescriptor(channel));
                } else {
                    logger.log(infoLevel, "Reconnected to {}", remoteAddress);
                }
                return true;
            } catch (TimeoutException e) {
                channelInitializer.channelInitialized().cancel(true);
            } catch (Exception e) {
                if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                    connectionFacade.reset();
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

        return false;
    }

    private void close(Channel channel) {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    boolean isReconnectSuspended() {
        return reconnectSuspended;
    }

    void setReconnectSuspended(boolean reconnectSuspended) {
        this.reconnectSuspended = reconnectSuspended;
    }

    long getTimeout() {
        return timeout;
    }

    void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    void prepareClose() {

        if (currentFuture != null && !currentFuture.isDone()) {
            currentFuture.cancel(true);
        }
    }

    ClientOptions getClientOptions() {
        return clientOptions;
    }
}
