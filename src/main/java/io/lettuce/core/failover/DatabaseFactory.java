package io.lettuce.core.failover;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.health.HealthStatusManager;

/**
 * Factory interface for creating database connections in a multi-database client.
 *
 * @param <C> Connection type
 * @param <K> Key type
 * @param <V> Value type
 *
 * @author Ali Takavci
 * @since 7.4
 */
@FunctionalInterface
interface DatabaseFactory<C extends StatefulRedisConnection<K, V>, K, V> {

    /**
     * Create a new database connection for the given configuration.
     *
     * @param config the database configuration
     * @param codec the codec to use for encoding/decoding
     * @return a new RedisDatabase instance
     */
    CompletableFuture<RedisDatabaseImpl<C>> createDatabaseAsync(DatabaseConfig config, HealthStatusManager healthStatusManager);

}
