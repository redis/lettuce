/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.SslConnectionBuilder;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceSets;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.util.Timer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author Mark Paluch
 * @author Aashish Amrute
 */
class ReconnectionHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReconnectionHandler.class);

    private static final Set<Class<?>> EXECUTION_EXCEPTION_TYPES = LettuceSets.unmodifiableSet(TimeoutException.class,
            CancellationException.class, RedisCommandTimeoutException.class, ConnectException.class);

    private final ClientOptions clientOptions;

    private final Bootstrap bootstrap;

    protected Mono<SocketAddress> socketAddressSupplier;

    private final ConnectionFacade connectionFacade;

    private volatile CompletableFuture<Channel> currentFuture;

    private volatile boolean reconnectSuspended;

    ReconnectionHandler(ClientOptions clientOptions, Bootstrap bootstrap, Mono<SocketAddress> socketAddressSupplier,
            Timer timer, ExecutorService reconnectWorkers, ConnectionFacade connectionFacade) {

        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");
        LettuceAssert.notNull(bootstrap, "Bootstrap must not be null");
        LettuceAssert.notNull(timer, "Timer must not be null");
        LettuceAssert.notNull(reconnectWorkers, "ExecutorService must not be null");
        LettuceAssert.notNull(connectionFacade, "ConnectionFacade must not be null");

        this.socketAddressSupplier = socketAddressSupplier;
        this.bootstrap = bootstrap;
        this.clientOptions = clientOptions;
        this.connectionFacade = connectionFacade;
    }

    /**
     * Initiate reconnect and return a {@link ChannelFuture} for synchronization. The resulting future either succeeds or fails.
     * It can be {@link ChannelFuture#cancel(boolean) canceled} to interrupt reconnection and channel initialization. A failed
     * {@link ChannelFuture} will close the channel.
     *
     * @return reconnect {@link ChannelFuture}.
     */
    protected Tuple2<CompletableFuture<Channel>, CompletableFuture<SocketAddress>> reconnect() {

        CompletableFuture<Channel> future = new CompletableFuture<>();
        CompletableFuture<SocketAddress> address = new CompletableFuture<>();

        socketAddressSupplier.subscribe(remoteAddress -> {

            address.complete(remoteAddress);

            if (future.isCancelled()) {
                return;
            }

            reconnect0(future, remoteAddress);

        }, ex -> {
            if (!address.isDone()) {
                address.completeExceptionally(ex);
            }
            future.completeExceptionally(ex);
        });

        this.currentFuture = future;
        return Tuples.of(future, address);
    }

    /**
     * Replace the existing @link SocketAddressSupplier} with a new one.
     * <p>
     * This could be used in a scenario where a re-bind is happening, e.g., the old node is going down and a new node is
     * supposed to handle the requests to the same endpoint.
     *
     * @param socketAddressSupplier the new address of the endpoint
     */
    public void setSocketAddressSupplier(SocketAddress socketAddressSupplier) {
        this.socketAddressSupplier = Mono.just(socketAddressSupplier);
    }

    private void reconnect0(CompletableFuture<Channel> result, SocketAddress remoteAddress) {

        ChannelHandler handler = bootstrap.config().handler();

        // reinitialize SslChannelInitializer if Redis - SSL connection.
        if (SslConnectionBuilder.isSslChannelInitializer(handler)) {
            bootstrap.handler(SslConnectionBuilder.withSocketAddress(handler, remoteAddress));
        }

        ChannelFuture connectFuture = bootstrap.connect(remoteAddress);

        logger.debug("Reconnecting to Redis at {}", remoteAddress);

        result.whenComplete((c, t) -> {

            if (t instanceof CancellationException) {
                connectFuture.cancel(true);
            }
        });

        connectFuture.addListener(future -> {

            if (!future.isSuccess()) {
                result.completeExceptionally(future.cause());
                return;
            }

            RedisHandshakeHandler handshakeHandler = connectFuture.channel().pipeline().get(RedisHandshakeHandler.class);

            if (handshakeHandler == null) {
                result.completeExceptionally(new IllegalStateException("RedisHandshakeHandler not registered"));
                return;
            }

            handshakeHandler.channelInitialized().whenComplete((success, throwable) -> {

                if (throwable != null) {

                    if (isExecutionException(throwable)) {
                        result.completeExceptionally(throwable);
                        return;
                    }

                    if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                        connectionFacade.reset();
                    }

                    if (clientOptions.isSuspendReconnectOnProtocolFailure()) {

                        logger.error("Disabling autoReconnect due to initialization failure", throwable);
                        setReconnectSuspended(true);
                    }

                    result.completeExceptionally(throwable);
                    return;
                }

                if (logger.isDebugEnabled()) {
                    logger.info("Reconnected to {}, Channel {}", remoteAddress,
                            ChannelLogDescriptor.logDescriptor(connectFuture.channel()));
                } else {
                    logger.info("Reconnected to {}", remoteAddress);
                }

                result.complete(connectFuture.channel());
            });

        });
    }

    boolean isReconnectSuspended() {
        return reconnectSuspended;
    }

    void setReconnectSuspended(boolean reconnectSuspended) {
        this.reconnectSuspended = reconnectSuspended;
    }

    void prepareClose() {

        CompletableFuture<?> currentFuture = this.currentFuture;
        if (currentFuture != null && !currentFuture.isDone()) {
            currentFuture.cancel(true);
        }
    }

    /**
     * @param throwable
     * @return {@code true} if {@code throwable} is an execution {@link Exception}.
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
