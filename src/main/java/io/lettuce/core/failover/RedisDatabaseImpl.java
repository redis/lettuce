package io.lettuce.core.failover;

import java.time.Clock;
import java.time.Duration;
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

    private final Clock clock;

    /**
     * Timestamp (in milliseconds) when the grace period ends. A value of 0 means no grace period is active. During the grace
     * period, this database cannot be selected for failover or failback.
     */
    private volatile long gracePeriodEndTime = 0;

    public RedisDatabaseImpl(DatabaseConfig config, C connection, DatabaseEndpoint databaseEndpoint,
            CircuitBreaker circuitBreaker, HealthCheck healthCheck) {
        this(config, connection, databaseEndpoint, circuitBreaker, healthCheck, Clock.systemUTC());
    }

    public RedisDatabaseImpl(DatabaseConfig config, C connection, DatabaseEndpoint databaseEndpoint,
            CircuitBreaker circuitBreaker, HealthCheck healthCheck, Clock clock) {

        this.id = config.getRedisURI().toString() + "-" + ID_COUNTER.getAndIncrement();
        this.redisURI = config.getRedisURI();
        this.weight = config.getWeight();
        this.connection = connection;
        this.databaseEndpoint = databaseEndpoint;
        this.circuitBreaker = circuitBreaker;
        this.healthCheck = healthCheck;
        this.clock = clock;
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

    boolean isHealthy() {
        return !isInGracePeriod() && getHealthCheckStatus().isHealthy() && getCircuitBreakerState().isClosed();
    }

    boolean isHealthyIgnoreGracePeriod() {
        return getHealthCheckStatus().isHealthy() && getCircuitBreakerState().isClosed();
    }

    /**
     * Starts a grace period for this database. During the grace period, this database cannot be selected for failover or
     * failback.
     *
     * @param durationMillis the duration of the grace period in milliseconds
     */
    void startGracePeriod(Duration durationMillis) {
        if (durationMillis.toMillis() > 0) {
            long endTime = clock.millis() + durationMillis.toMillis();
            // this is against a possible overflow
            this.gracePeriodEndTime = endTime < 0 ? Long.MAX_VALUE : endTime;
            logger.info("Started grace period of {}ms for database {}", durationMillis, getId());
        }
    }

    /**
     * Checks if this database is currently in a grace period.
     *
     * @return {@code true} if the database is in a grace period, {@code false} otherwise
     */
    boolean isInGracePeriod() {
        long endTime = this.gracePeriodEndTime;
        if (endTime == 0) {
            return false;
        }
        boolean inGracePeriod = clock.millis() < endTime;
        if (!inGracePeriod) {
            // Grace period has ended, reset the end time
            clearGracePeriod();
            circuitBreaker.transitionTo(CircuitBreaker.State.CLOSED);
        }
        return inGracePeriod;
    }

    /**
     * Clears the grace period for this database.
     */
    void clearGracePeriod() {
        this.gracePeriodEndTime = 0;
    }

}
