package io.lettuce.scenario;

import static io.lettuce.TestTags.SCENARIO_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final int MINIMUM_NUMBER_OF_FAILURES = 5;

    private static final float FAILURE_RATE_THRESHOLD = 5.0f;

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
        ClientOptions clientOptions = ClientOptions.builder()
                // Disable auto-reconnect to test failover behavior
                // when auto-reconnect is enabled, commands are retransmitted automatically
                // and failover is not triggered until command timeout
                .autoReconnect(false)
                // .socketOptions(connectionInterruptionSocketOptions())
                .build();

        List<DatabaseConfig> databaseConfigs = createDatabaseConfigs(activeActiveEndpoint, clientOptions);
        client = MultiDbClient.create(databaseConfigs);

        // Connect to the MultiDbClient
        // Issue 1 (TBD): DatabaseConfig.clientOptions not considered, and there is no API to configure custom one on
        // MultiDbClient
        connection = client.connect();

        // Get reactive commands interface
        RedisReactiveCommands<String, String> reactive = connection.reactive();

        String keyName = "multidb-counter";

        // Setup: Set initial counter value
        StepVerifier.create(reactive.set(keyName, "0")).expectNext("OK").verifyComplete();

        AtomicLong commandsSubmitted = new AtomicLong();

        // Number of commands that completed with error.
        // Based on CircuitBreakerConfig we exe expect at least (minimumNumberOfFailures or failureThreshold)
        // for example (failureThreshold=5% or minimumNumberOfFailures=5) we expect at least 5 failures to trigger failover
        AtomicLong failureCount = new AtomicLong();

        // track number of cancelled commands
        AtomicLong cancelledCommands = new AtomicLong();

        List<Throwable> capturedExceptions = new CopyOnWriteArrayList<>();

        AtomicBoolean faultTriggered = new AtomicBoolean(false);

        // Create the command flux that will run until we have 1000 successful commands
        Flux<Long> commandFlux = Flux.interval(Duration.ofMillis(100)).flatMap(i -> {
            // Count the attempt BEFORE executing the command
            long attemptNumber = commandsSubmitted.incrementAndGet();

            // Trigger fault injection after 10 commands (only once)
            if (attemptNumber > 10 && !faultTriggered.getAndSet(true)) {
                triggerNetworkFailure();
            }

            return reactive.incr(keyName).doOnNext(
                    (v) -> log.info("Command submitted: {}, counter: {}, failures: {}", attemptNumber, v, failureCount.get()))
                    .doOnCancel(cancelledCommands::incrementAndGet).onErrorResume(e -> {
                        log.error("Error executing command: {}", e.getMessage());
                        failureCount.incrementAndGet();
                        capturedExceptions.add(e);
                        return Mono.empty(); // Return empty to filter out failed commands
                    });
        }).filter(Objects::nonNull) // Filter out empty results from errors
                .take(1000); // Take exactly 1000 successful results

        // Execute the flux and wait for completion
        StepVerifier.create(commandFlux).thenConsumeWhile(result -> {
            if (commandsSubmitted.get() % 100 == 0) {
                log.info("Commands submitted: {}, successful: {}, failures: {}", commandsSubmitted.get(), result,
                        failureCount.get());
            }
            return true;
        }).verifyComplete(); // All elements consumed and stream completed

        log.info("Command execution completed. Total submitted: {}, failures: {}, cancelled: {}", commandsSubmitted.get(),
                failureCount.get(), cancelledCommands.get());
        log.info("Captured exceptions: {}", capturedExceptions);

        // verify that initial endpoint CB is OPEN
        CircuitBreaker circuitBreaker = connection.getCircuitBreaker(databaseConfigs.get(0).getRedisURI());
        assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

        // verify that active-active failover happened
        assertThat(connection.getCurrentEndpoint()).isNotEqualTo(databaseConfigs.get(0).getRedisURI());
        assertThat(connection.getCurrentEndpoint()).isEqualTo(databaseConfigs.get(1).getRedisURI());

        // verify that counter equals to number of commands submitted plus the number of failures,
        // If auto-reconnect is disabled, commands are not retried
        // If auto-reconnect is enabled, commands are retried until timeout.
        // Issue 2 (TBD): CB errors counted on command level not per attempt, and missing retry attempts
        // Issue 3 (TBD): If auto-reconnect is disabled, commands are rejected and errors are not counted!
        StepVerifier.create(reactive.get(keyName).map(Long::parseLong)).consumeNextWith(value -> {
            log.info("Final commands processed: submitted: {}, counter value: {},  commands failed: {}, cancelled: {}",
                    commandsSubmitted.get(), value, failureCount.get(), cancelledCommands.get());
            assertThat(value + failureCount.get()).isEqualTo(commandsSubmitted.get());
        }).verifyComplete();

        // verify that number of failures is equal to minimumNumberOfFailures with some margin
        // existing implementation will count all failed attempts in async mode on command completion,
        // meaning that some commands may continue to be sent to the failed endpoint before failover is triggered
        assertThat(failureCount.get()).isEqualTo(MINIMUM_NUMBER_OF_FAILURES);

        log.info("Test completed successfully. MultiDbClient handled network failure with reactive commands.");
    }

    private void triggerNetworkFailure() {
        log.info("Triggering network_failure for bdb_id: {}, delay: {}", activeActiveEndpoint.getBdbId(),
                NETWORK_FAILURE_INTERVAL);

        Map<String, Object> params = new HashMap<>();
        params.put("bdb_id", activeActiveEndpoint.getBdbId());
        params.put("delay", NETWORK_FAILURE_INTERVAL.getSeconds());

        // Trigger fault injection asynchronously (don't block the flux)
        faultClient.triggerActionAndWait("network_failure", params, CHECK_INTERVAL, DELAY_AFTER, Duration.ofSeconds(120))
                .subscribe(success -> log.info("Network failure triggered successfully: {}", success),
                        error -> log.error("Failed to trigger network failure", error));
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

        CircuitBreaker.CircuitBreakerConfig circuitBreakerConfig = new CircuitBreaker.CircuitBreakerConfig(
                FAILURE_RATE_THRESHOLD, MINIMUM_NUMBER_OF_FAILURES,
                CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());

        List<String> endpoints = endpoint.getEndpoints();
        if (endpoints == null || endpoints.isEmpty()) {
            throw new IllegalStateException("Endpoint has no configured endpoints");
        }

        // Create a DatabaseConfig for each endpoint
        float weight = 1.0f;
        for (String endpointUri : endpoints) {
            RedisURI uri = RedisURI.builder(RedisURI.create(endpointUri))
                    .withAuthentication(endpoint.getUsername(), endpoint.getPassword()).withTimeout(Duration.ofSeconds(5))
                    .build();

            configs.add(new DatabaseConfig(uri, weight, clientOptions, circuitBreakerConfig));
            weight /= 2; // Decrease weight for subsequent endpoints
        }

        log.info("Created {} DatabaseConfig(s) from endpoint", configs.size());
        return configs;
    }

}
