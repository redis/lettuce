package io.lettuce.core.failover;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.failover.health.HealthStatusManagerImpl;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * Failover-aware client that composes multiple standalone Redis endpoints and returns a single Stateful connection wrapper
 * which can switch the active endpoint without requiring users to recreate command objects.
 *
 * <p>
 * Standalone-only POC. Not for Sentinel/Cluster.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.4
 */
class MultiDbClientImpl extends RedisClient implements MultiDbClient {

    private static final RedisURI EMPTY_URI = new ImmutableRedisURI(new RedisURI());

    private final Map<RedisURI, DatabaseConfig> databaseConfigs;

    private ThreadLocal<ClientOptions> localClientOptions = new ThreadLocal<>();

    MultiDbClientImpl(Collection<DatabaseConfig> databaseConfigs) {
        this(null, databaseConfigs);
    }

    MultiDbClientImpl(ClientResources clientResources, Collection<DatabaseConfig> databaseConfigs) {
        super(clientResources, EMPTY_URI);

        if (databaseConfigs == null || databaseConfigs.isEmpty()) {
            this.databaseConfigs = new ConcurrentHashMap<>();
        } else {
            this.databaseConfigs = new ConcurrentHashMap<>(databaseConfigs.size());
            for (DatabaseConfig config : databaseConfigs) {
                LettuceAssert.notNull(config, "DatabaseConfig must not be null");
                LettuceAssert.notNull(config.getRedisURI(), "RedisURI must not be null");
                this.databaseConfigs.put(config.getRedisURI(), config);
            }
        }
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    public StatefulRedisMultiDbConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    @Override
    public Collection<RedisURI> getRedisURIs() {
        return databaseConfigs.keySet();
    }

    @Override
    public ClientOptions getOptions() {
        ClientOptions options = localClientOptions.get();
        if (options == null) {
            throw new IllegalStateException("ClientOptions not set!");
        }
        return options;
    }

    @Override
    public void setOptions(ClientOptions clientOptions) {
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        this.localClientOptions.set(clientOptions);
    }

    void resetOptions() {
        this.localClientOptions.remove();
    }

    /**
     * Open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     * <p>
     * This method is synchronous and will block until at least one healthy database is available. It initiates asynchronous
     * connections to all configured databases and waits for health checks to complete, selecting the highest-weighted healthy
     * database as the initial primary. Other database connections continue to be established in the background.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection
     */
    public <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbConnection<K, V>, StatefulRedisConnection<K, V>, K, V> builder = createConnectionBuilder(
                codec);

        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> future = builder.connectAsync(databaseConfigs);

        MultiDbConnectionFuture<StatefulRedisMultiDbConnection<K, V>> connectionFuture = MultiDbConnectionFuture
                .from((CompletableFuture<StatefulRedisMultiDbConnection<K, V>>) future, getResources().eventExecutorGroup());
        try {
            return connectionFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(e);
        } catch (Exception e) {
            throw RedisConnectionException.create(Exceptions.unwrap(e));
        }
    }

    protected HealthStatusManager createHealthStatusManager() {
        return new HealthStatusManagerImpl();
    }

    /**
     * Asynchronously open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     * <p>
     * This method is asynchronous and returns a {@link MultiDbConnectionFuture} that completes when at least one healthy
     * database is available. It initiates asynchronous connections to all configured databases and waits for health checks to
     * complete, selecting the highest-weighted healthy database as the initial primary. Other database connections continue to
     * be established in the background.
     * <p>
     * The returned {@link MultiDbConnectionFuture} ensures that all callbacks (thenApply, thenAccept, etc.) execute on a
     * separate thread pool rather than on Netty event loop threads, preventing deadlocks when calling blocking sync operations
     * inside callbacks.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection future
     */
    @Override
    public <K, V> MultiDbConnectionFuture<StatefulRedisMultiDbConnection<K, V>> connectAsync(RedisCodec<K, V> codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbConnection<K, V>, StatefulRedisConnection<K, V>, K, V> builder = createConnectionBuilder(
                codec);

        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> future = builder.connectAsync(databaseConfigs);

        return MultiDbConnectionFuture.from(future, getResources().eventExecutorGroup());
    }

    /**
     * Asynchronously open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     * <p>
     * The returned {@link MultiDbConnectionFuture} ensures that all callbacks (thenApply, thenAccept, etc.) execute on a
     * separate thread pool rather than on Netty event loop threads, preventing deadlocks when calling blocking sync operations
     * inside callbacks.
     *
     * @return A new stateful Redis connection future
     */
    @Override
    public MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> connectAsync() {
        return connectAsync(newStringStringCodec());
    }

    /**
     * Creates a new {@link AbstractRedisMultiDbConnectionBuilder} instance.
     *
     * @param <K> Key type
     * @param <V> Value type
     * @return a new multi-database async connection builder
     */
    protected <K, V> MultiDbAsyncConnectionBuilder<K, V> createConnectionBuilder(RedisCodec<K, V> codec) {

        return new MultiDbAsyncConnectionBuilder<>(this, getResources(), codec);
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    public StatefulRedisMultiDbPubSubConnection<String, String> connectPubSub() {
        return connectPubSub(newStringStringCodec());
    }

    /**
     * Open a new pub/sub connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful pub/sub connection
     */
    @Override
    public <K, V> StatefulRedisMultiDbPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbPubSubConnection<K, V>, StatefulRedisPubSubConnection<K, V>, K, V> builder = createPubSubConnectionBuilder(
                codec);

        CompletableFuture<StatefulRedisMultiDbPubSubConnection<K, V>> future = builder.connectAsync(databaseConfigs);

        MultiDbConnectionFuture<StatefulRedisMultiDbPubSubConnection<K, V>> connectionFuture = MultiDbConnectionFuture
                .from(future, getResources().eventExecutorGroup());
        try {
            return (StatefulRedisMultiDbPubSubConnection<K, V>) connectionFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(e);
        } catch (Exception e) {
            throw RedisConnectionException.create(Exceptions.unwrap(e));
        }
    }

    @Override
    public <K, V> MultiDbConnectionFuture<StatefulRedisMultiDbPubSubConnection<K, V>> connectPubSubAsync(
            RedisCodec<K, V> codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbPubSubConnection<K, V>, StatefulRedisPubSubConnection<K, V>, K, V> builder = createPubSubConnectionBuilder(
                codec);

        CompletableFuture<StatefulRedisMultiDbPubSubConnection<K, V>> future = builder.connectAsync(databaseConfigs);

        return MultiDbConnectionFuture.from(future, getResources().eventExecutorGroup());
    }

    @Override
    public MultiDbConnectionFuture<StatefulRedisMultiDbPubSubConnection<String, String>> connectPubSubAsync() {
        return connectPubSubAsync(newStringStringCodec());
    }

    protected <K, V> MultiDbAsyncPubSubConnectionBuilder<K, V> createPubSubConnectionBuilder(RedisCodec<K, V> codec) {

        return new MultiDbAsyncPubSubConnectionBuilder<>(this, getResources(), codec);
    }

    @Override
    protected DefaultEndpoint createEndpoint() {
        return new DatabaseEndpointImpl(getOptions(), getResources());
    }

    @Override
    protected <K, V> PubSubEndpoint<K, V> createPubSubEndpoint() {
        return new DatabasePubSubEndpointImpl<>(getOptions(), getResources());
    }

}
