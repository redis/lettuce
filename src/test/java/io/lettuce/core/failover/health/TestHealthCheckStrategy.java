package io.lettuce.core.failover.health;

import java.util.function.Function;

import io.lettuce.core.RedisURI;

/**
 * Test implementation of {@link HealthCheckStrategy} for unit testing.
 * <p>
 * This class allows tests to provide custom health check logic via a function, making it easy to simulate various health check
 * scenarios.
 *
 * @author Ivo Gaydazhiev
 */
class TestHealthCheckStrategy implements HealthCheckStrategy {

    private final HealthCheckStrategy.Config config;

    private final Function<RedisURI, HealthStatus> healthCheckFunction;

    /**
     * Creates a new test health check strategy.
     *
     * @param config the health check configuration
     * @param healthCheckFunction the function to execute for health checks
     */
    TestHealthCheckStrategy(HealthCheckStrategy.Config config, Function<RedisURI, HealthStatus> healthCheckFunction) {
        this.config = config;
        this.healthCheckFunction = healthCheckFunction;
    }

    @Override
    public int getInterval() {
        return config.getInterval();
    }

    @Override
    public int getTimeout() {
        return config.getTimeout();
    }

    @Override
    public HealthStatus doHealthCheck(RedisURI endpoint) {
        return healthCheckFunction.apply(endpoint);
    }

    @Override
    public int getNumProbes() {
        return config.getNumProbes();
    }

    @Override
    public ProbingPolicy getPolicy() {
        return config.getPolicy();
    }

    @Override
    public int getDelayInBetweenProbes() {
        return config.getDelayInBetweenProbes();
    }

}
