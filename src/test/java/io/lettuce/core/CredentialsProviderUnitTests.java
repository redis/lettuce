/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Unit tests for the reactor-free {@link CredentialsProvider} streaming SPI and its bridges to/from the deprecated
 * {@link RedisCredentialsProvider}.
 */
@Tag(UNIT_TEST)
class CredentialsProviderUnitTests {

    @Test
    void nonStreamingProviderRejectsSubscribe() {

        CredentialsProvider provider = () -> CompletableFuture.completedFuture(RedisCredentials.just("u", "p".toCharArray()));

        assertThat(provider.supportsStreaming()).isFalse();
        assertThatThrownBy(() -> provider.subscribeToCredentials(c -> {
        }, e -> {
        })).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedProviderBridgesReactiveStreamToCallback() {

        RedisCredentials first = RedisCredentials.just("alice", "s1".toCharArray());
        RedisCredentials second = RedisCredentials.just("bob", "s2".toCharArray());

        RedisCredentialsProvider reactive = new RedisCredentialsProvider() {

            @Override
            public Mono<RedisCredentials> resolveCredentials() {
                return Mono.just(first);
            }

            @Override
            public boolean supportsStreaming() {
                return true;
            }

            @Override
            public Flux<RedisCredentials> credentials() {
                return Flux.just(first, second);
            }

        };

        List<RedisCredentials> received = new ArrayList<>();
        reactive.subscribeToCredentials(received::add, e -> {
        });

        assertThat(received).containsExactly(first, second);
    }

    @Test
    void adapterDelegatesStreamingToReactorFreeProvider() {

        List<Consumer<RedisCredentials>> captured = new ArrayList<>();
        CredentialsProvider streaming = new CredentialsProvider() {

            @Override
            public CompletionStage<RedisCredentials> resolveCredentialsAsync() {
                return CompletableFuture.completedFuture(RedisCredentials.just("u", "p".toCharArray()));
            }

            @Override
            public boolean supportsStreaming() {
                return true;
            }

            @Override
            public Subscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
                captured.add(onNext);
                return () -> {
                };
            }

        };

        AsyncCredentialsProviderAdapter adapter = new AsyncCredentialsProviderAdapter(streaming);

        assertThat(adapter.supportsStreaming()).isTrue();
        adapter.subscribeToCredentials(c -> {
        }, e -> {
        });
        assertThat(captured).hasSize(1);
    }

}
