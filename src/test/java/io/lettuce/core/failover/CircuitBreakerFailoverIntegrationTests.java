package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.CircuitBreakerStateListener;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.WithPassword;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for circuit breaker automatic failover functionality using ToxiProxy for network failure simulation. These
 * tests simulate failures on one endpoint to trigger the circuit breaker and verify automatic database switching.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@Tag(INTEGRATION_TEST)
class CircuitBreakerFailoverIntegrationTests extends AbstractRedisClientTest {

    // Backing redis instances
    private static final int redis1_port = TestSettings.port(8);

    private static final int redis2_port = TestSettings.port(9);

    // Redis Endpoints exposed by toxiproxy
    private static final RedisURI redis1ProxyUri = RedisURI.Builder.redis(host, TestSettings.proxyPort()).withPassword(passwd)
            .build();

    private static final RedisURI redis2ProxyUri = RedisURI.Builder.redis(host, TestSettings.proxyPort(1)).withPassword(passwd)
            .build();

    // Redis Endpoints directly connecting to the backing redis instances
    private static final RedisURI redis1Uri = RedisURI.Builder.redis(host, redis1_port).build();

    private static final RedisURI redis2Uri = RedisURI.Builder.redis(host, redis2_port).build();

    private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", TestSettings.proxyAdminPort());

    private static Proxy redisProxy1;

    private static Proxy redisProxy2;

    // Map of proxy endpoints to proxy objects
    private static Map<RedisURI, Proxy> proxyMap = new HashMap<>();

    private RedisCommands<String, String> redis1Conn;

    private RedisCommands<String, String> redis2Conn;

    private StatefulRedisMultiDbConnection<String, String> connection;

    private MultiDbClient multiDbClient;

    CircuitBreaker.CircuitBreakerConfig cbConfig;

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
    public static void cleanupToxiproxy() throws IOException {
        if (redisProxy1 != null)
            redisProxy1.delete();
        if (redisProxy2 != null)
            redisProxy2.delete();
    }

    @BeforeEach
    void setUp() throws IOException {
        redis1Conn = client.connect(redis1Uri).sync();
        redis2Conn = client.connect(redis2Uri).sync();

        WithPassword.enableAuthentication(this.redis1Conn);
        this.redis1Conn.auth(passwd);

        WithPassword.enableAuthentication(this.redis2Conn);
        this.redis2Conn.auth(passwd);

        // Create circuit breaker config with low thresholds for testing
        cbConfig = new CircuitBreaker.CircuitBreakerConfig(10.0f, // 10% failure rate threshold
                5, // Only need 5 failures minimum (instead of default 1000)
                CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions(), 5);

        // Create MultiDbClient with proxy endpoints and custom circuit breaker config
        multiDbClient = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(cbConfig, redis1ProxyUri, redis2ProxyUri));
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(2)).build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(500))) // Enable command timeout
                .build();
        multiDbClient.setOptions(clientOptions);

        connection = multiDbClient.connect(StringCodec.UTF8);
        enableAllToxiproxy();
    }

    private void enableAllToxiproxy() throws IOException {
        tp.getProxies().forEach(proxy -> {
            try {
                for (Toxic toxic : proxy.toxics().getAll()) {
                    toxic.remove();
                }
                proxy.enable();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @AfterEach
    void tearDown() {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }

        if (multiDbClient != null) {
            multiDbClient.shutdown();
        }

        if (redis1Conn != null) {
            WithPassword.disableAuthentication(redis1Conn);
            redis1Conn.configRewrite();
            redis1Conn.getStatefulConnection().close();
        }

        if (redis2Conn != null) {
            WithPassword.disableAuthentication(redis2Conn);
            redis2Conn.configRewrite();
            redis2Conn.getStatefulConnection().close();
        }
    }

    @Test
    void shouldReceiveCircuitBreakerStateChangeEvents() throws InterruptedException {
        // Given: A listener to capture state change events
        List<CircuitBreakerStateChangeEvent> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        CircuitBreakerStateListener listener = event -> {
            events.add(event);
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                latch.countDown();
            }
        };

        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);
        cb.addListener(listener);

        // When: Trigger failures to open the circuit breaker
        // Simulate failures by executing commands against a shutdown Redis instance
        RedisURI failingEndpoint = currentEndpoint;
        shutdownRedisInstance(failingEndpoint);

        // Execute commands that will fail using ASYNC API
        for (int i = 0; i < 20; i++) {
            try {
                RedisFuture<String> future = connection.async().get("key" + i);
                future.get(1, TimeUnit.SECONDS); // Wait for completion
            } catch (Exception e) {
                // Expected - connection failures or timeouts
            }
        }

        // Force evaluation
        ((CircuitBreakerImpl) cb).evaluateMetrics();

        // Then: Should receive state change event
        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertThat(received).as("Should receive circuit breaker state change event").isTrue();
        assertThat(events).isNotEmpty();

        CircuitBreakerStateChangeEvent event = events.stream().filter(e -> e.getNewState() == CircuitBreaker.State.OPEN)
                .findFirst().orElse(null);

        assertThat(event).isNotNull();
        assertThat(event.getPreviousState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(event.getNewState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(event.getCircuitBreaker()).isSameAs(cb);
        assertThat(event.getTimestamp()).isGreaterThan(0);

        // Cleanup
        cb.removeListener(listener);
        restartRedisInstance(failingEndpoint);
    }

    @Test
    void shouldAutomaticallyFailoverWhenCircuitBreakerOpens() throws Exception {
        // Given: Two endpoints, start on endpoint1
        RedisURI endpoint1 = redis1ProxyUri;
        RedisURI endpoint2 = redis2ProxyUri;

        connection.switchToDatabase(endpoint1);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(endpoint1);

        // Write a test key to endpoint2 (so we can verify failover)
        connection.switchToDatabase(endpoint2);
        connection.async().set("failover-test-key", "endpoint2-value").get(1, TimeUnit.SECONDS);
        connection.switchToDatabase(endpoint1);

        // Track state changes
        CountDownLatch failoverLatch = new CountDownLatch(1);

        CircuitBreakerStateListener listener = event -> {
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                failoverLatch.countDown();
            }
        };

        CircuitBreaker cb1 = connection.getCircuitBreaker(endpoint1);
        cb1.addListener(listener);

        // When: Shutdown endpoint1 to trigger failures
        shutdownRedisInstance(endpoint1);

        // Execute commands that will fail on endpoint1 using ASYNC API
        for (int i = 0; i < 20; i++) {
            try {
                RedisFuture<String> future = connection.async().get("key" + i);
                future.get(1, TimeUnit.SECONDS); // Wait for completion
            } catch (Exception e) {
                // Expected - connection failures or timeouts
            }
        }

        // Force evaluation
        ((CircuitBreakerImpl) cb1).evaluateMetrics();

        // Then: Should automatically failover to endpoint2
        boolean failedOver = failoverLatch.await(5, TimeUnit.SECONDS);
        assertThat(failedOver).as("Should failover to healthy endpoint").isTrue();
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(endpoint2, connection.getCurrentEndpoint()));

        // Verify we can read from endpoint2
        RedisFuture<String> future = connection.async().get("failover-test-key");
        String value = future.get(1, TimeUnit.SECONDS);
        assertThat(value).isEqualTo("endpoint2-value");

        // Cleanup
        cb1.removeListener(listener);
        restartRedisInstance(endpoint1);
    }

    @Test
    void shouldTrackFailuresInCircuitBreakerMetrics() {
        // Given: Current endpoint
        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);

        assertEquals(0, cb.getSnapshot().getFailureCount());

        // When: Shutdown Redis to cause failures
        shutdownRedisInstance(currentEndpoint);

        int aimedFailureCount = cbConfig.getMinimumNumberOfFailures() - 1;
        // Execute commands that will fail using ASYNC API
        int failureCount = 0;
        for (int i = 0; i < aimedFailureCount; i++) {
            try {
                RedisFuture<String> future = connection.async().get("key" + i);
                future.get(1, TimeUnit.SECONDS); // Wait for completion
            } catch (Exception e) {
                failureCount++;
            }
        }

        // Then: Metrics should track failures
        long finalFailures = cb.getSnapshot().getFailureCount();
        assertEquals(aimedFailureCount, failureCount);
        assertEquals(aimedFailureCount, finalFailures);

        // Cleanup
        restartRedisInstance(currentEndpoint);
    }

    @Test
    void shouldOpenCircuitBreakerAfterThresholdExceeded() {
        // Given: Current endpoint with circuit breaker
        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);

        assertThat(cb.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // When: Shutdown Redis and trigger failures beyond threshold
        shutdownRedisInstance(currentEndpoint);

        for (int i = 0; i < 20; i++) {
            try {
                RedisFuture<String> future = connection.async().get("key" + i);
                future.get(1, TimeUnit.SECONDS); // Wait for completion
            } catch (Exception e) {
                // Expected
            }
        }

        // Force evaluation
        ((CircuitBreakerImpl) cb).evaluateMetrics();

        // Then: Circuit breaker should open
        assertThat(cb.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Cleanup
        restartRedisInstance(currentEndpoint);
    }

    @Test
    void shouldNotifyMultipleListenersOnStateChange() throws InterruptedException {
        // Given: Multiple listeners
        AtomicInteger listener1Count = new AtomicInteger(0);
        AtomicInteger listener2Count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        CircuitBreakerStateListener listener1 = event -> {
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                listener1Count.incrementAndGet();
                latch.countDown();
            }
        };

        CircuitBreakerStateListener listener2 = event -> {
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                listener2Count.incrementAndGet();
                latch.countDown();
            }
        };

        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);
        cb.addListener(listener1);
        cb.addListener(listener2);

        // When: Trigger circuit breaker to open
        shutdownRedisInstance(currentEndpoint);

        for (int i = 0; i < 20; i++) {
            try {
                RedisFuture<String> future = connection.async().get("key" + i);
                future.get(1, TimeUnit.SECONDS); // Wait for completion
            } catch (Exception e) {
                // Expected
            }
        }

        ((CircuitBreakerImpl) cb).evaluateMetrics();

        // Then: Both listeners should be notified
        boolean notified = latch.await(5, TimeUnit.SECONDS);
        assertThat(notified).isTrue();
        assertThat(listener1Count.get()).isEqualTo(1);
        assertThat(listener2Count.get()).isEqualTo(1);

        // Cleanup
        cb.removeListener(listener1);
        cb.removeListener(listener2);
        restartRedisInstance(currentEndpoint);
    }

    /**
     * Simulates shutting down a Redis instance by injecting a latency toxic with very high latency. This causes commands to
     * timeout, triggering circuit breaker failures.
     *
     * @param endpoint the endpoint to simulate shutdown for
     */
    private void shutdownRedisInstance(RedisURI endpoint) {
        try {
            Proxy proxy = proxyMap.get(endpoint);
            if (proxy != null) {
                // Inject latency toxic with 10 seconds latency to force command timeouts
                // (command timeout is 500ms, so this will definitely cause timeouts)
                proxy.toxics().latency("latency_" + endpoint.getPort(), ToxicDirection.DOWNSTREAM, 10000);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to shutdown Redis instance via ToxiProxy", e);
        }
    }

    /**
     * Simulates restarting a Redis instance by removing the latency toxic. This restores connectivity to the Redis instance.
     *
     * @param endpoint the endpoint to simulate restart for
     */
    private void restartRedisInstance(RedisURI endpoint) {
        try {
            Proxy proxy = proxyMap.get(endpoint);
            if (proxy != null) {
                // Remove the latency toxic to restore connectivity
                String toxicName = "latency_" + endpoint.getPort();
                Toxic toxic = proxy.toxics().get(toxicName);
                if (toxic != null) {
                    toxic.remove();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to restart Redis instance via ToxiProxy", e);
        }
    }

    // ========================================
    // Reactive API Tests
    // ========================================

    @Test
    void shouldReceiveCircuitBreakerStateChangeEventsReactive() throws InterruptedException {
        // Given: A listener for state change events
        List<CircuitBreakerStateChangeEvent> events = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        CircuitBreakerStateListener listener = event -> {
            events.add(event);
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                latch.countDown();
            }
        };

        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);
        cb.addListener(listener);

        // When: Trigger failures to open the circuit breaker
        // Simulate failures by executing commands against a shutdown Redis instance
        RedisURI failingEndpoint = currentEndpoint;
        shutdownRedisInstance(failingEndpoint);

        // Execute commands that will fail using REACTIVE API
        for (int i = 0; i < 20; i++) {
            try {
                connection.reactive().get("key" + i).block(Duration.ofSeconds(1));
            } catch (Exception e) {
                // Expected - connection failures or timeouts
            }
        }

        // Force evaluation
        ((CircuitBreakerImpl) cb).evaluateMetrics();

        // Then: Should receive state change event
        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertThat(received).as("Should receive circuit breaker state change event").isTrue();
        assertThat(events).isNotEmpty();

        CircuitBreakerStateChangeEvent event = events.stream().filter(e -> e.getNewState() == CircuitBreaker.State.OPEN)
                .findFirst().orElse(null);

        assertThat(event).isNotNull();
        assertThat(event.getPreviousState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(event.getNewState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(event.getCircuitBreaker()).isSameAs(cb);
        assertThat(event.getTimestamp()).isGreaterThan(0);

        // Cleanup
        cb.removeListener(listener);
        restartRedisInstance(failingEndpoint);
    }

    @Test
    void shouldAutomaticallyFailoverWhenCircuitBreakerOpensReactive() throws Exception {
        // Given: Two endpoints, start on endpoint1
        RedisURI endpoint1 = redis1ProxyUri;
        RedisURI endpoint2 = redis2ProxyUri;

        connection.switchToDatabase(endpoint1);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(endpoint1);

        // Write a test key to endpoint2 (so we can verify failover)
        connection.switchToDatabase(endpoint2);
        connection.reactive().set("failover-test-key-reactive", "endpoint2-value").block(Duration.ofSeconds(1));
        connection.switchToDatabase(endpoint1);

        // Track state changes
        // AtomicReference<RedisURI> failedOverTo = new AtomicReference<>();
        CountDownLatch failoverLatch = new CountDownLatch(1);

        CircuitBreakerStateListener listener = event -> {
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                failoverLatch.countDown();
            }
        };

        CircuitBreaker cb1 = connection.getCircuitBreaker(endpoint1);
        cb1.addListener(listener);

        // When: Shutdown endpoint1 to trigger failures
        shutdownRedisInstance(endpoint1);

        int aimedFailureCount = cbConfig.getMinimumNumberOfFailures();
        // Execute commands that will fail on endpoint1 using REACTIVE API
        for (int i = 0; i < aimedFailureCount; i++) {
            try {
                connection.reactive().get("key" + i).block(Duration.ofSeconds(1));
            } catch (Exception e) {
                // Expected - connection failures or timeouts
            }
        }

        // Force evaluation
        ((CircuitBreakerImpl) cb1).evaluateMetrics();

        // Then: Should automatically failover to endpoint2
        boolean failedOver = failoverLatch.await(5, TimeUnit.SECONDS);
        assertThat(failedOver).as("Should failover to healthy endpoint").isTrue();
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(endpoint2, connection.getCurrentEndpoint()));

        // Verify we can read from endpoint2
        String value = connection.reactive().get("failover-test-key-reactive").block(Duration.ofSeconds(1));
        assertThat(value).isEqualTo("endpoint2-value");

        // Cleanup
        cb1.removeListener(listener);
        restartRedisInstance(endpoint1);
    }

    @Test
    void shouldTrackFailuresInCircuitBreakerMetricsReactive() {
        // Given: Current endpoint
        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);

        assertEquals(0, cb.getSnapshot().getFailureCount());

        // When: Shutdown Redis to cause failures
        shutdownRedisInstance(currentEndpoint);

        // Execute commands that will fail using REACTIVE API
        int failureCount = 0;
        int aimedFailureCount = cbConfig.getMinimumNumberOfFailures() - 1;
        for (int i = 0; i < aimedFailureCount; i++) {
            try {
                connection.reactive().get("key" + i).block(Duration.ofSeconds(1));
            } catch (Exception e) {
                failureCount++;
            }
        }

        // Then: Metrics should track failures
        long finalFailures = cb.getSnapshot().getFailureCount();
        assertEquals(aimedFailureCount, failureCount);
        assertEquals(aimedFailureCount, finalFailures);

        // Cleanup
        restartRedisInstance(currentEndpoint);
    }

    @Test
    void shouldOpenCircuitBreakerAfterThresholdExceededReactive() {
        // Given: Current endpoint with circuit breaker
        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);

        assertThat(cb.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // When: Shutdown Redis and trigger failures beyond threshold
        shutdownRedisInstance(currentEndpoint);

        for (int i = 0; i < 20; i++) {
            try {
                connection.reactive().get("key" + i).block(Duration.ofSeconds(1));
            } catch (Exception e) {
                // Expected
            }
        }

        // Force evaluation
        ((CircuitBreakerImpl) cb).evaluateMetrics();

        // Then: Circuit breaker should open
        assertThat(cb.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Cleanup
        restartRedisInstance(currentEndpoint);
    }

    @Test
    void shouldNotifyMultipleListenersOnStateChangeReactive() throws InterruptedException {
        // Given: Multiple listeners
        AtomicInteger listener1Count = new AtomicInteger(0);
        AtomicInteger listener2Count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        CircuitBreakerStateListener listener1 = event -> {
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                listener1Count.incrementAndGet();
                latch.countDown();
            }
        };

        CircuitBreakerStateListener listener2 = event -> {
            if (event.getNewState() == CircuitBreaker.State.OPEN) {
                listener2Count.incrementAndGet();
                latch.countDown();
            }
        };

        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        CircuitBreaker cb = connection.getCircuitBreaker(currentEndpoint);
        cb.addListener(listener1);
        cb.addListener(listener2);

        // When: Trigger circuit breaker to open
        shutdownRedisInstance(currentEndpoint);

        for (int i = 0; i < 20; i++) {
            try {
                connection.reactive().get("key" + i).block(Duration.ofSeconds(1));
            } catch (Exception e) {
                // Expected
            }
        }

        ((CircuitBreakerImpl) cb).evaluateMetrics();

        // Then: Both listeners should be notified
        boolean notified = latch.await(5, TimeUnit.SECONDS);
        assertThat(notified).isTrue();
        assertThat(listener1Count.get()).isEqualTo(1);
        assertThat(listener2Count.get()).isEqualTo(1);

        // Cleanup
        cb.removeListener(listener1);
        cb.removeListener(listener2);
        restartRedisInstance(currentEndpoint);
    }

}
