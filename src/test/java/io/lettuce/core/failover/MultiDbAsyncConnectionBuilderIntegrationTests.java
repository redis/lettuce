package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.awaitility.Durations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.ProbingPolicy;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Comprehensive integration tests for {@link AbstractRedisMultiDbConnectionBuilder} async connection building with various
 * failure scenarios.
 * <p>
 * Tests cover:
 * <ul>
 * <li>Hanging (non-establishing) connections</li>
 * <li>Failing connections</li>
 * <li>Hanging (non-concluding) health checks</li>
 * <li>Unhealthy health check results</li>
 * <li>Weight-based selection order with mixed states</li>
 * <li>All endpoints failing scenarios</li>
 * </ul>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(INTEGRATION_TEST)
@DisplayName("MultiDb Async Connection Builder Integration Tests")
class MultiDbAsyncConnectionBuilderIntegrationTests {

    private static final Logger logger = LoggerFactory.getLogger(MultiDbAsyncConnectionBuilderIntegrationTests.class);

    private static final String TOXIPROXY_HOST = "localhost";

    private static final int TOXIPROXY_PORT = TestSettings.proxyAdminPort();

    private static final ToxiproxyClient toxiproxyClient = new ToxiproxyClient(TOXIPROXY_HOST, TOXIPROXY_PORT);

    // Real Redis instances
    private static final int REDIS1_PORT = TestSettings.port(10);

    private static final int REDIS2_PORT = TestSettings.port(11);

    private static final int REDIS3_PORT = TestSettings.port(12);

    private static final RedisURI REDIS1_URI = RedisURI.create(TestSettings.host(), REDIS1_PORT);

    private static final RedisURI REDIS2_URI = RedisURI.create(TestSettings.host(), REDIS2_PORT);

    private static final RedisURI REDIS3_URI = RedisURI.create(TestSettings.host(), REDIS3_PORT);

    // Toxiproxy proxies
    private static Proxy proxy1;

    private static Proxy proxy2;

    private static Proxy proxy3;

    private static RedisURI proxyUri1;

    private static RedisURI proxyUri2;

    private static RedisURI proxyUri3;

    // Non-existent endpoint for connection failures
    private static final RedisURI NONEXISTENT_URI = RedisURI.create(TestSettings.host(), TestSettings.nonexistentPort());

    private MultiDbClient client;

    private StatefulRedisMultiDbConnection<String, String> connection;

    @BeforeAll
    static void setUpProxies() throws IOException {
        logger.info("=== Setting up Toxiproxy proxies ===");
        logger.info("Toxiproxy host: " + TOXIPROXY_HOST + ":" + TOXIPROXY_PORT);
        logger.info("Proxy ports: " + TestSettings.proxyPort() + ", " + TestSettings.proxyPort(1) + ", "
                + TestSettings.proxyPort(2));
        logger.info("Redis ports: " + REDIS1_PORT + ", " + REDIS2_PORT + ", " + REDIS3_PORT);

        // Clean up any existing proxies
        if (toxiproxyClient.getProxyOrNull("redis1-proxy") != null) {
            logger.info("Deleting existing redis1-proxy");
            toxiproxyClient.getProxy("redis1-proxy").delete();
        }
        if (toxiproxyClient.getProxyOrNull("redis2-proxy") != null) {
            logger.info("Deleting existing redis2-proxy");
            toxiproxyClient.getProxy("redis2-proxy").delete();
        }
        if (toxiproxyClient.getProxyOrNull("redis3-proxy") != null) {
            logger.info("Deleting existing redis3-proxy");
            toxiproxyClient.getProxy("redis3-proxy").delete();
        }

        // Create Toxiproxy proxies for each Redis instance
        // Use "redis-failover" as the upstream host since Toxiproxy runs in Docker
        logger.info("Creating redis1-proxy...");
        proxy1 = toxiproxyClient.createProxy("redis1-proxy", "0.0.0.0:" + TestSettings.proxyPort(),
                "redis-failover:" + REDIS1_PORT);
        logger.info("Created redis1-proxy: " + proxy1.getName());

        logger.info("Creating redis2-proxy...");
        proxy2 = toxiproxyClient.createProxy("redis2-proxy", "0.0.0.0:" + TestSettings.proxyPort(1),
                "redis-failover:" + REDIS2_PORT);
        logger.info("Created redis2-proxy: " + proxy2.getName());

        logger.info("Creating redis3-proxy...");
        proxy3 = toxiproxyClient.createProxy("redis3-proxy", "0.0.0.0:" + TestSettings.proxyPort(2),
                "redis-failover:" + REDIS3_PORT);
        logger.info("Created redis3-proxy: " + proxy3.getName());

        proxyUri1 = RedisURI.create(TestSettings.host(), TestSettings.proxyPort());
        proxyUri2 = RedisURI.create(TestSettings.host(), TestSettings.proxyPort(1));
        proxyUri3 = RedisURI.create(TestSettings.host(), TestSettings.proxyPort(2));

        logger.info("=== Toxiproxy setup complete ===");
    }

    @AfterAll
    static void tearDownProxies() throws IOException {
        if (proxy1 != null) {
            proxy1.delete();
        }
        if (proxy2 != null) {
            proxy2.delete();
        }
        if (proxy3 != null) {
            proxy3.delete();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // Reset all proxies to enabled state
        enableAllProxies();

        // Flush all Redis instances
        try (RedisClient redis1 = RedisClient.create(REDIS1_URI)) {
            redis1.connect().sync().flushall();
        }
        try (RedisClient redis2 = RedisClient.create(REDIS2_URI)) {
            redis2.connect().sync().flushall();
        }
        try (RedisClient redis3 = RedisClient.create(REDIS3_URI)) {
            redis3.connect().sync().flushall();
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        if (client != null) {
            FastShutdown.shutdown(client);
        }
        // Reset all proxies
        enableAllProxies();
    }

    // ============ Helper Methods ============

    private void enableAllProxies() throws IOException {
        if (proxy1 != null) {
            proxy1.enable();
            proxy1.toxics().getAll().forEach(toxic -> {
                try {
                    toxic.remove();
                } catch (IOException e) {
                    // Ignore
                }
            });
        }
        if (proxy2 != null) {
            proxy2.enable();
            proxy2.toxics().getAll().forEach(toxic -> {
                try {
                    toxic.remove();
                } catch (IOException e) {
                    // Ignore
                }
            });
        }
        if (proxy3 != null) {
            proxy3.enable();
            proxy3.toxics().getAll().forEach(toxic -> {
                try {
                    toxic.remove();
                } catch (IOException e) {
                    // Ignore
                }
            });
        }
    }

    private HealthCheckStrategySupplier createAlwaysHealthySupplier() {
        return (uri, options) -> new TestHealthCheckStrategy(
                HealthCheckStrategy.Config.builder().interval(100).timeout(500).numProbes(1).build(),
                endpoint -> HealthStatus.HEALTHY);
    }

    private HealthCheckStrategySupplier createAlwaysUnhealthySupplier() {
        return (uri, options) -> new TestHealthCheckStrategy(
                HealthCheckStrategy.Config.builder().interval(100).timeout(500).numProbes(1).build(),
                endpoint -> HealthStatus.UNHEALTHY);
    }
    // ============ Weight-Based Selection Tests ============

    @Nested
    @DisplayName("Weight-Based Selection Tests")
    @Tag(INTEGRATION_TEST)
    class WeightBasedSelectionTests {

        @Test
        @DisplayName("Should select highest weighted healthy endpoint when all are healthy")
        void shouldSelectHighestWeightedHealthyEndpoint() throws Exception {
            // Given: Three endpoints with different weights, all healthy
            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            client = MultiDbClient.create(Arrays.asList(config1, config2, config3));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should select highest weighted endpoint (proxyUri1)
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri1);
            });
        }

        @Test
        @DisplayName("Should skip unhealthy highest weighted endpoint and select next healthy one")
        void shouldSkipUnhealthyHighestWeightedEndpoint() throws Exception {
            // Given: Highest weighted endpoint is unhealthy, second is healthy
            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysUnhealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            client = MultiDbClient.create(Arrays.asList(config1, config2, config3));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should select second highest weighted endpoint (proxyUri2)
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri2);
            });
        }

        @Test
        @DisplayName("Should select lowest weighted endpoint when higher weighted ones are unhealthy")
        void shouldSelectLowestWeightedWhenOthersUnhealthy() throws Exception {
            // Given: Only lowest weighted endpoint is healthy
            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysUnhealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysUnhealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            client = MultiDbClient.create(Arrays.asList(config1, config2, config3));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should select lowest weighted endpoint (proxyUri3)
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri3);
            });
        }

    }

    // ============ Hanging Connection Tests ============

    @Nested
    @DisplayName("Hanging Connection Tests")
    @Tag(INTEGRATION_TEST)
    class HangingConnectionTests {

        @Test
        @DisplayName("Should skip hanging connection and select next available endpoint")
        void shouldSkipHangingConnectionAndSelectNext() throws Exception {
            // Given: Highest weighted endpoint hangs, second is healthy
            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f)
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Create test client that will make proxyUri1 hang
            Set<RedisURI> hangingUris = new java.util.HashSet<>();
            hangingUris.add(proxyUri1);
            client = new TestMultiDbClient(Arrays.asList(config1, config2, config3), hangingUris);

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should skip hanging endpoint and select proxyUri2
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri2);
            });
        }

        @Test
        @DisplayName("Should eventually connect to slow endpoint when it completes")
        void shouldEventuallyConnectToHangingEndpoint() throws Exception {
            // Given: Highest weighted endpoint has slow connection, second is fast
            // Use a longer timeout for the slow endpoint
            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build()).build())
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Add latency to proxy1 to make it slower
            proxy1.toxics().latency("slow-connect", ToxicDirection.UPSTREAM, 2000);

            client = MultiDbClient.create(Arrays.asList(config1, config2));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Initially should select proxyUri2 (faster)
            assertThat(connection).isNotNull();
            await().atMost(Durations.TWO_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri2);
            });

            // Then: Eventually proxy1 should complete and be added to available endpoints
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(connection.getEndpoints()).contains(proxyUri1);
            });
        }

    }

    // ============ Failing Connection Tests ============

    @Nested
    @DisplayName("Failing Connection Tests")
    @Tag(INTEGRATION_TEST)
    class FailingConnectionTests {

        @Test
        @DisplayName("Should skip failing connection and select next available endpoint")
        void shouldSkipFailingConnectionAndSelectNext() throws Exception {
            // Given: Highest weighted endpoint fails to connect (non-existent), second is healthy
            DatabaseConfig config1 = DatabaseConfig.builder(NONEXISTENT_URI).weight(1.0f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build()).build())
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            client = MultiDbClient.create(Arrays.asList(config1, config2, config3));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should skip failing endpoint and select proxyUri2
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri2);
            });
        }

        @Test
        @DisplayName("Should fail when all endpoints fail to connect")
        void shouldFailWhenAllEndpointsFail() {
            // Given: All endpoints fail to connect
            DatabaseConfig config1 = DatabaseConfig.builder(NONEXISTENT_URI).weight(1.0f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build()).build())
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

            RedisURI nonexistent2 = RedisURI.create(TestSettings.host(), TestSettings.nonexistentPort() + 1);
            DatabaseConfig config2 = DatabaseConfig.builder(nonexistent2).weight(0.5f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build()).build())
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

            client = MultiDbClient.create(Arrays.asList(config1, config2));

            // When/Then: Connect should fail with RedisConnectionException
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);

            assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(RedisConnectionException.class)
                    .hasMessageContaining("No healthy database available");
        }

    }

    // ============ Hanging Health Check Tests ============

    @Nested
    @DisplayName("Hanging Health Check Tests")
    @Tag(INTEGRATION_TEST)
    class HangingHealthCheckTests {

        @Test
        @DisplayName("Should skip endpoint with hanging health check and select next available")
        void shouldSkipEndpointWithHangingHealthCheck() throws Exception {
            // Given: Highest weighted endpoint has hanging health check, second is healthy
            CountDownLatch hangLatch = new CountDownLatch(1);
            HealthCheckStrategySupplier hangingSupplier = (uri, options) -> new TestHealthCheckStrategy(
                    HealthCheckStrategy.Config.builder().interval(100).timeout(5000).numProbes(1).build(), endpoint -> {
                        try {
                            // Hang indefinitely
                            hangLatch.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return HealthStatus.HEALTHY;
                    });

            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f).healthCheckStrategySupplier(hangingSupplier)
                    .build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            client = MultiDbClient.create(Arrays.asList(config1, config2, config3));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should skip hanging health check and select proxyUri2
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri2);
            });

            // Cleanup
            hangLatch.countDown();
        }

        @Test
        @DisplayName("Should handle slow health checks and eventually select endpoint")
        void shouldHandleSlowHealthChecks() throws Exception {
            // Given: Endpoints with slow health checks (but not hanging)
            AtomicInteger healthCheckCount = new AtomicInteger(0);
            HealthCheckStrategySupplier slowSupplier = (uri, options) -> new TestHealthCheckStrategy(
                    HealthCheckStrategy.Config.builder().interval(100).timeout(3000).numProbes(1).build(), endpoint -> {
                        healthCheckCount.incrementAndGet();
                        try {
                            // Slow but not hanging (500ms delay)
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return HealthStatus.HEALTHY;
                    });

            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f).healthCheckStrategySupplier(slowSupplier)
                    .build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f).healthCheckStrategySupplier(slowSupplier)
                    .build();

            client = MultiDbClient.create(Arrays.asList(config1, config2));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should eventually select highest weighted endpoint
            assertThat(connection).isNotNull();
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri1);
            });

            // And: Health checks should have been called
            assertThat(healthCheckCount.get()).isGreaterThan(0);
        }

    }

    // ============ Mixed State Tests ============

    @Nested
    @DisplayName("Mixed State Tests")
    @Tag(INTEGRATION_TEST)
    class MixedStateTests {

        @Test
        @DisplayName("Should handle mix of hanging, failing, and healthy endpoints")
        void shouldHandleMixOfStates() throws Exception {
            // Given: Mix of states - hanging, failing, healthy
            // Endpoint 1: Hanging connection
            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f)
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

            // Endpoint 2: Failing connection
            DatabaseConfig config2 = DatabaseConfig.builder(NONEXISTENT_URI).weight(0.5f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build()).build())
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

            // Endpoint 3: Healthy
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Create test client that will make proxyUri1 hang
            Set<RedisURI> hangingUris = new java.util.HashSet<>();
            hangingUris.add(proxyUri1);
            client = new TestMultiDbClient(Arrays.asList(config1, config2, config3), hangingUris);

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should select the only healthy endpoint (proxyUri3)
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(proxyUri3);
            });
        }

        @Test
        @DisplayName("Should respect weight order when multiple endpoints become healthy at different times")
        void shouldRespectWeightOrderWithAsyncCompletion() throws Exception {
            // Given: All endpoints healthy but with different connection speeds
            // Fastest: lowest weight (proxyUri3)
            // Slowest: highest weight (proxyUri1) - add latency
            proxy1.toxics().latency("slow-connect", ToxicDirection.UPSTREAM, 3000);

            DatabaseConfig config1 = DatabaseConfig.builder(proxyUri1).weight(1.0f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build()).build())
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(proxyUri2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(proxyUri3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            client = MultiDbClient.create(Arrays.asList(config1, config2, config3));

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);
            connection = future.get(10, TimeUnit.SECONDS);

            // Then: Should initially select first available (likely proxyUri3 or proxyUri2)
            assertThat(connection).isNotNull();
            RedisURI initialEndpoint = connection.getCurrentEndpoint();
            assertThat(initialEndpoint).isIn(proxyUri2, proxyUri3);

            // Then: Eventually all endpoints should be available (including slow proxy1)
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(connection.getEndpoints()).contains(proxyUri1, proxyUri2, proxyUri3);
            });
        }

    }

    /**
     * Test client that allows injecting a custom connection builder.
     */
    static class TestMultiDbClient extends MultiDbClientImpl {

        private final Set<RedisURI> hangingUris;

        private TestMultiDbAsyncConnectionBuilder builder;

        TestMultiDbClient(List<DatabaseConfig> databaseConfigs, Set<RedisURI> hangingUris) {
            super(databaseConfigs);
            this.hangingUris = hangingUris;
        }

        @Override
        protected <K, V> MultiDbAsyncConnectionBuilder<K, V> createConnectionBuilder(RedisCodec<K, V> codec) {
            return new TestMultiDbAsyncConnectionBuilder<>(this, getResources(), codec, hangingUris);
        }

        public TestMultiDbAsyncConnectionBuilder getBuilder() {
            return builder;
        }

    }

    /**
     * Test builder that allows simulating hanging connections for specific URIs.
     */
    static class TestMultiDbAsyncConnectionBuilder<K, V> extends MultiDbAsyncConnectionBuilder<K, V> {

        private final Set<RedisURI> hangingUris;

        private final Map<RedisURI, CompletableFuture<StatefulRedisConnection<K, V>>> hangingFutures = new HashMap<>();

        private final Map<RedisURI, ConnectionFuture<StatefulRedisConnection<K, V>>> actualFuturesMap = new HashMap<>();

        TestMultiDbAsyncConnectionBuilder(MultiDbClientImpl client, ClientResources resources, RedisCodec<K, V> codec,
                Set<RedisURI> hangingUris) {
            super(client, resources, codec);
            this.hangingUris = hangingUris;
        }

        @Override
        protected ConnectionFuture<StatefulRedisConnection<K, V>> connectAsync(RedisCodec<K, V> codec, RedisURI uri) {
            if (hangingUris.contains(uri)) {
                // Return a never-completing future to simulate hanging connection
                CompletableFuture<StatefulRedisConnection<K, V>> hangingFuture = new CompletableFuture<>();
                hangingFutures.put(uri, hangingFuture);
                actualFuturesMap.put(uri, super.connectAsync(codec, uri));
                // Never complete this future
                return ConnectionFuture.from(null, hangingFuture);
            }
            return super.connectAsync(codec, uri);
        }

        public void completeHangingConnections() {
            for (RedisURI uri : hangingFutures.keySet()) {
                actualFuturesMap.get(uri).toCompletableFuture().whenComplete((c, e) -> {
                    if (c != null) {
                        hangingFutures.get(uri).complete(c);
                    } else {
                        hangingFutures.get(uri).completeExceptionally(e);
                    }
                });
            }
        }

    }

    /**
     * Test implementation of {@link HealthCheckStrategy} for integration testing.
     */
    static class TestHealthCheckStrategy implements HealthCheckStrategy {

        private final Config config;

        private final Function<RedisURI, HealthStatus> healthCheckFunction;

        TestHealthCheckStrategy(Config config, Function<RedisURI, HealthStatus> healthCheckFunction) {
            this.config = config;
            this.healthCheckFunction = healthCheckFunction;
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
        public HealthStatus doHealthCheck(RedisURI endpoint) {
            return healthCheckFunction.apply(endpoint);
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

    }

}
