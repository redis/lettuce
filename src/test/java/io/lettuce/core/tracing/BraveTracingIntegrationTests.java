/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.lettuce.core.AbstractRedisReactiveCommands.ReactorTraceContext;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.MaintNotificationsConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import brave.ScopedSpan;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.test.Wait;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import reactor.test.StepVerifier;
import zipkin2.Span;

/**
 * Integration tests for {@link BraveTracing}.
 *
 * @author Mark Paluch
 * @author Daniel Albuquerque
 * @author Anuraag Agrawal
 */
@Tag(INTEGRATION_TEST)
@EnabledOnCommand("HELLO")
class BraveTracingIntegrationTests extends TestSupport {

    private static ClientResources clientResources;

    private static ClientOptions clientOptions;

    private static RedisClient client;

    private static Tracing clientTracing;

    private static Queue<Span> spans = new LinkedBlockingQueue<>();

    @BeforeAll
    static void beforeClass() {

        clientTracing = Tracing.newBuilder().localServiceName("client")
                .currentTraceContext(CurrentTraceContext.Default.create()).spanReporter(spans::add).build();

        clientResources = DefaultClientResources.builder().tracing(BraveTracing.create(clientTracing)).build();
        clientOptions = ClientOptions.builder().maintNotificationsConfig(MaintNotificationsConfig.disabled()).build();
        client = RedisClient.create(clientResources,
                RedisURI.Builder.redis(host, port).withLibraryVersion("").withLibraryName("").build());
        client.setOptions(clientOptions);
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

        assertThat(spans.get(0).name()).isEqualTo("hello");
        assertThat(spans.get(0).remoteEndpoint()).isNotNull();
        assertThat(spans.get(1).name()).isEqualTo("ping");
        assertThat(spans.get(1).remoteEndpoint().port()).isEqualTo(port);
        assertThat(spans.get(0).tags()).containsEntry("db.redis.database_index", "0");
        assertThat(spans.get(1).tags()).containsEntry("db.redis.database_index", "0").containsKey("db.connection_string");
    }

    @Test
    @EnabledOnCommand("ACL")
    void shouldReportConnectionDetails() {

        WithPassword.run(client, () -> {

            RedisURI redisURI = RedisURI.Builder.redis(host, port).withDatabase(1)
                    .withAuthentication(TestSettings.aclUsername(), TestSettings.aclPassword()).build();

            try (StatefulRedisConnection<String, String> connection = client.connect(redisURI)) {
                connection.sync().ping();
            }

            Wait.untilTrue(
                    () -> spans.stream().anyMatch(span -> span.name().equals("ping") && span.tags().containsKey("db.user")))
                    .waitOrTimeout();
        });

        List<Span> connectionSpans = spans.stream().filter(span -> span.tags().containsKey("db.user"))
                .collect(Collectors.toList());

        assertThat(connectionSpans).extracting(Span::name).contains("hello", "ping");
        assertThat(connectionSpans).allSatisfy(span -> {
            assertThat(span.tags()).containsEntry("db.redis.database_index", "1")
                    .containsEntry("db.user", TestSettings.aclUsername())
                    .containsEntry("db.connection_string", "redis://" + host + ":" + port + "/1");
            assertThat(span.tags().get("db.connection_string")).doesNotContain(TestSettings.aclPassword());
        });
    }

    @Test
    void shouldReportClusterNodeUri() {

        RedisClusterClient clusterClient = RedisClusterClient.create(clientResources,
                RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1).build());

        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            for (RedisClusterNode node : connection.getPartitions()) {
                connection.getConnection(node.getNodeId()).sync().ping();
            }
        } finally {
            FastShutdown.shutdown(clusterClient);
        }

        List<Span> pingSpans = spans.stream().filter(span -> span.name().equals("ping")).collect(Collectors.toList());

        assertThat(pingSpans).hasSizeGreaterThan(1);
        assertThat(pingSpans).allSatisfy(span -> assertThat(span.tags()).containsEntry("db.connection_string",
                "redis://" + span.remoteEndpoint().ipv4() + ":" + span.remoteEndpoint().port()));
        assertThat(pingSpans).extracting(span -> span.tags().get("db.connection_string")).doesNotHaveDuplicates();
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

        Wait.untilTrue(() -> spans.size() > 2).waitOrTimeout();

        foo.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(1).name()).isEqualTo("set");
        assertThat(spans.get(2).name()).isEqualTo("hgetall");
        assertThat(spans.get(2).tags()).containsEntry("error",
                "WRONGTYPE Operation against a key holding the wrong kind of value");
        assertThat(spans.get(3).name()).isEqualTo("foo");
    }

    @Test
    void getAndSetWithTraceWithCommandArgsExcludedFromTags() {

        ClientResources clientResources = ClientResources.builder()
                .tracing(BraveTracing.builder().tracing(clientTracing).excludeCommandArgsFromSpanTags().build()).build();
        RedisClient client = RedisClient.create(clientResources,
                RedisURI.Builder.redis(host, port).withLibraryName("").withLibraryVersion("").build());
        client.setOptions(ClientOptions.builder().maintNotificationsConfig(MaintNotificationsConfig.disabled()).build());

        ScopedSpan trace = clientTracing.tracer().startScopedSpan("foo");

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.sync().set("foo", "bar");
        connect.sync().get("foo");

        Wait.untilTrue(() -> spans.size() > 2).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(1).name()).isEqualTo("set");
        assertThat(spans.get(1).tags()).doesNotContainKey("redis.args");
        assertThat(spans.get(2).name()).isEqualTo("get");
        assertThat(spans.get(2).tags()).doesNotContainKey("redis.args");
        assertThat(spans.get(3).name()).isEqualTo("foo");

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
                .contextWrite(it -> it.put(TraceContext.class, trace.context())) //
                .as(StepVerifier::create) //
                .expectNext("PONG").verifyComplete();

        Wait.untilNotEquals(true, spans::isEmpty).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(1).name()).isEqualTo("ping");
        assertThat(spans.get(2).name()).isEqualTo("foo");
    }

    @Test
    void reactiveGetAndSetWithTrace() {

        ScopedSpan trace = clientTracing.tracer().startScopedSpan("foo");

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.reactive().set("foo", "bar") //
                .then(connect.reactive().get("foo")) //
                .contextWrite(it -> it.put(TraceContext.class, trace.context())) //
                .as(StepVerifier::create) //
                .expectNext("bar").verifyComplete();

        Wait.untilTrue(() -> spans.size() > 2).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(1).name()).isEqualTo("set");
        assertThat(spans.get(1).tags()).containsEntry("redis.args", "key<foo> value<bar>");
        assertThat(spans.get(2).name()).isEqualTo("get");
        assertThat(spans.get(2).tags()).containsEntry("redis.args", "key<foo>");
        assertThat(spans.get(3).name()).isEqualTo("foo");
    }

    @Test
    void reactiveGetAndSetWithTraceProvider() {

        brave.Span trace = clientTracing.tracer().newTrace();

        StatefulRedisConnection<String, String> connect = client.connect();
        connect.reactive().set("foo", "bar").then(connect.reactive().get("foo"))
                .contextWrite(ReactorTraceContext
                        .withTraceContextProvider(() -> BraveTracing.BraveTraceContext.create(trace.context()))) //
                .as(StepVerifier::create) //
                .expectNext("bar").verifyComplete();

        Wait.untilTrue(() -> spans.size() > 2).waitOrTimeout();

        trace.finish();

        List<Span> spans = new ArrayList<>(BraveTracingIntegrationTests.spans);

        assertThat(spans.get(1).name()).isEqualTo("set");
        assertThat(spans.get(2).name()).isEqualTo("get");
    }

}
