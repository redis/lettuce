package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.test.Wait;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.SCENARIO_TEST;

/**
 * Tests for connection interruption using reactive API.
 */
@Tag(SCENARIO_TEST)
public class ConnectionInterruptionReactiveTest {

    private static final Logger log = LoggerFactory.getLogger(ConnectionInterruptionReactiveTest.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(1);

    private static final Duration DELAY_AFTER = Duration.ofMillis(500);

    private static Endpoint standalone;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    @BeforeAll
    public static void setup() {
        standalone = Endpoints.DEFAULT.getEndpoint("re-standalone");
        assumeTrue(standalone != null, "Skipping test because no Redis endpoint is configured!");
    }

    @ParameterizedTest(name = "Reactive Client Recovery on {0}")
    @ValueSource(strings = { "dmc_restart", "network_failure" })
    @DisplayName("Reactive client should reconnect automatically during connection interruptions")
    public void testWithReactiveCommands(String triggerAction) {
        RedisURI uri = RedisURI.builder(RedisURI.create(standalone.getEndpoints().get(0)))
                .withAuthentication(standalone.getUsername(), standalone.getPassword()).build();
        RedisClient client = RedisClient.create(uri);

        client.setOptions(ClientOptions.builder().autoReconnect(true).build());

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisReactiveCommands<String, String> reactive = connection.reactive();

        String keyName = "counter";

        // Setup: Set initial counter value
        StepVerifier.create(reactive.set(keyName, "0")).expectNext("OK").verifyComplete();

        AtomicLong commandsSubmitted = new AtomicLong();
        List<Throwable> capturedExceptions = new CopyOnWriteArrayList<>();

        // Start a flux that imitates an application using the client
        Disposable subscription = Flux.interval(Duration.ofMillis(100)).flatMap(i -> reactive.incr(keyName)
                // We should count all attempts, because Lettuce retransmits failed commands
                .doFinally(value -> {
                    commandsSubmitted.incrementAndGet();
                    log.info("Commands submitted {}", commandsSubmitted.get());
                }).onErrorResume(e -> {
                    log.warn("Error executing command", e);
                    capturedExceptions.add(e);
                    return Mono.empty();
                })).subscribe();

        // Trigger the fault injection
        Map<String, Object> params = new HashMap<>();
        params.put("bdb_id", standalone.getBdbId());

        Mono<Boolean> actionCompleted = faultClient.triggerActionAndWait(triggerAction, params, CHECK_INTERVAL, DELAY_AFTER,
                DEFAULT_TIMEOUT);

        StepVerifier.create(actionCompleted).expectNext(true).verifyComplete();

        // Stop the command execution
        subscription.dispose();

        // Verify results
        StepVerifier.create(reactive.get(keyName).map(Long::parseLong)).consumeNextWith(value -> {
            log.info("Final counter value: {}, commands submitted: {}", value, commandsSubmitted.get());
            assertThat(value).isEqualTo(commandsSubmitted.get());
        }).verifyComplete();

        log.info("Captured exceptions: {}", capturedExceptions);

        connection.close();
        client.shutdown();
    }

    @ParameterizedTest(name = "PubSub Reconnection on {0}")
    @ValueSource(strings = { "dmc_restart", "network_failure" })
    @DisplayName("PubSub connections should automatically reconnect and resume message delivery during failures")
    public void testWithPubSub(String triggerAction) {
        RedisURI uri = RedisURI.builder(RedisURI.create(standalone.getEndpoints().get(0)))
                .withAuthentication(standalone.getUsername(), standalone.getPassword()).build();

        RedisClient subscriberClient = RedisClient.create(uri);
        subscriberClient.setOptions(ClientOptions.builder().autoReconnect(true).build());

        RedisClient publisherClient = RedisClient.create(uri);
        publisherClient.setOptions(ClientOptions.builder().autoReconnect(true).build());

        StatefulRedisConnection<String, String> publisherConnection = publisherClient.connect();
        RedisReactiveCommands<String, String> publisherReactive = publisherConnection.reactive();

        AtomicLong messagesSent = new AtomicLong();
        AtomicLong messagesReceived = new AtomicLong();
        List<Throwable> subscriberExceptions = new CopyOnWriteArrayList<>();
        List<String> receivedMessages = new CopyOnWriteArrayList<>();

        StatefulRedisPubSubConnection<String, String> pubSubConnection = subscriberClient.connectPubSub();
        RedisPubSubReactiveCommands<String, String> pubSubReactive = pubSubConnection.reactive();
        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                log.info("Received message: {}", message);
                messagesReceived.incrementAndGet();
                receivedMessages.add(message);
            }

        });

        StepVerifier.create(pubSubReactive.subscribe("test")).verifyComplete();

        Disposable publisherSubscription = Flux.interval(Duration.ofMillis(200)).flatMap(
                i -> publisherReactive.publish("test", String.valueOf(messagesSent.getAndIncrement())).onErrorResume(e -> {
                    log.warn("Error publishing message", e);
                    subscriberExceptions.add(e);
                    return Mono.empty();
                })).subscribe();

        // Wait for messages to be sent and processed
        Wait.untilTrue(() -> messagesReceived.get() > 0).waitOrTimeout();

        // Trigger the fault injection
        Map<String, Object> params = new HashMap<>();
        params.put("bdb_id", standalone.getBdbId());

        Mono<Boolean> actionCompleted = faultClient.triggerActionAndWait(triggerAction, params, CHECK_INTERVAL, DELAY_AFTER,
                DEFAULT_TIMEOUT);

        StepVerifier.create(actionCompleted).expectNext(true).verifyComplete();

        // Stop the publisher
        publisherSubscription.dispose();

        log.info("Messages sent: {}, messages received: {}", messagesSent.get(), messagesReceived.get());
        log.info("Received messages: {}", receivedMessages);

        assertThat(messagesReceived.get()).isGreaterThan(0);
        assertThat(messagesReceived.get()).isLessThanOrEqualTo(messagesSent.get());

        // Assert that the last received message has an ID equal to the number of sent messages minus one
        assertThat(receivedMessages).isNotEmpty();

        String lastMessage = receivedMessages.get(receivedMessages.size() - 1);
        log.info("Last received message: {}, expected ID: {}", lastMessage, messagesSent.get() - 1);
        assertThat(lastMessage).isEqualTo(String.valueOf(messagesSent.get() - 1));

        log.info("Captured exceptions: {}", subscriberExceptions);

        pubSubConnection.close();
        publisherConnection.close();
        publisherClient.shutdown();
        subscriberClient.shutdown();
    }

}
