package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.AbstractRedisClientTest.client;
import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.api.sync.RedisCommands;
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

/**
 * @author Mark Paluch
 */
public class ClientMetricsTest extends AbstractTest {

    private RedisCommands<String, String> redis;

    @BeforeClass
    public static void setupClient() {
        client = RedisClient.create(RedisURI.Builder.redis(host, port).build());
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

        final TestSubscriber<CommandLatencyEvent> subscriber = new TestSubscriber<CommandLatencyEvent>();
        Subscription subscription = eventBus.get().filter(new Func1<Event, Boolean>() {
            @Override
            public Boolean call(Event redisEvent) {

                return redisEvent instanceof CommandLatencyEvent;
            }
        }).cast(CommandLatencyEvent.class).subscribe(subscriber);

        generateTestData();

        publisher.emitMetricsEvent();

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return !subscriber.getOnNextEvents().isEmpty();
            }
        }, timeout(seconds(5)));

        subscription.unsubscribe();

        subscriber.assertValueCount(1);

        CommandLatencyEvent event = subscriber.getOnNextEvents().get(0);

        assertThat(event.getLatencies()).hasSize(2);
        assertThat(event.toString()).contains("local:any ->");
        assertThat(event.toString()).contains("commandType=GET");
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
