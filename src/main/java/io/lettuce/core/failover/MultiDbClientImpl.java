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
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.api.ImmutableRedisURI;
import io.lettuce.core.failover.api.MultiDbConnectionFuture;
import io.lettuce.core.failover.api.MultiDbOptions;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
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

    private final Map<RedisURI, DatabaseConfig> databaseConfigMap;

    private final ThreadLocal<ClientOptions> localClientOptions = new ThreadLocal<>();

    private final MultiDbOptions multiDbOptions;

    MultiDbClientImpl(Collection<DatabaseConfig> databaseConfigs, MultiDbOptions multiDbOptions) {
        this(null, databaseConfigs, multiDbOptions);
    }

    MultiDbClientImpl(ClientResources clientResources, Collection<DatabaseConfig> databaseConfigs,
            MultiDbOptions multiDbOptions) {
        super(clientResources, EMPTY_URI);
        LettuceAssert.notNull(databaseConfigs, "DatabaseConfigs must not be null");
        LettuceAssert.noNullElements(databaseConfigs, "DatabaseConfigs must not contain null elements");
        LettuceAssert.notEmpty(databaseConfigs.toArray(), "DatabaseConfigs must not be empty");

        this.databaseConfigMap = new ConcurrentHashMap<>(databaseConfigs.size());
        for (DatabaseConfig config : databaseConfigs) {
            LettuceAssert.notNull(config.getRedisURI(), "RedisURI must not be null");
            this.databaseConfigMap.put(config.getRedisURI(), config);
        }
        LettuceAssert.notNull(multiDbOptions, "MultiDbOptions must not be null");
        this.multiDbOptions = multiDbOptions;
    }

    @Override
    public Collection<RedisURI> getRedisURIs() {
        return databaseConfigMap.keySet();
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
        localClientOptions.set(clientOptions);
    }

    /**
     * Resets the thread-local client options to use the default options.
     */
    void resetOptions() {
        localClientOptions.remove();
    }

    /**
     * Gets the multi-database options.
     *
     * @return the multi-database options
     */
    @Override
    public MultiDbOptions getMultiDbOptions() {
        return multiDbOptions;
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    @Override
    public StatefulRedisMultiDbConnection<String, String> connect() {
        return connect(newStringStringCodec());
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
    @Override
    public <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec) {
        MultiDbConnectionFuture<StatefulRedisMultiDbConnection<K, V>> connectionFuture = connectAsync(codec);
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
        LettuceAssert.notNull(codec, "codec must not be null");

        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbConnection<K, V>, StatefulRedisConnection<K, V>, K, V> builder = createConnectionBuilder(
                codec);

        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> future = builder.connectAsync(databaseConfigMap);

        return MultiDbConnectionFuture.from(future);
    }

    /**
     * Creates a new {@link AbstractRedisMultiDbConnectionBuilder} instance.
     *
     * @param codec the codec for encoding/decoding keys and values
     * @param <K> Key type
     * @param <V> Value type
     * @return a new multi-database async connection builder
     */
    protected <K, V> MultiDbAsyncConnectionBuilder<K, V> createConnectionBuilder(RedisCodec<K, V> codec) {
        return new MultiDbAsyncConnectionBuilder<>(this, getResources(), codec, closeableResources, multiDbOptions);
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    @Override
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
        MultiDbConnectionFuture<StatefulRedisMultiDbPubSubConnection<K, V>> connectionFuture = connectPubSubAsync(codec);
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
    public MultiDbConnectionFuture<StatefulRedisMultiDbPubSubConnection<String, String>> connectPubSubAsync() {
        return connectPubSubAsync(newStringStringCodec());
    }

    @Override
    public <K, V> MultiDbConnectionFuture<StatefulRedisMultiDbPubSubConnection<K, V>> connectPubSubAsync(
            RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "codec must not be null");

        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbPubSubConnection<K, V>, StatefulRedisPubSubConnection<K, V>, K, V> builder = createPubSubConnectionBuilder(
                codec);

        CompletableFuture<StatefulRedisMultiDbPubSubConnection<K, V>> future = builder.connectAsync(databaseConfigMap);

        return MultiDbConnectionFuture.from(future);
    }

    /**
     * Creates a new pub/sub connection builder.
     *
     * @param codec the codec for encoding/decoding keys and values
     * @param <K> Key type
     * @param <V> Value type
     * @return a new multi-database async pub/sub connection builder
     */
    protected <K, V> MultiDbAsyncPubSubConnectionBuilder<K, V> createPubSubConnectionBuilder(RedisCodec<K, V> codec) {
        return new MultiDbAsyncPubSubConnectionBuilder<>(this, getResources(), codec, closeableResources, multiDbOptions);
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
