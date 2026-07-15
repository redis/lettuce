/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.concurrent.CompletionStage;

import reactor.core.publisher.Mono;

/**
 * Adapts a {@link CredentialsProvider} to the deprecated reactive {@link RedisCredentialsProvider}, so that
 * {@link RedisURI#getCredentialsProvider()} can keep returning a {@link RedisCredentialsProvider} while credentials may be
 * configured as a {@link CredentialsProvider}. Only {@link #resolveCredentials()} materialises a {@link Mono}; the async path
 * delegates directly.
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
