package io.lettuce.core.failover;

import java.io.Closeable;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.health.HealthCheck;

/**
 * Represents a Redis database with a weight and a connection.
 *
 * @param <C> Connection type.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public class RedisDatabase<C extends StatefulRedisConnection<?, ?>> implements Closeable {

    private final float weight;

    private final C connection;

    private final RedisURI redisURI;

    private final DatabaseEndpoint databaseEndpoint;

    private final CircuitBreaker circuitBreaker;

    private final HealthCheck healthCheck;

    public RedisDatabase(DatabaseConfig config, C connection, DatabaseEndpoint databaseEndpoint, CircuitBreaker circuitBreaker,
            HealthCheck healthCheck) {
        this.redisURI = config.getRedisURI();
        this.weight = config.getWeight();
        this.connection = connection;
        this.databaseEndpoint = databaseEndpoint;
        this.circuitBreaker = circuitBreaker;
        this.healthCheck = healthCheck;

    }

    public float getWeight() {
        return weight;
    }

    public C getConnection() {
        return connection;
    }

    public RedisURI getRedisURI() {
        return redisURI;
    }

    public DatabaseEndpoint getDatabaseEndpoint() {
        return databaseEndpoint;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Get the health check for this database.
     *
     * @return the health check, or null if health checks are not configured
     */
    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    @Override
    public void close() {
        connection.close();
        circuitBreaker.close();
    }

}
