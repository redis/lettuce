package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.DatabaseRawConnectionFactory;

public class PingStrategy implements HealthCheckStrategy {

    private final DatabaseRawConnectionFactory connectionFactory;

    private final HealthCheckStrategy.Config config;

    public PingStrategy(RedisURI redisURI, DatabaseRawConnectionFactory connectionFactory) {
        this(redisURI, connectionFactory, HealthCheckStrategy.Config.create());
    }

    public PingStrategy(RedisURI redisURI, DatabaseRawConnectionFactory connectionFactory, HealthCheckStrategy.Config config) {
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
        try (StatefulRedisConnection<?, ?> connection = connectionFactory.connectToDatabase(endpoint)) {

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

    public static final HealthCheckStrategySupplier DEFAULT = PingStrategy::new;

}
