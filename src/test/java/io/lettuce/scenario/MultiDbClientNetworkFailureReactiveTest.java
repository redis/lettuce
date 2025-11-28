package io.lettuce.scenario;

import static io.lettuce.TestTags.SCENARIO_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import io.lettuce.core.failover.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.failover.DatabaseConfig;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.Wait;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Scenario test for MultiDbClient with reactive commands under network failures. This test combines patterns from
 * MultiDbClientIntegrationTests and ConnectionInterruptionReactiveTest to verify that MultiDbClient handles network failures
 * correctly when using reactive commands.
 */
@Tag(SCENARIO_TEST)
public class MultiDbClientNetworkFailureReactiveTest {

    private static final Logger log = LoggerFactory.getLogger(MultiDbClientNetworkFailureReactiveTest.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(1);

    private static final Duration DELAY_AFTER = Duration.ofMillis(500);

    private static final Duration NETWORK_FAILURE_INTERVAL = Duration.ofSeconds(60);

    private static Endpoint activeActiveEndpoint;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private MultiDbClient client;

    private StatefulRedisMultiDbConnection<String, String> connection;

    @BeforeAll
    public static void setup() {
        // Try to get a "re-active-active" endpoint, fall back to "re-standalone" if not available
        activeActiveEndpoint = Endpoints.DEFAULT.getEndpoint("re-active-active");
        assumeTrue(activeActiveEndpoint != null, "Skipping test because no Redis endpoint is configured!");
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    @DisplayName("MultiDbClient with reactive commands should handle network failures gracefully")
    public void testMultiDbClientWithReactiveCommandsDuringNetworkFailure() {
        // Setup: Configure MultiDbClient with endpoints from the active-active configuration
        // Pass recommended options for connection interruption through DatabaseConfig
        List<DatabaseConfig> databaseConfigs = createDatabaseConfigs(activeActiveEndpoint,
                RecommendedSettingsProvider.forConnectionInterruption());
        client = MultiDbClient.create(databaseConfigs);

        // Connect to the MultiDbClient
        connection = client.connect();

        // Get reactive commands interface
        RedisReactiveCommands<String, String> reactive = connection.reactive();

        String keyName = "multidb-counter";

        // Setup: Set initial counter value
        StepVerifier.create(reactive.set(keyName, "0")).expectNext("OK").verifyComplete();

        AtomicLong commandsSubmitted = new AtomicLong();
        List<Throwable> capturedExceptions = new CopyOnWriteArrayList<>();

        // Start a flux that imitates an application using the client
        // This follows the exact pattern from ConnectionInterruptionReactiveTest#testWithReactiveCommands
        Disposable subscription = Flux.interval(Duration.ofMillis(100)).flatMap(i -> reactive.incr(keyName)
                // We should count all attempts, because Lettuce retransmits failed commands
                .doFinally(value -> {
                    commandsSubmitted.incrementAndGet();
                    log.info("Commands submitted {}", commandsSubmitted.get());
                }).onErrorResume(e -> {
                    //log.warn("Error executing command", e);
                    capturedExceptions.add(e);
                    return Mono.empty();
                })).subscribe();

        // Wait for some commands to be executed before triggering the fault
        Wait.untilTrue(() -> commandsSubmitted.get() > 10).waitOrTimeout();

        // Trigger the fault injection: network_failure action
        Map<String, Object> params = new HashMap<>();
        params.put("bdb_id", activeActiveEndpoint.getBdbId());
        params.put("delay", NETWORK_FAILURE_INTERVAL.getSeconds());

        log.info("Triggering network_failure for bdb_id: {}, delay: {}", activeActiveEndpoint.getBdbId(),
                NETWORK_FAILURE_INTERVAL);

        Mono<Boolean> actionCompleted = faultClient.triggerActionAndWait("network_failure", params, CHECK_INTERVAL, DELAY_AFTER,
                Duration.ofSeconds(120));

        StepVerifier.create(actionCompleted).expectNext(true).verifyComplete();

        // Wait a bit more to allow recovery
        Wait.untilTrue(() -> commandsSubmitted.get() > 1000).waitOrTimeout();

        // Stop the command execution
        subscription.dispose();

        // Verify results
        StepVerifier.create(reactive.get(keyName).map(Long::parseLong)).consumeNextWith(value -> {
            log.info("Final counter value: {}, commands submitted: {}", value, commandsSubmitted.get());
            // The counter should have incremented, showing that commands were executed
            assertThat(value).isGreaterThan(0);
        }).verifyComplete();

        log.info("Captured exceptions: {}", capturedExceptions);

        // verify that counter equals to number of commands submitted
        StepVerifier.create(reactive.get(keyName).map(Long::parseLong)).consumeNextWith(value -> {
            assertThat(value).isEqualTo(commandsSubmitted.get());
        }).verifyComplete();

        log.info("Test completed successfully. MultiDbClient handled network failure with reactive commands.");
    }

    /**
     * Creates DatabaseConfig instances from an Endpoint. If the endpoint has multiple endpoints configured (active-active),
     * creates a DatabaseConfig for each. Otherwise, creates a single DatabaseConfig.
     *
     * @param endpoint the endpoint configuration
     * @param clientOptions the client options to use for all database configs
     * @return list of DatabaseConfig instances
     */
    private List<DatabaseConfig> createDatabaseConfigs(Endpoint endpoint, io.lettuce.core.ClientOptions clientOptions) {
        List<DatabaseConfig> configs = new ArrayList<>();

        CircuitBreaker.CircuitBreakerConfig circuitBreakerConfig = new CircuitBreaker.CircuitBreakerConfig(5.0f, 5,
                CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());

        List<String> endpoints = endpoint.getEndpoints();
        if (endpoints == null || endpoints.isEmpty()) {
            throw new IllegalStateException("Endpoint has no configured endpoints");
        }

        // Create a DatabaseConfig for each endpoint
        float weight = 1.0f;
        for (String endpointUri : endpoints) {
            RedisURI uri = RedisURI.builder(RedisURI.create(endpointUri))
                    .withAuthentication(endpoint.getUsername(), endpoint.getPassword())
                    .withTimeout(Duration.ofSeconds(5)).build();

            configs.add(new DatabaseConfig(uri, weight, clientOptions, circuitBreakerConfig));
            weight /= 2; // Decrease weight for subsequent endpoints
        }

        log.info("Created {} DatabaseConfig(s) from endpoint", configs.size());
        return configs;
    }

}
