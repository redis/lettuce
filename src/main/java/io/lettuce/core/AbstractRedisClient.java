/*
 * Copyright 2011-2019 the original author or authors.
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
package io.lettuce.core;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.Closeable;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Mono;
import io.lettuce.core.Transports.NativeTransports;
import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Base Redis client. This class holds the netty infrastructure, {@link ClientOptions} and the basic connection procedure. This
 * class creates the netty {@link EventLoopGroup}s for NIO ({@link NioEventLoopGroup}) and EPoll (
 * {@link io.netty.channel.epoll.EpollEventLoopGroup}) with a default of {@code Runtime.getRuntime().availableProcessors() * 4}
 * threads. Reuse the instance as much as possible since the {@link EventLoopGroup} instances are expensive and can consume a
 * huge part of your resources, if you create multiple instances.
 * <p>
 * You can set the number of threads per {@link NioEventLoopGroup} by setting the {@code io.netty.eventLoopThreads} system
 * property to a reasonable number of threads.
 * </p>
 *
 * @author Mark Paluch
 * @author Jongyeol Choi
 * @author Poorva Gokhale
 * @since 3.0
 * @see ClientResources
 */
public abstract class AbstractRedisClient {

    protected static final PooledByteBufAllocator BUF_ALLOCATOR = PooledByteBufAllocator.DEFAULT;
    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisClient.class);

    protected final Map<Class<? extends EventLoopGroup>, EventLoopGroup> eventLoopGroups = new ConcurrentHashMap<>(2);
    protected final ConnectionEvents connectionEvents = new ConnectionEvents();
    protected final Set<Closeable> closeableResources = ConcurrentHashMap.newKeySet();
    protected final EventExecutorGroup genericWorkerPool;
    protected final HashedWheelTimer timer;
    protected final ChannelGroup channels;
    protected final ClientResources clientResources;

    protected volatile ClientOptions clientOptions = ClientOptions.builder().build();

    protected Duration timeout = RedisURI.DEFAULT_TIMEOUT_DURATION;

    private final boolean sharedResources;
    private final AtomicBoolean shutdown = new AtomicBoolean();

    /**
     * Create a new instance with client resources.
     *
     * @param clientResources the client resources. If {@literal null}, the client will create a new dedicated instance of
     *        client resources and keep track of them.
     */
    protected AbstractRedisClient(ClientResources clientResources) {

        if (clientResources == null) {
            sharedResources = false;
            this.clientResources = DefaultClientResources.create();
        } else {
            sharedResources = true;
            this.clientResources = clientResources;
        }

        genericWorkerPool = this.clientResources.eventExecutorGroup();
        channels = new DefaultChannelGroup(genericWorkerPool.next());
        timer = (HashedWheelTimer) this.clientResources.timer();
    }

    /**
     * Set the default timeout for connections created by this client. The timeout applies to connection attempts and
     * non-blocking commands.
     *
     * @param timeout default connection timeout, must not be {@literal null}.
     * @since 5.0
     */
    public void setDefaultTimeout(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout duration must not be null");
        LettuceAssert.isTrue(!timeout.isNegative(), "Timeout duration must be greater or equal to zero");

        this.timeout = timeout;
    }

    /**
     * Set the default timeout for connections created by this client. The timeout applies to connection attempts and
     * non-blocking commands.
     *
     * @param timeout Default connection timeout.
     * @param unit Unit of time for the timeout.
     * @deprecated since 5.0, use {@link #setDefaultTimeout(Duration)}.
     */
    @Deprecated
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        setDefaultTimeout(Duration.ofNanos(unit.toNanos(timeout)));
    }

    /**
     * Populate connection builder with necessary resources.
     *
     * @param socketAddressSupplier address supplier for initial connect and re-connect
     * @param connectionBuilder connection builder to configure the connection
     * @param redisURI URI of the Redis instance
     */
    protected void connectionBuilder(Mono<SocketAddress> socketAddressSupplier, ConnectionBuilder connectionBuilder,
            RedisURI redisURI) {

        Bootstrap redisBootstrap = new Bootstrap();
        redisBootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        redisBootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
        redisBootstrap.option(ChannelOption.ALLOCATOR, BUF_ALLOCATOR);

        ClientOptions clientOptions = getOptions();
        SocketOptions socketOptions = clientOptions.getSocketOptions();

        redisBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                Math.toIntExact(socketOptions.getConnectTimeout().toMillis()));

        if (LettuceStrings.isEmpty(redisURI.getSocket())) {
            redisBootstrap.option(ChannelOption.SO_KEEPALIVE, socketOptions.isKeepAlive());
            redisBootstrap.option(ChannelOption.TCP_NODELAY, socketOptions.isTcpNoDelay());
        }

        connectionBuilder.timeout(redisURI.getTimeout());
        connectionBuilder.clientName(redisURI.getClientName());

        if (clientOptions.getProtocolVersion() == ProtocolVersion.RESP2) {

            if (hasPassword(redisURI)) {
                connectionBuilder.auth(redisURI.getPassword());
                connectionBuilder.handshakeAuthResp2();
            }
        } else if (clientOptions.getProtocolVersion() == ProtocolVersion.RESP3) {

            if (hasPassword(redisURI)) {
                if (LettuceStrings.isNotEmpty(redisURI.getUsername())) {
                    connectionBuilder.auth(redisURI.getUsername(), redisURI.getPassword());
                } else {
                    connectionBuilder.auth("default", redisURI.getPassword());
                }
                connectionBuilder.handshakeAuthResp3();

            } else {
                connectionBuilder.handshakeResp3();
            }
        }

        connectionBuilder.bootstrap(redisBootstrap);
        connectionBuilder.channelGroup(channels).connectionEvents(connectionEvents).timer(timer);
        connectionBuilder.socketAddressSupplier(socketAddressSupplier);
    }

    private boolean hasPassword(RedisURI connectionSettings) {
        return connectionSettings.getPassword() != null && connectionSettings.getPassword().length != 0;
    }

    protected void channelType(ConnectionBuilder connectionBuilder, ConnectionPoint connectionPoint) {

        LettuceAssert.notNull(connectionPoint, "ConnectionPoint must not be null");

        connectionBuilder.bootstrap().group(getEventLoopGroup(connectionPoint));

        if (connectionPoint.getSocket() != null) {
            NativeTransports.assertAvailable();
            connectionBuilder.bootstrap().channel(NativeTransports.domainSocketChannelClass());
        } else {
            connectionBuilder.bootstrap().channel(Transports.socketChannelClass());
        }
    }

    private synchronized EventLoopGroup getEventLoopGroup(ConnectionPoint connectionPoint) {

        if (connectionPoint.getSocket() == null && !eventLoopGroups.containsKey(Transports.eventLoopGroupClass())) {
            eventLoopGroups.put(Transports.eventLoopGroupClass(),
                    clientResources.eventLoopGroupProvider().allocate(Transports.eventLoopGroupClass()));
        }

        if (connectionPoint.getSocket() != null) {

            NativeTransports.assertAvailable();

            Class<? extends EventLoopGroup> eventLoopGroupClass = NativeTransports.eventLoopGroupClass();

            if (!eventLoopGroups.containsKey(NativeTransports.eventLoopGroupClass())) {
                eventLoopGroups.put(eventLoopGroupClass,
                        clientResources.eventLoopGroupProvider().allocate(eventLoopGroupClass));
            }
        }

        if (connectionPoint.getSocket() == null) {
            return eventLoopGroups.get(Transports.eventLoopGroupClass());
        }

        if (connectionPoint.getSocket() != null) {
            NativeTransports.assertAvailable();
            return eventLoopGroups.get(NativeTransports.eventLoopGroupClass());
        }

        throw new IllegalStateException("This should not have happened in a binary decision. Please file a bug.");
    }

    /**
     * Retrieve the connection from {@link ConnectionFuture}. Performs a blocking {@link ConnectionFuture#get()} to synchronize
     * the channel/connection initialization. Any exception is rethrown as {@link RedisConnectionException}.
     *
     * @param connectionFuture must not be null.
     * @param <T> Connection type.
     * @return the connection.
     * @throws RedisConnectionException in case of connection failures.
     * @since 4.4
     */
    protected <T> T getConnection(ConnectionFuture<T> connectionFuture) {

        try {
            return connectionFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(connectionFuture.getRemoteAddress(), e);
        } catch (Exception e) {

            if (e instanceof ExecutionException) {
                throw RedisConnectionException.create(connectionFuture.getRemoteAddress(), e.getCause());
            }

            throw RedisConnectionException.create(connectionFuture.getRemoteAddress(), e);
        }
    }

    /**
     * Retrieve the connection from {@link ConnectionFuture}. Performs a blocking {@link ConnectionFuture#get()} to synchronize
     * the channel/connection initialization. Any exception is rethrown as {@link RedisConnectionException}.
     *
     * @param connectionFuture must not be null.
     * @param <T> Connection type.
     * @return the connection.
     * @throws RedisConnectionException in case of connection failures.
     * @since 5.0
     */
    protected <T> T getConnection(CompletableFuture<T> connectionFuture) {

        try {
            return connectionFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(e);
        } catch (Exception e) {

            if (e instanceof ExecutionException) {
                throw RedisConnectionException.create(e.getCause());
            }

            throw RedisConnectionException.create(e);
        }
    }

    /**
     * Connect and initialize a channel from {@link ConnectionBuilder}.
     *
     * @param connectionBuilder must not be {@literal null}.
     * @return the {@link ConnectionFuture} to synchronize the connection process.
     * @since 4.4
     */
    @SuppressWarnings("unchecked")
    protected <K, V, T extends RedisChannelHandler<K, V>> ConnectionFuture<T> initializeChannelAsync(
            ConnectionBuilder connectionBuilder) {

        Mono<SocketAddress> socketAddressSupplier = connectionBuilder.socketAddress();

        if (clientResources.eventExecutorGroup().isShuttingDown()) {
            throw new IllegalStateException("Cannot connect, Event executor group is terminated.");
        }

        CompletableFuture<SocketAddress> socketAddressFuture = new CompletableFuture<>();
        CompletableFuture<Channel> channelReadyFuture = new CompletableFuture<>();

        socketAddressSupplier.doOnError(socketAddressFuture::completeExceptionally).doOnNext(socketAddressFuture::complete)
                .subscribe(redisAddress -> {

                    if (channelReadyFuture.isCancelled()) {
                        return;
                    }
                    initializeChannelAsync0(connectionBuilder, channelReadyFuture, redisAddress);
                }, channelReadyFuture::completeExceptionally);

        return new DefaultConnectionFuture<>(socketAddressFuture,
                channelReadyFuture.thenApply(channel -> (T) connectionBuilder.connection()));
    }

    private void initializeChannelAsync0(ConnectionBuilder connectionBuilder, CompletableFuture<Channel> channelReadyFuture,
            SocketAddress redisAddress) {

        logger.debug("Connecting to Redis at {}", redisAddress);

        Bootstrap redisBootstrap = connectionBuilder.bootstrap();

        RedisChannelInitializer initializer = connectionBuilder.build();
        redisBootstrap.handler(initializer);

        clientResources.nettyCustomizer().afterBootstrapInitialized(redisBootstrap);
        CompletableFuture<Boolean> initFuture = initializer.channelInitialized();
        ChannelFuture connectFuture = redisBootstrap.connect(redisAddress);

        channelReadyFuture.whenComplete((c, t) -> {

            if (t instanceof CancellationException) {
                connectFuture.cancel(true);
                initFuture.cancel(true);
            }
        });

        connectFuture.addListener(future -> {

            if (!future.isSuccess()) {

                logger.debug("Connecting to Redis at {}: {}", redisAddress, future.cause());
                connectionBuilder.endpoint().initialState();
                channelReadyFuture.completeExceptionally(future.cause());
                return;
            }

            initFuture.whenComplete((success, throwable) -> {

                if (throwable == null) {

                    logger.debug("Connecting to Redis at {}: Success", redisAddress);
                    RedisChannelHandler<?, ?> connection = connectionBuilder.connection();
                    connection.registerCloseables(closeableResources, connection);
                    channelReadyFuture.complete(connectFuture.channel());
                    return;
                }

                logger.debug("Connecting to Redis at {}, initialization: {}", redisAddress, throwable);
                connectionBuilder.endpoint().initialState();
                Throwable failure;

                if (throwable instanceof RedisConnectionException) {
                    failure = throwable;
                } else if (throwable instanceof TimeoutException) {
                    failure = new RedisConnectionException(
                            "Could not initialize channel within " + connectionBuilder.getTimeout(), throwable);
                } else {
                    failure = throwable;
                }
                channelReadyFuture.completeExceptionally(failure);
            });
        });
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown. The shutdown
     * has no quiet time and a timeout of 2 seconds.
     */
    public void shutdown() {
        shutdown(0, 2, TimeUnit.SECONDS);
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout the maximum amount of time to wait until the executor is shutdown regardless if a task was submitted
     *        during the quiet period
     * @since 5.0
     */
    public void shutdown(Duration quietPeriod, Duration timeout) {
        shutdown(quietPeriod.toNanos(), timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout the maximum amount of time to wait until the executor is shutdown regardless if a task was submitted
     *        during the quiet period
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}
     */
    public void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {


        try {
            shutdownAsync(quietPeriod, timeout, timeUnit).get();
        } catch (RuntimeException e) {
            throw e;
        } catch (ExecutionException e) {

            if (e.getCause() instanceof RedisCommandExecutionException) {
                throw ExceptionFactory.createExecutionException(e.getCause().getMessage(), e.getCause());
            }

            throw new RedisException(e.getCause());
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } catch (Exception e) {
            throw ExceptionFactory.createExecutionException(null, e);
        }
    }

    /**
     * Shutdown this client and close all open connections asynchronously. The client should be discarded after calling
     * shutdown. The shutdown has 2 secs quiet time and a timeout of 15 secs.
     *
     * @since 4.4
     */
    public CompletableFuture<Void> shutdownAsync() {
        return shutdownAsync(2, 15, TimeUnit.SECONDS);
    }

    /**
     * Shutdown this client and close all open connections asynchronously. The client should be discarded after calling
     * shutdown.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout the maximum amount of time to wait until the executor is shutdown regardless if a task was submitted
     *        during the quiet period
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}
     * @since 4.4
     */
    @SuppressWarnings("rawtypes")
    public CompletableFuture<Void> shutdownAsync(long quietPeriod, long timeout, TimeUnit timeUnit) {

        if (shutdown.compareAndSet(false, true)) {

            logger.debug("Initiate shutdown ({}, {}, {})", quietPeriod, timeout, timeUnit);
            return closeResources().thenCompose((value) -> closeClientResources(quietPeriod, timeout, timeUnit));
        }

        return completedFuture(null);
    }

    private CompletableFuture<Void> closeResources() {

        List<CompletionStage<Void>> closeFutures = new ArrayList<>();

        while (!closeableResources.isEmpty()) {
            Closeable closeableResource = closeableResources.iterator().next();

            if (closeableResource instanceof AsyncCloseable) {

                closeFutures.add(((AsyncCloseable) closeableResource).closeAsync());
            } else {
                try {
                    closeableResource.close();
                } catch (Exception e) {
                    logger.debug("Exception on Close: " + e.getMessage(), e);
                }
            }
            closeableResources.remove(closeableResource);
        }

        for (Channel c : channels) {

            ChannelPipeline pipeline = c.pipeline();

            ConnectionWatchdog commandHandler = pipeline.get(ConnectionWatchdog.class);
            if (commandHandler != null) {
                commandHandler.setListenOnChannelInactive(false);
            }
        }

        try {
            closeFutures.add(Futures.toCompletionStage(channels.close()));
        } catch (Exception e) {
            logger.debug("Cannot close channels", e);
        }

        return Futures.allOf(closeFutures);
    }

    private CompletableFuture<Void> closeClientResources(long quietPeriod, long timeout, TimeUnit timeUnit) {
        List<CompletionStage<?>> groupCloseFutures = new ArrayList<>();
        if (!sharedResources) {
            Future<?> groupCloseFuture = clientResources.shutdown(quietPeriod, timeout, timeUnit);
            groupCloseFutures.add(Futures.toCompletionStage(groupCloseFuture));
        } else {
            for (EventLoopGroup eventExecutors : eventLoopGroups.values()) {
                Future<?> groupCloseFuture = clientResources.eventLoopGroupProvider().release(eventExecutors, quietPeriod,
                        timeout, timeUnit);
                groupCloseFutures.add(Futures.toCompletionStage(groupCloseFuture));
            }
        }
        return Futures.allOf(groupCloseFutures);
    }

    protected int getResourceCount() {
        return closeableResources.size();
    }

    protected int getChannelCount() {
        return channels.size();
    }

    /**
     * Add a listener for the RedisConnectionState. The listener is notified every time a connect/disconnect/IO exception
     * happens. The listeners are not bound to a specific connection, so every time a connection event happens on any
     * connection, the listener will be notified. The corresponding netty channel handler (async connection) is passed on the
     * event.
     *
     * @param listener must not be {@literal null}
     */
    public void addListener(RedisConnectionStateListener listener) {
        LettuceAssert.notNull(listener, "RedisConnectionStateListener must not be null");
        connectionEvents.addListener(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener must not be {@literal null}
     */
    public void removeListener(RedisConnectionStateListener listener) {

        LettuceAssert.notNull(listener, "RedisConnectionStateListener must not be null");
        connectionEvents.removeListener(listener);
    }

    /**
     * Returns the {@link ClientOptions} which are valid for that client. Connections inherit the current options at the moment
     * the connection is created. Changes to options will not affect existing connections.
     *
     * @return the {@link ClientOptions} for this client
     */
    public ClientOptions getOptions() {
        return clientOptions;
    }

    /**
     * Set the {@link ClientOptions} for the client.
     *
     * @param clientOptions client options for the client and connections that are created after setting the options
     */
    protected void setOptions(ClientOptions clientOptions) {
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        this.clientOptions = clientOptions;
    }
}
