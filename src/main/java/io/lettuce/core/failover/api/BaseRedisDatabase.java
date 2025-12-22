package io.lettuce.core.failover.api;

import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.CircuitBreaker;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.metrics.MetricsSnapshot;

public interface BaseRedisDatabase {

    float getWeight();

    RedisURI getRedisURI();

    MetricsSnapshot getMetricsSnapshot();

    HealthStatus getHealthCheckStatus();

    CircuitBreaker.State getCircuitBreakerState();

}
