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
package io.lettuce.core;

import static io.lettuce.core.LettuceStrings.isEmpty;
import static io.lettuce.core.LettuceStrings.isNotEmpty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.core.pubsub.PubSubCommandHandler;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.sentinel.StatefulRedisSentinelConnectionImpl;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;

/**
 * A scalable and thread-safe <a href="http://redis.io/">Redis</a> client supporting synchronous, asynchronous and reactive
 * execution models. Multiple threads may share one connection if they avoid blocking and transactional operations such as BLPOP
 * and MULTI/EXEC.
 * <p>
 * {@link RedisClient} can be used with:
 * <ul>
 * <li>Redis Standalone</li>
 * <li>Redis Pub/Sub</li>
 * <li>Redis Sentinel, Sentinel connections</li>
 * <li>Redis Sentinel, Master connections</li>
 * </ul>
 *
 * Redis Cluster is used through {@link io.lettuce.core.cluster.RedisClusterClient}. Master/Slave connections through
 * {@link io.lettuce.core.masterslave.MasterSlave} provide connections to Redis Master/Slave setups which run either in a static
 * Master/Slave setup or are managed by Redis Sentinel.
 * <p>
 * {@link RedisClient} is an expensive resource. It holds a set of netty's {@link io.netty.channel.EventLoopGroup}'s that use
 * multiple threads. Reuse this instance as much as possible or share a {@link ClientResources} instance amongst multiple client
 * instances.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @see RedisURI
 * @see StatefulRedisConnection
 * @see RedisFuture
 * @see reactor.core.publisher.Mono
 * @see reactor.core.publisher.Flux
 * @see RedisCodec
 * @see ClientOptions
 * @see ClientResources
 * @see io.lettuce.core.masterslave.MasterSlave
 * @see io.lettuce.core.cluster.RedisClusterClient
 */
public class RedisClient extends AbstractRedisClient {

    private static final RedisURI EMPTY_URI = new RedisURI();

    private final RedisURI redisURI;

    protected RedisClient(ClientResources clientResources, RedisURI redisURI) {

        super(clientResources);

        assertNotNull(redisURI);

        this.redisURI = redisURI;
        setDefaultTimeout(redisURI.getTimeout());
    }

    /**
     * Creates a uri-less RedisClient. You can connect to different Redis servers but you must supply a {@link RedisURI} on
     * connecting. Methods without having a {@link RedisURI} will fail with a {@link java.lang.IllegalStateException}.
     * Non-private constructor to make {@link RedisClient} proxyable.
     */
    protected RedisClient() {
        this(null, EMPTY_URI);
    }

    /**
     * Creates a uri-less RedisClient with default {@link ClientResources}. You can connect to different Redis servers but you
     * must supply a {@link RedisURI} on connecting. Methods without having a {@link RedisURI} will fail with a
     * {@link java.lang.IllegalStateException}.
     *
     * @return a new instance of {@link RedisClient}.
     */
    public static RedisClient create() {
        return new RedisClient(null, EMPTY_URI);
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with default {@link ClientResources}. You can
     * connect to different Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param redisURI the Redis URI, must not be {@code null}.
     * @return a new instance of {@link RedisClient}.
     */
    public static RedisClient create(RedisURI redisURI) {
        assertNotNull(redisURI);
        return new RedisClient(null, redisURI);
    }

    /**
     * Create a new client that connects to the supplied uri with default {@link ClientResources}. You can connect to different
     * Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param uri the Redis URI, must not be {@code null}.
     * @return a new instance of {@link RedisClient}.
     */
    public static RedisClient create(String uri) {
        LettuceAssert.notEmpty(uri, "URI must not be empty");
        return new RedisClient(null, RedisURI.create(uri));
    }

    /**
     * Creates a uri-less RedisClient with shared {@link ClientResources}. You need to shut down the {@link ClientResources}
     * upon shutting down your application. You can connect to different Redis servers but you must supply a {@link RedisURI} on
     * connecting. Methods without having a {@link RedisURI} will fail with a {@link java.lang.IllegalStateException}.
     *
     * @param clientResources the client resources, must not be {@code null}.
     * @return a new instance of {@link RedisClient}.
     */
    public static RedisClient create(ClientResources clientResources) {
        assertNotNull(clientResources);
        return new RedisClient(clientResources, EMPTY_URI);
    }

    /**
     * Create a new client that connects to the supplied uri with shared {@link ClientResources}.You need to shut down the
     * {@link ClientResources} upon shutting down your application. You can connect to different Redis servers but you must
     * supply a {@link RedisURI} on connecting.
     *
     * @param clientResources the client resources, must not be {@code null}.
     * @param uri the Redis URI, must not be {@code null}.
     * @return a new instance of {@link RedisClient}.
     */
    public static RedisClient create(ClientResources clientResources, String uri) {
        assertNotNull(clientResources);
        LettuceAssert.notEmpty(uri, "URI must not be empty");
        return create(clientResources, RedisURI.create(uri));
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with shared {@link ClientResources}. You need to
     * shut down the {@link ClientResources} upon shutting down your application.You can connect to different Redis servers but
     * you must supply a {@link RedisURI} on connecting.
     *
     * @param clientResources the client resources, must not be {@code null}.
     * @param redisURI the Redis URI, must not be {@code null}.
     * @return a new instance of {@link RedisClient}.
     */
    public static RedisClient create(ClientResources clientResources, RedisURI redisURI) {
        assertNotNull(clientResources);
        assertNotNull(redisURI);
        return new RedisClient(clientResources, redisURI);
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection.
     */
    public StatefulRedisConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    /**
     * Open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new stateful Redis connection.
     */
    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec) {

        checkForRedisURI();

        return getConnection(connectStandaloneAsync(codec, this.redisURI, timeout));
    }

    /**
     * Open a new connection to a Redis server using the supplied {@link RedisURI} that treats keys and values as UTF-8 strings.
     *
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @return A new connection.
     */
    public StatefulRedisConnection<String, String> connect(RedisURI redisURI) {

        assertNotNull(redisURI);

        return getConnection(connectStandaloneAsync(newStringStringCodec(), redisURI, redisURI.getTimeout()));
    }

    /**
     * Open a new connection to a Redis server using the supplied {@link RedisURI} and the supplied {@link RedisCodec codec} to
     * encode/decode keys.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec, RedisURI redisURI) {

        assertNotNull(redisURI);

        return getConnection(connectStandaloneAsync(codec, redisURI, redisURI.getTimeout()));
    }

    /**
     * Open asynchronously a new connection to a Redis server using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return {@link ConnectionFuture} to indicate success or failure to connect.
     * @since 5.0
     */
    public <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectAsync(RedisCodec<K, V> codec, RedisURI redisURI) {

        assertNotNull(redisURI);

        return transformAsyncConnectionException(connectStandaloneAsync(codec, redisURI, redisURI.getTimeout()));
    }

    private <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectStandaloneAsync(RedisCodec<K, V> codec,
            RedisURI redisURI, Duration timeout) {

        assertNotNull(codec);
        checkValidRedisURI(redisURI);

        logger.debug("Trying to get a Redis connection for: " + redisURI);

        DefaultEndpoint endpoint = new DefaultEndpoint(clientOptions, clientResources);
        RedisChannelWriter writer = endpoint;

        if (CommandExpiryWriter.isSupported(clientOptions)) {
            writer = new CommandExpiryWriter(writer, clientOptions, clientResources);
        }

        StatefulRedisConnectionImpl<K, V> connection = newStatefulRedisConnection(writer, codec, timeout);
        ConnectionFuture<StatefulRedisConnection<K, V>> future = connectStatefulAsync(connection, codec, endpoint, redisURI,
                () -> new CommandHandler(clientOptions, clientResources, endpoint));

        future.whenComplete((channelHandler, throwable) -> {

            if (throwable != null) {
                connection.close();
            }
        });

        return future;
    }

    @SuppressWarnings("unchecked")
    private <K, V, S> ConnectionFuture<S> connectStatefulAsync(StatefulRedisConnectionImpl<K, V> connection,
            RedisCodec<K, V> codec, Endpoint endpoint, RedisURI redisURI, Supplier<CommandHandler> commandHandlerSupplier) {

        ConnectionBuilder connectionBuilder = getConnectionBuilder(endpoint, redisURI, commandHandlerSupplier);
        connectionBuilder.connection(connection);

        ConnectionFuture<RedisChannelHandler<K, V>> future = initializeChannelAsync(connectionBuilder);
        ConnectionFuture<?> sync = future;

        if (!clientOptions.isPingBeforeActivateConnection() && hasPassword(redisURI)) {

            sync = sync.thenCompose(channelHandler -> {

                CommandArgs<K, V> args = new CommandArgs<>(codec).add(redisURI.getPassword());
                return connection.async().dispatch(CommandType.AUTH, new StatusOutput<>(codec), args);
            });
        }

        if (LettuceStrings.isNotEmpty(redisURI.getClientName())) {
            sync = sync.thenApply(channelHandler -> {
                connection.setClientName(redisURI.getClientName());
                return channelHandler;
            });
        }

        if (redisURI.getDatabase() != 0) {

            sync = sync.thenCompose(channelHandler -> {

                CommandArgs<K, V> args = new CommandArgs<>(codec).add(redisURI.getDatabase());
                return connection.async().dispatch(CommandType.SELECT, new StatusOutput<>(codec), args);
            });
        }

        return sync.thenApply(channelHandler -> (S) connection);
    }

    private <K, V> ConnectionBuilder getConnectionBuilder(Endpoint endpoint, RedisURI redisURI,
            Supplier<CommandHandler> commandHandlerSupplier) {

        ConnectionBuilder connectionBuilder;

        if (redisURI.isSsl()) {
            SslConnectionBuilder sslConnectionBuilder = SslConnectionBuilder.sslConnectionBuilder();
            sslConnectionBuilder.ssl(redisURI);
            connectionBuilder = sslConnectionBuilder;
        } else {
            connectionBuilder = ConnectionBuilder.connectionBuilder();
        }

        connectionBuilder.clientOptions(clientOptions);
        connectionBuilder.clientResources(clientResources);
        connectionBuilder.commandHandler(commandHandlerSupplier).endpoint(endpoint);

        connectionBuilder(getSocketAddressSupplier(redisURI), connectionBuilder, redisURI);
        channelType(connectionBuilder, redisURI);

        if (clientOptions.isPingBeforeActivateConnection()) {
            if (hasPassword(redisURI)) {
                connectionBuilder.enableAuthPingBeforeConnect();
            } else {
                connectionBuilder.enablePingBeforeConnect();
            }
        }

        return connectionBuilder;
    }

    private static boolean hasPassword(RedisURI redisURI) {
        return redisURI.getPassword() != null && redisURI.getPassword().length != 0;
    }

    /**
     * Open a new pub/sub connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful pub/sub connection.
     */
    public StatefulRedisPubSubConnection<String, String> connectPubSub() {
        return getConnection(connectPubSubAsync(newStringStringCodec(), redisURI, timeout));
    }

    /**
     * Open a new pub/sub connection to a Redis server using the supplied {@link RedisURI} that treats keys and values as UTF-8
     * strings.
     *
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @return A new stateful pub/sub connection.
     */
    public StatefulRedisPubSubConnection<String, String> connectPubSub(RedisURI redisURI) {

        assertNotNull(redisURI);
        return getConnection(connectPubSubAsync(newStringStringCodec(), redisURI, redisURI.getTimeout()));
    }

    /**
     * Open a new pub/sub connection to the Redis server using the supplied {@link RedisURI} and use the supplied
     * {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new stateful pub/sub connection.
     */
    public <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {
        checkForRedisURI();
        return getConnection(connectPubSubAsync(codec, redisURI, timeout));
    }

    /**
     * Open a new pub/sub connection to the Redis server using the supplied {@link RedisURI} and use the supplied
     * {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    public <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec, RedisURI redisURI) {

        assertNotNull(redisURI);
        return getConnection(connectPubSubAsync(codec, redisURI, redisURI.getTimeout()));
    }

    /**
     * Open asynchronously a new pub/sub connection to the Redis server using the supplied {@link RedisURI} and use the supplied
     * {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return {@link ConnectionFuture} to indicate success or failure to connect.
     * @since 5.0
     */
    public <K, V> ConnectionFuture<StatefulRedisPubSubConnection<K, V>> connectPubSubAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {

        assertNotNull(redisURI);
        return transformAsyncConnectionException(connectPubSubAsync(codec, redisURI, redisURI.getTimeout()));
    }

    private <K, V> ConnectionFuture<StatefulRedisPubSubConnection<K, V>> connectPubSubAsync(RedisCodec<K, V> codec,
            RedisURI redisURI, Duration timeout) {

        assertNotNull(codec);
        checkValidRedisURI(redisURI);

        PubSubEndpoint<K, V> endpoint = new PubSubEndpoint<>(clientOptions, clientResources);
        RedisChannelWriter writer = endpoint;

        if (CommandExpiryWriter.isSupported(clientOptions)) {
            writer = new CommandExpiryWriter(writer, clientOptions, clientResources);
        }

        StatefulRedisPubSubConnectionImpl<K, V> connection = newStatefulRedisPubSubConnection(endpoint, writer, codec, timeout);

        ConnectionFuture<StatefulRedisPubSubConnection<K, V>> future = connectStatefulAsync(connection, codec, endpoint,
                redisURI, () -> new PubSubCommandHandler<>(clientOptions, clientResources, codec, endpoint));

        return future.whenComplete((conn, throwable) -> {

            if (throwable != null) {
                conn.close();
            }
        });
    }

    /**
     * Open a connection to a Redis Sentinel that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis Sentinel connection.
     */
    public StatefulRedisSentinelConnection<String, String> connectSentinel() {
        return connectSentinel(newStringStringCodec());
    }

    /**
     * Open a connection to a Redis Sentinel that treats keys and use the supplied {@link RedisCodec codec} to encode/decode
     * keys and values. The client {@link RedisURI} must contain one or more sentinels.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new stateful Redis Sentinel connection.
     */
    public <K, V> StatefulRedisSentinelConnection<K, V> connectSentinel(RedisCodec<K, V> codec) {
        checkForRedisURI();
        return getConnection(connectSentinelAsync(codec, redisURI, timeout));
    }

    /**
     * Open a connection to a Redis Sentinel using the supplied {@link RedisURI} that treats keys and values as UTF-8 strings.
     * The client {@link RedisURI} must contain one or more sentinels.
     *
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @return A new connection.
     */
    public StatefulRedisSentinelConnection<String, String> connectSentinel(RedisURI redisURI) {

        assertNotNull(redisURI);

        return getConnection(connectSentinelAsync(newStringStringCodec(), redisURI, redisURI.getTimeout()));
    }

    /**
     * Open a connection to a Redis Sentinel using the supplied {@link RedisURI} and use the supplied {@link RedisCodec codec}
     * to encode/decode keys and values. The client {@link RedisURI} must contain one or more sentinels.
     *
     * @param codec the Redis server to connect to, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     */
    public <K, V> StatefulRedisSentinelConnection<K, V> connectSentinel(RedisCodec<K, V> codec, RedisURI redisURI) {

        assertNotNull(redisURI);

        return getConnection(connectSentinelAsync(codec, redisURI, redisURI.getTimeout()));
    }

    /**
     * Open asynchronously a connection to a Redis Sentinel using the supplied {@link RedisURI} and use the supplied
     * {@link RedisCodec codec} to encode/decode keys and values. The client {@link RedisURI} must contain one or more
     * sentinels.
     *
     * @param codec the Redis server to connect to, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return A new connection.
     * @since 5.1
     */
    public <K, V> CompletableFuture<StatefulRedisSentinelConnection<K, V>> connectSentinelAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {

        assertNotNull(redisURI);

        return transformAsyncConnectionException(connectSentinelAsync(codec, redisURI, redisURI.getTimeout()), redisURI);
    }

    private <K, V> CompletableFuture<StatefulRedisSentinelConnection<K, V>> connectSentinelAsync(RedisCodec<K, V> codec,
            RedisURI redisURI, Duration timeout) {

        assertNotNull(codec);
        checkValidRedisURI(redisURI);

        logger.debug("Trying to get a Redis Sentinel connection for one of: " + redisURI.getSentinels());

        if (redisURI.getSentinels().isEmpty() && (isNotEmpty(redisURI.getHost()) || !isEmpty(redisURI.getSocket()))) {
            return doConnectSentinelAsync(codec, redisURI.getClientName(), redisURI, timeout).toCompletableFuture();
        }

        List<RedisURI> sentinels = redisURI.getSentinels();
        Queue<Throwable> exceptionCollector = new LinkedBlockingQueue<>();
        validateUrisAreOfSameConnectionType(sentinels);

        Mono<StatefulRedisSentinelConnection<K, V>> connectionLoop = null;

        for (RedisURI uri : sentinels) {

            String clientName = LettuceStrings.isNotEmpty(uri.getClientName()) ? uri.getClientName() : redisURI.getClientName();

            Mono<StatefulRedisSentinelConnection<K, V>> connectionMono = Mono
                    .defer(() -> Mono.fromCompletionStage(doConnectSentinelAsync(codec, clientName, uri, timeout)))
                    .onErrorMap(CompletionException.class, Throwable::getCause)
                    .onErrorMap(e -> new RedisConnectionException("Cannot connect Redis Sentinel at " + uri, e))
                    .doOnError(exceptionCollector::add);

            if (connectionLoop == null) {
                connectionLoop = connectionMono;
            } else {
                connectionLoop = connectionLoop.onErrorResume(t -> connectionMono);
            }
        }

        if (connectionLoop == null) {
            return Mono
                    .<StatefulRedisSentinelConnection<K, V>> error(
                            new RedisConnectionException("Cannot connect to a Redis Sentinel: " + redisURI.getSentinels()))
                    .toFuture();
        }

        return connectionLoop.onErrorMap(e -> {

            RedisConnectionException ex = new RedisConnectionException(
                    "Cannot connect to a Redis Sentinel: " + redisURI.getSentinels(), e);

            for (Throwable throwable : exceptionCollector) {
                if (e != throwable) {
                    ex.addSuppressed(throwable);
                }
            }

            return ex;
        }).toFuture();
    }

    private <K, V> ConnectionFuture<StatefulRedisSentinelConnection<K, V>> doConnectSentinelAsync(RedisCodec<K, V> codec,
            String clientName, RedisURI redisURI, Duration timeout) {

        DefaultEndpoint endpoint = new DefaultEndpoint(clientOptions, clientResources);
        RedisChannelWriter writer = endpoint;

        if (CommandExpiryWriter.isSupported(clientOptions)) {
            writer = new CommandExpiryWriter(writer, clientOptions, clientResources);
        }

        ConnectionBuilder connectionBuilder = getConnectionBuilder(endpoint, redisURI,
                () -> new CommandHandler(clientOptions, clientResources, endpoint));

        StatefulRedisSentinelConnectionImpl<K, V> connection = newStatefulRedisSentinelConnection(writer, codec, timeout);

        logger.debug("Connecting to Redis Sentinel, address: " + redisURI);

        connectionBuilder.connection(connection);
        connectionBuilder(getSocketAddressSupplier(redisURI), connectionBuilder, redisURI);

        if (clientOptions.isPingBeforeActivateConnection()) {
            if (hasPassword(redisURI)) {
                connectionBuilder.enableAuthPingBeforeConnect();
            } else {
                connectionBuilder.enablePingBeforeConnect();
            }
        }

        channelType(connectionBuilder, redisURI);
        ConnectionFuture<?> sync = initializeChannelAsync(connectionBuilder);

        if (!clientOptions.isPingBeforeActivateConnection() && hasPassword(redisURI)) {

            sync = sync.thenCompose(channelHandler -> {

                CommandArgs<K, V> args = new CommandArgs<>(codec).add(redisURI.getPassword());
                return connection.async().dispatch(CommandType.AUTH, new StatusOutput<>(codec), args).toCompletableFuture();
            });
        }

        if (LettuceStrings.isNotEmpty(clientName)) {
            sync = sync.thenApply(channelHandler -> {
                connection.setClientName(clientName);
                return channelHandler;
            });
        }

        return sync.thenApply(ignore -> (StatefulRedisSentinelConnection<K, V>) connection).whenComplete((ignore, e) -> {

            if (e != null) {
                logger.warn("Cannot connect Redis Sentinel at " + redisURI + ": " + e.toString());
                connection.close();
            }
        });
    }

    /**
     * Set the {@link ClientOptions} for the client.
     *
     * @param clientOptions the new client options.
     * @throws IllegalArgumentException if {@literal clientOptions} is null
     */
    @Override
    public void setOptions(ClientOptions clientOptions) {
        super.setOptions(clientOptions);
    }

    /**
     * Returns the {@link ClientResources} which are used with that client.
     *
     * @return the {@link ClientResources} for this client.
     */
    public ClientResources getResources() {
        return clientResources;
    }

    // -------------------------------------------------------------------------
    // Implementation hooks and helper methods
    // -------------------------------------------------------------------------

    /**
     * Create a new instance of {@link StatefulRedisPubSubConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param endpoint the endpoint.
     * @param channelWriter the channel writer.
     * @param codec codec.
     * @param timeout default timeout.
     * @param <K> Key-Type.
     * @param <V> Value Type.
     * @return new instance of StatefulRedisPubSubConnectionImpl.
     */
    protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(PubSubEndpoint<K, V> endpoint,
            RedisChannelWriter channelWriter, RedisCodec<K, V> codec, Duration timeout) {
        return new StatefulRedisPubSubConnectionImpl<>(endpoint, channelWriter, codec, timeout);
    }

    /**
     * Create a new instance of {@link StatefulRedisSentinelConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param channelWriter the channel writer.
     * @param codec codec.
     * @param timeout default timeout.
     * @param <K> Key-Type.
     * @param <V> Value Type.
     * @return new instance of StatefulRedisSentinelConnectionImpl.
     */
    protected <K, V> StatefulRedisSentinelConnectionImpl<K, V> newStatefulRedisSentinelConnection(
            RedisChannelWriter channelWriter, RedisCodec<K, V> codec, Duration timeout) {
        return new StatefulRedisSentinelConnectionImpl<>(channelWriter, codec, timeout);
    }

    /**
     * Create a new instance of {@link StatefulRedisConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param channelWriter the channel writer.
     * @param codec codec.
     * @param timeout default timeout.
     * @param <K> Key-Type.
     * @param <V> Value Type.
     * @return new instance of StatefulRedisConnectionImpl.
     */
    protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(RedisChannelWriter channelWriter,
            RedisCodec<K, V> codec, Duration timeout) {
        return new StatefulRedisConnectionImpl<>(channelWriter, codec, timeout);
    }

    /**
     * Get a {@link Mono} that resolves {@link RedisURI} to a {@link SocketAddress}. Resolution is performed either using Redis
     * Sentinel (if the {@link RedisURI} is configured with Sentinels) or via DNS resolution.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param redisURI must not be {@code null}.
     * @return the resolved {@link SocketAddress}.
     * @see ClientResources#dnsResolver()
     * @see RedisURI#getSentinels()
     * @see RedisURI#getSentinelMasterId()
     */
    protected Mono<SocketAddress> getSocketAddress(RedisURI redisURI) {

        return Mono.defer(() -> {

            if (redisURI.getSentinelMasterId() != null && !redisURI.getSentinels().isEmpty()) {
                logger.debug("Connecting to Redis using Sentinels {}, MasterId {}", redisURI.getSentinels(),
                        redisURI.getSentinelMasterId());
                return lookupRedis(redisURI).switchIfEmpty(Mono.error(new RedisConnectionException(
                        "Cannot provide redisAddress using sentinel for masterId " + redisURI.getSentinelMasterId())));

            } else {
                return Mono.fromCallable(() -> clientResources.socketAddressResolver().resolve((redisURI)));
            }
        });
    }

    /**
     * Returns a {@link String} {@link RedisCodec codec}.
     *
     * @return a {@link String} {@link RedisCodec codec}.
     * @see StringCodec#UTF8
     */
    protected RedisCodec<String, String> newStringStringCodec() {
        return StringCodec.UTF8;
    }

    private static void validateUrisAreOfSameConnectionType(List<RedisURI> redisUris) {

        boolean unixDomainSocket = false;
        boolean inetSocket = false;
        for (RedisURI sentinel : redisUris) {
            if (sentinel.getSocket() != null) {
                unixDomainSocket = true;
            }
            if (sentinel.getHost() != null) {
                inetSocket = true;
            }
        }

        if (unixDomainSocket && inetSocket) {
            throw new RedisConnectionException("You cannot mix unix domain socket and IP socket URI's");
        }
    }

    private Mono<SocketAddress> getSocketAddressSupplier(RedisURI redisURI) {
        return getSocketAddress(redisURI).doOnNext(addr -> logger.debug("Resolved SocketAddress {} using {}", addr, redisURI));
    }

    private Mono<SocketAddress> lookupRedis(RedisURI sentinelUri) {

        Mono<StatefulRedisSentinelConnection<String, String>> connection = Mono
                .fromCompletionStage(() -> connectSentinelAsync(newStringStringCodec(), sentinelUri, timeout));

        return connection.flatMap(c -> {

            String sentinelMasterId = sentinelUri.getSentinelMasterId();
            return c.reactive().getMasterAddrByName(sentinelMasterId).map(it -> {

                if (it instanceof InetSocketAddress) {

                    InetSocketAddress isa = (InetSocketAddress) it;
                    SocketAddress resolved = clientResources.socketAddressResolver()
                            .resolve(RedisURI.create(isa.getHostString(), isa.getPort()));

                    logger.debug("Resolved Master {} SocketAddress {}:{} to {}", sentinelMasterId, isa.getHostString(),
                            isa.getPort(), resolved);

                    return resolved;
                }

                return it;
            }).timeout(this.timeout) //
                    .onErrorResume(e -> {

                        RedisCommandTimeoutException ex = ExceptionFactory
                                .createTimeoutException("Cannot obtain master using SENTINEL MASTER", timeout);
                        ex.addSuppressed(e);

                        return Mono.fromCompletionStage(c::closeAsync).then(Mono.error(ex));
                    }).flatMap(it -> Mono.fromCompletionStage(c::closeAsync) //
                            .thenReturn(it));
        });
    }

    private static <T> ConnectionFuture<T> transformAsyncConnectionException(ConnectionFuture<T> future) {

        return future.thenCompose((v, e) -> {

            if (e != null) {
                return Futures.failed(RedisConnectionException.create(future.getRemoteAddress(), e));
            }

            return CompletableFuture.completedFuture(v);
        });
    }

    private static <T> CompletableFuture<T> transformAsyncConnectionException(CompletionStage<T> future, RedisURI target) {

        return ConnectionFuture.from(null, future.toCompletableFuture()).thenCompose((v, e) -> {

            if (e != null) {
                return Futures.failed(RedisConnectionException.create(target.toString(), e));
            }

            return CompletableFuture.completedFuture(v);
        }).toCompletableFuture();
    }

    private static void checkValidRedisURI(RedisURI redisURI) {

        LettuceAssert.notNull(redisURI, "A valid RedisURI is required");

        if (redisURI.getSentinels().isEmpty()) {
            if (isEmpty(redisURI.getHost()) && isEmpty(redisURI.getSocket())) {
                throw new IllegalArgumentException("RedisURI for Redis Standalone does not contain a host or a socket");
            }
        } else {

            if (isEmpty(redisURI.getSentinelMasterId())) {
                throw new IllegalArgumentException("TRedisURI for Redis Sentinel requires a masterId");
            }

            for (RedisURI sentinel : redisURI.getSentinels()) {
                if (isEmpty(sentinel.getHost()) && isEmpty(sentinel.getSocket())) {
                    throw new IllegalArgumentException("RedisURI for Redis Sentinel does not contain a host or a socket");
                }
            }
        }
    }

    private static <K, V> void assertNotNull(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
    }

    private static void assertNotNull(RedisURI redisURI) {
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");
    }

    private static void assertNotNull(ClientResources clientResources) {
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");
    }

    private void checkForRedisURI() {
        LettuceAssert.assertState(this.redisURI != EMPTY_URI,
                "RedisURI is not available. Use RedisClient(Host), RedisClient(Host, Port) or RedisClient(RedisURI) to construct your client.");
        checkValidRedisURI(this.redisURI);
    }

}
