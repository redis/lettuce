/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

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

}
