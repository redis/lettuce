package io.lettuce.core.failover;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.AbstractRedisMultiDbConnectionBuilder.DatabaseFutureMap;
import io.lettuce.core.failover.api.InitializationPolicy;
import io.lettuce.core.failover.api.InitializationPolicy.Decision;
import io.lettuce.core.failover.health.HealthStatus;

class ConnectionInitializationContext implements InitializationPolicy.InitializationContext {

    private int available = 0;

    private int failed = 0;

    private int pending = 0;

    public ConnectionInitializationContext(DatabaseFutureMap<?> databaseFutures,
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatuses) {

        for (Map.Entry<RedisURI, ? extends CompletableFuture<? extends RedisDatabaseImpl<?>>> entry : databaseFutures
                .entrySet()) {
            RedisURI endpoint = entry.getKey();
            CompletableFuture<? extends RedisDatabaseImpl<?>> dbFuture = entry.getValue();
            if (dbFuture.isDone()) {
                if (dbFuture.isCompletedExceptionally()) {
                    // database connection failed
                    failed++;
                } else {
                    CompletableFuture<HealthStatus> statusFuture = healthStatuses.get(endpoint);
                    if (statusFuture.isDone()) {
                        if (!statusFuture.isCompletedExceptionally() && statusFuture.getNow(null) == HealthStatus.HEALTHY) {
                            // database connection and health check completed successfully
                            available++;
                        } else {
                            // health check failed
                            failed++;
                        }
                    } else {
                        // health check not completed yet
                        pending++;
                    }
                }
            } else {
                // database connection not completed yet
                pending++;
            }
        }
    }

    @Override
    public int getAvailableConnections() {
        return available;
    }

    @Override
    public int getFailedConnections() {
        return failed;
    }

    @Override
    public int getPendingConnections() {
        return pending;
    }

    public Decision conformsTo(InitializationPolicy policy) {
        return policy.evaluate(this);
    }

    @Override
    public String toString() {
        return "ConnectionInitializationContext{" + "available=" + available + ", failed=" + failed + ", pending=" + pending
                + '}';
    }

}
