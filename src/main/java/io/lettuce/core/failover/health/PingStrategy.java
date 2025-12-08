package io.lettuce.core.failover.health;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.DatabaseConnectionProvider;

public class PingStrategy implements HealthCheckStrategy {

    private final DatabaseConnectionProvider connectionProvider;

    private final HealthCheckStrategy.Config config;

    public PingStrategy(RedisURI redisURI, DatabaseConnectionProvider connectionProvider) {
        this(redisURI, connectionProvider, HealthCheckStrategy.Config.create());
    }

    public PingStrategy(RedisURI redisURI, DatabaseConnectionProvider connectionProvider, HealthCheckStrategy.Config config) {
        this.connectionProvider = connectionProvider;
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
        try {
            // Reuse existing connection from MultiDbClient
            StatefulRedisConnection<?, ?> rawConnection = connectionProvider.getConnection(endpoint);
            if (rawConnection == null) {
                return HealthStatus.UNHEALTHY;
            }
            @SuppressWarnings("unchecked")
            StatefulRedisConnection<String, String> connection = (StatefulRedisConnection<String, String>) rawConnection;
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

    /**
     * Default supplier that uses connection provider when available for resource efficiency.
     */
    public static final HealthCheckStrategySupplier DEFAULT_WITH_PROVIDER = PingStrategy::new;

}
