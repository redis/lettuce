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
package com.lambdaworks.redis;

import static com.lambdaworks.redis.LettuceStrings.isEmpty;
import static com.lambdaworks.redis.LettuceStrings.isNotEmpty;
import static com.lambdaworks.redis.internal.LettuceClassUtils.isPresent;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnectionImpl;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.SocketAddressResolver;
import com.lambdaworks.redis.sentinel.StatefulRedisSentinelConnectionImpl;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.support.ConnectionPoolSupport;

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
 * Redis Cluster is used through {@link com.lambdaworks.redis.cluster.RedisClusterClient}. Master/Slave connections through
 * {@link com.lambdaworks.redis.masterslave.MasterSlave} provide connections to Redis Master/Slave setups which run either in a
 * static Master/Slave setup or are managed by Redis Sentinel.
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
 * @see rx.Observable
 * @see RedisCodec
 * @see ClientOptions
 * @see ClientResources
 * @see com.lambdaworks.redis.masterslave.MasterSlave
 * @see com.lambdaworks.redis.cluster.RedisClusterClient
 */
public class RedisClient extends AbstractRedisClient {

    private static final RedisURI EMPTY_URI = new RedisURI();
    private static final boolean POOL_AVAILABLE = isPresent("org.apache.commons.pool2.impl.GenericObjectPool");

    private final RedisURI redisURI;

    protected RedisClient(ClientResources clientResources, RedisURI redisURI) {
        super(clientResources);

        assertNotNull(redisURI);

        this.redisURI = redisURI;
        setDefaultTimeout(redisURI.getTimeout(), redisURI.getUnit());
    }

    /**
     * Creates a uri-less RedisClient. You can connect to different Redis servers but you must supply a {@link RedisURI} on
     * connecting. Methods without having a {@link RedisURI} will fail with a {@link java.lang.IllegalStateException}.
     *
     * @deprecated Use the factory method {@link #create()}
     */
    @Deprecated
    public RedisClient() {
        this(EMPTY_URI);
    }

    /**
     * Create a new client that connects to the supplied host on the default port.
     *
     * @param host Server hostname.
     * @deprecated Use the factory method {@link #create(String)}
     */
    @Deprecated
    public RedisClient(String host) {
        this(host, RedisURI.DEFAULT_REDIS_PORT);
    }

    /**
     * Create a new client that connects to the supplied host and port. Connection attempts and non-blocking commands will
     * {@link #setDefaultTimeout timeout} after 60 seconds.
     *
     * @param host Server hostname.
     * @param port Server port.
     * @deprecated Use the factory method {@link #create(RedisURI)}
     */
    @Deprecated
    public RedisClient(String host, int port) {
        this(RedisURI.Builder.redis(host, port).build());
    }

    /**
     * Create a new client that connects to the supplied host and port. Connection attempts and non-blocking commands will
     * {@link #setDefaultTimeout timeout} after 60 seconds.
     *
     * @param redisURI Redis URI.
     * @deprecated Use the factory method {@link #create(RedisURI)}
     */
    @Deprecated
    public RedisClient(RedisURI redisURI) {
        this(null, redisURI);
    }

    /**
     * Creates a uri-less RedisClient with default {@link ClientResources}. You can connect to different Redis servers but you
     * must supply a {@link RedisURI} on connecting. Methods without having a {@link RedisURI} will fail with a
     * {@link java.lang.IllegalStateException}.
     *
     * @return a new instance of {@link RedisClient}
     */
    public static RedisClient create() {
        return new RedisClient(null, EMPTY_URI);
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with default {@link ClientResources}. You can
     * connect to different Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param redisURI the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClient}
     */
    public static RedisClient create(RedisURI redisURI) {
        assertNotNull(redisURI);
        return new RedisClient(null, redisURI);
    }

    /**
     * Create a new client that connects to the supplied uri with default {@link ClientResources}. You can connect to different
     * Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param uri the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClient}
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
     * @param clientResources the client resources, must not be {@literal null}
     * @return a new instance of {@link RedisClient}
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
     * @param clientResources the client resources, must not be {@literal null}
     * @param uri the Redis URI, must not be {@literal null}
     *
     * @return a new instance of {@link RedisClient}
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
     * @param clientResources the client resources, must not be {@literal null}
     * @param redisURI the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClient}
     */
    public static RedisClient create(ClientResources clientResources, RedisURI redisURI) {
        assertNotNull(clientResources);
        assertNotNull(redisURI);
        return new RedisClient(clientResources, redisURI);
    }

    /**
     * Creates a connection pool for synchronous connections. 5 max idle connections and 20 max active connections. Please keep
     * in mind to free all collections and close the pool once you do not need it anymore. Requires Apache commons-pool2
     * dependency.
     *
     * @return a new {@link RedisConnectionPool} instance
     * @deprecated Will be removed in future versions. Use {@link ConnectionPoolSupport}.
     */
    @Deprecated
    public RedisConnectionPool<RedisCommands<String, String>> pool() {
        return pool(5, 20);
    }

    /**
     * Creates a connection pool for synchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore. Requires Apache commons-pool2 dependency.
     *
     * @param maxIdle max idle connections in pool
     * @param maxActive max active connections in pool
     * @return a new {@link RedisConnectionPool} instance
     * @deprecated Will be removed in future versions. Use {@link ConnectionPoolSupport}.
     */
    @Deprecated
    public RedisConnectionPool<RedisCommands<String, String>> pool(int maxIdle, int maxActive) {
        return pool(newStringStringCodec(), maxIdle, maxActive);
    }

    /**
     * Creates a connection pool for synchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore. Requires Apache commons-pool2 dependency.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param maxIdle max idle connections in pool
     * @param maxActive max active connections in pool
     * @param <K> Key type
     * @param <V> Value type
     * @return a new {@link RedisConnectionPool} instance
     * @deprecated Will be removed in future versions. Use {@link ConnectionPoolSupport}.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public <K, V> RedisConnectionPool<RedisCommands<K, V>> pool(final RedisCodec<K, V> codec, int maxIdle, int maxActive) {

        checkPoolDependency();
        checkForRedisURI();
        LettuceAssert.notNull(codec, "RedisCodec must not be null");

        long maxWait = makeTimeout();
        RedisConnectionPool<RedisCommands<K, V>> pool = new RedisConnectionPool<>(
                new RedisConnectionPool.RedisConnectionProvider<RedisCommands<K, V>>() {
                    @Override
                    public RedisCommands<K, V> createConnection() {
                        return connect(codec, redisURI).sync();
                    }

                    @Override
                    @SuppressWarnings("rawtypes")
                    public Class<? extends RedisCommands<K, V>> getComponentType() {
                        return (Class) RedisCommands.class;
                    }
                }, maxActive, maxIdle, maxWait);

        pool.addListener(closeableResources::remove);

        closeableResources.add(pool);

        return pool;
    }

    protected long makeTimeout() {
        return TimeUnit.MILLISECONDS.convert(timeout, unit);
    }

    /**
     * Creates a connection pool for asynchronous connections. 5 max idle connections and 20 max active connections. Please keep
     * in mind to free all collections and close the pool once you do not need it anymore. Requires Apache commons-pool2
     * dependency.
     *
     * @return a new {@link RedisConnectionPool} instance
     * @deprecated Will be removed in future versions. Use {@link ConnectionPoolSupport}.
     */
    @Deprecated
    public RedisConnectionPool<RedisAsyncCommands<String, String>> asyncPool() {
        return asyncPool(5, 20);
    }

    /**
     * Creates a connection pool for asynchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore. Requires Apache commons-pool2 dependency.
     *
     * @param maxIdle max idle connections in pool
     * @param maxActive max active connections in pool
     * @return a new {@link RedisConnectionPool} instance
     * @deprecated Will be removed in future versions. Use {@link ConnectionPoolSupport}.
     */
    @Deprecated
    public RedisConnectionPool<RedisAsyncCommands<String, String>> asyncPool(int maxIdle, int maxActive) {
        return asyncPool(newStringStringCodec(), maxIdle, maxActive);
    }

    /**
     * Creates a connection pool for asynchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore. Requires Apache commons-pool2 dependency.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param maxIdle max idle connections in pool
     * @param maxActive max active connections in pool
     * @param <K> Key type
     * @param <V> Value type
     * @return a new {@link RedisConnectionPool} instance
     * @deprecated Will be removed in future versions. Use {@link ConnectionPoolSupport}.
     */
    @Deprecated
    public <K, V> RedisConnectionPool<RedisAsyncCommands<K, V>> asyncPool(final RedisCodec<K, V> codec, int maxIdle,
            int maxActive) {

        checkPoolDependency();
        checkForRedisURI();
        LettuceAssert.notNull(codec, "RedisCodec must not be null");

        long maxWait = makeTimeout();
        RedisConnectionPool<RedisAsyncCommands<K, V>> pool = new RedisConnectionPool<>(
                new RedisConnectionPool.RedisConnectionProvider<RedisAsyncCommands<K, V>>() {
                    @Override
                    public RedisAsyncCommands<K, V> createConnection() {
                        return connectStandalone(codec, redisURI, defaultTimeout()).async();
                    }

                    @Override
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    public Class<? extends RedisAsyncCommands<K, V>> getComponentType() {
                        return (Class) RedisAsyncCommands.class;
                    }
                }, maxActive, maxIdle, maxWait);

        pool.addListener(closeableResources::remove);

        closeableResources.add(pool);

        return pool;
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    public StatefulRedisConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    /**
     * Open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection
     */
    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec) {
        checkForRedisURI();
        return connectStandalone(codec, this.redisURI, defaultTimeout());
    }

    /**
     * Open a new connection to a Redis server using the supplied {@link RedisURI} that treats keys and values as UTF-8 strings.
     *
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @return A new connection
     */
    public StatefulRedisConnection<String, String> connect(RedisURI redisURI) {
        return connectStandalone(newStringStringCodec(), redisURI, Timeout.from(redisURI));
    }

    /**
     * Open a new connection to a Redis server using the supplied {@link RedisURI} and the supplied {@link RedisCodec codec} to
     * encode/decode keys.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     */
    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec, RedisURI redisURI) {
        return connectStandalone(codec, redisURI, Timeout.from(redisURI));
    }

    /**
     * Open a new asynchronous connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new connection
     */
    @Deprecated
    public RedisAsyncCommands<String, String> connectAsync() {
        return connect(newStringStringCodec()).async();
    }

    /**
     * Open a new asynchronous connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     * @deprecated Use {@code connect(codec).async()}
     */
    @Deprecated
    public <K, V> RedisAsyncCommands<K, V> connectAsync(RedisCodec<K, V> codec) {
        return connectStandalone(codec, redisURI, defaultTimeout()).async();
    }

    /**
     * Open a new asynchronous connection to a Redis server using the supplied {@link RedisURI} that treats keys and values as
     * UTF-8 strings.
     *
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @return A new connection
     * @deprecated Use {@code connect(redisURI).async()}
     */
    @Deprecated
    public RedisAsyncCommands<String, String> connectAsync(RedisURI redisURI) {
        return connectStandalone(newStringStringCodec(), redisURI, Timeout.from(redisURI)).async();
    }

    /**
     * Open a new asynchronous connection to a Redis server using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     * @deprecated Use {@code connect(codec, redisURI).async()}
     */
    @Deprecated
    public <K, V> RedisAsyncCommands<K, V> connectAsync(RedisCodec<K, V> codec, RedisURI redisURI) {
        return connectStandalone(codec, redisURI, Timeout.from(redisURI)).async();
    }

    private <K, V> StatefulRedisConnection<K, V> connectStandalone(RedisCodec<K, V> codec, RedisURI redisURI, Timeout timeout) {

        ConnectionFuture<StatefulRedisConnection<K, V>> future = connectStandaloneAsync(codec, redisURI, timeout);
        return getConnection(future);
    }

    @SuppressWarnings("unused")
    // Required by ReflectiveNodeConnectionFactory.
    <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectStandaloneAsync(RedisCodec<K, V> codec, RedisURI redisURI) {
        return connectStandaloneAsync(codec, redisURI, Timeout.from(redisURI));
    }

    private <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectStandaloneAsync(RedisCodec<K, V> codec,
            RedisURI redisURI, Timeout timeout) {

        assertNotNull(codec);
        checkValidRedisURI(redisURI);

        logger.debug("Trying to get a Redis connection for: " + redisURI);

        CommandHandler<K, V> handler = new CommandHandler<>(clientOptions, clientResources);

        StatefulRedisConnectionImpl<K, V> connection = newStatefulRedisConnection(handler, codec, timeout.timeout,
                timeout.timeUnit);
        ConnectionFuture<StatefulRedisConnection<K, V>> future = connectStatefulAsync(handler, connection, redisURI);

        future.whenComplete((channelHandler, throwable) -> {

            if (throwable != null) {
                connection.close();
            }
        });

        return future;
    }

    @SuppressWarnings("unchecked")
    private <K, V, T extends RedisChannelHandler<K, V>, S> ConnectionFuture<S> connectStatefulAsync(
            CommandHandler<K, V> handler, StatefulRedisConnectionImpl<K, V> connection, RedisURI redisURI) {

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
        connectionBuilder(handler, connection, getSocketAddressSupplier(redisURI), connectionBuilder, redisURI);
        channelType(connectionBuilder, redisURI);

        if (clientOptions.isPingBeforeActivateConnection()) {
            if (hasPassword(redisURI)) {
                connectionBuilder.enableAuthPingBeforeConnect();
            } else {
                connectionBuilder.enablePingBeforeConnect();
            }
        }

        ConnectionFuture<RedisChannelHandler<K, V>> future = initializeChannelAsync(connectionBuilder);

        if (!clientOptions.isPingBeforeActivateConnection() && hasPassword(redisURI)) {

            future = future.thenApplyAsync(channelHandler -> {

                connection.async().auth(new String(redisURI.getPassword()));

                return channelHandler;
            }, clientResources.eventExecutorGroup());
        }

        if (LettuceStrings.isNotEmpty(redisURI.getClientName())) {
            future.thenApply(channelHandler -> {
                connection.setClientName(redisURI.getClientName());
                return channelHandler;
            });

        }

        if (redisURI.getDatabase() != 0) {

            future = future.thenApplyAsync(channelHandler -> {

                connection.async().select(redisURI.getDatabase());

                return channelHandler;
            }, clientResources.eventExecutorGroup());
        }

        return future.thenApply(channelHandler -> (S) connection);
    }

    private boolean hasPassword(RedisURI redisURI) {
        return redisURI.getPassword() != null && redisURI.getPassword().length != 0;
    }

    /**
     * Open a new pub/sub connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful pub/sub connection
     */
    public StatefulRedisPubSubConnection<String, String> connectPubSub() {
        return connectPubSub(newStringStringCodec(), redisURI, defaultTimeout());
    }

    /**
     * Open a new pub/sub connection to a Redis server using the supplied {@link RedisURI} that treats keys and values as UTF-8
     * strings.
     *
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @return A new stateful pub/sub connection
     */
    public StatefulRedisPubSubConnection<String, String> connectPubSub(RedisURI redisURI) {
        return connectPubSub(newStringStringCodec(), redisURI, Timeout.from(redisURI));
    }

    /**
     * Open a new pub/sub connection to the Redis server using the supplied {@link RedisURI} and use the supplied
     * {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful pub/sub connection
     */
    public <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {
        checkForRedisURI();
        return connectPubSub(codec, redisURI, defaultTimeout());
    }

    /**
     * Open a new pub/sub connection to the Redis server using the supplied {@link RedisURI} and use the supplied
     * {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param redisURI the redis server to connect to, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     */
    public <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec, RedisURI redisURI) {
        return connectPubSub(codec, redisURI, Timeout.from(redisURI));
    }

    private <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec, RedisURI redisURI, Timeout timeout) {

        assertNotNull(codec);
        checkValidRedisURI(redisURI);

        PubSubCommandHandler<K, V> handler = new PubSubCommandHandler<>(clientOptions, clientResources, codec);
        StatefulRedisPubSubConnectionImpl<K, V> connection = newStatefulRedisPubSubConnection(handler, codec, timeout.timeout,
                timeout.timeUnit);

        ConnectionFuture<StatefulRedisConnectionImpl<K, V>> future = connectStatefulAsync(handler, connection, redisURI);

        getConnection(future.whenComplete((conn, throwable) -> {

            if (throwable != null) {
                conn.close();
            }
        }));

        return connection;
    }

    /**
     * Open a connection to a Redis Sentinel that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis Sentinel connection
     */
    public StatefulRedisSentinelConnection<String, String> connectSentinel() {
        return connectSentinel(newStringStringCodec());
    }

    /**
     * Open a connection to a Redis Sentinel that treats keys and use the supplied {@link RedisCodec codec} to encode/decode
     * keys and values. The client {@link RedisURI} must contain one or more sentinels.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis Sentinel connection
     */
    public <K, V> StatefulRedisSentinelConnection<K, V> connectSentinel(RedisCodec<K, V> codec) {
        checkForRedisURI();
        return connectSentinel(codec, redisURI, defaultTimeout());
    }

    /**
     * Open a connection to a Redis Sentinel using the supplied {@link RedisURI} that treats keys and values as UTF-8 strings.
     * The client {@link RedisURI} must contain one or more sentinels.
     *
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @return A new connection
     */
    public StatefulRedisSentinelConnection<String, String> connectSentinel(RedisURI redisURI) {
        return connectSentinel(newStringStringCodec(), redisURI, Timeout.from(redisURI));
    }

    /**
     * Open a connection to a Redis Sentinel using the supplied {@link RedisURI} and use the supplied {@link RedisCodec codec}
     * to encode/decode keys and values. The client {@link RedisURI} must contain one or more sentinels.
     *
     * @param codec the Redis server to connect to, must not be {@literal null}
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     */
    public <K, V> StatefulRedisSentinelConnection<K, V> connectSentinel(RedisCodec<K, V> codec, RedisURI redisURI) {
        return connectSentinel(codec, redisURI, Timeout.from(redisURI));
    }

    /**
     * Open a new asynchronous connection to a Redis Sentinel that treats keys and values as UTF-8 strings. You must supply a
     * valid RedisURI containing one or more sentinels.
     *
     * @return a new connection
     * @deprecated Use {@code connectSentinel().async()}
     */
    @Deprecated
    public RedisSentinelAsyncCommands<String, String> connectSentinelAsync() {
        return connectSentinel(newStringStringCodec(), redisURI, defaultTimeout()).async();
    }

    /**
     * Open a new asynchronous connection to a Redis Sentinela nd use the supplied {@link RedisCodec codec} to encode/decode
     * keys and values. You must supply a valid RedisURI containing one or more sentinels.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return a new connection
     * @deprecated Use {@code connectSentinel(codec).async()}
     */
    @Deprecated
    public <K, V> RedisSentinelAsyncCommands<K, V> connectSentinelAsync(RedisCodec<K, V> codec) {
        checkForRedisURI();
        return connectSentinel(codec, redisURI, defaultTimeout()).async();
    }

    /**
     * Open a new asynchronous connection to a Redis Sentinel using the supplied {@link RedisURI} that treats keys and values as
     * UTF-8 strings. You must supply a valid RedisURI containing a redis host or one or more sentinels.
     *
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @return A new connection
     * @deprecated Use {@code connectSentinel(redisURI).async()}
     */
    @Deprecated
    public RedisSentinelAsyncCommands<String, String> connectSentinelAsync(RedisURI redisURI) {
        return connectSentinel(newStringStringCodec(), redisURI, Timeout.from(redisURI)).async();
    }

    /**
     * Open a new asynchronous connection to a Redis Sentinel using the supplied {@link RedisURI} and use the supplied
     * {@link RedisCodec codec} to encode/decode keys and values. You must supply a valid RedisURI containing a redis host or
     * one or more sentinels.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     * @deprecated Use {@code connectSentinel(codec, redisURI).async()}
     */
    @Deprecated
    public <K, V> RedisSentinelAsyncCommands<K, V> connectSentinelAsync(RedisCodec<K, V> codec, RedisURI redisURI) {
        return connectSentinel(codec, redisURI, Timeout.from(redisURI)).async();
    }

    private <K, V> StatefulRedisSentinelConnection<K, V> connectSentinel(RedisCodec<K, V> codec, RedisURI redisURI,
            Timeout timeout) {
        assertNotNull(codec);
        checkValidRedisURI(redisURI);


        ConnectionBuilder connectionBuilder = ConnectionBuilder.connectionBuilder();
        connectionBuilder.clientOptions(ClientOptions.copyOf(getOptions()));
        connectionBuilder.clientResources(clientResources);

        final CommandHandler<K, V> commandHandler = new CommandHandler<>(clientOptions, clientResources);

        StatefulRedisSentinelConnectionImpl<K, V> connection = newStatefulRedisSentinelConnection(commandHandler, codec,
                timeout.timeout, timeout.timeUnit);

        logger.debug("Trying to get a Redis Sentinel connection for one of: " + redisURI.getSentinels());

        connectionBuilder(commandHandler, connection, getSocketAddressSupplier(redisURI), connectionBuilder, redisURI);

        if (clientOptions.isPingBeforeActivateConnection()) {
            connectionBuilder.enablePingBeforeConnect();
        }

        if (redisURI.getSentinels().isEmpty() && (isNotEmpty(redisURI.getHost()) || !isEmpty(redisURI.getSocket()))) {
            channelType(connectionBuilder, redisURI);
            try {
                initializeChannel(connectionBuilder);
            } catch (RuntimeException e) {
                connection.close();
                throw e;
            }
        } else {

            boolean connected = false;
            boolean first = true;
            Exception causingException = null;
            validateUrisAreOfSameConnectionType(redisURI.getSentinels());

            for (RedisURI uri : redisURI.getSentinels()) {
                if (first) {
                    channelType(connectionBuilder, uri);
                    first = false;
                }
                connectionBuilder.socketAddressSupplier(getSocketAddressSupplier(uri));

                if (logger.isDebugEnabled()) {
                    SocketAddress socketAddress = SocketAddressResolver.resolve(redisURI, clientResources.dnsResolver());
                    logger.debug("Connecting to Redis Sentinel, address: " + socketAddress);
                }
                try {
                    initializeChannel(connectionBuilder);
                    connected = true;
                    break;
                } catch (Exception e) {
                    logger.warn("Cannot connect Redis Sentinel at " + uri + ": " + e.toString());
                    causingException = e;
                }
            }

            if (!connected) {
                connection.close();
                throw new RedisConnectionException("Cannot connect to a Redis Sentinel: " + redisURI.getSentinels(),
                        causingException);
            }
        }

        if (LettuceStrings.isNotEmpty(redisURI.getClientName())) {
            connection.setClientName(redisURI.getClientName());
        }

        return connection;
    }

    /**
     * Set the {@link ClientOptions} for the client.
     *
     * @param clientOptions the new client options
     * @throws IllegalArgumentException if {@literal clientOptions} is null
     */
    @Override
    public void setOptions(ClientOptions clientOptions) {
        super.setOptions(clientOptions);
    }

    /**
     * Returns the {@link ClientResources} which are used with that client.
     *
     * @return the {@link ClientResources} for this client
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
     * @param commandHandler the command handler
     * @param codec codec
     * @param <K> Key-Type
     * @param <V> Value Type
     * @return new instance of StatefulRedisPubSubConnectionImpl
     * @deprecated Use {@link #newStatefulRedisPubSubConnection(PubSubCommandHandler, RedisCodec, long, TimeUnit)}
     */
    @Deprecated
    protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(
            PubSubCommandHandler<K, V> commandHandler, RedisCodec<K, V> codec) {
        return newStatefulRedisPubSubConnection(commandHandler, codec, timeout, unit);
    }

    /**
     * Create a new instance of {@link StatefulRedisPubSubConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param commandHandler the command handler
     * @param codec codec
     * @param timeout default timeout
     * @param unit default timeout unit
     * @param <K> Key-Type
     * @param <V> Value Type
     * @return new instance of StatefulRedisPubSubConnectionImpl
     */
    protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(
            PubSubCommandHandler<K, V> commandHandler, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new StatefulRedisPubSubConnectionImpl<>(commandHandler, codec, timeout, unit);
    }

    /**
     * Create a new instance of {@link StatefulRedisSentinelConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param commandHandler the command handler
     * @param codec codec
     * @param <K> Key-Type
     * @param <V> Value Type
     * @return new instance of StatefulRedisSentinelConnectionImpl
     * @deprecated Use {@link #newStatefulRedisSentinelConnection(CommandHandler, RedisCodec, long, TimeUnit)}
     */
    @Deprecated
    protected <K, V> StatefulRedisSentinelConnectionImpl<K, V> newStatefulRedisSentinelConnection(
            CommandHandler<K, V> commandHandler, RedisCodec<K, V> codec) {
        return newStatefulRedisSentinelConnection(commandHandler, codec, timeout, unit);
    }

    /**
     * Create a new instance of {@link StatefulRedisSentinelConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param commandHandler the command handler
     * @param codec codec
     * @param timeout default timeout
     * @param unit default timeout unit
     * @param <K> Key-Type
     * @param <V> Value Type
     * @return new instance of StatefulRedisSentinelConnectionImpl
     */
    protected <K, V> StatefulRedisSentinelConnectionImpl<K, V> newStatefulRedisSentinelConnection(
            CommandHandler<K, V> commandHandler, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new StatefulRedisSentinelConnectionImpl<>(commandHandler, codec, timeout, unit);
    }

    /**
     * Create a new instance of {@link StatefulRedisConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param commandHandler the command handler
     * @param codec codec
     * @param <K> Key-Type
     * @param <V> Value Type
     * @return new instance of StatefulRedisConnectionImpl
     * @deprecated use {@link #newStatefulRedisConnection(CommandHandler, RedisCodec, long, TimeUnit)}
     */
    @Deprecated
    protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(CommandHandler<K, V> commandHandler,
            RedisCodec<K, V> codec) {
        return newStatefulRedisConnection(commandHandler, codec, timeout, unit);
    }

    /**
     * Create a new instance of {@link StatefulRedisConnectionImpl} or a subclass.
     * <p>
     * Subclasses of {@link RedisClient} may override that method.
     *
     * @param commandHandler the command handler
     * @param codec codec
     * @param timeout default timeout
     * @param unit default timeout unit
     * @param <K> Key-Type
     * @param <V> Value Type
     * @return new instance of StatefulRedisConnectionImpl
     */
    protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(CommandHandler<K, V> commandHandler,
            RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new StatefulRedisConnectionImpl<>(commandHandler, codec, timeout, unit);
    }

    /**
     * Resolve a {@link RedisURI} to a {@link SocketAddress}. Resolution is performed either using Redis Sentinel (if the
     * {@link RedisURI} is configured with Sentinels) or via DNS resolution.
     *
     * @param redisURI must not be {@literal null}.
     * @return the resolved {@link SocketAddress}.
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     * @see ClientResources#dnsResolver()
     * @see RedisURI#getSentinels()
     * @see RedisURI#getSentinelMasterId()
     */
    protected SocketAddress getSocketAddress(RedisURI redisURI) throws InterruptedException, TimeoutException,
            ExecutionException {
        SocketAddress redisAddress;

        if (redisURI.getSentinelMasterId() != null && !redisURI.getSentinels().isEmpty()) {
            logger.debug("Connecting to Redis using Sentinels {}, MasterId {}", redisURI.getSentinels(),
                    redisURI.getSentinelMasterId());
            redisAddress = lookupRedis(redisURI);

            if (redisAddress == null) {
                throw new RedisConnectionException("Cannot provide redisAddress using sentinel for masterId "
                        + redisURI.getSentinelMasterId());
            }

        } else {
            redisAddress = SocketAddressResolver.resolve(redisURI, clientResources.dnsResolver());
        }
        return redisAddress;
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

    private void validateUrisAreOfSameConnectionType(List<RedisURI> redisUris) {
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

    private Supplier<SocketAddress> getSocketAddressSupplier(final RedisURI redisURI) {
        return () -> {
            try {
                SocketAddress socketAddress = getSocketAddress(redisURI);
                logger.debug("Resolved SocketAddress {} using {}", socketAddress, redisURI);
                return socketAddress;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisCommandInterruptedException(e);
            } catch (TimeoutException | ExecutionException e) {
                throw new RedisException(e);
            }
        };
    }

    private SocketAddress lookupRedis(RedisURI sentinelUri) throws InterruptedException, TimeoutException, ExecutionException {
        RedisSentinelAsyncCommands<String, String> connection = connectSentinel(sentinelUri).async();
        try {
            return connection.getMasterAddrByName(sentinelUri.getSentinelMasterId()).get(timeout, unit);
        } finally {
            connection.close();
        }
    }

    private void checkValidRedisURI(RedisURI redisURI) {

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
        LettuceAssert
                .assertState(this.redisURI != EMPTY_URI,
                        "RedisURI is not available. Use RedisClient(Host), RedisClient(Host, Port) or RedisClient(RedisURI) to construct your client.");
        checkValidRedisURI(this.redisURI);
    }

    private void checkPoolDependency() {
        LettuceAssert.assertState(POOL_AVAILABLE,
                "Cannot use connection pooling without the optional Apache commons-pool2 library on the class path");
    }

    private Timeout defaultTimeout() {
        return Timeout.of(timeout, unit);
    }

    private static class Timeout {

        final long timeout;
        final TimeUnit timeUnit;

        private Timeout(long timeout, TimeUnit timeUnit) {
            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }

        private static Timeout of(long timeout, TimeUnit timeUnit) {
            return new Timeout(timeout, timeUnit);
        }

        private static Timeout from(RedisURI redisURI) {

            LettuceAssert.notNull(redisURI, "A valid RedisURI is needed");
            return new Timeout(redisURI.getTimeout(), redisURI.getUnit());
        }
    }
}
