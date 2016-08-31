package com.lambdaworks.redis;

import static com.lambdaworks.redis.AbstractRedisClientTest.client;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.Wait;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.metrics.CommandLatencyEvent;
import com.lambdaworks.redis.event.metrics.MetricEventPublisher;
import com.lambdaworks.redis.metrics.CommandLatencyId;
import com.lambdaworks.redis.metrics.CommandMetrics;

import reactor.test.TestSubscriber;

/**
 * @author Mark Paluch
 */
public class ClientMetricsTest extends AbstractTest {

    private RedisCommands<String, String> redis;

    @BeforeClass
    public static void setupClient() {
        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port).build());
    }

    @Before
    public void before() throws Exception {
        redis = client.connect().sync();
    }

    @AfterClass
    public static void afterClass() {
        FastShutdown.shutdown(client);
    }

    @Test
    public void testMetricsEvent() throws Exception {

        List<CommandLatencyEvent> events = new ArrayList<>();
        EventBus eventBus = client.getResources().eventBus();
        MetricEventPublisher publisher = (MetricEventPublisher) ReflectionTestUtils.getField(client.getResources(),
                "metricEventPublisher");
        publisher.emitMetricsEvent();

        TestSubscriber<CommandLatencyEvent> subscriber = TestSubscriber.create();
        eventBus.get().filter(redisEvent -> redisEvent instanceof CommandLatencyEvent).cast(CommandLatencyEvent.class)
                .doOnNext(events::add).subscribe(subscriber);

        generateTestData();
        publisher.emitMetricsEvent();

        subscriber.request(1);
        Wait.untilTrue(() -> !events.isEmpty()).waitOrTimeout();

        assertThat(events).isNotEmpty();

        subscriber.assertNotComplete();
        subscriber.cancel();
    }

    @Test
    public void testMetrics() throws Exception {

        EventBus eventBus = client.getResources().eventBus();
        MetricEventPublisher publisher = (MetricEventPublisher) ReflectionTestUtils.getField(client.getResources(),
                "metricEventPublisher");
        publisher.emitMetricsEvent();

        TestSubscriber<CommandLatencyEvent> subscriber = TestSubscriber.create();
        eventBus.get().filter(redisEvent -> redisEvent instanceof CommandLatencyEvent).cast(CommandLatencyEvent.class)
                .subscribe(subscriber);

        generateTestData();
        publisher.emitMetricsEvent();

        subscriber.awaitAndAssertNextValuesWith(event -> {

            Set<CommandLatencyId> ids = event.getLatencies().keySet();
            CommandMetrics commandMetrics = event.getLatencies().get(ids.iterator().next());
            assertThat(commandMetrics.getCompletion().getMin()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(100));
            assertThat(commandMetrics.getCompletion().getMax()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(200));

            assertThat(commandMetrics.getFirstResponse().getMin()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(100));
            assertThat(commandMetrics.getFirstResponse().getMax()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(200));
        });

    }

    private void generateTestData() {
        redis.set(key, value);
        redis.set(key, value);
        redis.set(key, value);
        redis.set(key, value);
        redis.set(key, value);
        redis.set(key, value);

        redis.get(key);
        redis.get(key);
        redis.get(key);
        redis.get(key);
        redis.get(key);
    }
}
