package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.lettuce.test.ReflectionTestUtils;

import reactor.core.Disposable;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.metrics.CommandLatencyEvent;
import io.lettuce.core.event.metrics.MetricEventPublisher;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ClientMetricsIntegrationTests extends TestSupport {

    @Test
    @Inject
    void testMetricsEvent(RedisClient client, StatefulRedisConnection<String, String> connection) {

        Collection<CommandLatencyEvent> events = new LinkedBlockingQueue<>();
        EventBus eventBus = client.getResources().eventBus();
        MetricEventPublisher publisher = (MetricEventPublisher) ReflectionTestUtils.getField(client.getResources(),
                "metricEventPublisher");
        publisher.emitMetricsEvent();

        Disposable disposable = eventBus.get().filter(redisEvent -> redisEvent instanceof CommandLatencyEvent)
                .cast(CommandLatencyEvent.class).doOnNext(events::add).subscribe();

        generateTestData(connection.sync());
        publisher.emitMetricsEvent();

        Wait.untilTrue(() -> !events.isEmpty()).waitOrTimeout();

        assertThat(events).isNotEmpty();

        disposable.dispose();
    }

    private void generateTestData(RedisCommands<String, String> redis) {
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
