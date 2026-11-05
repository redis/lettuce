/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.tracing;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.CredentialsProvider;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import io.lettuce.core.RedisURI;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link RedisUriDetails}.
 */
@Tag(UNIT_TEST)
class RedisUriDetailsUnitTests {

    @Test
    void shouldReturnNullWithoutRedisUri() {
        assertThat(RedisUriDetails.from(null)).isNull();
    }

    @Test
    void shouldRenderUnixSocketWithoutCredentials() {

        RedisURI redisURI = RedisURI.Builder.socket("/tmp/redis.sock").withDatabase(2).withAuthentication("alice", "secret")
                .build();

        RedisUriDetails details = RedisUriDetails.from(redisURI);

        assertThat(details.getDatabase()).isEqualTo("2");
        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getUrl()).isEqualTo("redis-socket:///tmp/redis.sock?database=2");
    }

    @Test
    void shouldRenderSentinelWithoutCredentials() {

        RedisURI redisURI = RedisURI.Builder.sentinel("host1", 26379, "mymaster").withSentinel("host2", 26380)
                .withPassword("secret".toCharArray()).withDatabase(1).build();

        RedisUriDetails details = RedisUriDetails.from(redisURI);

        assertThat(details.getDatabase()).isEqualTo("1");
        assertThat(details.getUsername()).isNull();
        assertThat(details.getUrl()).isEqualTo("redis-sentinel://host1,host2:26380/1?sentinelMasterId=mymaster");
    }

    @Test
    void shouldIgnoreFailingCredentialsProvider() {

        CredentialsProvider failed = () -> {
            CompletableFuture<RedisCredentials> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("no credentials"));
            return future;
        };
        CredentialsProvider throwing = () -> {
            throw new IllegalStateException("no credentials");
        };

        assertThat(
                RedisUriDetails.from(RedisURI.builder().withHost("localhost").withAuthentication(failed).build()).getUsername())
                        .isNull();
        assertThat(RedisUriDetails.from(RedisURI.builder().withHost("localhost").withAuthentication(throwing).build())
                .getUsername()).isNull();
    }

    @Test
    void shouldUseImmediateCredentialsProviderWithoutFuture() {

        CredentialsProvider.ImmediateCredentialsProvider provider = new CredentialsProvider.ImmediateCredentialsProvider() {

            @Override
            public RedisCredentials resolveCredentialsNow() {
                return RedisCredentials.just("alice", "secret");
            }

            @Override
            public CompletionStage<RedisCredentials> resolveCredentialsAsync() {
                throw new AssertionError("resolveCredentialsAsync must not be called");
            }

        };

        assertThat(RedisUriDetails.from(RedisURI.builder().withHost("localhost").withAuthentication(provider).build())
                .getUsername()).isEqualTo("alice");
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldNotSubscribeToReactiveCredentialsProvider() {

        AtomicInteger subscriptions = new AtomicInteger();
        RedisCredentialsProvider provider = () -> Mono.fromCallable(() -> {
            subscriptions.incrementAndGet();
            return RedisCredentials.just("alice", "secret");
        });

        assertThat(RedisUriDetails.from(RedisURI.builder().withHost("localhost").withAuthentication(provider).build())
                .getUsername()).isNull();
        assertThat(subscriptions).hasValue(0);
    }

}
