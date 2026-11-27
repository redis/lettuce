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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

/**
 * Unit tests for the null- and error-handling contract of the deprecated reactive {@link RedisCredentialsProvider}.
 */
@Tag(UNIT_TEST)
@SuppressWarnings("deprecation")
class RedisCredentialsProviderUnitTests {

    @Test
    void fromResolvesSuppliedCredentials() {

        RedisCredentialsProvider provider = RedisCredentialsProvider
                .from(() -> RedisCredentials.just("alice", "secret".toCharArray()));

        StepVerifier.create(provider.resolveCredentials())
                .assertNext(credentials -> assertThat(credentials.getUsername()).isEqualTo("alice")).verifyComplete();
    }

    @Test
    void fromFailsWhenSupplierReturnsNull() {

        RedisCredentialsProvider provider = RedisCredentialsProvider.from(() -> null);

        // Must signal an error rather than complete empty (which would later NPE in the handshake).
        StepVerifier.create(provider.resolveCredentials()).expectError(IllegalStateException.class).verify();

        assertThatThrownBy(() -> provider.resolveCredentialsAsync().toCompletableFuture().join())
                .isInstanceOf(CompletionException.class).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void immediateProviderSignalsErrorWhenResolvingNull() {

        RedisCredentialsProvider.ImmediateRedisCredentialsProvider provider = () -> null;

        StepVerifier.create(provider.resolveCredentials()).expectError(IllegalStateException.class).verify();
    }

    @Test
    void immediateProviderPropagatesThrownExceptionAsError() {

        RedisCredentialsProvider.ImmediateRedisCredentialsProvider provider = () -> {
            throw new IllegalStateException("boom");
        };

        StepVerifier.create(provider.resolveCredentials()).expectErrorMessage("boom").verify();
    }

    @Test
    void adaptReturnsReactiveProviderAsIs() {

        RedisCredentialsProvider reactive = RedisCredentialsProvider.from(() -> RedisCredentials.just("u", "p"));

        assertThat(RedisCredentialsProvider.adapt(reactive)).isSameAs(reactive);
    }

    @Test
    void adaptRejectsNull() {
        assertThatThrownBy(() -> RedisCredentialsProvider.adapt(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adaptBridgesResolutionAndStreaming() {

        StreamingCredentialsProvider delegate = new StreamingCredentialsProvider();
        RedisCredentialsProvider adapted = RedisCredentialsProvider.adapt(delegate);

        StepVerifier.create(adapted.resolveCredentials()).assertNext(c -> assertThat(c.getUsername()).isEqualTo("user"))
                .verifyComplete();
        assertThat(adapted.supportsStreaming()).isTrue();

        List<String> received = new ArrayList<>();
        Subscription subscription = adapted.subscribeToCredentials(c -> received.add(new String(c.getPassword())), e -> {
        });
        delegate.emit("rotated");
        assertThat(received).containsExactly("rotated");

        subscription.close();
        delegate.emit("after-close");
        assertThat(received).containsExactly("rotated");
    }

    @Test
    void adaptedProviderKeepsStreamingWhenSetThroughDeprecatedRedisUriSetter() {

        // An API that only accepts a RedisCredentialsProvider (for example a Spring Data Redis RedisCredentialsProviderFactory)
        // must not lose re-authentication on credential rotation.
        StreamingCredentialsProvider delegate = new StreamingCredentialsProvider();
        RedisURI uri = RedisURI.create("redis://localhost");
        uri.setCredentialsProvider(RedisCredentialsProvider.adapt(delegate));

        CredentialsProvider effective = uri.getCredentialsProviderAsync();
        assertThat(effective.supportsStreaming()).isTrue();

        List<String> received = new ArrayList<>();
        effective.subscribeToCredentials(c -> received.add(new String(c.getPassword())), e -> {
        });
        delegate.emit("rotated");
        assertThat(received).containsExactly("rotated");
    }

    /**
     * A reactor-free streaming {@link CredentialsProvider} whose emissions are driven by the test.
     */
    private static class StreamingCredentialsProvider implements CredentialsProvider {

        private final List<Consumer<RedisCredentials>> listeners = new CopyOnWriteArrayList<>();

        @Override
        public CompletionStage<RedisCredentials> resolveCredentialsAsync() {
            return CompletableFuture.completedFuture(RedisCredentials.just("user", "initial".toCharArray()));
        }

        @Override
        public boolean supportsStreaming() {
            return true;
        }

        @Override
        public Subscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
            listeners.add(onNext);
            return () -> listeners.remove(onNext);
        }

        void emit(String password) {
            listeners.forEach(l -> l.accept(RedisCredentials.just("user", password.toCharArray())));
        }

    }

}
