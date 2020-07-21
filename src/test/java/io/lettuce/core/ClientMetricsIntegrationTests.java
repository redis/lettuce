/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

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
