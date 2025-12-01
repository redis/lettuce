package io.lettuce.core.failover;

import java.io.Closeable;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Represents a Redis database with a weight and a connection.
 *
 * @param <C> Connection type.
 * 
 * @author Ali Takavci
 * @since 7.1
 */
public class RedisDatabase<C extends StatefulRedisConnection<?, ?>> implements Closeable {

    private final float weight;

    private final C connection;

    private final RedisURI redisURI;

    private final DatabaseEndpoint databaseEndpoint;

    private final CircuitBreaker circuitBreaker;

    private HealthStatus healthStatus = HealthStatus.HEALTHY;

    public RedisDatabase(DatabaseConfig config, C connection, DatabaseEndpoint databaseEndpoint,
            CircuitBreaker circuitBreaker) {
        this.redisURI = config.getRedisURI();
        this.weight = config.getWeight();
        this.connection = connection;
        this.databaseEndpoint = databaseEndpoint;
        this.circuitBreaker = circuitBreaker;
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

    @Override
    public void close() {
        connection.close();
        circuitBreaker.close();
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

}
