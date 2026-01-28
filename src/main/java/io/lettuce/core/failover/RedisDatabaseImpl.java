package io.lettuce.core.failover;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.CircuitBreaker.State;
import io.lettuce.core.failover.api.RedisDatabase;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.metrics.MetricsSnapshot;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Represents a Redis database with a weight and a connection.
 *
 * @param <C> Connection type.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
class RedisDatabaseImpl<C extends StatefulRedisConnection<?, ?>> implements RedisDatabase, Closeable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisDatabaseImpl.class);

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    private final float weight;

    private final C connection;

    private final RedisURI redisURI;

    private final DatabaseEndpoint databaseEndpoint;

    private final CircuitBreaker circuitBreaker;

    private final HealthCheck healthCheck;

    private final String id;

    /**
     * Timestamp (in milliseconds) when the grace period ends. A value of 0 means no grace period is active. During the grace
     * period, this database cannot be selected for failover or failback.
     */
    private volatile long gracePeriodEndTime = 0;

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
    void startGracePeriod(long durationMillis) {
        if (durationMillis > 0) {
            this.gracePeriodEndTime = System.currentTimeMillis() + durationMillis;
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
        boolean inGracePeriod = System.currentTimeMillis() < endTime;
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
