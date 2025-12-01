package io.lettuce.core.failover;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.lettuce.core.Delegating;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * Failover-aware client that composes multiple standalone Redis endpoints and returns a single Stateful connection wrapper
 * which can switch the active endpoint without requiring users to recreate command objects.
 *
 * Standalone-only POC. Not for Sentinel/Cluster.
 *
 * @author Ali Takavci
 * @since 7.1
 */
class MultiDbClientImpl extends RedisClient implements MultiDbClient {

    private static final RedisURI EMPTY_URI = new RedisURI();

    private final Map<RedisURI, DatabaseConfig> databaseConfigs;

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

    public <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        Map<RedisURI, RedisDatabase<StatefulRedisConnection<K, V>>> databases = new ConcurrentHashMap<>(databaseConfigs.size());
        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            // HACK: looks like repeating the implementation all around 'RedisClient.connect' is an overkill.
            // connections.put(uri, connect(codec, uri));
            // Instead we will use it from delegate
            RedisDatabase<StatefulRedisConnection<K, V>> database = createRedisDatabase(config, codec);

            databases.put(uri, database);
        }

        // Provide a connection factory for dynamic database addition
        return new StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<K, V>, K, V>(databases, getResources(), codec,
                getOptions().getJsonParser(), this::createRedisDatabase);
    }

    private <K, V> RedisDatabase<StatefulRedisConnection<K, V>> createRedisDatabase(DatabaseConfig config,
            RedisCodec<K, V> codec) {
        RedisURI uri = config.getRedisURI();
        StatefulRedisConnection<K, V> connection = connect(codec, uri);
        DatabaseEndpoint databaseEndpoint = extractDatabaseEndpoint(connection);
        CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config.getCircuitBreakerConfig());
        databaseEndpoint.bind(circuitBreaker);
        RedisDatabase<StatefulRedisConnection<K, V>> database = new RedisDatabase<>(config, connection, databaseEndpoint,
                circuitBreaker);

        return database;
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    public StatefulRedisMultiDbPubSubConnection<String, String> connectPubSub() {
        return connectPubSub(newStringStringCodec());
    }

    public <K, V> StatefulRedisMultiDbPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        Map<RedisURI, RedisDatabase<StatefulRedisPubSubConnection<K, V>>> databases = new ConcurrentHashMap<>(
                databaseConfigs.size());
        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            RedisDatabase<StatefulRedisPubSubConnection<K, V>> database = createRedisDatabaseWithPubSub(config, codec);
            databases.put(uri, database);
        }

        // Provide a connection factory for dynamic database addition
        return new StatefulRedisMultiDbPubSubConnectionImpl<K, V>(databases, getResources(), codec,
                getOptions().getJsonParser(), this::createRedisDatabaseWithPubSub);
    }

    private <K, V> RedisDatabase<StatefulRedisPubSubConnection<K, V>> createRedisDatabaseWithPubSub(DatabaseConfig config,
            RedisCodec<K, V> codec) {
        RedisURI uri = config.getRedisURI();
        StatefulRedisPubSubConnection<K, V> connection = connectPubSub(codec, uri);
        DatabaseEndpoint databaseEndpoint = extractDatabaseEndpoint(connection);
        CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config.getCircuitBreakerConfig());
        databaseEndpoint.bind(circuitBreaker);
        RedisDatabase<StatefulRedisPubSubConnection<K, V>> database = new RedisDatabase<>(config, connection, databaseEndpoint,
                circuitBreaker);
        return database;
    }

    private DatabaseEndpoint extractDatabaseEndpoint(StatefulRedisConnection<?, ?> connection) {
        RedisChannelWriter writer = ((StatefulRedisConnectionImpl<?, ?>) connection).getChannelWriter();
        if (writer instanceof Delegating) {
            writer = (RedisChannelWriter) ((Delegating<?>) writer).unwrap();
        }
        return (DatabaseEndpoint) writer;
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
