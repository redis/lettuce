package io.lettuce.core.failover;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.CircuitBreaker.State;
import io.lettuce.core.failover.api.BaseRedisDatabase;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.metrics.MetricsSnapshot;

/**
 * Represents a Redis database with a weight and a connection.
 *
 * @param <C> Connection type.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public class RedisDatabase<C extends StatefulRedisConnection<?, ?>> implements BaseRedisDatabase, Closeable {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    private final float weight;

    private final C connection;

    private final RedisURI redisURI;

    private final DatabaseEndpoint databaseEndpoint;

    private final CircuitBreaker circuitBreaker;

    private final HealthCheck healthCheck;

    private final String id;

    public RedisDatabase(DatabaseConfig config, C connection, DatabaseEndpoint databaseEndpoint, CircuitBreaker circuitBreaker,
            HealthCheck healthCheck) {

        this.id = config.getRedisURI().toString() + "-" + ID_COUNTER.getAndIncrement();
        this.redisURI = config.getRedisURI();
        this.weight = config.getWeight();
        this.connection = connection;
        this.databaseEndpoint = databaseEndpoint;
        this.circuitBreaker = circuitBreaker;
        this.healthCheck = healthCheck;
    }

    public String getId() {
        return id;
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

    @Override
    public MetricsSnapshot getMetricsSnapshot() {
        return circuitBreaker.getSnapshot();
    }

    @Override
    public HealthStatus getHealthCheckStatus() {
        return healthCheck != null ? healthCheck.getStatus() : HealthStatus.UNKNOWN;
    }

    @Override
    public State getCircuitBreakerState() {
        return circuitBreaker.getCurrentState();
    }

}
