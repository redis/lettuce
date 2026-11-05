/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.tracing;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.CredentialsProvider;
import io.lettuce.core.RedisURI;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.ObservationRegistry;

/**
 * Unit tests for {@link MicrometerTracing} and {@link DefaultLettuceObservationConvention}.
 */
@Tag(UNIT_TEST)
class MicrometerTracingUnitTests {

    private final MicrometerTracing tracing = new MicrometerTracing(ObservationRegistry.create(), "Redis", false);

    private final DefaultLettuceObservationConvention convention = new DefaultLettuceObservationConvention(false);

    @Test
    void shouldReportConnectionDetailsFromRedisUri() {

        RedisURI redisURI = RedisURI.builder().withHost("localhost").withPort(6379).withDatabase(3)
                .withAuthentication("alice", "secret").build();

        LettuceObservationContext context = createContext(
                tracing.createEndpoint(new InetSocketAddress("127.0.0.1", 6379), redisURI));

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("db.redis.database_index", "3"));
        assertThat(convention.getHighCardinalityKeyValues(context)).contains(KeyValue.of("db.user", "alice"),
                KeyValue.of("db.connection_string", "redis://localhost/3"));
    }

    @Test
    void shouldNotReportUsernameForPendingCredentials() {

        CredentialsProvider pending = CompletableFuture::new;
        RedisURI redisURI = RedisURI.builder().withHost("localhost").withPort(6379).withAuthentication(pending).build();

        LettuceObservationContext context = createContext(
                tracing.createEndpoint(new InetSocketAddress("127.0.0.1", 6379), redisURI));

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("db.redis.database_index", "0"));
        assertThat(keys(convention.getHighCardinalityKeyValues(context))).doesNotContain("db.user");
        assertThat(convention.getHighCardinalityKeyValues(context))
                .contains(KeyValue.of("db.connection_string", "redis://localhost"));
    }

    @Test
    void shouldNotReportPasswordInConnectionString() {

        RedisURI redisURI = RedisURI.builder().withHost("localhost").withPort(6380).withPassword("secret".toCharArray())
                .withClientName("my-client").build();

        LettuceObservationContext context = createContext(
                tracing.createEndpoint(new InetSocketAddress("127.0.0.1", 6380), redisURI));

        assertThat(keys(convention.getHighCardinalityKeyValues(context))).doesNotContain("db.user");
        assertThat(convention.getHighCardinalityKeyValues(context))
                .contains(KeyValue.of("db.connection_string", "redis://localhost:6380?clientName=my-client"));
    }

    @Test
    void shouldNotReportConnectionDetailsWithoutRedisUri() {

        LettuceObservationContext context = createContext(tracing.createEndpoint(new InetSocketAddress("127.0.0.1", 6379)));

        assertThat(keys(convention.getLowCardinalityKeyValues(context))).doesNotContain("db.redis.database_index");
        assertThat(keys(convention.getHighCardinalityKeyValues(context))).doesNotContain("db.user", "db.connection_string");
    }

    @Test
    void shouldUseEndpointCreatedBySubclass() {

        Tracing.Endpoint custom = new Tracing.Endpoint() {
        };
        MicrometerTracing tracing = new MicrometerTracing(ObservationRegistry.create(), "Redis", false) {

            @Override
            public Endpoint createEndpoint(SocketAddress socketAddress) {
                return custom;
            }

        };

        assertThat(tracing.createEndpoint(new InetSocketAddress("127.0.0.1", 6379), RedisURI.create("redis://localhost")))
                .isSameAs(custom);
    }

    private static LettuceObservationContext createContext(Tracing.Endpoint endpoint) {

        LettuceObservationContext context = new LettuceObservationContext("Redis");
        context.setCommand(new Command<>(CommandType.PING, null));
        context.setEndpoint(endpoint);
        return context;
    }

    private static String[] keys(KeyValues keyValues) {
        return keyValues.stream().map(KeyValue::getKey).toArray(String[]::new);
    }

}
