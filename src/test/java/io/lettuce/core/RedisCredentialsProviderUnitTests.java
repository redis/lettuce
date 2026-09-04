/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Unit tests for the 7.x forward-compatible additions to {@link RedisCredentialsProvider}: {@code resolveCredentialsAsync()}
 * and {@code subscribeToCredentials(...)}.
 *
 * @author Aleksandar Todorov
 */
@SuppressWarnings("deprecation")
@Tag(UNIT_TEST)
class RedisCredentialsProviderUnitTests {

    private final RedisCredentials creds = RedisCredentials.just("user", "pass".toCharArray());

    @Test
    void resolveCredentialsAsyncBridgesResolveCredentials() throws Exception {

        RedisCredentialsProvider provider = () -> Mono.just(creds);

        assertThat(provider.resolveCredentialsAsync().toCompletableFuture().get()).isSameAs(creds);
    }

    @Test
    void subscribeToCredentialsDeliversAndCloseStops() {

        Sinks.Many<RedisCredentials> sink = Sinks.many().multicast().directBestEffort();
        RedisCredentialsProvider provider = new RedisCredentialsProvider() {

            @Override
            public Mono<RedisCredentials> resolveCredentials() {
                return sink.asFlux().next();
            }

            @Override
            public boolean supportsStreaming() {
                return true;
            }

            @Override
            public Flux<RedisCredentials> credentials() {
                return sink.asFlux();
            }

        };

        List<RedisCredentials> received = new ArrayList<>();
        Subscription subscription = provider.subscribeToCredentials(received::add, t -> {
        });

        sink.tryEmitNext(creds);
        assertThat(received).containsExactly(creds);

        subscription.close();
        sink.tryEmitNext(RedisCredentials.just("user2", "pass2".toCharArray()));
        assertThat(received).containsExactly(creds); // nothing delivered after close
    }

}
