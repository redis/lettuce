/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

import com.lambdaworks.redis.Transports.NativeTransports;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.ConcurrentSet;
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
 * @since 3.0
 */
public abstract class AbstractRedisClient {

    protected static final PooledByteBufAllocator BUF_ALLOCATOR = PooledByteBufAllocator.DEFAULT;
    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisClient.class);

    /**
     * @deprecated use map eventLoopGroups instead.
     */
    @Deprecated
    protected EventLoopGroup eventLoopGroup;
    protected EventExecutorGroup genericWorkerPool;

    protected final Map<Class<? extends EventLoopGroup>, EventLoopGroup> eventLoopGroups = new ConcurrentHashMap<>(2);
    protected final HashedWheelTimer timer;
    protected final ChannelGroup channels;
    protected final ClientResources clientResources;
    protected long timeout = 60;
    protected TimeUnit unit;
    protected ConnectionEvents connectionEvents = new ConnectionEvents();
    protected Set<Closeable> closeableResources = new ConcurrentSet<>();

    protected volatile ClientOptions clientOptions = ClientOptions.builder().build();

    private final boolean sharedResources;
    private final AtomicBoolean shutdown = new AtomicBoolean();

    /**
     * @deprecated use {@link #AbstractRedisClient(ClientResources)}
     */
    @Deprecated
    protected AbstractRedisClient() {
        this(null);
    }

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

        unit = TimeUnit.SECONDS;

        genericWorkerPool = this.clientResources.eventExecutorGroup();
        channels = new DefaultChannelGroup(genericWorkerPool.next());
        timer = (HashedWheelTimer) this.clientResources.timer();
    }

    /**
     * Set the default timeout for {@link com.lambdaworks.redis.RedisConnection connections} created by this client. The timeout
     * applies to connection attempts and non-blocking commands.
     *
     * @param timeout Default connection timeout.
     * @param unit Unit of time for the timeout.
     */
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    @SuppressWarnings("unchecked")
    protected <K, V, T extends RedisChannelHandler<K, V>> T connectAsyncImpl(final CommandHandler<K, V> handler,
            final T connection, final Supplier<SocketAddress> socketAddressSupplier) {

        ConnectionBuilder connectionBuilder = ConnectionBuilder.connectionBuilder();
        connectionBuilder.clientOptions(clientOptions);
        connectionBuilder.clientResources(clientResources);
        connectionBuilder(handler, connection, socketAddressSupplier, connectionBuilder, null);
        channelType(connectionBuilder, null);
        return (T) initializeChannel(connectionBuilder);
    }

    /**
     * Populate connection builder with necessary resources.
     *
     * @param handler instance of a CommandHandler for writing redis commands
     * @param connection implementation of a RedisConnection
     * @param socketAddressSupplier address supplier for initial connect and re-connect
     * @param connectionBuilder connection builder to configure the connection
     * @param redisURI URI of the redis instance
     */
    protected void connectionBuilder(CommandHandler<?, ?> handler, RedisChannelHandler<?, ?> connection,
            Supplier<SocketAddress> socketAddressSupplier, ConnectionBuilder connectionBuilder, RedisURI redisURI) {

        Bootstrap redisBootstrap = new Bootstrap();
        redisBootstrap.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        redisBootstrap.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
        redisBootstrap.option(ChannelOption.ALLOCATOR, BUF_ALLOCATOR);

        SocketOptions socketOptions = getOptions().getSocketOptions();

        redisBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                (int) socketOptions.getConnectTimeoutUnit().toMillis(socketOptions.getConnectTimeout()));
        redisBootstrap.option(ChannelOption.SO_KEEPALIVE, socketOptions.isKeepAlive());
        redisBootstrap.option(ChannelOption.TCP_NODELAY, socketOptions.isTcpNoDelay());

        if (redisURI == null) {
            connectionBuilder.timeout(timeout, unit);
        } else {
            connectionBuilder.timeout(redisURI.getTimeout(), redisURI.getUnit());
            connectionBuilder.password(redisURI.getPassword());
        }

        connectionBuilder.bootstrap(redisBootstrap);
        connectionBuilder.channelGroup(channels).connectionEvents(connectionEvents).timer(timer);
        connectionBuilder.commandHandler(handler).socketAddressSupplier(socketAddressSupplier).connection(connection);
        connectionBuilder.workerPool(genericWorkerPool);
    }

    protected void channelType(ConnectionBuilder connectionBuilder, ConnectionPoint connectionPoint) {

        LettuceAssert.notNull(connectionPoint, "ConnectionPoint must not be null");

        connectionBuilder.bootstrap().group(getEventLoopGroup(connectionPoint));

        if (connectionPoint.getSocket() != null) {
            checkForEpollLibrary();
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

            checkForEpollLibrary();

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
            checkForEpollLibrary();
            return eventLoopGroups.get(NativeTransports.eventLoopGroupClass());
        }

        throw new IllegalStateException("This should not have happened in a binary decision. Please file a bug.");
    }

    private void checkForEpollLibrary() {
        EpollProvider.checkForEpollLibrary();
    }

    /**
     * Initialize the connection from {@link ConnectionBuilder}.
     *
     * @param connectionBuilder must not be {@literal null}.
     * @return the {@link CompletableFuture} to synchronize the connection process.
     */
    @SuppressWarnings("unchecked")
    protected <K, V, T extends RedisChannelHandler<K, V>> T initializeChannel(ConnectionBuilder connectionBuilder) {
        return getConnection(initializeChannelAsync(connectionBuilder));
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

        String msg = String.format("Unable to connect to %s", connectionFuture.getRemoteAddress());

        try {
            return connectionFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisConnectionException(msg, e);
        } catch (Exception e) {

            if (e instanceof ExecutionException) {
                throw new RedisConnectionException(msg, e.getCause());
            }

            throw new RedisConnectionException(msg, e);
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

        SocketAddress redisAddress = connectionBuilder.socketAddress();

        if (clientResources.eventExecutorGroup().isShuttingDown()) {
            throw new IllegalStateException("Cannot connect, Event executor group is terminated.");
        }

        logger.debug("Connecting to Redis at {}", redisAddress);

        CompletableFuture<Channel> channelReadyFuture = new CompletableFuture<>();
        Bootstrap redisBootstrap = connectionBuilder.bootstrap();

        RedisChannelInitializer initializer = connectionBuilder.build();
        redisBootstrap.handler(initializer);
        ChannelFuture connectFuture = redisBootstrap.connect(redisAddress);

        connectFuture.addListener(future -> {

            if (!future.isSuccess()) {

                logger.debug("Connecting to Redis at {}: {}", redisAddress, future.cause());
                connectionBuilder.commandHandler().initialState();
                channelReadyFuture.completeExceptionally(future.cause());
                return;
            }

            CompletableFuture<Boolean> initFuture = (CompletableFuture<Boolean>) initializer.channelInitialized();
            initFuture.whenComplete((success, throwable) -> {

                if (throwable == null) {
                    logger.debug("Connecting to Redis at {}: Success", redisAddress);
                    RedisChannelHandler<?, ?> connection = connectionBuilder.connection();
                    connection.registerCloseables(closeableResources, connection);
                    channelReadyFuture.complete(connectFuture.channel());
                    return;
                }

                logger.debug("Connecting to Redis at {}, initialization: {}", redisAddress, throwable);
                connectionBuilder.commandHandler().initialState();
                Throwable failure;

                if (throwable instanceof RedisConnectionException) {
                    failure = throwable;
                } else if (throwable instanceof TimeoutException) {
                    failure = new RedisConnectionException("Could not initialize channel within "
                            + connectionBuilder.getTimeout() + " " + connectionBuilder.getTimeUnit(), throwable);
                } else {
                    failure = throwable;
                }
                channelReadyFuture.completeExceptionally(failure);

                CompletableFuture<Boolean> response = new CompletableFuture<>();
                response.completeExceptionally(failure);

            });
        });

        return new ConnectionFuture<>(redisAddress,
                channelReadyFuture.thenApply(channel -> (T) connectionBuilder.connection()));
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown. The shutdown
     * has 2 secs quiet time and a timeout of 15 secs.
     */
    public void shutdown() {
        shutdown(2, 15, TimeUnit.SECONDS);
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

        if (shutdown.compareAndSet(false, true)) {

            while (!closeableResources.isEmpty()) {
                Closeable closeableResource = closeableResources.iterator().next();
                try {
                    closeableResource.close();
                } catch (Exception e) {
                    logger.debug("Exception on Close: " + e.getMessage(), e);
                }
                closeableResources.remove(closeableResource);
            }

            List<Future<?>> closeFutures = new ArrayList<>();

            for (Channel c : channels) {
                ChannelPipeline pipeline = c.pipeline();

                CommandHandler<?, ?> commandHandler = pipeline.get(CommandHandler.class);
                if (commandHandler != null && !commandHandler.isClosed()) {
                    commandHandler.close();
                }

                PubSubCommandHandler<?, ?> psCommandHandler = pipeline.get(PubSubCommandHandler.class);
                if (psCommandHandler != null && !psCommandHandler.isClosed()) {
                    psCommandHandler.close();
                }
            }

            try {
                closeFutures.add(channels.close());
            } catch (Exception e) {
                logger.debug("Cannot close channels", e);
            }

            if (!sharedResources) {
                clientResources.shutdown(quietPeriod, timeout, timeUnit);
            } else {
                for (EventLoopGroup eventExecutors : eventLoopGroups.values()) {
                    Future<?> groupCloseFuture = clientResources.eventLoopGroupProvider().release(eventExecutors, quietPeriod,
                            timeout, timeUnit);
                    closeFutures.add(groupCloseFuture);
                }
            }

            for (Future<?> future : closeFutures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RedisException(e);
                }
            }
        }
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

    /**
     * lettuce-specific connection {@link CompletableFuture future}. Delegates calls to the decorated {@link CompletableFuture}
     * and provides a {@link SocketAddress}.
     *
     * @since 4.4
     */
    protected class ConnectionFuture<T> extends CompletableFuture<T> {

        private final SocketAddress remoteAddress;
        private final CompletableFuture<T> delegate;

        protected ConnectionFuture(SocketAddress remoteAddress, CompletableFuture<T> delegate) {

            this.remoteAddress = remoteAddress;
            this.delegate = delegate;
        }

        public SocketAddress getRemoteAddress() {
            return remoteAddress;
        }

        private <U> ConnectionFuture<U> adopt(CompletableFuture<U> newFuture) {
            return new ConnectionFuture<>(remoteAddress, newFuture);
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }

        @Override
        public T join() {
            return delegate.join();
        }

        @Override
        public T getNow(T valueIfAbsent) {
            return delegate.getNow(valueIfAbsent);
        }

        @Override
        public boolean complete(T value) {
            return delegate.complete(value);
        }

        @Override
        public boolean completeExceptionally(Throwable ex) {
            return delegate.completeExceptionally(ex);
        }

        @Override
        public <U> ConnectionFuture<U> thenApply(Function<? super T, ? extends U> fn) {
            return adopt(delegate.thenApply(fn));
        }

        @Override
        public <U> ConnectionFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
            return adopt(delegate.thenApplyAsync(fn));
        }

        @Override
        public <U> ConnectionFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
            return adopt(delegate.thenApplyAsync(fn, executor));
        }

        @Override
        public ConnectionFuture<Void> thenAccept(Consumer<? super T> action) {
            return adopt(delegate.thenAccept(action));
        }

        @Override
        public ConnectionFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
            return adopt(delegate.thenAcceptAsync(action));
        }

        @Override
        public ConnectionFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
            return adopt(delegate.thenAcceptAsync(action, executor));
        }

        @Override
        public ConnectionFuture<Void> thenRun(Runnable action) {
            return adopt(delegate.thenRun(action));
        }

        @Override
        public ConnectionFuture<Void> thenRunAsync(Runnable action) {
            return adopt(delegate.thenRunAsync(action));
        }

        @Override
        public ConnectionFuture<Void> thenRunAsync(Runnable action, Executor executor) {
            return adopt(delegate.thenRunAsync(action, executor));
        }

        @Override
        public <U, V> ConnectionFuture<V> thenCombine(CompletionStage<? extends U> other,
                BiFunction<? super T, ? super U, ? extends V> fn) {
            return adopt(delegate.thenCombine(other, fn));
        }

        @Override
        public <U, V> ConnectionFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
                BiFunction<? super T, ? super U, ? extends V> fn) {
            return adopt(delegate.thenCombineAsync(other, fn));
        }

        @Override
        public <U, V> ConnectionFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
                BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
            return adopt(delegate.thenCombineAsync(other, fn, executor));
        }

        @Override
        public <U> ConnectionFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                BiConsumer<? super T, ? super U> action) {
            return adopt(delegate.thenAcceptBoth(other, action));
        }

        @Override
        public <U> ConnectionFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                BiConsumer<? super T, ? super U> action) {
            return adopt(delegate.thenAcceptBothAsync(other, action));
        }

        @Override
        public <U> ConnectionFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                BiConsumer<? super T, ? super U> action, Executor executor) {
            return adopt(delegate.thenAcceptBothAsync(other, action, executor));
        }

        @Override
        public ConnectionFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
            return adopt(delegate.runAfterBoth(other, action));
        }

        @Override
        public ConnectionFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
            return adopt(delegate.runAfterBothAsync(other, action));
        }

        @Override
        public ConnectionFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
            return adopt(delegate.runAfterBothAsync(other, action, executor));
        }

        @Override
        public <U> ConnectionFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
            return adopt(delegate.applyToEither(other, fn));
        }

        @Override
        public <U> ConnectionFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
            return adopt(delegate.applyToEitherAsync(other, fn));
        }

        @Override
        public <U> ConnectionFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
                Executor executor) {
            return adopt(delegate.applyToEitherAsync(other, fn, executor));
        }

        @Override
        public ConnectionFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
            return adopt(delegate.acceptEither(other, action));
        }

        @Override
        public ConnectionFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
            return adopt(delegate.acceptEitherAsync(other, action));
        }

        @Override
        public ConnectionFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
                Executor executor) {
            return adopt(delegate.acceptEitherAsync(other, action, executor));
        }

        @Override
        public ConnectionFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
            return adopt(delegate.runAfterEither(other, action));
        }

        @Override
        public ConnectionFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
            return adopt(delegate.runAfterEitherAsync(other, action));
        }

        @Override
        public ConnectionFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
            return adopt(delegate.runAfterEitherAsync(other, action, executor));
        }

        @Override
        public <U> ConnectionFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
            return adopt(delegate.thenCompose(fn));
        }

        @Override
        public <U> ConnectionFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
            return adopt(delegate.thenComposeAsync(fn));
        }

        @Override
        public <U> ConnectionFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
                Executor executor) {
            return adopt(delegate.thenComposeAsync(fn, executor));
        }

        @Override
        public ConnectionFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
            return adopt(delegate.whenComplete(action));
        }

        @Override
        public ConnectionFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
            return adopt(delegate.whenCompleteAsync(action));
        }

        @Override
        public ConnectionFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
            return adopt(delegate.whenCompleteAsync(action, executor));
        }

        @Override
        public <U> ConnectionFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
            return adopt(delegate.handle(fn));
        }

        @Override
        public <U> ConnectionFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
            return adopt(delegate.handleAsync(fn));
        }

        @Override
        public <U> ConnectionFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
            return adopt(delegate.handleAsync(fn, executor));
        }

        @Override
        public CompletableFuture<T> toCompletableFuture() {
            return delegate.toCompletableFuture();
        }

        @Override
        public ConnectionFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
            return adopt(delegate.exceptionally(fn));
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isCompletedExceptionally() {
            return delegate.isCompletedExceptionally();
        }

        @Override
        public void obtrudeValue(T value) {
            delegate.obtrudeValue(value);
        }

        @Override
        public void obtrudeException(Throwable ex) {
            delegate.obtrudeException(ex);
        }

        @Override
        public int getNumberOfDependents() {
            return delegate.getNumberOfDependents();
        }
    }
}
