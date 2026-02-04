package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter.Red;
import org.awaitility.Durations;
import org.junit.jupiter.api.*;

import com.google.common.reflect.Reflection;

import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.failover.api.RedisDatabase;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.PingStrategy;
import io.lettuce.core.failover.health.ProbingPolicy;
import io.lettuce.test.MutableClock;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for grace period functionality in multi-database failover.
 * <p>
 * These tests verify that the grace period prevents immediate failback to a database that was just failed over from, ensuring
 * stability and preventing rapid switching between databases.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(INTEGRATION_TEST)
@DisplayName("Grace Period Integration Tests")
class GracePeriodIntegrationTests extends TestSupport {

    private static final RedisURI redis1Uri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();

    private static final RedisURI redis2Uri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port() + 1).build();

    private MultiDbClient multiDbClient;

    private StatefulRedisMultiDbConnection<String, String> connection;

    @BeforeEach
    void setUp() {
        // Create client with short grace period for faster tests
        DatabaseConfig db1Config = DatabaseConfig.builder(redis1Uri).weight(1.0f)
                .healthCheckStrategySupplier(PingStrategy.DEFAULT)
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();
        DatabaseConfig db2Config = DatabaseConfig.builder(redis2Uri).weight(0.5f)
                .healthCheckStrategySupplier(PingStrategy.DEFAULT)
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        MultiDbOptions options = MultiDbOptions.builder().gracePeriod(Durations.ONE_SECOND) // 1 second grace period
                .failbackCheckInterval(Durations.TWO_HUNDRED_MILLISECONDS) // Check every 500ms
                .build();

        multiDbClient = MultiDbClient.create(Arrays.asList(db1Config, db2Config), options);
        connection = multiDbClient.connect();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (multiDbClient != null) {
            FastShutdown.shutdown(multiDbClient);
        }
    }

    @Test
    @DisplayName("Should allow manual switch and automatic failback without grace period")
    void shouldAllowManualSwitchAndAutomaticFailback() {
        // Given: Connected to redis1 (highest weight)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(connection.isHealthy(redis1Uri)).isTrue();
            assertThat(connection.getCurrentEndpoint()).isEqualTo(redis1Uri);
        });

        // When: Manually switch to redis2 (lower weight)
        // Note: Manual switches do NOT start grace period
        connection.switchTo(redis2Uri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(redis2Uri);

        // Then: Automatic failback to redis1 should occur (no grace period blocking it)
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(connection.getCurrentEndpoint()).isEqualTo(redis1Uri);
        });
    }

    @Test
    @DisplayName("Should allow forced switch to any healthy database")
    void shouldAllowForcedSwitchToHealthyDatabase() {
        // Given: Connected to redis1
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(connection.getCurrentEndpoint()).isEqualTo(redis1Uri);
        });

        // When: Force switch to redis2
        connection.switchTo(redis2Uri);

        // Then: Should be connected to redis2
        assertThat(connection.getCurrentEndpoint()).isEqualTo(redis2Uri);

        // And: Should be able to execute commands
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        // When: Force switch back to redis1
        connection.switchTo(redis1Uri);

        // Then: Should be connected to redis1
        assertThat(connection.getCurrentEndpoint()).isEqualTo(redis1Uri);
    }

    @Test
    @DisplayName("Should handle zero grace period - immediate failback allowed")
    void shouldHandleZeroGracePeriod() {
        // Given: Client with zero grace period (immediate failback allowed)
        DatabaseConfig db1Config = DatabaseConfig.builder(redis1Uri).weight(1.0f)
                .healthCheckStrategySupplier(PingStrategy.DEFAULT).build();
        DatabaseConfig db2Config = DatabaseConfig.builder(redis2Uri).weight(0.5f)
                .healthCheckStrategySupplier(PingStrategy.DEFAULT).build();

        MultiDbOptions options = MultiDbOptions.builder().gracePeriod(Duration.ZERO) // No grace period
                .failbackCheckInterval(Durations.FIVE_HUNDRED_MILLISECONDS).build();

        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(db1Config, db2Config), options);
        StatefulRedisMultiDbConnection<String, String> testConnection = testClient.connect();

        try {
            // Given: Connected to redis1
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis1Uri);
            });

            // When: Manually switch to redis2
            testConnection.switchTo(redis2Uri);
            assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis2Uri);

            // Then: With zero grace period, should failback to redis1 immediately
            // (within 1 second since failback check is every 500ms)
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis1Uri);
            });
        } finally {
            testConnection.close();
            FastShutdown.shutdown(testClient);
        }
    }

    @Test
    @DisplayName("Should verify grace period configuration is applied")
    void shouldVerifyGracePeriodConfiguration() {
        // Given: Client with custom grace period
        assertThat(multiDbClient.getMultiDbOptions().getGracePeriod()).isEqualTo(Durations.ONE_SECOND);

        // When: Create client with different grace period
        MultiDbOptions options = MultiDbOptions.builder().gracePeriod(Durations.FIVE_SECONDS).build();
        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(DatabaseConfig.builder(redis1Uri).weight(1.0f).build(),
                DatabaseConfig.builder(redis2Uri).weight(0.5f).build()), options);

        try {
            // Then: Should have the configured grace period
            assertThat(testClient.getMultiDbOptions().getGracePeriod()).isEqualTo(Durations.FIVE_SECONDS);
        } finally {
            FastShutdown.shutdown(testClient);
        }
    }

    @Test
    @DisplayName("Should not failback when higher weight database is in grace period after health check transition")
    void shouldNotFailbackWhenCandidateInGracePeriod() {
        // Given: Client with controllable health check strategy and short grace period
        HealthCheckStrategy.Config healthCheckConfig = HealthCheckStrategy.Config.builder().interval(100) // 100ms
                .timeout(1000).numProbes(1).delayInBetweenProbes(10).build();

        TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(healthCheckConfig);
        HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

        DatabaseConfig config1 = DatabaseConfig.builder(redis1Uri).weight(1.0f).healthCheckStrategySupplier(supplier).build();
        DatabaseConfig config2 = DatabaseConfig.builder(redis2Uri).weight(0.5f).healthCheckStrategySupplier(supplier).build();

        MultiDbOptions options = MultiDbOptions.builder().gracePeriod(Duration.ofSeconds(3)) // 3 second grace period
                .failbackCheckInterval(Durations.FIVE_HUNDRED_MILLISECONDS) // Check every 500ms
                .build();

        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2), options);
        StatefulRedisMultiDbConnection<String, String> testConnection = testClient.connect();

        try {
            // Given: Connected to redis1 (highest weight) and both are healthy
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.isHealthy(redis1Uri)).isTrue();
                assertThat(testConnection.isHealthy(redis2Uri)).isTrue();
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis1Uri);
            });

            RedisDatabase currentDatabase = testConnection.getCurrentDatabase();
            MutableClock clock = new MutableClock();
            ReflectionTestUtils.setField(currentDatabase, "clock", clock);

            // When: Make redis1 unhealthy (this triggers failover and starts grace period)
            testStrategy.setHealthStatus(redis1Uri, HealthStatus.UNHEALTHY);

            // Then: Should failover to redis2
            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis2Uri);
            });

            // When: Make redis1 healthy again
            testStrategy.setHealthStatus(redis1Uri, HealthStatus.HEALTHY);

            // Then: Should NOT failback to redis1 during grace period (3 seconds)
            // Advance the clock by 1.5 seconds (half the grace period) and verify still on redis2
            clock.tick(Duration.ofMillis(1500));

            // Wait 1.5 seconds (half the grace period) and verify still on redis2
            assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis2Uri);

            // Then advance the clock by another 1.5 seconds (total 3 seconds)
            clock.tick(Duration.ofMillis(1500));

            // Then: After grace period expires, should failback to redis1
            await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis1Uri);
            });

        } finally {
            testConnection.close();
            FastShutdown.shutdown(testClient);
        }
    }

    @Test
    @DisplayName("Should allow failback when higher weight database exits grace period after health check transition")
    void shouldFailbackWhenCandidateExitsGracePeriod() {
        // Given: Client with controllable health check strategy and short grace period
        HealthCheckStrategy.Config healthCheckConfig = HealthCheckStrategy.Config.builder().interval(100) // 100ms
                .timeout(1000).numProbes(1).delayInBetweenProbes(10).build();

        TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(healthCheckConfig);
        HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

        DatabaseConfig config1 = DatabaseConfig.builder(redis1Uri).weight(1.0f).healthCheckStrategySupplier(supplier).build();
        DatabaseConfig config2 = DatabaseConfig.builder(redis2Uri).weight(0.5f).healthCheckStrategySupplier(supplier).build();

        MultiDbOptions options = MultiDbOptions.builder().gracePeriod(Durations.ONE_SECOND) // 1 second grace period
                .failbackCheckInterval(Durations.TWO_HUNDRED_MILLISECONDS) // Check every 500ms
                .build();

        MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2), options);
        StatefulRedisMultiDbConnection<String, String> testConnection = testClient.connect();

        try {
            // Given: Connected to redis1 (highest weight) and both are healthy
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.isHealthy(redis1Uri)).isTrue();
                assertThat(testConnection.isHealthy(redis2Uri)).isTrue();
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis1Uri);
            });

            // When: Make redis1 unhealthy (this triggers failover and starts grace period)
            testStrategy.setHealthStatus(redis1Uri, HealthStatus.UNHEALTHY);

            // Then: Should failover to redis2
            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis2Uri);
            });

            // When: Make redis1 healthy again
            testStrategy.setHealthStatus(redis1Uri, HealthStatus.HEALTHY);

            // Then: After grace period expires (2 seconds), should failback to redis1
            await().atMost(4, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(testConnection.getCurrentEndpoint()).isEqualTo(redis1Uri);
            });

        } finally {
            testConnection.close();
            FastShutdown.shutdown(testClient);
        }
    }

    /**
     * Test implementation of HealthCheckStrategy with controllable health status.
     */
    static class TestHealthCheckStrategy implements HealthCheckStrategy {

        private final HealthCheckStrategy.Config config;

        private final Map<RedisURI, AtomicReference<HealthStatus>> endpointHealth = new ConcurrentHashMap<>();

        public TestHealthCheckStrategy(HealthCheckStrategy.Config config) {
            this.config = config;
        }

        @Override
        public HealthStatus doHealthCheck(RedisURI endpoint) {
            return endpointHealth.computeIfAbsent(endpoint, k -> new AtomicReference<>(HealthStatus.HEALTHY)).get();
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

        public void setHealthStatus(RedisURI endpoint, HealthStatus status) {
            endpointHealth.computeIfAbsent(endpoint, k -> new AtomicReference<>(HealthStatus.HEALTHY)).set(status);
        }

    }

}
