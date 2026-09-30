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

import java.util.concurrent.CompletionException;

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

}
