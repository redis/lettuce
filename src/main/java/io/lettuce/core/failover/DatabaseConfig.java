package io.lettuce.core.failover;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Configuration for a database in a multi-database client. Holds the Redis URI, weight for load balancing, client options,
 * circuit breaker configuration, and optional health check strategy supplier.
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class DatabaseConfig {

    private final RedisURI redisURI;

    private final float weight;

    private final ClientOptions clientOptions;

    private final CircuitBreakerConfig circuitBreakerConfig;

    private final HealthCheckStrategySupplier healthCheckStrategySupplier;

    /**
     * Create a new database configuration.
     *
     * @param redisURI the Redis URI, must not be {@code null}
     * @param weight the weight for load balancing, must be greater than 0
     * @param clientOptions the client options, can be {@code null} to use defaults
     * @param circuitBreakerConfig the circuit breaker configuration, can be {@code null} to use defaults
     * @param healthCheckStrategySupplier the health check strategy supplier, can be {@code null} to disable health checks
     */
    public DatabaseConfig(RedisURI redisURI, float weight, ClientOptions clientOptions,
            CircuitBreakerConfig circuitBreakerConfig, HealthCheckStrategySupplier healthCheckStrategySupplier) {
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");
        LettuceAssert.isTrue(weight > 0, "Weight must be greater than 0");

        this.redisURI = redisURI;
        this.weight = weight;
        this.clientOptions = clientOptions;
        this.circuitBreakerConfig = circuitBreakerConfig != null ? circuitBreakerConfig : CircuitBreakerConfig.DEFAULT;
        this.healthCheckStrategySupplier = healthCheckStrategySupplier;
    }

    /**
     * Create a new database configuration with default health check strategy supplier.
     *
     * @param redisURI the Redis URI, must not be {@code null}
     * @param weight the weight for load balancing, must be greater than 0
     * @param clientOptions the client options, can be {@code null} to use defaults
     * @param circuitBreakerConfig the circuit breaker configuration, can be {@code null} to use defaults
     */
    public DatabaseConfig(RedisURI redisURI, float weight, ClientOptions clientOptions,
            CircuitBreakerConfig circuitBreakerConfig) {
        this(redisURI, weight, clientOptions, circuitBreakerConfig, null);
    }

    /**
     * Create a new database configuration with default client options and health check strategy supplier.
     *
     * @param redisURI the Redis URI, must not be {@code null}
     * @param weight the weight for load balancing, must be greater than 0
     * @param clientOptions the client options, can be {@code null} to use defaults
     */
    public DatabaseConfig(RedisURI redisURI, float weight, ClientOptions clientOptions) {
        this(redisURI, weight, clientOptions, null, null);
    }

    /**
     * Create a new database configuration with default client options and health check strategy supplier.
     *
     * @param redisURI the Redis URI, must not be {@code null}
     * @param weight the weight for load balancing, must be greater than 0
     */
    public DatabaseConfig(RedisURI redisURI, float weight) {
        this(redisURI, weight, null, null, null);
    }

    /**
     * Get the Redis URI.
     *
     * @return the Redis URI
     */
    public RedisURI getRedisURI() {
        return redisURI;
    }

    /**
     * Get the weight for load balancing.
     *
     * @return the weight
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Get the client options.
     *
     * @return the client options, can be {@code null}
     */
    public ClientOptions getClientOptions() {
        return clientOptions;
    }

    /**
     * Get the circuit breaker configuration.
     *
     * @return the circuit breaker configuration
     */
    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    /**
     * Get the health check strategy supplier.
     *
     * @return the health check strategy supplier, can be {@code null}
     */
    public HealthCheckStrategySupplier getHealthCheckStrategySupplier() {
        return healthCheckStrategySupplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DatabaseConfig))
            return false;

        DatabaseConfig that = (DatabaseConfig) o;

        if (Float.compare(that.weight, weight) != 0)
            return false;
        if (!redisURI.equals(that.redisURI))
            return false;
        if (clientOptions != null ? !clientOptions.equals(that.clientOptions) : that.clientOptions != null)
            return false;
        if (circuitBreakerConfig != null ? !circuitBreakerConfig.equals(that.circuitBreakerConfig)
                : that.circuitBreakerConfig != null)
            return false;
        return healthCheckStrategySupplier != null ? healthCheckStrategySupplier.equals(that.healthCheckStrategySupplier)
                : that.healthCheckStrategySupplier == null;
    }

    @Override
    public int hashCode() {
        int result = redisURI.hashCode();
        result = 31 * result + (weight != +0.0f ? Float.floatToIntBits(weight) : 0);
        result = 31 * result + (clientOptions != null ? clientOptions.hashCode() : 0);
        result = 31 * result + (circuitBreakerConfig != null ? circuitBreakerConfig.hashCode() : 0);
        result = 31 * result + (healthCheckStrategySupplier != null ? healthCheckStrategySupplier.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" + "redisURI=" + redisURI + ", weight=" + weight + ", clientOptions=" + clientOptions
                + ", circuitBreakerConfig=" + circuitBreakerConfig + ", healthCheckStrategySupplier="
                + healthCheckStrategySupplier + '}';
    }

}
