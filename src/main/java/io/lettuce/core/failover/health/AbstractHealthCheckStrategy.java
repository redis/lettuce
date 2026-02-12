package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;

/**
 * Abstract base class for health check strategies.
 * 
 * <p>
 * This class provides a basic implementation of the {@link HealthCheckStrategy} interface. It can be extended to implement
 * custom health check strategies.
 * </p>
 * <p>
 * <ul>
 * <li>Application-specific health validation logic</li>
 * <li>Integration with external monitoring systems</li>
 * <li>Custom performance or latency-based health checks</li>
 * </ul>
 * 
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
@Experimental
public class AbstractHealthCheckStrategy implements HealthCheckStrategy {

    private final HealthCheckStrategy.Config config;

    public AbstractHealthCheckStrategy(HealthCheckStrategy.Config config) {
        this.config = config;
    }

    /**
     * Implement custom health check logic
     * 
     * @param endpoint the endpoint to check
     * @return one of HealthStatus.HEALTHY, HealthStatus.UNHEALTHY, or HealthStatus.UNKNOWN
     */
    @Override
    public HealthStatus doHealthCheck(RedisURI endpoint) {
        // Implement custom health check logic by overriding this method
        return HealthStatus.HEALTHY;
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
    public int getNumProbes() {
        return config.getNumProbes();
    }

    @Override
    public int getDelayInBetweenProbes() {
        return config.getDelayInBetweenProbes();
    }

    @Override
    public ProbingPolicy getPolicy() {
        return config.getPolicy();
    }

}
