package io.lettuce.examples;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.failover.CircuitBreaker;
import io.lettuce.core.failover.DatabaseConfig;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.PingStrategy;
import io.lettuce.test.Wait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Example of automatic failover using MultiDbClient. Automatic Failover API is subject to change since we are still in Beta and
 * actively improving the API.*
 */
public class AutomaticFailover {

    private static final Logger log = LoggerFactory.getLogger(AutomaticFailover.class);

    public static void main(String[] args) {
        // Setup: Configure MultiDbClient
        // Both endpoints should be available in the test environment.
        // At this point client lib does not check if the endpoints are actually available.
        // This limitation will be addressed in the future.
        // Local Redis instances can be started with:
        // docker run -p 6380:6379 -it redis:8.2.1
        // docker run -p 6381:6379 -it redis:8.2.1
        List<String> endpoints = Arrays.asList("redis://localhost:6380", "redis://localhost:6381");

        SocketOptions.TcpUserTimeoutOptions tcpUserTimeout = SocketOptions.TcpUserTimeoutOptions.builder()
                .tcpUserTimeout(Duration.ofSeconds(4)).enable().build();

        SocketOptions.KeepAliveOptions keepAliveOptions = SocketOptions.KeepAliveOptions.builder()
                .interval(Duration.ofSeconds(1)).idle(Duration.ofSeconds(1)).count(3).enable().build();

        ClientOptions clientOptions = ClientOptions.builder().autoReconnect(false)
                .socketOptions(SocketOptions.builder().tcpUserTimeout(tcpUserTimeout).keepAlive(keepAliveOptions).build())
                .build();

        List<DatabaseConfig> databaseConfigs = createDatabaseConfigs(endpoints, clientOptions);
        MultiDbClient client = MultiDbClient.create(databaseConfigs);

        // Auto-reconnect and automatic failback are not supported in the current Beta release.
        client.setOptions(clientOptions);

        // Connect to the MultiDbClient
        StatefulRedisMultiDbConnection<String, String> connection = client.connect();

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
                    capturedExceptions.add(e);
                    return Mono.empty();
                })).subscribe();

        // Wait for some commands to be executed before triggering the fault
        Wait.untilTrue(() -> commandsSubmitted.get() > 10).waitOrTimeout();

        log.info("Executing commands. Stop the first Redis server to trigger failover");

        // Wait a bit more to allow recovery
        Wait.untilTrue(() -> commandsSubmitted.get() > 10000).during(Duration.ofSeconds(120)).waitOrTimeout();

        log.info("Captured exceptions: {}", capturedExceptions);

        // Stop the command execution
        subscription.dispose();
        connection.close();
        client.shutdown();
    }

    /**
     * @param clientOptions the client options to use for all database configs
     * @return list of DatabaseConfig instances
     */
    private static List<DatabaseConfig> createDatabaseConfigs(List<String> endpoints,
            io.lettuce.core.ClientOptions clientOptions) {
        List<DatabaseConfig> configs = new ArrayList<>();

        CircuitBreaker.CircuitBreakerConfig circuitBreakerConfig = new CircuitBreaker.CircuitBreakerConfig(5.0f, 5,
                CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions(), 2);

        HealthCheckStrategySupplier pingSupplier = (uri, options) -> new PingStrategy(uri, options,
                HealthCheckStrategy.Config.builder().interval(500).timeout(1000).numProbes(1).build());

        // Create a DatabaseConfig for each endpoint
        float weight = 1.0f;
        for (String endpointUri : endpoints) {
            RedisURI uri = RedisURI.builder(RedisURI.create(endpointUri)).withTimeout(Duration.ofSeconds(5)).build();

            configs.add(new DatabaseConfig(uri, weight, clientOptions, circuitBreakerConfig, pingSupplier));
            weight /= 2; // Decrease weight for subsequent endpoints
        }

        log.info("Created {} DatabaseConfig(s) from endpoint", configs.size());
        return configs;
    }

}
