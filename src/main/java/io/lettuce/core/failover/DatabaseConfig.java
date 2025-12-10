package io.lettuce.core.failover;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.PingStrategy;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Configuration for a database in a multi-database client. Holds the Redis URI, weight for load balancing, client options,
 * circuit breaker configuration, and optional health check strategy supplier.
 *
 * <p>
 * Example usage with builder:
 * </p>
 *
 * <pre>
 *
 * DatabaseConfig config = DatabaseConfig.builder(RedisURI.create("redis://localhost:6379")).weight(1.0f)
 *         .clientOptions(ClientOptions.create()).circuitBreakerConfig(CircuitBreakerConfig.DEFAULT)
 *         .healthCheckStrategySupplier(PingStrategy.DEFAULT).build();
 * </pre>
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class DatabaseConfig {

    private static final float DEFAULT_WEIGHT = 1.0f;

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
     * @param healthCheckStrategySupplier the health check strategy supplier, use
     *        {@link HealthCheckStrategySupplier#NO_HEALTH_CHECK} to disable health checks, must not be {@code null}
     */
    public DatabaseConfig(RedisURI redisURI, float weight, ClientOptions clientOptions,
            CircuitBreakerConfig circuitBreakerConfig, HealthCheckStrategySupplier healthCheckStrategySupplier) {
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");
        LettuceAssert.isTrue(weight > 0, "Weight must be greater than 0");
        LettuceAssert.notNull(healthCheckStrategySupplier, "HealthCheckStrategySupplier must not be null");

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
        this(redisURI, weight, clientOptions, circuitBreakerConfig, PingStrategy.DEFAULT);
    }

    /**
     * Create a new database configuration with default client options and health check strategy supplier.
     *
     * @param redisURI the Redis URI, must not be {@code null}
     * @param weight the weight for load balancing, must be greater than 0
     * @param clientOptions the client options, can be {@code null} to use defaults
     */
    public DatabaseConfig(RedisURI redisURI, float weight, ClientOptions clientOptions) {
        this(redisURI, weight, clientOptions, null, PingStrategy.DEFAULT);
    }

    /**
     * Create a new database configuration with default client options and health check strategy supplier.
     *
     * @param redisURI the Redis URI, must not be {@code null}
     * @param weight the weight for load balancing, must be greater than 0
     */
    public DatabaseConfig(RedisURI redisURI, float weight) {
        this(redisURI, weight, null, null, PingStrategy.DEFAULT);
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

    /**
     * Create a new {@link Builder} to construct {@link DatabaseConfig}.
     *
     * @param redisURI the Redis URI, must not be {@code null}
     * @return a new {@link Builder}
     * @since 7.4
     */
    public static Builder builder(RedisURI redisURI) {
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");
        return new Builder(redisURI);
    }

    /**
     * Create a {@link Builder} initialized with this {@link DatabaseConfig}'s settings.
     *
     * @return a new {@link Builder} initialized with this {@link DatabaseConfig}'s settings
     * @since 7.4
     */
    public Builder mutate() {
        Builder builder = new Builder(this.redisURI);
        builder.weight = this.weight;
        builder.clientOptions = this.clientOptions;
        builder.circuitBreakerConfig = this.circuitBreakerConfig;
        builder.healthCheckStrategySupplier = this.healthCheckStrategySupplier;
        return builder;
    }

    /**
     * Builder for {@link DatabaseConfig}.
     *
     * @since 7.4
     */
    public static class Builder {

        private final RedisURI redisURI;

        private float weight = DEFAULT_WEIGHT;

        private ClientOptions clientOptions;

        private CircuitBreakerConfig circuitBreakerConfig;

        private HealthCheckStrategySupplier healthCheckStrategySupplier = PingStrategy.DEFAULT;

        private Builder(RedisURI redisURI) {
            this.redisURI = redisURI;
        }

        /**
         * Set the weight for load balancing. Defaults to {@code 1.0}.
         *
         * @param weight the weight, must be greater than 0
         * @return {@code this} builder
         */
        public Builder weight(float weight) {
            LettuceAssert.isTrue(weight > 0, "Weight must be greater than 0");
            this.weight = weight;
            return this;
        }

        /**
         * Set the client options.
         *
         * @param clientOptions the client options, can be {@code null} to use defaults
         * @return {@code this} builder
         */
        public Builder clientOptions(ClientOptions clientOptions) {
            this.clientOptions = clientOptions;
            return this;
        }

        /**
         * Set the circuit breaker configuration.
         *
         * @param circuitBreakerConfig the circuit breaker configuration, can be {@code null} to use defaults
         * @return {@code this} builder
         */
        public Builder circuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
            this.circuitBreakerConfig = circuitBreakerConfig;
            return this;
        }

        /**
         * Set the health check strategy supplier. Defaults to {@link PingStrategy#DEFAULT}.
         *
         * @param healthCheckStrategySupplier the health check strategy supplier, use
         *        {@link HealthCheckStrategySupplier#NO_HEALTH_CHECK} to disable health checks, must not be {@code null}
         * @return {@code this} builder
         */
        public Builder healthCheckStrategySupplier(HealthCheckStrategySupplier healthCheckStrategySupplier) {
            LettuceAssert.notNull(healthCheckStrategySupplier, "HealthCheckStrategySupplier must not be null");
            this.healthCheckStrategySupplier = healthCheckStrategySupplier;
            return this;
        }

        /**
         * Build a new {@link DatabaseConfig} instance.
         *
         * @return a new {@link DatabaseConfig}
         */
        public DatabaseConfig build() {
            return new DatabaseConfig(redisURI, weight, clientOptions, circuitBreakerConfig, healthCheckStrategySupplier);
        }

    }

}
