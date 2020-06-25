/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.core.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import zipkin2.Span;
import brave.ScopedSpan;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;

/**
 * Integration tests for {@link BraveTracing}.
 *
 * @author Mark Paluch
 * @author Daniel Albuquerque
 */
class BraveTracingIntegrationTests extends TestSupport {

    private static ClientResources clientResources;

    private static RedisClient client;

    private static Tracing clientTracing;

    private static Queue<Span> spans = new LinkedBlockingQueue<>();

    @BeforeAll
    static void beforeClass() {

        clientTracing = Tracing.newBuilder().localServiceName("client")
                .currentTraceContext(CurrentTraceContext.Default.create()).spanReporter(spans::add).build();

        clientResources = DefaultClientResources.builder().tracing(BraveTracing.create(clientTracing)).build();
        client = RedisClient.create(clientResources, RedisURI.Builder.redis(host, port).build());
    }

    @BeforeEach
    void before() {

        Tracer tracer = clientTracing.tracer();
        if (tracer.currentSpan() != null) {
            clientTracing.tracer().currentSpan().abandon();
        }

        spans.clear();
    }

    @AfterAll
    static void afterClass() {

        clientTracing.close();
        clientResources.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Test
    void pingWithTrace() {

        ScopedSpan foo = clientTracing.tracer().startScopedSpan("foo");

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.sync().ping();
        Wait.untilNotEquals(true, spans::isEmpty).waitOrTimeout();

        foo.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(0).name()).isEqualTo("ping");
        assertThat(spans.get(1).name()).isEqualTo("foo");
    }

    @Test
    void pingWithTraceShouldCatchErrors() {

        ScopedSpan foo = clientTracing.tracer().startScopedSpan("foo");

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.sync().set("foo", "bar");
        try {
            connect.sync().hgetall("foo");
        } catch (Exception e) {
        }

        Wait.untilEquals(2, spans::size).waitOrTimeout();

        foo.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(0).name()).isEqualTo("set");
        assertThat(spans.get(1).name()).isEqualTo("hgetall");
        assertThat(spans.get(1).tags()).containsEntry("error",
                "WRONGTYPE Operation against a key holding the wrong kind of value");
        assertThat(spans.get(2).name()).isEqualTo("foo");
    }

    @Test
    void getAndSetWithTraceWithCommandArgsExcludedFromTags() {

        ClientResources clientResources = ClientResources.builder()
                .tracing(BraveTracing.builder().tracing(clientTracing).excludeCommandArgsFromSpanTags().build()).build();
        RedisClient client = RedisClient.create(clientResources, RedisURI.Builder.redis(host, port).build());

        ScopedSpan trace = clientTracing.tracer().startScopedSpan("foo");

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.sync().set("foo", "bar");
        connect.sync().get("foo");

        Wait.untilEquals(2, spans::size).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(0).name()).isEqualTo("set");
        assertThat(spans.get(0).tags()).doesNotContainKey("redis.args");
        assertThat(spans.get(1).name()).isEqualTo("get");
        assertThat(spans.get(1).tags()).doesNotContainKey("redis.args");
        assertThat(spans.get(2).name()).isEqualTo("foo");

        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clientResources);
    }

    @Test
    void reactivePing() {

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.reactive().ping().as(StepVerifier::create).expectNext("PONG").verifyComplete();

        Wait.untilNotEquals(true, spans::isEmpty).waitOrTimeout();
        assertThat(spans).isNotEmpty();
    }

    @Test
    void reactivePingWithTrace() {

        ScopedSpan trace = clientTracing.tracer().startScopedSpan("foo");

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.reactive().ping() //
                .subscriberContext(it -> it.put(TraceContext.class, trace.context())) //
                .as(StepVerifier::create) //
                .expectNext("PONG").verifyComplete();

        Wait.untilNotEquals(true, spans::isEmpty).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(0).name()).isEqualTo("ping");
        assertThat(spans.get(1).name()).isEqualTo("foo");
    }

    @Test
    void reactiveGetAndSetWithTrace() {

        ScopedSpan trace = clientTracing.tracer().startScopedSpan("foo");

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.reactive().set("foo", "bar") //
                .then(connect.reactive().get("foo")) //
                .subscriberContext(it -> it.put(TraceContext.class, trace.context())) //
                .as(StepVerifier::create) //
                .expectNext("bar").verifyComplete();

        Wait.untilEquals(2, spans::size).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(0).name()).isEqualTo("set");
        assertThat(spans.get(0).tags()).containsEntry("redis.args", "key<foo> value<bar>");
        assertThat(spans.get(1).name()).isEqualTo("get");
        assertThat(spans.get(1).tags()).containsEntry("redis.args", "key<foo>");
        assertThat(spans.get(2).name()).isEqualTo("foo");
    }

    @Test
    void reactiveGetAndSetWithTraceProvider() {

        brave.Span trace = clientTracing.tracer().newTrace();

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.reactive().set("foo", "bar").then(connect.reactive().get("foo"))
                .subscriberContext(io.lettuce.core.tracing.Tracing
                        .withTraceContextProvider(() -> BraveTracing.BraveTraceContext.create(trace.context()))) //
                .as(StepVerifier::create) //
                .expectNext("bar").verifyComplete();

        Wait.untilEquals(2, spans::size).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(0).name()).isEqualTo("set");
        assertThat(spans.get(1).name()).isEqualTo("get");
    }

}
