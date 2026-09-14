/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapts a {@link CredentialsProvider} to the deprecated reactive {@link RedisCredentialsProvider}, so that
 * {@link RedisURI#getCredentialsProvider()} can keep returning a {@link RedisCredentialsProvider} while credentials may be
 * configured as a {@link CredentialsProvider}. The async and streaming capabilities delegate to the reactor-free provider;
 * {@link #resolveCredentials()} and {@link #credentials()} only materialise the reactive types at that boundary.
 *
 * @author Aleksandar Todorov
 * @since 7.8
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
    public Subscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
        return delegate.subscribeToCredentials(onNext, onError);
    }

    @Override
    public Flux<RedisCredentials> credentials() {
        if (!delegate.supportsStreaming()) {
            throw new UnsupportedOperationException("Streaming credentials are not supported by this provider.");
        }
        return Flux.create(sink -> {
            Subscription subscription = delegate.subscribeToCredentials(sink::next, sink::error);
            sink.onDispose(subscription::close);
        });
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
