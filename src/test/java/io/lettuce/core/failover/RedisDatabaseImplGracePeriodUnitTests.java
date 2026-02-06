package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;
import static io.lettuce.TestTags.UNIT_TEST;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testcontainers.shaded.org.awaitility.Durations;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.api.CircuitBreakerConfig;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.test.MutableClock;

/**
 * Unit tests for grace period functionality in {@link RedisDatabaseImpl}.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag(UNIT_TEST)
@DisplayName("RedisDatabaseImpl Grace Period Unit Tests")
class RedisDatabaseImplGracePeriodUnitTests {

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private DatabaseEndpoint endpoint;

    @Mock
    private HealthCheck healthCheck;

    private CircuitBreakerImpl circuitBreaker;

    private RedisURI uri;

    private RedisDatabaseImpl<StatefulRedisConnection<String, String>> database;

    private MutableClock clock;

    @BeforeEach
    void setUp() {
        uri = RedisURI.create("redis://dummy:9999");
        DatabaseConfig config = DatabaseConfig.builder(uri).weight(1.0f).build();

        // Create a real circuit breaker for testing
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.builder().failureRateThreshold(50.0f).minimumNumberOfFailures(10)
                .build();
        circuitBreaker = new CircuitBreakerImpl(cbConfig);

        // Setup health check mock
        when(healthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);

        clock = new MutableClock();

        database = new RedisDatabaseImpl<>(config, connection, endpoint, circuitBreaker, healthCheck, clock);
    }

    @Nested
    @DisplayName("Grace Period Tracking Tests")
    class GracePeriodTrackingTests {

        @Test
        @DisplayName("Should not be in grace period initially")
        void shouldNotBeInGracePeriodInitially() {
            // Then: Database should not be in grace period
            assertThat(database.isInGracePeriod()).isFalse();
        }

        @Test
        @DisplayName("Should start grace period with positive duration")
        void shouldStartGracePeriodWithPositiveDuration() {
            // When: Start grace period with 1000ms
            database.startGracePeriod(Durations.ONE_SECOND);

            // Then: Database should be in grace period
            assertThat(database.isInGracePeriod()).isTrue();
        }

        @Test
        @DisplayName("Should not start grace period with zero duration")
        void shouldNotStartGracePeriodWithZeroDuration() {
            // When: Start grace period with 0ms
            database.startGracePeriod(Duration.ZERO);

            // Then: Database should not be in grace period
            assertThat(database.isInGracePeriod()).isFalse();
        }

        @Test
        @DisplayName("Should not start grace period with negative duration")
        void shouldNotStartGracePeriodWithNegativeDuration() {
            // When: Start grace period with negative duration
            database.startGracePeriod(Duration.ofMillis(-1000L));

            // Then: Database should not be in grace period
            assertThat(database.isInGracePeriod()).isFalse();
        }

        @Test
        @DisplayName("Should automatically exit grace period after duration expires")
        void shouldAutomaticallyExitGracePeriodAfterExpiry() throws InterruptedException {
            // When: Start grace period with 100ms
            database.startGracePeriod(Durations.ONE_HUNDRED_MILLISECONDS);
            assertThat(database.isInGracePeriod()).isTrue();

            // Advance the clock by 150ms
            clock.tick(Duration.ofMillis(150));

            // Then: Database should not be in grace period
            assertThat(database.isInGracePeriod()).isFalse();
        }

        @Test
        @DisplayName("Should reset circuit breaker to CLOSED when grace period expires")
        void shouldResetCircuitBreakerWhenGracePeriodExpires() throws InterruptedException {
            // Given: Circuit breaker is OPEN
            circuitBreaker.transitionTo(CircuitBreaker.State.OPEN);
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

            // When: Start grace period with 100ms
            database.startGracePeriod(Durations.ONE_HUNDRED_MILLISECONDS);
            assertThat(database.isInGracePeriod()).isTrue();

            // Advance the clock by 150ms
            clock.tick(Duration.ofMillis(150));

            // When: Check if in grace period (this triggers the reset logic)
            boolean inGracePeriod = database.isInGracePeriod();

            // Then: Database should not be in grace period and circuit breaker should be CLOSED
            assertThat(inGracePeriod).isFalse();
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("Should not reset circuit breaker if grace period is still active")
        void shouldNotResetCircuitBreakerDuringGracePeriod() {
            // Given: Circuit breaker is OPEN
            circuitBreaker.transitionTo(CircuitBreaker.State.OPEN);
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

            // When: Start grace period with long duration
            database.startGracePeriod(Durations.TEN_SECONDS);

            // Then: Circuit breaker should still be OPEN during grace period
            assertThat(database.isInGracePeriod()).isTrue();
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("Should allow restarting grace period with new duration")
        void shouldAllowRestartingGracePeriod() {
            // Given: Database is in grace period
            database.startGracePeriod(Durations.TEN_SECONDS);
            assertThat(database.isInGracePeriod()).isTrue();

            // When: Restart grace period with new duration
            database.startGracePeriod(Duration.ofSeconds(20));

            // Then: Database should still be in grace period
            assertThat(database.isInGracePeriod()).isTrue();
        }

    }

    @Nested
    @DisplayName("Health Check Predicate Tests")
    class HealthCheckPredicateTests {

        @Test
        @DisplayName("isHealthy() should return false when in grace period even if otherwise healthy")
        void shouldNotBeHealthyWhenInGracePeriod() {
            // Given: Health check is healthy, circuit breaker is closed, but in grace period
            when(healthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker.transitionTo(CircuitBreaker.State.CLOSED);
            database.startGracePeriod(Durations.TEN_SECONDS);

            // Then: Database should not be healthy (grace period blocks automatic selection)
            assertThat(database.isHealthy()).isFalse();
        }

        @Test
        @DisplayName("isHealthy() should return true when grace period expires")
        void shouldBeHealthyWhenGracePeriodExpires() throws InterruptedException {
            // Given: Database in grace period
            when(healthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker.transitionTo(CircuitBreaker.State.CLOSED);
            database.startGracePeriod(Durations.ONE_HUNDRED_MILLISECONDS);
            assertThat(database.isHealthy()).isFalse();

            // Advance the clock by 150ms
            clock.tick(Duration.ofMillis(150));

            // Then: Database should be healthy
            assertThat(database.isHealthy()).isTrue();
        }

        @Test
        @DisplayName("isHealthyIgnoreGracePeriod() should return true during grace period if otherwise healthy")
        void shouldBeHealthyIgnoringGracePeriodWhenOtherConditionsMet() {
            // Given: Health check is healthy, circuit breaker is closed, in grace period
            when(healthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker.transitionTo(CircuitBreaker.State.CLOSED);
            database.startGracePeriod(Durations.TEN_SECONDS);

            // Then: Database should be healthy when ignoring grace period (for forced switches)
            assertThat(database.isHealthyIgnoreGracePeriod()).isTrue();
        }

        @Test
        @DisplayName("isHealthyIgnoreGracePeriod() should return false when health check is unhealthy")
        void shouldNotBeHealthyIgnoringGracePeriodWhenHealthCheckUnhealthy() {
            // Given: Health check is unhealthy, in grace period
            when(healthCheck.getStatus()).thenReturn(HealthStatus.UNHEALTHY);
            circuitBreaker.transitionTo(CircuitBreaker.State.CLOSED);
            database.startGracePeriod(Durations.TEN_SECONDS);

            // Then: Database should not be healthy even when ignoring grace period
            assertThat(database.isHealthyIgnoreGracePeriod()).isFalse();
        }

        @Test
        @DisplayName("isHealthyIgnoreGracePeriod() should return false when circuit breaker is open")
        void shouldNotBeHealthyIgnoringGracePeriodWhenCircuitBreakerOpen() {
            // Given: Circuit breaker is open, in grace period
            when(healthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker.transitionTo(CircuitBreaker.State.OPEN);
            database.startGracePeriod(Durations.TEN_SECONDS);

            // Then: Database should not be healthy even when ignoring grace period
            assertThat(database.isHealthyIgnoreGracePeriod()).isFalse();
        }

        @Test
        @DisplayName("isHealthy() should check all conditions: grace period, health check, and circuit breaker")
        void shouldCheckAllConditionsForHealthy() {
            // Given: All conditions are met
            when(healthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker.transitionTo(CircuitBreaker.State.CLOSED);

            // Then: Database should be healthy
            assertThat(database.isHealthy()).isTrue();

            // When: Enter grace period
            database.startGracePeriod(Durations.TEN_SECONDS);
            // Then: Should not be healthy
            assertThat(database.isHealthy()).isFalse();

            // When: Exit grace period but health check becomes unhealthy
            database.clearGracePeriod();
            when(healthCheck.getStatus()).thenReturn(HealthStatus.UNHEALTHY);
            // Then: Should not be healthy
            assertThat(database.isHealthy()).isFalse();

            // When: Health check becomes healthy but circuit breaker opens
            when(healthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker.transitionTo(CircuitBreaker.State.OPEN);
            // Then: Should not be healthy
            assertThat(database.isHealthy()).isFalse();
        }

    }

    @Nested
    @DisplayName("Failback Blocking Tests")
    class FailbacksBehaviourTests {

        @Test
        @DisplayName("Should block failback when higher weight database is in grace period")
        void shouldBlockFailbackWhenHigherWeightDatabaseInGracePeriod() {
            // Given: High weight database (redis1) in grace period
            RedisURI redis1Uri = RedisURI.create("redis://dummy:9999");
            DatabaseConfig config1 = DatabaseConfig.builder(redis1Uri).weight(1.0f).build();
            StatefulRedisConnection<String, String> connection1 = mock(StatefulRedisConnection.class);
            DatabaseEndpoint endpoint1 = mock(DatabaseEndpoint.class);
            HealthCheck healthCheck1 = mock(HealthCheck.class);
            CircuitBreakerImpl circuitBreaker1 = new CircuitBreakerImpl(CircuitBreakerConfig.builder().build());

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> redis1 = new RedisDatabaseImpl<>(config1, connection1,
                    endpoint1, circuitBreaker1, healthCheck1);

            when(healthCheck1.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker1.transitionTo(CircuitBreaker.State.CLOSED);
            redis1.startGracePeriod(Durations.TEN_SECONDS);

            // And: Low weight database (redis2) is healthy
            RedisURI redis2Uri = RedisURI.create("redis://dummy:9998");
            DatabaseConfig config2 = DatabaseConfig.builder(redis2Uri).weight(0.5f).build();
            StatefulRedisConnection<String, String> connection2 = mock(StatefulRedisConnection.class);
            DatabaseEndpoint endpoint2 = mock(DatabaseEndpoint.class);
            HealthCheck healthCheck2 = mock(HealthCheck.class);
            CircuitBreakerImpl circuitBreaker2 = new CircuitBreakerImpl(CircuitBreakerConfig.builder().build());

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> redis2 = new RedisDatabaseImpl<>(config2, connection2,
                    endpoint2, circuitBreaker2, healthCheck2);

            when(healthCheck2.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker2.transitionTo(CircuitBreaker.State.CLOSED);

            // Then: redis1 should not be healthy (blocked by grace period)
            assertThat(redis1.isHealthy()).isFalse();
            // And: redis2 should be healthy
            assertThat(redis2.isHealthy()).isTrue();

            // Then: Failback to redis1 should be blocked (it's not healthy due to grace period)
            // In a real scenario, the failback check would select redis2 over redis1
        }

        @Test
        @DisplayName("Should allow failback when higher weight database exits grace period")
        void shouldAllowFailbackWhenHigherWeightDatabaseExitsGracePeriod() throws InterruptedException {
            // Given: High weight database (redis1) in grace period
            RedisURI redis1Uri = RedisURI.create("redis://dummy:9999");
            DatabaseConfig config1 = DatabaseConfig.builder(redis1Uri).weight(1.0f).build();
            StatefulRedisConnection<String, String> connection1 = mock(StatefulRedisConnection.class);
            DatabaseEndpoint endpoint1 = mock(DatabaseEndpoint.class);
            HealthCheck healthCheck1 = mock(HealthCheck.class);
            CircuitBreakerImpl circuitBreaker1 = new CircuitBreakerImpl(CircuitBreakerConfig.builder().build());

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> redis1 = new RedisDatabaseImpl<>(config1, connection1,
                    endpoint1, circuitBreaker1, healthCheck1, clock);

            when(healthCheck1.getStatus()).thenReturn(HealthStatus.HEALTHY);
            circuitBreaker1.transitionTo(CircuitBreaker.State.CLOSED);
            redis1.startGracePeriod(Durations.ONE_HUNDRED_MILLISECONDS);

            // Then: redis1 should not be healthy initially
            assertThat(redis1.isHealthy()).isFalse();

            // Advance the clock by 150ms
            clock.tick(Duration.ofMillis(150));

            // Then: redis1 should be healthy (grace period expired)
            assertThat(redis1.isHealthy()).isTrue();

            // Then: Failback to redis1 should be allowed (it's healthy now)
        }

    }

}
