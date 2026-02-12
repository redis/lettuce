package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.AbstractRedisMultiDbConnectionBuilder.DatabaseFutureMap;
import io.lettuce.core.failover.api.InitializationPolicy.Decision;
import io.lettuce.core.failover.health.HealthStatus;

/**
 * Unit tests for {@link ConnectionInitializationContext}.
 * <p>
 * Tests verify correct counting of available, failed, and pending connections based on the state of database futures and health
 * status futures.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@DisplayName("ConnectionInitializationContext Unit Tests")
class ConnectionInitializationContextUnitTests {

    private RedisURI uri1;

    private RedisURI uri2;

    private RedisURI uri3;

    @BeforeEach
    void setUp() {
        uri1 = RedisURI.create("redis://localhost:6379");
        uri2 = RedisURI.create("redis://localhost:6380");
        uri3 = RedisURI.create("redis://localhost:6381");
    }

    @SuppressWarnings("unchecked")
    private RedisDatabaseImpl<StatefulRedisConnection<String, String>> createMockDatabase() {
        return Mockito.mock(RedisDatabaseImpl.class);
    }

    @Nested
    @DisplayName("Available Connections Counting Tests")
    class AvailableConnectionsTests {

        @Test
        @DisplayName("Should count all connections as available when all are healthy")
        void shouldCountAllAsAvailable() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri3, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri3, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(3);
            assertThat(ctx.getFailedConnections()).isEqualTo(0);
            assertThat(ctx.getPendingConnections()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should count single connection as available")
        void shouldCountSingleAsAvailable() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(1);
            assertThat(ctx.getFailedConnections()).isEqualTo(0);
            assertThat(ctx.getPendingConnections()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should count only healthy connections as available")
        void shouldCountOnlyHealthyAsAvailable() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri3, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.UNHEALTHY));
            healthStatusFutures.put(uri3, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(2);
            assertThat(ctx.getFailedConnections()).isEqualTo(1);
            assertThat(ctx.getPendingConnections()).isEqualTo(0);
        }

    }

    @Nested
    @DisplayName("Failed Connections Counting Tests")
    class FailedConnectionsTests {

        @Test
        @DisplayName("Should count database connection failures")
        void shouldCountDatabaseConnectionFailures() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Connection failed"));
            databaseFutures.put(uri1, failedFuture);
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.UNKNOWN));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(1);
            assertThat(ctx.getFailedConnections()).isEqualTo(1);
            assertThat(ctx.getPendingConnections()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should count health check failures")
        void shouldCountHealthCheckFailures() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.UNHEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(1);
            assertThat(ctx.getFailedConnections()).isEqualTo(1);
            assertThat(ctx.getPendingConnections()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should count exceptionally completed health checks as failures")
        void shouldCountExceptionalHealthChecksAsFailures() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            CompletableFuture<HealthStatus> failedHealthCheck = new CompletableFuture<>();
            failedHealthCheck.completeExceptionally(new RuntimeException("Health check failed"));
            healthStatusFutures.put(uri1, failedHealthCheck);

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(0);
            assertThat(ctx.getFailedConnections()).isEqualTo(1);
            assertThat(ctx.getPendingConnections()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should count all as failed when all connections fail")
        void shouldCountAllAsFailed() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failed1 = new CompletableFuture<>();
            failed1.completeExceptionally(new RuntimeException("Failed"));
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failed2 = new CompletableFuture<>();
            failed2.completeExceptionally(new RuntimeException("Failed"));
            databaseFutures.put(uri1, failed1);
            databaseFutures.put(uri2, failed2);

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.UNKNOWN));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.UNKNOWN));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(0);
            assertThat(ctx.getFailedConnections()).isEqualTo(2);
            assertThat(ctx.getPendingConnections()).isEqualTo(0);
        }

    }

    @Nested
    @DisplayName("Pending Connections Counting Tests")
    class PendingConnectionsTests {

        @Test
        @DisplayName("Should count pending database connections")
        void shouldCountPendingDatabaseConnections() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, new CompletableFuture<>()); // Pending
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.UNKNOWN));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(1);
            assertThat(ctx.getFailedConnections()).isEqualTo(0);
            assertThat(ctx.getPendingConnections()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count pending health checks")
        void shouldCountPendingHealthChecks() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, new CompletableFuture<>()); // Pending
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(1);
            assertThat(ctx.getFailedConnections()).isEqualTo(0);
            assertThat(ctx.getPendingConnections()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count all as pending when all are incomplete")
        void shouldCountAllAsPending() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, new CompletableFuture<>());
            databaseFutures.put(uri2, new CompletableFuture<>());
            databaseFutures.put(uri3, new CompletableFuture<>());

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, new CompletableFuture<>());
            healthStatusFutures.put(uri2, new CompletableFuture<>());
            healthStatusFutures.put(uri3, new CompletableFuture<>());

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(0);
            assertThat(ctx.getFailedConnections()).isEqualTo(0);
            assertThat(ctx.getPendingConnections()).isEqualTo(3);
        }

    }

    @Nested
    @DisplayName("Mixed State Tests")
    class MixedStateTests {

        @Test
        @DisplayName("Should correctly count mixed states")
        void shouldCountMixedStates() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase())); // Available
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new RuntimeException("Failed"));
            databaseFutures.put(uri2, failed); // Failed
            databaseFutures.put(uri3, new CompletableFuture<>()); // Pending

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.UNKNOWN));
            healthStatusFutures.put(uri3, new CompletableFuture<>());

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.getAvailableConnections()).isEqualTo(1);
            assertThat(ctx.getFailedConnections()).isEqualTo(1);
            assertThat(ctx.getPendingConnections()).isEqualTo(1);
        }

    }

    @Nested
    @DisplayName("Policy Conformance Tests")
    class PolicyConformanceTests {

        @Test
        @DisplayName("Should evaluate ALL_AVAILABLE policy correctly")
        void shouldEvaluateAllAvailablePolicy() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.conformsTo(io.lettuce.core.failover.api.InitializationPolicy.BuiltIn.ALL_AVAILABLE))
                    .isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should evaluate MAJORITY_AVAILABLE policy correctly")
        void shouldEvaluateMajorityAvailablePolicy() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));
            databaseFutures.put(uri2, CompletableFuture.completedFuture(createMockDatabase()));
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new RuntimeException("Failed"));
            databaseFutures.put(uri3, failed);

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri3, CompletableFuture.completedFuture(HealthStatus.UNKNOWN));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.conformsTo(io.lettuce.core.failover.api.InitializationPolicy.BuiltIn.MAJORITY_AVAILABLE))
                    .isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should evaluate ONE_AVAILABLE policy correctly")
        void shouldEvaluateOneAvailablePolicy() {
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();
            databaseFutures.put(uri1, CompletableFuture.completedFuture(createMockDatabase()));
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new RuntimeException("Failed"));
            databaseFutures.put(uri2, failed);

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.UNKNOWN));

            ConnectionInitializationContext ctx = new ConnectionInitializationContext(databaseFutures, healthStatusFutures);

            assertThat(ctx.conformsTo(io.lettuce.core.failover.api.InitializationPolicy.BuiltIn.ONE_AVAILABLE))
                    .isEqualTo(Decision.SUCCESS);
        }

    }

}
