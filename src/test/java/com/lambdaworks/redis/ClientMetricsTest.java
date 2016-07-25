package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.AbstractRedisClientTest.client;
import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.metrics.CommandLatencyId;
import com.lambdaworks.redis.metrics.CommandMetrics;
import org.junit.*;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.metrics.CommandLatencyEvent;
import com.lambdaworks.redis.event.metrics.MetricEventPublisher;

import rx.Subscription;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.util.Set;
import java.util.concurrent.TimeUnit;

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

        EventBus eventBus = client.getResources().eventBus();
        MetricEventPublisher publisher = (MetricEventPublisher) ReflectionTestUtils.getField(client.getResources(),
                "metricEventPublisher");
        publisher.emitMetricsEvent();

        TestSubscriber<CommandLatencyEvent> subscriber = new TestSubscriber<CommandLatencyEvent>();
        Subscription subscription = eventBus.get().filter(redisEvent -> redisEvent instanceof CommandLatencyEvent).cast(CommandLatencyEvent.class).subscribe(subscriber);

        generateTestData();
        publisher.emitMetricsEvent();

        WaitFor.waitOrTimeout(() -> !subscriber.getOnNextEvents().isEmpty(), timeout(seconds(5)));

        subscription.unsubscribe();

        subscriber.assertValueCount(1);
    }

    @Test
    public void testMetrics() throws Exception {

        EventBus eventBus = client.getResources().eventBus();
        MetricEventPublisher publisher = (MetricEventPublisher) ReflectionTestUtils.getField(client.getResources(),
                "metricEventPublisher");
        publisher.emitMetricsEvent();

        TestSubscriber<CommandLatencyEvent> subscriber = new TestSubscriber<CommandLatencyEvent>();
        Subscription subscription = eventBus.get().filter(redisEvent -> redisEvent instanceof CommandLatencyEvent).cast(CommandLatencyEvent.class).subscribe(subscriber);

        generateTestData();
        publisher.emitMetricsEvent();

        WaitFor.waitOrTimeout(() -> !subscriber.getOnNextEvents().isEmpty(), timeout(seconds(5)));
        subscription.unsubscribe();

        subscriber.assertValueCount(1);

        CommandLatencyEvent event = subscriber.getOnNextEvents().get(0);

        Set<CommandLatencyId> ids = event.getLatencies().keySet();
        CommandMetrics commandMetrics = event.getLatencies().get(ids.iterator().next());
        assertThat(commandMetrics.getCompletion().getMin()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(100));
        assertThat(commandMetrics.getCompletion().getMax()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(200));

        assertThat(commandMetrics.getFirstResponse().getMin()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(100));
        assertThat(commandMetrics.getFirstResponse().getMax()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(200));
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
