package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.Closeable;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionFuture;
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
import io.lettuce.core.failover.health.TestHealthCheckStrategy;
import io.lettuce.core.resource.ClientResources;
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

    private static final RedisURI REDIS_URI_1 = RedisURI.create(TestSettings.host(), TestSettings.port(10));

    private static final RedisURI REDIS_URI_2 = RedisURI.create(TestSettings.host(), TestSettings.port(11));

    private static final RedisURI REDIS_URI_3 = RedisURI.create(TestSettings.host(), TestSettings.port(12));

    // Non-existent endpoint for connection failures
    private static final RedisURI NONEXISTENT_URI = RedisURI.create(TestSettings.host(), TestSettings.nonexistentPort());

    private MultiDbClient forCleanup;

    private StatefulRedisMultiDbConnection<String, String> connection;

    @AfterEach
    void tearDown() {
        if (forCleanup != null) {
            forCleanup.shutdown();
            forCleanup = null;
        }
    }

    // ============ Helper Methods ============

    private HealthCheckStrategySupplier createAlwaysHealthySupplier() {
        return (uri, options) -> new TestHealthCheckStrategy(
                HealthCheckStrategy.Config.builder().interval(100).timeout(2000).numProbes(1).build(),
                endpoint -> HealthStatus.HEALTHY);
    }

    private HealthCheckStrategySupplier createAlwaysUnhealthySupplier() {
        return (uri, options) -> new TestHealthCheckStrategy(
                HealthCheckStrategy.Config.builder().interval(100).timeout(2000).numProbes(1).build(),
                endpoint -> HealthStatus.UNHEALTHY);
    }

    // ============ Test Infrastructure for Simulating Hanging Connections ============

    /**
     * Test client that allows injecting a custom connection builder.
     */
    static class TestMultiDbClient extends MultiDbClientImpl {

        private final Set<RedisURI> hangingUris;

        private TestMultiDbAsyncConnectionBuilder<?, ?> builder;

        TestMultiDbClient(List<DatabaseConfig> databaseConfigs, Set<RedisURI> hangingUris) {
            super(databaseConfigs, MultiDbOptions.create());
            this.hangingUris = hangingUris;
        }

        @Override
        protected <K, V> MultiDbAsyncConnectionBuilder<K, V> createConnectionBuilder(RedisCodec<K, V> codec) {
            TestMultiDbAsyncConnectionBuilder<K, V> testBuilder = new TestMultiDbAsyncConnectionBuilder<K, V>(this,
                    getResources(), codec, getMultiDbOptions(), hangingUris);
            builder = testBuilder;
            return testBuilder;
        }

        public TestMultiDbAsyncConnectionBuilder<?, ?> getBuilder() {
            return builder;
        }

    }

    /**
     * Test connection builder that simulates hanging connections for specified URIs.
     */
    static class TestMultiDbAsyncConnectionBuilder<K, V> extends MultiDbAsyncConnectionBuilder<K, V> {

        private static Collection<Closeable> closeableResources = ConcurrentHashMap.newKeySet();

        private final Set<RedisURI> hangingUris;

        private final Map<RedisURI, CompletableFuture<StatefulRedisConnection<K, V>>> hangingFutures = new ConcurrentHashMap<>();

        private final Map<RedisURI, ConnectionFuture<StatefulRedisConnection<K, V>>> actualFuturesMap = new ConcurrentHashMap<>();

        TestMultiDbAsyncConnectionBuilder(MultiDbClientImpl client, ClientResources resources, RedisCodec<K, V> codec,
                MultiDbOptions multiDbOptions, Set<RedisURI> hangingUris) {
            super(client, resources, codec, closeableResources, multiDbOptions);
            this.hangingUris = hangingUris;
        }

        @Override
        protected ConnectionFuture<StatefulRedisConnection<K, V>> connectAsync(RedisCodec<K, V> codec, RedisURI uri) {
            if (hangingUris.contains(uri)) {
                // Return a never-completing future to simulate hanging connection
                CompletableFuture<StatefulRedisConnection<K, V>> hangingFuture = new CompletableFuture<>();
                hangingFutures.put(uri, hangingFuture);
                actualFuturesMap.put(uri, super.connectAsync(codec, uri));
                return ConnectionFuture.from(null, hangingFuture);
            }
            return super.connectAsync(codec, uri);
        }

        public void proceedHangingConnections() {
            for (RedisURI uri : hangingFutures.keySet()) {
                actualFuturesMap.get(uri).toCompletableFuture().whenCompleteAsync((c, e) -> {
                    if (c != null) {
                        hangingFutures.get(uri).complete(c);
                    } else {
                        hangingFutures.get(uri).completeExceptionally(e);
                    }
                });
            }
        }

    }

    // ============ Weight-Based Selection Tests ============

    @Nested
    @DisplayName("Weight-Based Selection Tests")
    @Tag(INTEGRATION_TEST)
    class WeightBasedSelectionTests {

        @Test
        @DisplayName("Should select highest weighted healthy endpoint when all are healthy")
        void shouldSelectHighestWeightedHealthyEndpoint() {
            // Given: Three endpoints with different weights, all healthy
            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2, config3));
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Should select highest weighted endpoint (REDIS_URI_1)
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_1);
            });
        }

        @Test
        @DisplayName("Should skip unhealthy highest weighted endpoint and select next healthy one")
        void shouldSkipUnhealthyHighestWeightedEndpoint() {
            // Given: Highest weighted endpoint is unhealthy, second is healthy
            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysUnhealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2, config3));
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Should select second highest weighted endpoint (REDIS_URI_2)
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_2);
            });
        }

        @Test
        @DisplayName("Should select lowest weighted endpoint when higher weighted ones are unhealthy")
        void shouldSelectLowestWeightedWhenOthersUnhealthy() {
            // Given: Only lowest weighted endpoint is healthy
            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysUnhealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysUnhealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2, config3),
                    MultiDbOptions.builder().initializationPolicy(InitializationPolicy.BuiltIn.ONE_AVAILABLE).build());
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Should select lowest weighted endpoint (REDIS_URI_3)
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_3);
            });
        }

    }

    // ============ Hanging Connection Tests ============

    @Nested
    @DisplayName("Hanging Connection Tests")
    @Tag(INTEGRATION_TEST)
    class HangingConnectionTests {

        @Test
        @DisplayName("Should wait for highest weighted connection to complete")
        void shouldWaitForHighestWeightedConnection() {
            // Given: Highest weighted endpoint hangs initially, others are healthy
            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Create test client that will make REDIS_URI_1 hang
            Set<RedisURI> hangingUris = new HashSet<>();
            hangingUris.add(REDIS_URI_1);
            TestMultiDbClient testClient = new TestMultiDbClient(Arrays.asList(config1, config2, config3), hangingUris);
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Future should NOT complete yet (highest weight is still hanging)
            await().during(Durations.TWO_SECONDS).atMost(Duration.ofSeconds(3)).until(() -> !future.isDone());

            // When: Complete the hanging connection
            testClient.getBuilder().proceedHangingConnections();

            // Then: Future should now complete with REDIS_URI_1 as the selected endpoint
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_1);
        }

        @Test
        @DisplayName("Should not block on lower weighted hanging connections - they go to async completion")
        void shouldNotBlockOnLowerWeightedHangingConnections() {
            // Given: Highest weighted is healthy, lower weighted hangs
            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Create test client that will make REDIS_URI_3 (lowest weight) hang
            Set<RedisURI> hangingUris = new HashSet<>();
            hangingUris.add(REDIS_URI_3);
            TestMultiDbClient testClient = new TestMultiDbClient(Arrays.asList(config1, config2, config3), hangingUris);
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Future should complete quickly with REDIS_URI_1 (not blocked by REDIS_URI_3)
            await().atMost(Durations.FIVE_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_1);

            // And: REDIS_URI_3 should NOT be in endpoints yet (still hanging)
            assertThat(connection.getEndpoints()).doesNotContain(REDIS_URI_3);

            // When: Complete the hanging connection
            testClient.getBuilder().proceedHangingConnections();

            // Then: REDIS_URI_3 should eventually be added to available endpoints
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(connection.getEndpoints()).contains(REDIS_URI_3);
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
        void shouldSkipFailingConnectionAndSelectNext() {
            // Given: Highest weighted endpoint fails to connect (non-existent), second is healthy
            DatabaseConfig config1 = DatabaseConfig.builder(NONEXISTENT_URI).weight(1.0f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build()).build())
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2, config3));
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Should skip failing endpoint and select REDIS_URI_2
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_2);
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

            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2), MultiDbOptions.builder()
                    .initializationPolicy(InitializationPolicy.BuiltIn.ONE_AVAILABLE).build());
            forCleanup = testClient;

            // When/Then: Connect should fail with RedisConnectionException
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            assertThatThrownBy(() -> {
                await().atMost(Durations.TEN_SECONDS).until(future::isDone);
                future.toCompletableFuture().join();
            }).hasCauseInstanceOf(RedisConnectionException.class).hasMessageContaining("No healthy database available");
        }

    }

    // ============ Hanging Health Check Tests ============

    @Nested
    @DisplayName("Hanging Health Check Tests")
    @Tag(INTEGRATION_TEST)
    class HangingHealthCheckTests {

        @Test
        @DisplayName("Should skip endpoint with hanging health check and select next available")
        void shouldSkipEndpointWithHangingHealthCheck() {
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

            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(hangingSupplier).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2, config3));
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Should skip hanging health check and select REDIS_URI_2
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_2);
            });

            // Cleanup
            hangLatch.countDown();
        }

        @Test
        @DisplayName("Should handle slow health checks and eventually select endpoint")
        void shouldHandleSlowHealthChecks() {
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

            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f).healthCheckStrategySupplier(slowSupplier)
                    .build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f).healthCheckStrategySupplier(slowSupplier)
                    .build();

            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2));
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Should eventually select highest weighted endpoint
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_1);
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
        @DisplayName("Should wait for highest weighted hanging connection before falling back to lower weights")
        void shouldWaitForHighestWeightBeforeFallback() {
            // Given: Mix of states - highest weight hangs, second fails, third is healthy
            // Endpoint 1: Hanging connection (highest weight)
            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Endpoint 2: Failing connection (second weight)
            DatabaseConfig config2 = DatabaseConfig.builder(NONEXISTENT_URI).weight(0.5f)
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build()).build())
                    .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

            // Endpoint 3: Healthy (lowest weight)
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Create test client that will make REDIS_URI_1 hang
            Set<RedisURI> hangingUris = new HashSet<>();
            hangingUris.add(REDIS_URI_1);
            TestMultiDbClient testClient = new TestMultiDbClient(Arrays.asList(config1, config2, config3), hangingUris);
            forCleanup = testClient;

            // When: Connect asynchronously - should NOT complete yet
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Future should not complete even though REDIS_URI_3 is ready
            // because we're waiting for highest weight (REDIS_URI_1) to conclude
            await().during(Durations.TWO_SECONDS).atMost(Duration.ofSeconds(3)).until(() -> !future.isDone());

            // When: Complete the hanging connection
            testClient.getBuilder().proceedHangingConnections();

            // Then: Future should now complete with REDIS_URI_1 (highest weight, now healthy)
            await().atMost(Durations.TEN_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_1);
        }

        @Test
        @DisplayName("Should not block on lower weighted hanging - they complete async and are added later")
        void shouldCompleteAsyncForLowerWeightedHanging() {
            // Given: Highest weight healthy, middle weight hangs, lowest weight healthy
            DatabaseConfig config1 = DatabaseConfig.builder(REDIS_URI_1).weight(1.0f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config2 = DatabaseConfig.builder(REDIS_URI_2).weight(0.5f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();
            DatabaseConfig config3 = DatabaseConfig.builder(REDIS_URI_3).weight(0.25f)
                    .healthCheckStrategySupplier(createAlwaysHealthySupplier()).build();

            // Create test client that will make REDIS_URI_2 (middle weight) hang
            Set<RedisURI> hangingUris = new HashSet<>();
            hangingUris.add(REDIS_URI_2);
            TestMultiDbClient testClient = new TestMultiDbClient(Arrays.asList(config1, config2, config3), hangingUris);
            forCleanup = testClient;

            // When: Connect asynchronously
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = testClient
                    .connectAsync(StringCodec.UTF8);

            // Then: Should complete quickly with REDIS_URI_1 (not blocked by REDIS_URI_2)
            await().atMost(Durations.FIVE_SECONDS).until(future::isDone);
            connection = future.toCompletableFuture().join();
            assertThat(connection).isNotNull();
            assertThat(connection.getCurrentEndpoint()).isEqualTo(REDIS_URI_1);

            // And: REDIS_URI_2 should NOT be in endpoints yet (still hanging)
            assertThat(connection.getEndpoints()).doesNotContain(REDIS_URI_2);

            // When: Complete the hanging connection
            testClient.getBuilder().proceedHangingConnections();

            // Then: Eventually all endpoints should be available (including REDIS_URI_2)
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(connection.getEndpoints()).contains(REDIS_URI_1, REDIS_URI_2, REDIS_URI_3);
            });
        }

    }

}
