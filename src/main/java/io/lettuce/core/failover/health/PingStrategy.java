package io.lettuce.core.failover.health;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

public class PingStrategy implements HealthCheckStrategy {

    private final RedisClient client;

    private final HealthCheckStrategy.Config config;

    public PingStrategy(RedisURI redisURI, ClientOptions clientOptions) {
        this(redisURI, clientOptions, HealthCheckStrategy.Config.create());
    }

    public PingStrategy(RedisURI redisURI, ClientOptions clientOptions, HealthCheckStrategy.Config config) {
        client = RedisClient.create(redisURI);
        client.setOptions(clientOptions);

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
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            return "PONG".equals(connection.sync().ping()) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
        } catch (Exception e) {
            return HealthStatus.UNHEALTHY;
        }
    }

    @Override
    public void close() {
        client.shutdown();
    }

    public static final HealthCheckStrategySupplier DEFAULT = PingStrategy::new;

}
