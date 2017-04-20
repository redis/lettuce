/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.protocol;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisChannelInitializer;
import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceSets;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author Mark Paluch
 */
class ReconnectionHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReconnectionHandler.class);

    private static final Set<Class<?>> EXECUTION_EXCEPTION_TYPES = LettuceSets.unmodifiableSet(TimeoutException.class,
            CancellationException.class, RedisCommandTimeoutException.class, ConnectException.class);

    private final Supplier<SocketAddress> socketAddressSupplier;
    private final Bootstrap bootstrap;
    private final ClientOptions clientOptions;
    private final Timer timer;
    private final ExecutorService reconnectWorkers;

    private TimeUnit timeoutUnit = TimeUnit.SECONDS;
    private long timeout = 60;

    private volatile ChannelFuture currentFuture;
    private volatile boolean reconnectSuspended;

    ReconnectionHandler(ClientOptions clientOptions, Bootstrap bootstrap, Supplier<SocketAddress> socketAddressSupplier,
            Timer timer, ExecutorService reconnectWorkers) {

        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");
        LettuceAssert.notNull(bootstrap, "Bootstrap must not be null");
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(timer, "Timer must not be null");
        LettuceAssert.notNull(reconnectWorkers, "ExecutorService must not be null");

        this.socketAddressSupplier = socketAddressSupplier;
        this.bootstrap = bootstrap;
        this.clientOptions = clientOptions;
        this.timer = timer;
        this.reconnectWorkers = reconnectWorkers;
    }

    /**
     * Initiate reconnect and return a {@link ChannelFuture} for synchronization. The resulting future either succeeds or fails.
     * It can be {@link ChannelFuture#cancel(boolean) canceled} to interrupt reconnection and channel initialization. A failed
     * {@link ChannelFuture} will close the channel.
     *
     * @return reconnect {@link ChannelFuture}.
     */
    protected ChannelFuture reconnect() {

        SocketAddress remoteAddress = socketAddressSupplier.get();

        logger.debug("Reconnecting to Redis at {}", remoteAddress);

        ChannelFuture connectFuture = bootstrap.connect(remoteAddress);
        ChannelPromise initFuture = connectFuture.channel().newPromise();

        initFuture.addListener((ChannelFuture it) -> {

            if (it.cause() != null) {

                connectFuture.cancel(true);
                close(it.channel());
            }
        });

        connectFuture.addListener((ChannelFuture it) -> {

            if (it.cause() != null) {

                initFuture.tryFailure(it.cause());
                return;
            }

            ChannelPipeline pipeline = it.channel().pipeline();

            RedisChannelInitializer channelInitializer = pipeline.get(RedisChannelInitializer.class);
            CommandHandler<?, ?> commandHandler = pipeline.get(CommandHandler.class);

            if (channelInitializer == null) {

                initFuture.tryFailure(new IllegalStateException(
                        "Reconnection attempt without a RedisChannelInitializer in the channel pipeline"));
                return;
            }

            if (commandHandler == null) {

                initFuture.tryFailure(new IllegalStateException(
                        "Reconnection attempt without a CommandHandler in the channel pipeline"));
                return;
            }

            channelInitializer.channelInitialized().whenComplete(
                    (state, throwable) -> {

                        if (throwable != null) {

                            if (isExecutionException(throwable)) {
                                initFuture.tryFailure(throwable);
                                return;
                            }

                            if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                                commandHandler.reset();
                            }

                            if (clientOptions.isSuspendReconnectOnProtocolFailure()) {

                                logger.error("Disabling autoReconnect due to initialization failure", throwable);
                                setReconnectSuspended(true);
                            }

                            initFuture.tryFailure(throwable);

                            return;
                        }

                        if (logger.isDebugEnabled()) {
                            logger.info("Reconnected to {}, Channel {}", remoteAddress,
                                    ChannelLogDescriptor.logDescriptor(it.channel()));
                        } else {
                            logger.info("Reconnected to {}", remoteAddress);
                        }

                        initFuture.trySuccess();
                    });
        });

        Runnable timeoutAction = () -> {
            initFuture.tryFailure(new TimeoutException(String.format("Reconnection attempt exceeded timeout of %d %s ",
                    timeout, timeoutUnit)));
        };

        Timeout timeoutHandle = timer.newTimeout(it -> {

            if (connectFuture.isDone() && initFuture.isDone()) {
                return;
            }

            if (reconnectWorkers.isShutdown()) {
                timeoutAction.run();
                return;
            }

            reconnectWorkers.submit(timeoutAction);

        }, this.timeout, timeoutUnit);

        initFuture.addListener(it -> timeoutHandle.cancel());

        return this.currentFuture = initFuture;
    }

    private void close(Channel channel) {
        if (channel != null) {
            channel.close();
        }
    }

    public boolean isReconnectSuspended() {
        return reconnectSuspended;
    }

    public void setReconnectSuspended(boolean reconnectSuspended) {
        this.reconnectSuspended = reconnectSuspended;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    public void setTimeoutUnit(TimeUnit timeoutUnit) {
        this.timeoutUnit = timeoutUnit;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void prepareClose() {

        ChannelFuture currentFuture = this.currentFuture;
        if (currentFuture != null && !currentFuture.isDone()) {
            currentFuture.cancel(true);
        }
    }

    /**
     * @param throwable
     * @return {@literal true} if {@code throwable} is an execution {@link Exception}.
     */
    public static boolean isExecutionException(Throwable throwable) {

        for (Class<?> type : EXECUTION_EXCEPTION_TYPES) {
            if (type.isAssignableFrom(throwable.getClass())) {
                return true;
            }
        }

        return false;
    }

    ClientOptions getClientOptions() {
        return clientOptions;
    }
}
