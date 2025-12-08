package io.lettuce.core.failover.health;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.DatabaseConfig;
import io.lettuce.core.failover.DatabaseConnectionProvider;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.MultiDbTestSupport;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.settings.TestSettings;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for {@link PingStrategy} using Toxiproxy for network failure simulation.
 *
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag("integration")
@DisplayName("PingStrategy Integration Tests")
public class PingStrategyIntegrationTests extends MultiDbTestSupport {

    private static final ToxiproxyClient toxiproxyClient = new ToxiproxyClient("localhost", 8474);

    private static Proxy redisProxy1;

    private static Proxy redisProxy2;

    private static RedisURI proxyUri1;

    private static RedisURI proxyUri2;

    private static TestDatabaseConnectionProviderImpl connectionProvider;

    @Inject
    PingStrategyIntegrationTests(MultiDbClient client) {
        super(client);
    }

    @BeforeAll
    static void setupProxies() throws IOException {
        // Clean up any existing proxies
        if (toxiproxyClient.getProxyOrNull("redis-ping-test-1") != null) {
            toxiproxyClient.getProxy("redis-ping-test-1").delete();
        }
        if (toxiproxyClient.getProxyOrNull("redis-ping-test-2") != null) {
            toxiproxyClient.getProxy("redis-ping-test-2").delete();
        }

        // Create proxies pointing to redis-standalone-1 (port 6479) and redis-standalone-2 (port 6480)
        redisProxy1 = toxiproxyClient.createProxy("redis-ping-test-1", "0.0.0.0:9479", "redis-standalone-1:6479");
        redisProxy2 = toxiproxyClient.createProxy("redis-ping-test-2", "0.0.0.0:9480", "redis-standalone-2:6480");

        // Create RedisURIs for the proxy ports
        proxyUri1 = RedisURI.create(TestSettings.host(), 9479);
        proxyUri2 = RedisURI.create(TestSettings.host(), 9480);
    }

    @AfterAll
    static void cleanupProxies() throws IOException {
        if (redisProxy1 != null) {
            redisProxy1.delete();
        }
        if (redisProxy2 != null) {
            redisProxy2.delete();
        }
    }

    @BeforeEach
    void resetProxies() throws IOException {
        // Enable proxies and remove all toxics before each test
        redisProxy1.enable();
        redisProxy1.toxics().getAll().forEach(toxic -> {
            try {
                toxic.remove();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        redisProxy2.enable();
        redisProxy2.toxics().getAll().forEach(toxic -> {
            try {
                toxic.remove();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @BeforeAll
    static void setupDatabaseConnectionProvider() {
        connectionProvider = new TestDatabaseConnectionProviderImpl();
    }

    @AfterAll
    static void cleanupDatabaseConnectionProvider() {
        connectionProvider.close();
    }

    @Test
    @DisplayName("Should create MultiDbClient with PingStrategy health checks")
    void shouldCreateClientWithPingStrategy() {
        // Given: DatabaseConfigs with PingStrategy using proxy URIs
        HealthCheckStrategySupplier pingSupplier = (uri, options) -> new PingStrategy(uri, options,
                HealthCheckStrategy.Config.builder().interval(100) // Fast interval for testing
                        .timeout(1000).numProbes(1).build());

        DatabaseConfig config1 = new DatabaseConfig(proxyUri1, 1.0f, null, null, pingSupplier);
        DatabaseConfig config2 = new DatabaseConfig(proxyUri2, 0.5f, null, null, pingSupplier);

        // When: Create MultiDbClient and connect
        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2));
        StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

        try {
            // Then: Connection should work normally
            assertThat(connection.sync().ping()).isEqualTo("PONG");
            assertThat(connection.getCurrentEndpoint()).isNotNull();

            // And: Should be able to execute commands
            connection.sync().set("test-key", "test-value");
            assertThat(connection.sync().get("test-key")).isEqualTo("test-value");

        } finally {
            connection.close();
            testClient.shutdown();
        }
    }

    @Test
    @DisplayName("Should recover after network disconnect")
    void shouldRecoverAfterDisconnect() throws Exception {
        // Given: RedisURI with short timeout
        RedisURI uri = RedisURI.builder().withHost(TestSettings.host()).withPort(9479).withTimeout(Duration.ofMillis(1000))
                .build();

        PingStrategy strategy = new PingStrategy(uri, connectionProvider,
                HealthCheckStrategy.Config.builder().interval(1000).timeout(500).numProbes(1).build());

        try {
            // When: Initial health check should work
            HealthStatus initialStatus = strategy.doHealthCheck(uri);
            assertThat(initialStatus).isEqualTo(HealthStatus.HEALTHY);

            // And: Disable the proxy to simulate network failure
            redisProxy1.disable();

            // Then: Health check should return UNHEALTHY
            HealthStatus unhealthyStatus = strategy.doHealthCheck(uri);
            assertThat(unhealthyStatus).isEqualTo(HealthStatus.UNHEALTHY);

            // When: Re-enable proxy
            redisProxy1.enable();

            // Then: Health check should recover
            HealthStatus statusAfterEnable = strategy.doHealthCheck(uri);
            assertThat(statusAfterEnable).isEqualTo(HealthStatus.HEALTHY);

        } finally {
            strategy.close();
        }
    }

    @Test
    @DisplayName("Should handle connection timeout")
    void shouldHandleConnectionTimeout() throws Exception {
        // Given: RedisURI with very short timeout
        RedisURI uri = RedisURI.builder().withHost(TestSettings.host()).withPort(9479).withTimeout(Duration.ofMillis(100))
                .build();

        PingStrategy strategy = new PingStrategy(uri, connectionProvider,
                HealthCheckStrategy.Config.builder().interval(1000).timeout(500).numProbes(1).build());

        try {
            // When: Initial health check should work
            assertThat(strategy.doHealthCheck(uri)).isEqualTo(HealthStatus.HEALTHY);

            // And: Add latency toxic to simulate slow network
            redisProxy1.toxics().latency("slow-connection", ToxicDirection.DOWNSTREAM, 1000);

            // Then: Health check should timeout and return unhealthy
            HealthStatus unhealthyStatus = strategy.doHealthCheck(uri);
            assertThat(unhealthyStatus).isEqualTo(HealthStatus.UNHEALTHY);

            // When: Remove toxic
            redisProxy1.toxics().get("slow-connection").remove();

            // Then: Health check should recover
            HealthStatus recoveredStatus = strategy.doHealthCheck(uri);
            assertThat(recoveredStatus).as("Health check should recover from high latency").isEqualTo(HealthStatus.HEALTHY);

        } finally {
            strategy.close();
        }
    }

    @Test
    @DisplayName("Should handle connection drop during health check")
    void shouldHandleConnectionDrop() throws Exception {
        // Given: RedisURI with normal timeout
        RedisURI uri = RedisURI.builder().withHost(TestSettings.host()).withPort(9479).withTimeout(Duration.ofMillis(2000))
                .build();

        PingStrategy strategy = new PingStrategy(uri, connectionProvider, HealthCheckStrategy.Config.create());

        try {
            // When: Initial health check
            assertThat(strategy.doHealthCheck(uri)).isEqualTo(HealthStatus.HEALTHY);

            // And: Simulate connection drop by limiting data transfer
            redisProxy1.toxics().limitData("connection-drop", ToxicDirection.UPSTREAM, 10);

            // Then: This should fail due to connection issues
            HealthStatus unhealthyStatus = strategy.doHealthCheck(uri);
            assertThat(unhealthyStatus).isEqualTo(HealthStatus.UNHEALTHY);

            // When: Remove toxic
            redisProxy1.toxics().get("connection-drop").remove();

            // Then: Health check should recover
            HealthStatus afterRecovery = strategy.doHealthCheck(uri);
            assertThat(afterRecovery).isEqualTo(HealthStatus.HEALTHY);

        } finally {
            strategy.close();
        }
    }

    @Test
    @DisplayName("Should detect healthy endpoint with PingStrategy")
    void shouldDetectHealthyEndpoint() {
        // Given: DatabaseConfig with PingStrategy using proxy URI
        HealthCheckStrategySupplier pingSupplier = (uri, options) -> new PingStrategy(uri, options,
                HealthCheckStrategy.Config.builder().interval(50) // Very fast for testing
                        .timeout(1000).numProbes(1).build());

        DatabaseConfig config = new DatabaseConfig(proxyUri1, 1.0f, null, null, pingSupplier);

        // When: Create MultiDbClient and connect
        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config));
        StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

        try {
            // Then: Endpoint should be healthy
            assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri1);

            // And: Health checks should be running (verify by waiting a bit and checking connection still works)
            await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofMillis(500)).untilAsserted(() -> {
                assertThat(connection.sync().ping()).isEqualTo("PONG");
            });

        } finally {
            connection.close();
            testClient.shutdown();
        }
    }

    @Test
    @DisplayName("Should use default PingStrategy supplier")
    void shouldUseDefaultPingStrategySupplier() {
        // Given: DatabaseConfig with default PingStrategy supplier using proxy URIs
        DatabaseConfig config1 = new DatabaseConfig(proxyUri1, 1.0f, null, null, PingStrategy.DEFAULT);
        DatabaseConfig config2 = new DatabaseConfig(proxyUri2, 0.5f, null, null, PingStrategy.DEFAULT);

        // When: Create MultiDbClient and connect
        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2));
        StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

        try {
            // Then: Connection should work with default PingStrategy
            assertThat(connection.sync().ping()).isEqualTo("PONG");
            assertThat(connection.getCurrentEndpoint()).isNotNull();

            // And: Should be able to execute commands
            connection.sync().set("default-ping-key", "default-ping-value");
            assertThat(connection.sync().get("default-ping-key")).isEqualTo("default-ping-value");

        } finally {
            connection.close();
            testClient.shutdown();
        }
    }

    @Test
    @DisplayName("Should handle multiple probes with PingStrategy")
    void shouldHandleMultipleProbes() {
        // Given: DatabaseConfig with PingStrategy configured for multiple probes using proxy URI
        HealthCheckStrategySupplier pingSupplier = (uri, options) -> new PingStrategy(uri, options,
                HealthCheckStrategy.Config.builder().interval(100).timeout(1000).numProbes(3) // Multiple probes
                        .delayInBetweenProbes(50).policy(ProbingPolicy.BuiltIn.MAJORITY_SUCCESS).build());

        DatabaseConfig config = new DatabaseConfig(proxyUri1, 1.0f, null, null, pingSupplier);

        // When: Create MultiDbClient and connect
        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config));
        StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

        try {
            // Then: Connection should work with multiple probes
            assertThat(connection.sync().ping()).isEqualTo("PONG");

        } finally {
            connection.close();
            testClient.shutdown();
        }
    }

    @Test
    @DisplayName("Should reuse MultiDbClient connections with DEFAULT_WITH_PROVIDER supplier")
    void shouldReuseConnectionsWithProvider() {
        // Given: DatabaseConfig with DEFAULT_WITH_PROVIDER supplier that reuses connections
        DatabaseConfig config1 = new DatabaseConfig(proxyUri1, 1.0f, null, null, PingStrategy.DEFAULT_WITH_PROVIDER);
        DatabaseConfig config2 = new DatabaseConfig(proxyUri2, 0.5f, null, null, PingStrategy.DEFAULT_WITH_PROVIDER);

        // When: Create MultiDbClient and connect
        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2));
        StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

        try {
            // Then: Connection should work normally
            assertThat(connection.sync().ping()).isEqualTo("PONG");
            assertThat(connection.getCurrentEndpoint()).isNotNull();

            // And: Should be able to execute commands
            connection.sync().set("provider-test-key", "provider-test-value");
            assertThat(connection.sync().get("provider-test-key")).isEqualTo("provider-test-value");

            // And: Health checks should be running using the same connection
            await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofMillis(500)).untilAsserted(() -> {
                assertThat(connection.sync().ping()).isEqualTo("PONG");
            });

        } finally {
            connection.close();
            testClient.shutdown();
        }
    }

    private static class TestDatabaseConnectionProviderImpl implements DatabaseConnectionProvider {

        private final RedisClient client;

        public TestDatabaseConnectionProviderImpl() {
            this.client = RedisClient.create();
        }

        @Override
        public StatefulRedisConnection<?, ?> getConnection(RedisURI endpoint) {
            return client.connect(endpoint);
        }

        public void close() {
            client.shutdown();
        }

    }

}
