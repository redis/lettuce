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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link AsyncCredentialsProviderAdapter}.
 */
@Tag(UNIT_TEST)
class AsyncCredentialsProviderAdapterUnitTests {

    @Test
    void resolveCredentialsIsColdAndResolvesPerSubscription() {

        AtomicInteger resolveCount = new AtomicInteger();
        CredentialsProvider delegate = () -> {
            resolveCount.incrementAndGet();
            return CompletableFuture.completedFuture(RedisCredentials.just("user", "secret".toCharArray()));
        };

        AsyncCredentialsProviderAdapter adapter = new AsyncCredentialsProviderAdapter(delegate);
        Mono<RedisCredentials> credentials = adapter.resolveCredentials();

        // Cold: merely creating the Mono must not invoke the delegate.
        assertThat(resolveCount).hasValue(0);

        // Each subscription resolves credentials afresh, as other RedisCredentialsProvider implementations do.
        RedisCredentials first = credentials.block();
        RedisCredentials second = credentials.block();

        assertThat(resolveCount).hasValue(2);
        assertThat(first.getUsername()).isEqualTo("user");
        assertThat(second.getUsername()).isEqualTo("user");
    }

    @Test
    void credentialsBridgesStreamingDelegate() {

        // A reactor-free delegate that supports streaming and emits one value on subscription.
        CredentialsProvider delegate = new CredentialsProvider() {

            @Override
            public CompletionStage<RedisCredentials> resolveCredentialsAsync() {
                return CompletableFuture.completedFuture(RedisCredentials.just("user", "secret".toCharArray()));
            }

            @Override
            public boolean supportsStreaming() {
                return true;
            }

            @Override
            public Subscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
                onNext.accept(RedisCredentials.just("user", "secret".toCharArray()));
                return () -> {
                };
            }

        };

        AsyncCredentialsProviderAdapter adapter = new AsyncCredentialsProviderAdapter(delegate);

        // The adapter advertises streaming, so credentials() must bridge instead of throwing.
        assertThat(adapter.supportsStreaming()).isTrue();
        StepVerifier.create(adapter.credentials().next())
                .assertNext(credentials -> assertThat(credentials.getUsername()).isEqualTo("user")).verifyComplete();
    }

    @Test
    void credentialsThrowsWhenDelegateDoesNotStream() {

        CredentialsProvider delegate = () -> CompletableFuture
                .completedFuture(RedisCredentials.just("user", "secret".toCharArray()));
        AsyncCredentialsProviderAdapter adapter = new AsyncCredentialsProviderAdapter(delegate);

        assertThat(adapter.supportsStreaming()).isFalse();
        assertThatThrownBy(adapter::credentials).isInstanceOf(UnsupportedOperationException.class);
    }

}
