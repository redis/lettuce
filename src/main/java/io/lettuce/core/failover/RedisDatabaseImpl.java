package io.lettuce.core.failover;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.AsyncCloseable;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.CircuitBreaker.State;
import io.lettuce.core.failover.api.RedisDatabase;
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
class RedisDatabaseImpl<C extends StatefulRedisConnection<?, ?>> implements RedisDatabase, AsyncCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RedisDatabaseImpl.class);

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    private final float weight;

    private final C connection;

    private final RedisURI redisURI;

    private final DatabaseEndpoint databaseEndpoint;

    private final CircuitBreaker circuitBreaker;

    private final HealthCheck healthCheck;

    private final String id;

    public RedisDatabaseImpl(DatabaseConfig config, C connection, DatabaseEndpoint databaseEndpoint,
            CircuitBreaker circuitBreaker, HealthCheck healthCheck) {

        this.id = config.getRedisURI().toString() + "-" + ID_COUNTER.getAndIncrement();
        this.redisURI = config.getRedisURI();
        this.weight = config.getWeight();
        this.connection = connection;
        this.databaseEndpoint = databaseEndpoint;
        this.circuitBreaker = circuitBreaker;
        this.healthCheck = healthCheck;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public float getWeight() {
        return weight;
    }

    C getConnection() {
        return connection;
    }

    @Override
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
    public CompletableFuture<Void> closeAsync() {
        CompletableFuture<Void> closeFuture = connection.closeAsync().whenComplete((v, t) -> {
            circuitBreaker.close();
        });
        closeFuture.exceptionally(exc -> {
            logger.error("Error while closing database :", exc);
            return null;
        });
        return closeFuture;
    }

    @Override
    public MetricsSnapshot getMetricsSnapshot() {
        return circuitBreaker.getSnapshot();
    }

    @Override
    public HealthStatus getHealthCheckStatus() {
        return healthCheck != null ? healthCheck.getStatus() : HealthStatus.HEALTHY;
    }

    @Override
    public State getCircuitBreakerState() {
        return circuitBreaker.getCurrentState();
    }

}
