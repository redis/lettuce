package io.lettuce.core.failover;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.health.HealthStatusManager;

/**
 * Factory interface for creating database connections in a multi-database client.
 *
 * @param <C> Connection type
 * @param <K> Key type
 * @param <V> Value type
 * 
 * @author Ali Takavci
 * @since 7.1
 */
@FunctionalInterface
interface DatabaseConnectionFactory<C extends StatefulRedisConnection<K, V>, K, V> {

    /**
     * Create a new database connection for the given configuration.
     *
     * @param config the database configuration
     * @param codec the codec to use for encoding/decoding
     * @return a new RedisDatabase instance
     */
    RedisDatabase<C> createDatabase(DatabaseConfig config, RedisCodec<K, V> codec, HealthStatusManager healthStatusManager);

}
