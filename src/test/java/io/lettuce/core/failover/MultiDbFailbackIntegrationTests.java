package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;

import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.PingStrategy;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for failback functionality in {@link StatefulRedisMultiDbConnectionImpl}.
 * <p>
 * Tests the automatic failback to higher-priority databases when they become healthy again.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(INTEGRATION_TEST)
class MultiDbFailbackIntegrationTests {

    private static final RedisURI URI1 = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();

    private static final RedisURI URI2 = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port() + 1).build();

    private static final RedisURI URI3 = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port() + 2).build();

    private static final HealthCheckStrategySupplier SIMPLE_PING_STRATEGY = (uri, options) -> new PingStrategy(options,
            HealthCheckStrategy.Config.builder().interval(1000).timeout(1000).numProbes(1).build());

    private MultiDbClient client;

    private StatefulRedisMultiDbConnection<String, String> connection;

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            FastShutdown.shutdown(client);
        }
    }

    @Nested
    @DisplayName("Failback Configuration Tests")
    class FailbackConfigurationTests {

        @Test
        @DisplayName("Should create connection with failback enabled by default")
        void shouldCreateConnectionWithFailbackEnabledByDefault() {
            // Given: Client with default options (failback enabled)
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            client = MultiDbClient.create(Arrays.asList(db1, db2));

            // When: Connect
            connection = client.connect();

            // Then: Connection should be created successfully
            assertThat(connection).isNotNull();
            assertThat(connection.isOpen()).isTrue();
            assertThat(client.getMultiDbOptions().isFailbackSupported()).isTrue();
        }

        @Test
        @DisplayName("Should create connection with failback disabled")
        void shouldCreateConnectionWithFailbackDisabled() {
            // Given: Client with failback disabled
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false).build();
            client = MultiDbClient.create(Arrays.asList(db1, db2), options);

            // When: Connect
            connection = client.connect();

            // Then: Connection should be created successfully
            assertThat(connection).isNotNull();
            assertThat(connection.isOpen()).isTrue();
            assertThat(client.getMultiDbOptions().isFailbackSupported()).isFalse();
        }

        @Test
        @DisplayName("Should create connection with custom failback check interval")
        void shouldCreateConnectionWithCustomFailbackCheckInterval() {
            // Given: Client with custom failback interval (10 seconds)
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackCheckInterval(Durations.TEN_SECONDS).build();
            client = MultiDbClient.create(Arrays.asList(db1, db2), options);

            // When: Connect
            connection = client.connect();

            // Then: Connection should be created successfully with custom interval
            assertThat(connection).isNotNull();
            assertThat(connection.isOpen()).isTrue();
            assertThat(client.getMultiDbOptions().getFailbackCheckInterval()).isEqualTo(Durations.TEN_SECONDS);
        }

    }

    @Nested
    @DisplayName("Failback Behavior Tests")
    class FailbackBehaviorTests {

        @Test
        @DisplayName("Should failback to higher-weighted database after manual switch")
        void shouldFailbackToHigherWeightedDatabaseAfterManualSwitch() {
            // Given: Two databases with health checks and very short failback interval for faster testing
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            // Use short failback interval (500ms) for faster testing
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true)
                    .failbackCheckInterval(Durations.ONE_HUNDRED_MILLISECONDS).build();
            client = MultiDbClient.create(Arrays.asList(db1, db2), options);
            connection = client.connect();

            // When: Connection starts on highest-weighted database (URI1)
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            // And: Manually switch to lower-weighted database (simulating a failover scenario)
            connection.switchTo(URI2);
            assertThat(connection.getCurrentEndpoint()).isEqualTo(URI2);

            // Then: Failback should occur to URI1 (higher weight) within a few failback intervals
            // Since URI1 is healthy and has higher weight, failback should happen
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });
        }

        @Test
        @DisplayName("Should failback to highest-weighted database among multiple databases")
        void shouldFailbackToHighestWeightedDatabaseAmongMultiple() {
            // Given: Three databases with different weights
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.7f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db3 = DatabaseConfig.builder(URI3).weight(0.3f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true)
                    .failbackCheckInterval(Durations.ONE_HUNDRED_MILLISECONDS).build();
            client = MultiDbClient.create(Arrays.asList(db1, db2, db3), options);
            connection = client.connect();

            // When: Start on highest-weighted database
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            // And: Manually switch to middle-weighted database
            connection.switchTo(URI2);
            assertThat(connection.getCurrentEndpoint()).isEqualTo(URI2);

            // Then: Should failback to highest-weighted database (URI1)
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });
        }

        @Test
        @DisplayName("Should not failback when current database has highest weight")
        void shouldNotFailbackWhenCurrentDatabaseHasHighestWeight() {
            // Given: Two databases with health checks
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true)
                    .failbackCheckInterval(Durations.ONE_HUNDRED_MILLISECONDS).build();
            client = MultiDbClient.create(Arrays.asList(db1, db2), options);
            connection = client.connect();

            // When: Connection is on highest-weighted database (URI1)
            await().atMost(Durations.FIVE_SECONDS).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            RedisURI initialEndpoint = connection.getCurrentEndpoint();

            // Then: Should maintain connection (no failback needed)
            await().during(Durations.ONE_SECOND).atMost(Durations.TWO_SECONDS).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(connection.getCurrentEndpoint()).isEqualTo(initialEndpoint);
                    });
        }

        @Test
        @DisplayName("Should perform multiple failbacks over time")
        void shouldPerformMultipleFailbacksOverTime() {
            // Given: Two databases with health checks
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true)
                    .failbackCheckInterval(Durations.ONE_HUNDRED_MILLISECONDS).build();
            client = MultiDbClient.create(Arrays.asList(db1, db2), options);
            connection = client.connect();

            // When: Start on highest-weighted database
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            // First failback cycle: switch to URI2, then failback to URI1
            connection.switchTo(URI2);
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            // Second failback cycle: switch to URI2 again, then failback to URI1 again
            connection.switchTo(URI2);
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            // Then: Should successfully perform multiple failbacks
            assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
        }

    }

    @Nested
    @DisplayName("Failback Disabled Tests")
    class FailbackDisabledTests {

        @Test
        @DisplayName("Should not perform failback when disabled")
        void shouldNotPerformFailbackWhenDisabled() {
            // Given: Client with failback disabled
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false).build();

            client = MultiDbClient.create(Arrays.asList(db1, db2), options);
            connection = client.connect();

            // When: Connection is established and manually switched to lower-weighted database
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            // Manually switch to lower-weighted database
            connection.switchTo(URI2);
            assertThat(connection.getCurrentEndpoint()).isEqualTo(URI2);

            // Then: Should maintain connection to URI2 (no failback should occur even though URI1 has higher weight)
            await().during(Durations.ONE_SECOND).atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI2);
            });
        }

    }

    @Nested
    @DisplayName("Failback with Different Intervals Tests")
    class FailbackWithDifferentIntervalsTests {

        @Test
        @DisplayName("Should respect custom failback check interval")
        void shouldRespectCustomFailbackCheckInterval() {
            // Given: Databases with very short failback interval (300ms)
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true)
                    .failbackCheckInterval(Duration.ofMillis(300L)).build();
            client = MultiDbClient.create(Arrays.asList(db1, db2), options);
            connection = client.connect();

            // When: Start on URI1, then switch to URI2
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            connection.switchTo(URI2);
            assertThat(connection.getCurrentEndpoint()).isEqualTo(URI2);

            // Then: Failback should happen quickly (within 2 seconds due to short interval)
            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });
        }

        @Test
        @DisplayName("Should work with default failback interval")
        void shouldWorkWithDefaultFailbackInterval() {
            // Given: Databases with default failback interval (120 seconds)
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            // Use default options (failback enabled with 120s interval)
            client = MultiDbClient.create(Arrays.asList(db1, db2));
            connection = client.connect();

            // When: Connection is established
            await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentEndpoint()).isEqualTo(URI1);
            });

            // Then: Connection should work normally with default interval
            assertThat(connection).isNotNull();
            assertThat(connection.isOpen()).isTrue();
            assertThat(client.getMultiDbOptions().getFailbackCheckInterval()).isEqualTo(Durations.TWO_MINUTES);
        }

    }

    @Nested
    @DisplayName("Connection Lifecycle Tests")
    class ConnectionLifecycleTests {

        @Test
        @DisplayName("Should cleanup failback task on connection close")
        void shouldCleanupFailbackTaskOnClose() {
            // Given: Connection with failback enabled
            DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();
            DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).healthCheckStrategySupplier(SIMPLE_PING_STRATEGY)
                    .build();

            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true)
                    .failbackCheckInterval(Durations.ONE_SECOND).build();

            client = MultiDbClient.create(Arrays.asList(db1, db2), options);
            connection = client.connect();

            assertThat(connection.isOpen()).isTrue();

            // When: Close connection
            connection.close();

            // Then: Connection should be closed
            assertThat(connection.isOpen()).isFalse();
            ScheduledFuture<?> failbackTask = ReflectionTestUtils.getField(connection, "failbackTask");
            assertTrue(failbackTask.isCancelled());
        }

    }

}
