package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.metrics.EndpointQueueMonitor;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.metrics.MicrometerQueueMonitor;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.TestClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class EndpointQueueMonitorIntegrationTests extends TestSupport {

    private final static MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private static RedisClient client;

    private static ClientResources resources;

    @BeforeAll
    static void beforeAll() {
        MicrometerOptions options = MicrometerOptions.create();
        EndpointQueueMonitor monitor = new MicrometerQueueMonitor(meterRegistry, options);
        resources = TestClientResources.get().mutate().endpointQueueMonitor(monitor).build();
        client = RedisClient.create(resources, RedisURI.Builder.redis(host, port).build());
    }

    @BeforeEach
    void setUp() {
        client.setOptions(ClientOptions.builder().build());
    }

    @AfterEach
    void tearDown() {
        meterRegistry.clear();
    }

    @Test
    void queueMonitorDisconnectedBuffer() {

        ClientOptions clientOptions = ClientOptions.builder().autoReconnect(false)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS).build();
        client.setOptions(clientOptions);

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisAsyncCommands<String, String> redis = connection.async();

            // Force disconnection
            redis.quit();
            Wait.untilTrue(() -> !connection.isOpen()).during(Duration.ofSeconds(1)).waitOrTimeout();

            redis.set("key", "value");

            assertThat(meterRegistry.find("lettuce.endpoint.disconnected.buffer").gauge().value()).isEqualTo(1);
        }
    }

    @Test
    void queueMonitorCommandsBuffer() {

        RedisClient client = RedisClient.create(resources, RedisURI.Builder.redis(host, port).build());

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisAsyncCommands<String, String> redis = connection.async();
            redis.setAutoFlushCommands(false);
            redis.set("key", "value");

            assertThat(meterRegistry.find("lettuce.endpoint.command.buffer").gauge().value()).isEqualTo(1);
            redis.flushCommands();
            assertThat(meterRegistry.find("lettuce.endpoint.command.buffer").gauge().value()).isEqualTo(0);
        }
    }

    @Test
    void queueMonitorCommandHandlerStackSize() throws ExecutionException, InterruptedException {

        try (StatefulRedisConnection<String, String> connection = client.connect();
                StatefulRedisConnection<String, String> connection2 = client.connect()) {
            RedisAsyncCommands<String, String> redis = connection.async();
            RedisFuture<KeyValue<String, String>> blpop = redis.blpop(1, "blpop:key");
            assertThat(meterRegistry.find("lettuce.command.handler.queue").gauge().value()).isEqualTo(1);

            Long lpush = connection2.sync().lpush("blpop:key", "value");
            assertThat(lpush).isEqualTo(1);
            assertThat(blpop.get()).isNotNull();

            assertThat(meterRegistry.find("lettuce.command.handler.queue").gauge().value()).isEqualTo(0);
        }
    }

}
