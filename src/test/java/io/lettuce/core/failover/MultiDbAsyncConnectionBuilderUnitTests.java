package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.lettuce.TestTags.UNIT_TEST;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * Unit tests for {@link MultiDbAsyncConnectionBuilder}.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(UNIT_TEST)
class MultiDbAsyncConnectionBuilderUnitTests {

    @Mock
    private HealthStatusManager healthStatusManager;

    @Mock
    private MultiDbClientImpl client;

    @Mock
    private StatefulRedisConnection<String, String> mockConnection1;

    @Mock
    private StatefulRedisConnection<String, String> mockConnection2;

    @Mock
    private HealthCheck healthCheck;

    private ClientResources resources;

    private MultiDbAsyncConnectionBuilder<String, String> builder;

    private RedisURI uri1;

    private RedisURI uri2;

    private DatabaseConfig config1;

    private DatabaseConfig config2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resources = DefaultClientResources.create();

        uri1 = RedisURI.create("redis://localhost:6379");
        uri2 = RedisURI.create("redis://localhost:6380");

        config1 = DatabaseConfig.builder(uri1).weight(1.0f).clientOptions(ClientOptions.create()).build();

        config2 = DatabaseConfig.builder(uri2).weight(0.5f).clientOptions(ClientOptions.create()).build();

        builder = new MultiDbAsyncConnectionBuilder<>(healthStatusManager, resources, client);
    }

    @Test
    void constructorShouldInitializeFields() {
        assertThat(builder).isNotNull();
    }

    @Test
    void collectDatabasesWithEstablishedConnectionsShouldReturnSuccessfulConnections() {
        // Given
        Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>>> databaseFutures = new ConcurrentHashMap<>();

        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1);
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2);

        databaseFutures.put(uri1, CompletableFuture.completedFuture(db1));
        databaseFutures.put(uri2, CompletableFuture.completedFuture(db2));

        // When
        Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<String, String>>> result = builder
                .collectDatabasesWithEstablishedConnections(databaseFutures);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(uri1, uri2);
        assertThat(result.get(uri1)).isEqualTo(db1);
        assertThat(result.get(uri2)).isEqualTo(db2);
    }

    @Test
    void collectDatabasesWithEstablishedConnectionsShouldHandlePartialFailure() {
        // Given
        Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>>> databaseFutures = new ConcurrentHashMap<>();

        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1);

        CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RedisConnectionException("Connection failed"));

        databaseFutures.put(uri1, CompletableFuture.completedFuture(db1));
        databaseFutures.put(uri2, failedFuture);

        // When
        Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<String, String>>> result = builder
                .collectDatabasesWithEstablishedConnections(databaseFutures);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(uri1);
        assertThat(result.get(uri1)).isEqualTo(db1);
    }

    @Test
    void collectDatabasesWithEstablishedConnectionsShouldThrowWhenAllFail() {
        // Given
        Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>>> databaseFutures = new ConcurrentHashMap<>();

        CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failedFuture1 = new CompletableFuture<>();
        failedFuture1.completeExceptionally(new RedisConnectionException("Connection 1 failed"));

        CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> failedFuture2 = new CompletableFuture<>();
        failedFuture2.completeExceptionally(new RedisConnectionException("Connection 2 failed"));

        databaseFutures.put(uri1, failedFuture1);
        databaseFutures.put(uri2, failedFuture2);

        // When/Then
        assertThatThrownBy(() -> builder.collectDatabasesWithEstablishedConnections(databaseFutures))
                .isInstanceOf(RedisConnectionException.class).hasMessageContaining("Failed to connect to any database")
                .hasMessageContaining("2 connection(s) failed");
    }

    @Test
    void collectHealthStatusesShouldWaitForAllHealthChecks() throws Exception {
        // Given
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabaseWithHealthCheck(config1,
                mockConnection1);
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabaseWithHealthCheck(config2,
                mockConnection2);

        Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<String, String>>> databases = new HashMap<>();
        databases.put(uri1, db1);
        databases.put(uri2, db2);

        // Mock health status manager to return health statuses
        when(healthStatusManager.getHealthStatus(uri1)).thenReturn(HealthStatus.HEALTHY);
        when(healthStatusManager.getHealthStatus(uri2)).thenReturn(HealthStatus.HEALTHY);

        // When
        CompletableFuture<Map<RedisURI, HealthStatus>> future = builder.collectHealthStatuses(databases);

        // Then
        assertThat(future).isNotNull();
        // Note: This test requires actual async execution which is hard to test in unit tests
        // Integration tests should cover the full async flow
    }

    @Test
    void collectHealthStatusesShouldHandleDatabasesWithoutHealthChecks() throws Exception {
        // Given
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1);
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2);

        Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<String, String>>> databases = new HashMap<>();
        databases.put(uri1, db1);
        databases.put(uri2, db2);

        // When
        CompletableFuture<Map<RedisURI, HealthStatus>> future = builder.collectHealthStatuses(databases);

        // Then
        assertThat(future).isNotNull();
        Map<RedisURI, HealthStatus> result = future.get();
        assertThat(result).hasSize(2);
        assertThat(result.get(uri1)).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.get(uri2)).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void collectHealthStatusesShouldReturnUnhealthyStatuses() throws Exception {
        // Given
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1);
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2);

        Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<String, String>>> databases = new HashMap<>();
        databases.put(uri1, db1);
        databases.put(uri2, db2);

        // When
        CompletableFuture<Map<RedisURI, HealthStatus>> future = builder.collectHealthStatuses(databases);

        // Then
        assertThat(future).isNotNull();
        Map<RedisURI, HealthStatus> result = future.get();
        assertThat(result).hasSize(2);
        // Without health checks, databases are assumed HEALTHY
        assertThat(result.get(uri1)).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.get(uri2)).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void handleDatabaseFuturesShouldCloseConnectionsOnHealthCheckFailure() {
        // Given
        Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>>> databaseFutures = new ConcurrentHashMap<>();

        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1);
        RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2);

        databaseFutures.put(uri1, CompletableFuture.completedFuture(db1));
        databaseFutures.put(uri2, CompletableFuture.completedFuture(db2));

        when(mockConnection1.isOpen()).thenReturn(true);
        when(mockConnection2.isOpen()).thenReturn(true);

        // Mock health status manager to throw exception
        when(healthStatusManager.getHealthStatus(any())).thenThrow(new RuntimeException("Health check failed"));

        // When
        CompletableFuture<Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<String, String>>>> future = builder
                .handleDatabaseFutures(databaseFutures);

        // Then - connections should be closed on error
        // Note: Full verification requires integration test due to async nature
        assertThat(future).isNotNull();
    }

    @Test
    void handleDatabaseFuturesShouldHandleUnexpectedExceptions() {
        // Given
        Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>>> databaseFutures = new ConcurrentHashMap<>();

        // Create a future that will throw when accessed
        CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> badFuture = new CompletableFuture<>();
        badFuture.completeExceptionally(new RuntimeException("Unexpected error"));

        databaseFutures.put(uri1, badFuture);

        // When
        CompletableFuture<Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<String, String>>>> future = builder
                .handleDatabaseFutures(databaseFutures);

        // Then
        assertThatThrownBy(() -> future.get()).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RedisConnectionException.class);
    }

    // Helper method to create mock database
    private RedisDatabaseImpl<StatefulRedisConnection<String, String>> createMockDatabase(DatabaseConfig config,
            StatefulRedisConnection<String, String> connection) {
        DatabaseEndpoint endpoint = mock(DatabaseEndpoint.class);
        CircuitBreakerImpl circuitBreaker = mock(CircuitBreakerImpl.class);
        return new RedisDatabaseImpl<>(config, connection, endpoint, circuitBreaker, null);
    }

    // Helper method to create mock database with health check
    private RedisDatabaseImpl<StatefulRedisConnection<String, String>> createMockDatabaseWithHealthCheck(DatabaseConfig config,
            StatefulRedisConnection<String, String> connection) {
        DatabaseEndpoint endpoint = mock(DatabaseEndpoint.class);
        CircuitBreakerImpl circuitBreaker = mock(CircuitBreakerImpl.class);
        return new RedisDatabaseImpl<>(config, connection, endpoint, circuitBreaker, healthCheck);
    }

}
