package io.lettuce.examples;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.api.CircuitBreakerConfig;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.event.DatabaseSwitchEvent;
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
 * actively improving the API.
 */
public class AutomaticFailover {

    private static final Logger log = LoggerFactory.getLogger(AutomaticFailover.class);

    public static void main(String[] args) {
        // Setup: Configure MultiDbClient
        // Both endpoints should be available in the test environment.
        // At this point multiDbClient lib does not check if the endpoints are actually available.
        // This limitation will be addressed in the future.
        // Local Redis instances can be started with:
        // docker run -p 6380:6379 -it redis:8.2.1
        // docker run -p 6381:6379 -it redis:8.2.1
        List<String> endpoints = Arrays.asList("redis://localhost:6380", "redis://localhost:6381");

        SocketOptions.TcpUserTimeoutOptions tcpUserTimeout = SocketOptions.TcpUserTimeoutOptions.builder()
                .tcpUserTimeout(Duration.ofSeconds(4)).enable().build();

        SocketOptions.KeepAliveOptions keepAliveOptions = SocketOptions.KeepAliveOptions.builder()
                .interval(Duration.ofSeconds(1)).idle(Duration.ofSeconds(1)).count(3).enable().build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().tcpUserTimeout(tcpUserTimeout).keepAlive(keepAliveOptions).build())
                .build();

        List<DatabaseConfig> databaseConfigs = createDatabaseConfigs(clientOptions, endpoints);
        MultiDbClient multiDbClient = MultiDbClient.create(databaseConfigs);

        // Automatic failback are not supported in the current Beta release.

        // Listen to database switch events
        multiDbClient.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof DatabaseSwitchEvent) {
                DatabaseSwitchEvent switchEvent = (DatabaseSwitchEvent) event;
                log.info("Database switch from {} to {} (reason: {})", switchEvent.getFromDb(), switchEvent.getToDb(),
                        switchEvent.getReason());

                // Access the source connection
                StatefulRedisMultiDbConnection<?, ?> connection = switchEvent.getSource();

                // Query connection state
                RedisURI currentEndpoint = connection.getCurrentEndpoint();
                log.info("Current endpoint after switch: {}", currentEndpoint);
            }
        });

        // Connect to the MultiDbClient
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        log.info("Connected to {}", connection.getCurrentEndpoint());
        log.info("Available Endpoints: {}", connection.getEndpoints());

        // Get reactive commands interface
        RedisReactiveCommands<String, String> reactive = connection.reactive();

        String keyName = "multidb-counter";

        // Setup: Set initial counter value
        StepVerifier.create(reactive.set(keyName, "0")).expectNext("OK").verifyComplete();

        AtomicLong commandsSubmitted = new AtomicLong();
        List<Throwable> capturedExceptions = new CopyOnWriteArrayList<>();

        // Start a flux that imitates an application using the multiDbClient
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

        // Direct connection to the Redis instances used to trigger Primary Redis instance shutdown
        // and verify that commands are executed on the secondary Redis instance
        RedisClient directClient = RedisClient.create();

        // Shutdown the current Redis instance to trigger failover
        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        log.info("Stoping Redis server [{}] to trigger failover", currentEndpoint);
        shutdownRedisInstance(directClient, currentEndpoint);

        // Wait for Commands to start being executed on the secondary Redis instances
        RedisURI secondaryUri = RedisURI.create(endpoints.get(1));
        StatefulRedisConnection<String, String> secondary = directClient.connect(secondaryUri);
        log.info("Waiting for commands to be executed on [{}]", secondaryUri);
        Wait.untilTrue(() -> (getMulitDbCounter(secondary)) > 20).during(Duration.ofSeconds(10)).waitOrTimeout();

        // Stop the command execution
        subscription.dispose();

        log.info("Commands submitted total: {}", commandsSubmitted.get());
        log.info("Commands executed on secondary [{}]: {}", secondaryUri, getMulitDbCounter(secondary));
        log.info("Captured exceptions: {}", capturedExceptions);

        // Cleanup
        directClient.shutdown();
        multiDbClient.shutdown();
    }

    private static int getMulitDbCounter(StatefulRedisConnection<String, String> connection) {
        String count = connection.sync().get("multidb-counter");
        return count == null ? 0 : Integer.parseInt(count);
    }

    private static void shutdownRedisInstance(RedisClient redis, RedisURI redisUri) {
        log.info("Shutting down Redis instance [{}]", redisUri);
        try (StatefulRedisConnection<String, String> redis1 = redis.connect(redisUri)) {
            redis1.sync().shutdown(true);

            Wait.untilTrue(() -> {
                try {
                    redis.connect(redisUri);
                } catch (Exception e) {
                    return e instanceof RedisConnectionException;
                }
                return false;
            }).waitOrTimeout();

            log.info("Redis instance [{}] is down", redisUri);
        }
    }

    /**
     * @return list of DatabaseConfig instances
     */
    private static List<DatabaseConfig> createDatabaseConfigs(ClientOptions clientOptions, List<String> endpoints) {
        List<DatabaseConfig> configs = new ArrayList<>();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.builder().failureRateThreshold(10.0f)
                .minimumNumberOfFailures(5).metricsWindowSize(5).build();

        // Create a DatabaseConfig for each endpoint
        float weight = 1.0f;
        for (String endpointUri : endpoints) {
            configs.add(DatabaseConfig.builder(RedisURI.create(endpointUri)).weight(weight).clientOptions(clientOptions)
                    .circuitBreakerConfig(circuitBreakerConfig).healthCheckStrategySupplier(PingStrategy.DEFAULT).build());

            weight /= 2; // Decrease weight for subsequent endpoints
        }

        log.info("Created {} DatabaseConfig(s) from endpoint", configs.size());
        return configs;
    }

}
