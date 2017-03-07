/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static io.lettuce.core.AbstractRedisClientTest.client;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import io.lettuce.TestClientResources;
import io.lettuce.Wait;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.metrics.CommandLatencyEvent;
import io.lettuce.core.event.metrics.MetricEventPublisher;
import io.lettuce.core.metrics.CommandLatencyId;
import io.lettuce.core.metrics.CommandMetrics;

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

        Disposable disposable = eventBus.get().filter(redisEvent -> redisEvent instanceof CommandLatencyEvent)
                .cast(CommandLatencyEvent.class).doOnNext(events::add).subscribe();

        generateTestData();
        publisher.emitMetricsEvent();

        Wait.untilTrue(() -> !events.isEmpty()).waitOrTimeout();

        assertThat(events).isNotEmpty();

        disposable.dispose();
    }

    @Test
    public void testMetrics() throws Exception {

        EventBus eventBus = client.getResources().eventBus();
        MetricEventPublisher publisher = (MetricEventPublisher) ReflectionTestUtils.getField(client.getResources(),
                "metricEventPublisher");

        Flux<CommandLatencyEvent> flux = eventBus.get().filter(redisEvent -> redisEvent instanceof CommandLatencyEvent)
                .cast(CommandLatencyEvent.class);

        StepVerifier.create(flux).then(() -> {
            generateTestData();
            publisher.emitMetricsEvent();
        }).consumeNextWith(actual -> {
            // just consume
            }).consumeNextWith(actual -> {

            Set<CommandLatencyId> ids = actual.getLatencies().keySet();
            CommandMetrics commandMetrics = actual.getLatencies().get(ids.iterator().next());
            assertThat(commandMetrics.getCompletion().getMin()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(100));
            assertThat(commandMetrics.getCompletion().getMax()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(200));

            assertThat(commandMetrics.getFirstResponse().getMin()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(100));
            assertThat(commandMetrics.getFirstResponse().getMax()).isBetween(0L, TimeUnit.MILLISECONDS.toMicros(200));
        }).thenCancel().verify();

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
