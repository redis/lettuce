package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.api.RawConnectionFactory;

/**
 * Health check strategy that uses PING command to check endpoint health.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public class PingStrategy implements HealthCheckStrategy {

    private final RawConnectionFactory connectionFactory;

    private final HealthCheckStrategy.Config config;

    public PingStrategy(RawConnectionFactory connectionFactory) {
        this(connectionFactory, HealthCheckStrategy.Config.create());
    }

    public PingStrategy(RawConnectionFactory connectionFactory, HealthCheckStrategy.Config config) {
        this.connectionFactory = connectionFactory;
        this.config = config;
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
    public ProbingPolicy getPolicy() {
        return config.getPolicy();
    }

    @Override
    public int getDelayInBetweenProbes() {
        return config.getDelayInBetweenProbes();
    }

    @Override
    public HealthStatus doHealthCheck(RedisURI endpoint) {
        try (StatefulRedisConnection<?, ?> connection = connectionFactory.create(endpoint)) {

            if (connection == null) {
                return HealthStatus.UNHEALTHY;
            }

            return "PONG".equals(connection.sync().ping()) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
        } catch (Exception e) {
            return HealthStatus.UNHEALTHY;
        }
    }

    @Override
    public void close() {
        // No resources to close
    }

    public static final HealthCheckStrategySupplier DEFAULT = (uri, connectionFactory) -> new PingStrategy(connectionFactory);

}
