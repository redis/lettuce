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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Durations;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.CircuitBreakerConfig;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.api.MultiDbOptions;
import io.lettuce.core.failover.api.RedisNoHealthyDatabaseException;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.PingStrategy;
import io.lettuce.test.settings.TestSettings;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Integration tests for MultiDb behavior when no healthy database is available. Tests cover all API types (sync, async,
 * reactive) and various scenarios where all databases become unhealthy.
 * <p>
 * Scenarios tested:
 * <ul>
 * <li>All databases circuit breakers open - commands should fail with {@link RedisNoHealthyDatabaseException}</li>
 * <li>All databases in grace period - commands should fail with {@link RedisNoHealthyDatabaseException} (on current unhealthy
 * DB)</li>
 * <li>All databases unreachable - commands should fail with {@link RedisNoHealthyDatabaseException} when no healthy database is
 * available</li>
 * <li>Behavior across sync, async, and reactive APIs</li>
 * </ul>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(INTEGRATION_TEST)
@DisplayName("No Healthy Database Behavior Integration Tests")
class NoHealthyDatabaseBehaviorIntegrationTests extends AbstractRedisClientTest {

    // Backing redis instances
    private static final int redis1_port = TestSettings.port(8);

    private static final int redis2_port = TestSettings.port(9);

    // Redis Endpoints exposed by toxiproxy
    private static final RedisURI redis1ProxyUri = RedisURI.Builder.redis(host, TestSettings.proxyPort()).build();

    private static final RedisURI redis2ProxyUri = RedisURI.Builder.redis(host, TestSettings.proxyPort(1)).build();

    private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", TestSettings.proxyAdminPort());

    private static Proxy redisProxy1;

    private static Proxy redisProxy2;

    // Map of proxy endpoints to proxy objects
    private static Map<RedisURI, Proxy> proxyMap = new HashMap<>();

    private StatefulRedisMultiDbConnection<String, String> connection;

    private MultiDbClient multiDbClient;

    private CircuitBreakerConfig cbConfig;

    // Direct clients for cleanup
    private RedisClient directClient1;

    private RedisClient directClient2;

    // Timeout config
    private static final Duration COMMAND_TIMEOUT = Durations.TWO_HUNDRED_MILLISECONDS;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

    /// CB config

    /* 10% failure rate threshold */
    private static final int CB_FAILURE_RATE_THRESHOLD = 10;

    /* minimum 5 failures */
    private static final int CB_MIN_FAILURES = 5;

    /* 5 second window */
    private static final int CB_METRICS_WINDOW_SIZE = 5;

    /* 500 ms grace period */
    private static final Duration GRACE_PERIOD = Durations.FIVE_HUNDRED_MILLISECONDS;

    private static final int HEALTH_CHECK_INTERVAL = 400;

    private static final int HEALTH_CHECK_NUM_PROBES = 2;

    private static final int HEALTH_CHECK_DELAY_IN_BETWEEN_PROBES = 50;

    @BeforeAll
    public static void setupToxiproxy() throws IOException {
        if (tp.getProxyOrNull("redis-1") != null) {
            tp.getProxy("redis-1").delete();
        }
        if (tp.getProxyOrNull("redis-2") != null) {
            tp.getProxy("redis-2").delete();
        }

        redisProxy1 = tp.createProxy("redis-1", "0.0.0.0:" + TestSettings.proxyPort(), "redis-failover:" + redis1_port);
        redisProxy2 = tp.createProxy("redis-2", "0.0.0.0:" + TestSettings.proxyPort(1), "redis-failover:" + redis2_port);

        proxyMap.put(redis1ProxyUri, redisProxy1);
        proxyMap.put(redis2ProxyUri, redisProxy2);
    }

    @AfterAll
    public static void tearDownToxiproxy() throws IOException {
        if (redisProxy1 != null) {
            redisProxy1.delete();
        }
        if (redisProxy2 != null) {
            redisProxy2.delete();
        }
    }

    @BeforeEach
    void setUp() {
        // Enable all proxies
        enableAllToxiproxy();

        // Create direct clients for cleanup
        directClient1 = RedisClient.create(RedisURI.Builder.redis(host, redis1_port).build());
        directClient2 = RedisClient.create(RedisURI.Builder.redis(host, redis2_port).build());

        // Flush all data
        directClient1.connect().sync().flushall();
        directClient2.connect().sync().flushall();

        // Create circuit breaker config with low thresholds for testing
        // Only need 5 failures minimum (instead of the default 1000)
        cbConfig = CircuitBreakerConfig.builder().failureRateThreshold(CB_FAILURE_RATE_THRESHOLD)
                .minimumNumberOfFailures(CB_MIN_FAILURES).metricsWindowSize(CB_METRICS_WINDOW_SIZE).build();

        // Create client options with short timeouts
        ClientOptions clientOptions = ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(COMMAND_TIMEOUT).build())
                .socketOptions(SocketOptions.builder().connectTimeout(CONNECT_TIMEOUT).build()).build();

        // Create health check config and supplier
        HealthCheckStrategy.Config healthCheckConfig = HealthCheckStrategy.Config.builder().interval(HEALTH_CHECK_INTERVAL)
                .numProbes(HEALTH_CHECK_NUM_PROBES).delayInBetweenProbes(HEALTH_CHECK_DELAY_IN_BETWEEN_PROBES).build();
        HealthCheckStrategySupplier healthCheckSupplier = (uri, options) -> new PingStrategy(options, healthCheckConfig);

        // Create MultiDbClient with proxy endpoints and custom circuit breaker config
        DatabaseConfig config1 = DatabaseConfig.builder(redis1ProxyUri).weight(1.0f).circuitBreakerConfig(cbConfig)
                .clientOptions(clientOptions).healthCheckStrategySupplier(healthCheckSupplier).build();
        DatabaseConfig config2 = DatabaseConfig.builder(redis2ProxyUri).weight(0.5f).circuitBreakerConfig(cbConfig)
                .clientOptions(clientOptions).healthCheckStrategySupplier(healthCheckSupplier).build();

        multiDbClient = MultiDbClient.create(Arrays.asList(config1, config2),
                MultiDbOptions.builder().gracePeriod(GRACE_PERIOD).build());
        connection = multiDbClient.connect(StringCodec.UTF8);
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (multiDbClient != null) {
            multiDbClient.shutdown();
        }
        if (directClient1 != null) {
            directClient1.shutdown();
        }
        if (directClient2 != null) {
            directClient2.shutdown();
        }
        enableAllToxiproxy();
    }

    private void enableAllToxiproxy() {
        try {
            if (redisProxy1 != null) {
                redisProxy1.enable();
            }
            if (redisProxy2 != null) {
                redisProxy2.enable();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disableAllToxiproxy() {
        try {
            if (redisProxy1 != null) {
                redisProxy1.disable();
            }
            if (redisProxy2 != null) {
                redisProxy2.disable();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens circuit breaker by triggering enough failures.
     */
    private CircuitBreaker openCircuitBreaker(RedisURI endpoint) {
        RedisDatabaseImpl<?> database = (RedisDatabaseImpl<?>) connection.getDatabase(endpoint);
        CircuitBreaker cb = database.getCircuitBreaker();

        // Disable proxy to cause failures
        try {
            proxyMap.get(endpoint).disable();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Execute commands that will fail
        for (int i = 0; i < cbConfig.getMinimumNumberOfFailures() + 5; i++) {
            try {
                connection.async().get("key" + i);
            } catch (Exception e) {
                // Ignore
            }
        }
        return cb;
    }

    private void waitCircuitBreakerFor(CircuitBreaker cb, CircuitBreaker.State state) {
        // Wait for circuit breaker to open
        await().atMost(Durations.TWO_SECONDS).untilAsserted(() -> {
            assertThat(cb.getCurrentState()).isEqualTo(state);
        });
    }

    private void triggerCircuitBreakerAndWaitForState(CircuitBreaker.State state, RedisURI... endpoint) {
        for (RedisURI uri : endpoint) {
            CircuitBreaker cb = openCircuitBreaker(uri);
            waitCircuitBreakerFor(cb, state);
        }
    }

    @Nested
    @DisplayName("Sync API - No Healthy Database Tests")
    class SyncApiNoHealthyDatabaseTests {

        @Test
        @DisplayName("Should fail sync commands when all circuit breakers are open")
        void shouldFailSyncCommandsWhenAllCircuitBreakersOpen() {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When/Then: Sync commands should fail with RedisCircuitBreakerException
            assertThatThrownBy(() -> connection.sync().set("key", "value")).isInstanceOf(RedisNoHealthyDatabaseException.class)
                    .hasMessageContaining("No healthy database available!");

            assertThatThrownBy(() -> connection.sync().get("key")).isInstanceOf(RedisNoHealthyDatabaseException.class)
                    .hasMessageContaining("No healthy database available!");
        }

        @Test
        @DisplayName("Should fail sync commands when all databases are unreachable by health check")
        void shouldFailSyncCommandsWhenAllDatabasesUnreachableByHealthCheck() throws Exception {
            // Given: All proxies disabled
            disableAllToxiproxy();

            List<RedisDatabaseImpl<?>> dbList = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                    .map(uri -> (RedisDatabaseImpl<?>) connection.getDatabase(uri)).collect(Collectors.toList());

            // Wait for all databases to become unhealthy
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(dbList.stream().allMatch(db -> !db.isHealthy())).isTrue();
            });

            // When/Then: Sync commands should fail with timeout
            assertThatThrownBy(() -> connection.sync().set("key", "value")).isInstanceOf(RedisNoHealthyDatabaseException.class);

            assertThatThrownBy(() -> connection.sync().get("key")).isInstanceOf(RedisNoHealthyDatabaseException.class);
        }

    }

    @Nested
    @DisplayName("Async API - No Healthy Database Tests")
    class AsyncApiNoHealthyDatabaseTests {

        @Test
        @DisplayName("Should fail async commands when all circuit breakers are open")
        void shouldFailAsyncCommandsWhenAllCircuitBreakersOpen() throws Exception {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When: Execute async commands
            RedisFuture<String> setFuture = connection.async().set("key", "value");
            RedisFuture<String> getFuture = connection.async().get("key");

            // Then: Futures should complete exceptionally with RedisCircuitBreakerException
            assertThatThrownBy(() -> setFuture.get()).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisNoHealthyDatabaseException.class);

            assertThatThrownBy(() -> getFuture.get()).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisNoHealthyDatabaseException.class);
        }

        @Test
        @DisplayName("Should fail async commands when all databases are unreachable")
        void shouldFailAsyncCommandsWhenAllDatabasesUnreachableByHealthCheck() throws Exception {
            // Given: All proxies disabled
            disableAllToxiproxy();

            List<RedisDatabaseImpl<?>> dbList = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                    .map(uri -> (RedisDatabaseImpl<?>) connection.getDatabase(uri)).collect(Collectors.toList());
            // Wait for current database to become unhealthy
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(dbList.stream().allMatch(db -> !db.isHealthy())).isTrue();
            });

            // When: Execute async commands
            RedisFuture<String> setFuture = connection.async().set("key", "value");
            RedisFuture<String> getFuture = connection.async().get("key");

            // Then: Futures should complete exceptionally
            assertThatThrownBy(() -> setFuture.get()).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisNoHealthyDatabaseException.class);

            assertThatThrownBy(() -> getFuture.get()).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisNoHealthyDatabaseException.class);
        }

        @Test
        @DisplayName("Should fail multiple async commands when all circuit breakers are open")
        void shouldFailMultipleAsyncCommandsWhenAllCircuitBreakersOpen() {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When: Execute multiple async commands
            AtomicInteger failureCount = new AtomicInteger(0);
            int commandCount = 10;

            for (int i = 0; i < commandCount; i++) {
                connection.async().set("key" + i, "value" + i).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        failureCount.incrementAndGet();
                    }
                });
            }

            // Then: All commands should fail
            await().atMost(Durations.TWO_SECONDS).untilAsserted(() -> {
                assertThat(failureCount.get()).isEqualTo(commandCount);
            });
        }

    }

    @Nested
    @DisplayName("Reactive API - No Healthy Database Tests")
    class ReactiveApiNoHealthyDatabaseTests {

        @Test
        @DisplayName("Should fail reactive commands when all circuit breakers are open")
        void shouldFailReactiveCommandsWhenAllCircuitBreakersOpen() {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When/Then: Reactive commands should emit error
            RedisReactiveCommands<String, String> reactive = connection.reactive();

            StepVerifier.create(reactive.set("key", "value")).expectError(RedisNoHealthyDatabaseException.class).verify();

            StepVerifier.create(reactive.get("key")).expectError(RedisNoHealthyDatabaseException.class).verify();
        }

        @Test
        @DisplayName("Should fail reactive commands when all databases are unreachable by health check")
        void shouldFailReactiveCommandsWhenAllDatabasesUnreachableByHealthCheck() throws Exception {
            // Given: All proxies disabled
            disableAllToxiproxy();

            List<RedisDatabaseImpl<?>> dbList = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                    .map(uri -> (RedisDatabaseImpl<?>) connection.getDatabase(uri)).collect(Collectors.toList());

            // Wait for all databases to become unhealthy
            await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
                assertThat(dbList.stream().allMatch(db -> !db.isHealthy())).isTrue();
            });

            // When/Then: Reactive commands should emit timeout error
            RedisReactiveCommands<String, String> reactive = connection.reactive();

            StepVerifier.create(reactive.set("key", "value")).expectError(RedisNoHealthyDatabaseException.class).verify();

            StepVerifier.create(reactive.get("key")).expectError(RedisNoHealthyDatabaseException.class).verify();
        }

        @Test
        @DisplayName("Should fail reactive command stream when all circuit breakers are open")
        void shouldFailReactiveCommandStreamWhenAllCircuitBreakersOpen() {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When/Then: Reactive command stream should emit errors
            RedisReactiveCommands<String, String> reactive = connection.reactive();

            Mono<String> command1 = reactive.set("key1", "value1");
            Mono<String> command2 = reactive.set("key2", "value2");
            Mono<String> command3 = reactive.get("key1");

            StepVerifier.create(command1).expectError(RedisNoHealthyDatabaseException.class).verify();

            StepVerifier.create(command2).expectError(RedisNoHealthyDatabaseException.class).verify();

            StepVerifier.create(command3).expectError(RedisNoHealthyDatabaseException.class).verify();
        }

        @Test
        @DisplayName("Should handle reactive command errors gracefully with onErrorResume")
        void shouldHandleReactiveCommandErrorsGracefullyWithOnErrorResume() {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When: Use onErrorResume to handle errors
            RedisReactiveCommands<String, String> reactive = connection.reactive();

            Mono<String> commandWithFallback = reactive.set("key", "value").onErrorResume(RedisNoHealthyDatabaseException.class,
                    e -> Mono.just("FALLBACK"));

            // Then: Should receive fallback value
            StepVerifier.create(commandWithFallback).expectNext("FALLBACK").verifyComplete();
        }

    }

    @Nested
    @DisplayName("Mixed API - No Healthy Database Tests")
    class MixedApiNoHealthyDatabaseTests {

        @Test
        @DisplayName("Should fail commands across all API types when all circuit breakers are open")
        void shouldFailCommandsAcrossAllApiTypesWhenAllCircuitBreakersOpen() throws Exception {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When/Then: All API types should fail

            // Sync API
            assertThatThrownBy(() -> connection.sync().set("syncKey", "syncValue"))
                    .isInstanceOf(RedisNoHealthyDatabaseException.class);

            // Async API
            RedisFuture<String> asyncFuture = connection.async().set("asyncKey", "asyncValue");
            assertThatThrownBy(() -> asyncFuture.get()).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisNoHealthyDatabaseException.class);

            // Reactive API
            StepVerifier.create(connection.reactive().set("reactiveKey", "reactiveValue"))
                    .expectError(RedisNoHealthyDatabaseException.class).verify();
        }

        @Test
        @DisplayName("Should consistently fail commands when switching between API types")
        void shouldConsistentlyFailCommandsWhenSwitchingBetweenApiTypes() throws Exception {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // When/Then: Switching between API types should consistently fail
            for (int i = 0; i < 5; i++) {
                final int index = i;
                // Try sync
                assertThatThrownBy(() -> connection.sync().get("key" + index))
                        .isInstanceOf(RedisNoHealthyDatabaseException.class);

                // Try async
                RedisFuture<String> future = connection.async().get("key" + index);
                assertThatThrownBy(() -> future.get()).isInstanceOf(ExecutionException.class)
                        .hasCauseInstanceOf(RedisNoHealthyDatabaseException.class);

                // Try reactive
                StepVerifier.create(connection.reactive().get("key" + index)).expectError(RedisNoHealthyDatabaseException.class)
                        .verify();
            }
        }

    }

    @Nested
    @DisplayName("Recovery After No Healthy Database Tests")
    class RecoveryAfterNoHealthyDatabaseTests {

        @Test
        @DisplayName("Should recover sync commands after databases become healthy again")
        void shouldRecoverSyncCommandsAfterDatabasesBecomeHealthyAgain() {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // Verify commands fail
            assertThatThrownBy(() -> connection.sync().set("key", "value")).isInstanceOf(RedisNoHealthyDatabaseException.class);

            // When: Re-enable proxies
            enableAllToxiproxy();

            // Wait for grace period to expire and circuit breakers to close
            await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentDatabase().isHealthy()).isTrue();
            });

            // Then: Commands should succeed
            String result = connection.sync().set("recoveryKey", "recoveryValue");
            assertThat(result).isEqualTo("OK");

            String value = connection.sync().get("recoveryKey");
            assertThat(value).isEqualTo("recoveryValue");
        }

        @Test
        @DisplayName("Should recover async commands after databases become healthy again")
        void shouldRecoverAsyncCommandsAfterDatabasesBecomeHealthyAgain() throws Exception {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // Verify commands fail
            RedisFuture<String> failedFuture = connection.async().set("key", "value");
            assertThatThrownBy(() -> failedFuture.get()).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisNoHealthyDatabaseException.class);

            // When: Re-enable proxies
            enableAllToxiproxy();

            // Wait for grace period to expire and circuit breakers to close
            await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentDatabase().isHealthy()).isTrue();
            });

            // Then: Commands should succeed
            RedisFuture<String> setFuture = connection.async().set("recoveryKey", "recoveryValue");
            assertThat(setFuture.get(5, TimeUnit.SECONDS)).isEqualTo("OK");

            RedisFuture<String> getFuture = connection.async().get("recoveryKey");
            assertThat(getFuture.get(5, TimeUnit.SECONDS)).isEqualTo("recoveryValue");
        }

        @Test
        @DisplayName("Should recover reactive commands after databases become healthy again")
        void shouldRecoverReactiveCommandsAfterDatabasesBecomeHealthyAgain() {
            // Given: Both databases have open circuit breakers
            triggerCircuitBreakerAndWaitForState(CircuitBreaker.State.OPEN, redis1ProxyUri, redis2ProxyUri);

            // Verify commands fail
            StepVerifier.create(connection.reactive().set("key", "value")).expectError(RedisNoHealthyDatabaseException.class)
                    .verify();

            // When: Re-enable proxies
            enableAllToxiproxy();

            // Wait for grace period to expire and circuit breakers to close
            await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
                assertThat(connection.getCurrentDatabase().isHealthy()).isTrue();
            });

            // Then: Commands should succeed
            StepVerifier.create(connection.reactive().set("recoveryKey", "recoveryValue")).expectNext("OK").verifyComplete();

            StepVerifier.create(connection.reactive().get("recoveryKey")).expectNext("recoveryValue").verifyComplete();
        }

    }

}
