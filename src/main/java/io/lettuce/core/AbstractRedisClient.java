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
package io.lettuce.core;

import java.io.Closeable;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.MaintNotificationsConfig.EndpointTypeSource;
import io.lettuce.core.api.BaseRedisClient;
import reactor.core.publisher.Mono;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.event.connection.ConnectEvent;
import io.lettuce.core.event.connection.ConnectionCreatedEvent;
import io.lettuce.core.event.jfr.EventRecorder;
import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.protocol.RedisHandshakeHandler;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Transports;
import io.lettuce.core.resource.Transports.NativeTransports;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Base Redis client. This class holds the netty infrastructure, {@link ClientOptions} and the basic connection procedure. This
 * class creates different Netty {@link EventLoopGroup}s depending on the {@link NativeTransports} used.
 * <p>
 * You can set the number of threads by using the {@link ClientResources} configuration. For more details, check the
 * documentation of the {@link DefaultClientResources} class.
 * </p>
 *
 * @author Mark Paluch
 * @author Jongyeol Choi
 * @author Poorva Gokhale
 * @author Tihomir Mateev
 * @since 3.0
 * @see ClientResources
 * @see DefaultClientResources
 */
public abstract class AbstractRedisClient implements BaseRedisClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRedisClient.class);

    private static final int EVENTLOOP_ACQ_INACTIVE = 0;

    private static final int EVENTLOOP_ACQ_ACTIVE = 1;

    private final AtomicInteger eventLoopGroupCas = new AtomicInteger();

    protected final ConnectionEvents connectionEvents = new ConnectionEvents();

    protected final Set<Closeable> closeableResources = ConcurrentHashMap.newKeySet();

    protected final ChannelGroup channels;

    private final ClientResources clientResources;

    private final List<CommandListener> commandListeners = new ArrayList<>();

    private final Map<Class<? extends EventLoopGroup>, EventLoopGroup> eventLoopGroups = new ConcurrentHashMap<>(2);

    private final boolean sharedResources;

    private final AtomicBoolean shutdown = new AtomicBoolean();

    private volatile ClientOptions clientOptions = ClientOptions.create();

    /**
     * Create a new instance with client resources.
     *
     * @param clientResources the client resources. If {@code null}, the client will create a new dedicated instance of client
     *        resources and keep track of them.
     */
    protected AbstractRedisClient(ClientResources clientResources) {

        if (clientResources == null) {
            this.sharedResources = false;
            this.clientResources = DefaultClientResources.create();
        } else {
            this.sharedResources = true;
            this.clientResources = clientResources;
        }

        this.channels = new DefaultChannelGroup(this.clientResources.eventExecutorGroup().next());
    }

    protected int getChannelCount() {
        return channels.size();
    }

    /**
     * Returns the {@link ClientOptions} which are valid for that client. Connections inherit the current options at the moment
     * the connection is created. Changes to options will not affect existing connections.
     *
     * @return the {@link ClientOptions} for this client
     */
    @Override
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
     * Returns the {@link ClientResources} which are used with that client.
     *
     * @return the {@link ClientResources} for this client.
     * @since 6.0
     *
     */
    @Override
    public ClientResources getResources() {
        return clientResources;
    }

    protected int getResourceCount() {
        return closeableResources.size();
    }

    /**
     * Add a listener for the RedisConnectionState. The listener is notified every time a connect/disconnect/IO exception
     * happens. The listeners are not bound to a specific connection, so every time a connection event happens on any
     * connection, the listener will be notified. The corresponding netty channel handler (async connection) is passed on the
     * event.
     *
     * @param listener must not be {@code null}.
     */
    @Override
    public void addListener(RedisConnectionStateListener listener) {

        LettuceAssert.notNull(listener, "RedisConnectionStateListener must not be null");
        connectionEvents.addListener(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener must not be {@code null}.
     */
    @Override
    public void removeListener(RedisConnectionStateListener listener) {

        LettuceAssert.notNull(listener, "RedisConnectionStateListener must not be null");
        connectionEvents.removeListener(listener);
    }

    /**
     * Add a listener for Redis Command events. The listener is notified on each command start/success/failure.
     *
     * @param listener must not be {@code null}.
     * @since 6.1
     */
    @Override
    public void addListener(CommandListener listener) {

        LettuceAssert.notNull(listener, "CommandListener must not be null");
        commandListeners.add(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener must not be {@code null}.
     * @since 6.1
     */
    @Override
    public void removeListener(CommandListener listener) {

        LettuceAssert.notNull(listener, "CommandListener must not be null");
        commandListeners.remove(listener);
    }

    protected List<CommandListener> getCommandListeners() {
        return commandListeners;
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
        connectionBuilder(socketAddressSupplier, connectionBuilder, connectionEvents, redisURI);
    }

    /**
     * Populate connection builder with necessary resources.
     *
     * @param socketAddressSupplier address supplier for initial connect and re-connect
     * @param connectionBuilder connection builder to configure the connection
     * @param connectionEvents connection events dispatcher
     * @param redisURI URI of the Redis instance
     * @since 6.2
     */
    protected void connectionBuilder(Mono<SocketAddress> socketAddressSupplier, ConnectionBuilder connectionBuilder,
            ConnectionEvents connectionEvents, RedisURI redisURI) {

        Bootstrap redisBootstrap = new Bootstrap();
        redisBootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

        connectionBuilder.bootstrap(redisBootstrap);
        connectionBuilder.apply(redisURI);
        connectionBuilder.configureBootstrap(!LettuceStrings.isEmpty(redisURI.getSocket()), this::getEventLoopGroup);
        connectionBuilder.channelGroup(channels).connectionEvents(connectionEvents == this.connectionEvents ? connectionEvents
                : ConnectionEvents.of(this.connectionEvents, connectionEvents));
        connectionBuilder.socketAddressSupplier(socketAddressSupplier);
    }

    protected void channelType(ConnectionBuilder connectionBuilder, ConnectionPoint connectionPoint) {

        LettuceAssert.notNull(connectionPoint, "ConnectionPoint must not be null");

        boolean domainSocket = LettuceStrings.isNotEmpty(connectionPoint.getSocket());
        connectionBuilder.bootstrap().group(getEventLoopGroup(
                domainSocket ? NativeTransports.eventLoopGroupClass(true) : Transports.eventLoopGroupClass()));

        if (connectionPoint.getSocket() != null) {
            NativeTransports.assertDomainSocketAvailable();
            connectionBuilder.bootstrap().channel(NativeTransports.domainSocketChannelClass());
        } else {
            connectionBuilder.bootstrap().channel(Transports.socketChannelClass());
        }
    }

    private EventLoopGroup getEventLoopGroup(Class<? extends EventLoopGroup> eventLoopGroupClass) {

        for (;;) {
            if (!eventLoopGroupCas.compareAndSet(EVENTLOOP_ACQ_INACTIVE, EVENTLOOP_ACQ_ACTIVE)) {
                continue;
            }

            try {

                return eventLoopGroups.computeIfAbsent(eventLoopGroupClass,
                        it -> clientResources.eventLoopGroupProvider().allocate(it));
            } finally {
                eventLoopGroupCas.set(EVENTLOOP_ACQ_INACTIVE);
            }
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
     * @since 4.4
     */
    protected <T> T getConnection(ConnectionFuture<T> connectionFuture) {

        try {
            return connectionFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(connectionFuture.getRemoteAddress(), e);
        } catch (Exception e) {
            throw RedisConnectionException.create(connectionFuture.getRemoteAddress(), Exceptions.unwrap(e));
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
            throw RedisConnectionException.create(Exceptions.unwrap(e));
        }
    }

    /**
     * Connect and initialize a channel from {@link ConnectionBuilder}.
     *
     * @param connectionBuilder must not be {@code null}.
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

        String uriString = connectionBuilder.getRedisURI().toString();

        EventRecorder.getInstance().record(new ConnectionCreatedEvent(uriString, connectionBuilder.endpoint().getId()));
        EventRecorder.RecordableEvent event = EventRecorder.getInstance()
                .start(new ConnectEvent(uriString, connectionBuilder.endpoint().getId()));

        channelReadyFuture.whenComplete((channel, throwable) -> {
            event.record();
        });

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

        ChannelInitializer<Channel> initializer = connectionBuilder.build(redisAddress);
        redisBootstrap.handler(initializer);

        clientResources.nettyCustomizer().afterBootstrapInitialized(redisBootstrap);
        ChannelFuture connectFuture = redisBootstrap.connect(redisAddress);

        channelReadyFuture.whenComplete((c, t) -> {

            if (t instanceof CancellationException) {
                connectFuture.cancel(true);
            }
        });

        connectFuture.addListener(future -> {

            Channel channel = connectFuture.channel();
            if (!future.isSuccess()) {

                Throwable cause = future.cause();
                Throwable detail = channel.attr(ConnectionBuilder.INIT_FAILURE).get();

                if (detail != null) {
                    detail.addSuppressed(cause);
                    cause = detail;
                }

                logger.debug("Connecting to Redis at {}: {}", redisAddress, cause);
                connectionBuilder.endpoint().initialState();
                channelReadyFuture.completeExceptionally(cause);
                return;
            }

            RedisHandshakeHandler handshakeHandler = channel.pipeline().get(RedisHandshakeHandler.class);

            if (handshakeHandler == null) {
                channelReadyFuture.completeExceptionally(new IllegalStateException("RedisHandshakeHandler not registered"));
                return;
            }

            handshakeHandler.channelInitialized().whenComplete((success, throwable) -> {

                if (throwable == null) {

                    logger.debug("Connecting to Redis at {}: Success", redisAddress);
                    RedisChannelHandler<?, ?> connection = connectionBuilder.connection();
                    connection.registerCloseables(closeableResources, connection);
                    channelReadyFuture.complete(channel);
                    return;
                }

                logger.debug("Connecting to Redis at {}, initialization: {}", redisAddress, throwable);
                connectionBuilder.endpoint().initialState();
                channelReadyFuture.completeExceptionally(throwable);
            });
        });
    }

    /**
     * Shutdown this client and close all open connections once this method is called. Once all connections are closed, the
     * associated {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The
     * client should be discarded after calling shutdown. The shutdown is executed without quiet time and a timeout of 2
     * {@link TimeUnit#SECONDS}.
     *
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    @Override
    public void shutdown() {
        shutdown(0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Shutdown this client and close all open connections once this method is called. Once all connections are closed, the
     * associated {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The
     * client should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period to allow the executor gracefully shut down.
     * @param timeout the maximum amount of time to wait until the backing executor is shutdown regardless if a task was
     *        submitted during the quiet period.
     * @since 5.0
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    @Override
    public void shutdown(Duration quietPeriod, Duration timeout) {
        shutdown(quietPeriod.toNanos(), timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Shutdown this client and close all open connections once this method is called. Once all connections are closed, the
     * associated {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The
     * client should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period to allow the executor gracefully shut down.
     * @param timeout the maximum amount of time to wait until the backing executor is shutdown regardless if a task was
     *        submitted during the quiet period.
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}.
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    @Override
    public void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {

        try {
            shutdownAsync(quietPeriod, timeout, timeUnit).get();
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

    /**
     * Shutdown this client and close all open connections asynchronously. Once all connections are closed, the associated
     * {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The client
     * should be discarded after calling shutdown. The shutdown is executed without quiet time and a timeout of 2
     * {@link TimeUnit#SECONDS}.
     *
     * @since 4.4
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    @Override
    public CompletableFuture<Void> shutdownAsync() {
        return shutdownAsync(0, 2, TimeUnit.SECONDS);
    }

    /**
     * Shutdown this client and close all open connections asynchronously. Once all connections are closed, the associated
     * {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The client
     * should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period to allow the executor gracefully shut down.
     * @param timeout the maximum amount of time to wait until the backing executor is shutdown regardless if a task was
     *        submitted during the quiet period.
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}.
     * @since 4.4
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    @Override
    public CompletableFuture<Void> shutdownAsync(long quietPeriod, long timeout, TimeUnit timeUnit) {

        if (shutdown.compareAndSet(false, true)) {

            logger.debug("Initiate shutdown ({}, {}, {})", quietPeriod, timeout, timeUnit);
            return closeResources().thenCompose((value) -> closeClientResources(quietPeriod, timeout, timeUnit));
        }

        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> closeResources() {

        List<CompletionStage<Void>> closeFutures = new ArrayList<>();
        List<Closeable> closeableResources = new ArrayList<>(this.closeableResources);

        for (Closeable closeableResource : closeableResources) {

            if (closeableResource instanceof AsyncCloseable) {

                closeFutures.add(((AsyncCloseable) closeableResource).closeAsync());
            } else {
                try {
                    closeableResource.close();
                } catch (Exception e) {
                    logger.debug("Exception on Close: " + e.getMessage(), e);
                }
            }
            this.closeableResources.remove(closeableResource);
        }

        for (Channel c : channels.toArray(new Channel[0])) {

            if (c == null) {
                continue;
            }

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

    protected RedisHandshake createHandshake(ConnectionState state) {
        EndpointTypeSource source = null;
        if (clientOptions.getMaintNotificationsConfig().maintNotificationsEnabled()) {
            LettuceAssert.notNull(clientOptions.getMaintNotificationsConfig().getEndpointTypeSource(),
                    "Address type source must not be null");

            source = clientOptions.getMaintNotificationsConfig().getEndpointTypeSource();
        }

        return new RedisHandshake(clientOptions.getConfiguredProtocolVersion(), clientOptions.isPingBeforeActivateConnection(),
                state, source);
    }

}
