/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapts a reactor-free {@link CredentialsProvider} to the deprecated reactive {@link RedisCredentialsProvider}, so
 * {@link RedisURI#getCredentialsProvider()} can keep returning a {@link RedisCredentialsProvider} while credentials are stored
 * internally as a {@link CredentialsProvider}. Only {@link #resolveCredentials()} materialises a {@link Mono}; the async and
 * streaming paths delegate directly and stay reactor-free.
 * <p>
 * Use {@link #of(CredentialsProvider)} to create an adapter: an adapted
 * {@link CredentialsProvider.ImmediateCredentialsProvider} stays immediate, as a
 * {@link RedisCredentialsProvider.ImmediateRedisCredentialsProvider}.
 *
 * @author Aleksandar Todorov
 * @since 7.9
 */
class AsyncCredentialsProviderAdapter implements RedisCredentialsProvider {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(AsyncCredentialsProviderAdapter.class);

    private final CredentialsProvider delegate;

    AsyncCredentialsProviderAdapter(CredentialsProvider delegate) {
        this.delegate = delegate;
    }

    /**
     * Adapt {@code delegate} to {@link RedisCredentialsProvider}. If {@code delegate} is a
     * {@link CredentialsProvider.ImmediateCredentialsProvider}, the adapter is a
     * {@link RedisCredentialsProvider.ImmediateRedisCredentialsProvider} that resolves credentials through
     * {@link CredentialsProvider.ImmediateCredentialsProvider#resolveCredentialsNow()}, so code checking for an immediate
     * provider keeps working.
     *
     * @param delegate the provider to adapt, must not be {@code null}.
     * @return the adapter.
     */
    static AsyncCredentialsProviderAdapter of(CredentialsProvider delegate) {

        if (delegate instanceof CredentialsProvider.ImmediateCredentialsProvider) {
            return new Immediate((CredentialsProvider.ImmediateCredentialsProvider) delegate);
        }

        return new AsyncCredentialsProviderAdapter(delegate);
    }

    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        // Defer to the Supplier overload so the delegate is invoked per subscription rather than eagerly at Mono creation;
        // this keeps the returned Mono cold and re-subscribable, matching other RedisCredentialsProvider implementations.
        return Mono.fromCompletionStage(delegate::resolveCredentialsAsync);
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
            Subscription subscription = delegate.subscribeToCredentials(sink::next,
                    e -> log.warn("Credentials provider reported an error; the credentials stream stays open", e));
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

    /**
     * Adapter for a {@link CredentialsProvider.ImmediateCredentialsProvider}, keeping it immediate.
     */
    static class Immediate extends AsyncCredentialsProviderAdapter
            implements RedisCredentialsProvider.ImmediateRedisCredentialsProvider {

        private final CredentialsProvider.ImmediateCredentialsProvider delegate;

        Immediate(CredentialsProvider.ImmediateCredentialsProvider delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public RedisCredentials resolveCredentialsNow() {
            return delegate.resolveCredentialsNow();
        }

    }

}
