/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

/**
 * Adapts a reactor-free {@link CredentialsProvider} to the deprecated reactive {@link RedisCredentialsProvider}, so
 * {@link RedisURI#getCredentialsProvider()} can keep returning a {@link RedisCredentialsProvider} while credentials are stored
 * internally as a {@link CredentialsProvider}. Only {@link #resolveCredentials()} materialises a {@link Mono}; the async and
 * streaming paths delegate directly and stay reactor-free.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
class AsyncCredentialsProviderAdapter implements RedisCredentialsProvider {

    private final CredentialsProvider delegate;

    AsyncCredentialsProviderAdapter(CredentialsProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        return Mono.fromCompletionStage(delegate.resolveCredentialsAsync());
    }

    @Override
    public CompletionStage<RedisCredentials> resolveCredentialsAsync() {
        return delegate.resolveCredentialsAsync();
    }

    @Override
    public boolean supportsStreaming() {
        return delegate.supportsStreaming();
    }

    @Override
    public CredentialsSubscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
        return delegate.subscribeToCredentials(onNext, onError);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AsyncCredentialsProviderAdapter)) {
            return false;
        }
        return delegate.equals(((AsyncCredentialsProviderAdapter) o).delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

}
