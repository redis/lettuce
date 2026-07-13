/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Interface for loading {@link RedisCredentials} used to authenticate a Redis connection, resolving credentials asynchronously
 * as a {@link CompletionStage}. Replaces {@link RedisCredentialsProvider}.
 * <p>
 * Credentials are requested by the driver after connecting to the server. Therefore, credential retrieval is subject to
 * complete within the connection creation timeout to avoid connection failures.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
@FunctionalInterface
public interface CredentialsProvider {

    /**
     * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
     * {@code CredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation might
     * load credentials from an existing key management system, or load new credentials when credentials are rotated. If an
     * error occurs during the loading of credentials or credentials could not be found, the returned {@link CompletionStage}
     * completes exceptionally.
     *
     * @return a {@link CompletionStage} that completes with the {@link RedisCredentials} used to authorize a Redis connection.
     */
    CompletionStage<RedisCredentials> resolveCredentialsAsync();

    /**
     * Creates a new {@link CredentialsProvider} from a given {@link Supplier}.
     *
     * @param supplier must not be {@code null}.
     * @return a {@link CredentialsProvider} resolving the {@link RedisCredentials} produced by the {@link Supplier}.
     */
    static CredentialsProvider from(Supplier<RedisCredentials> supplier) {

        LettuceAssert.notNull(supplier, "Supplier must not be null");

        return () -> {
            try {
                RedisCredentials credentials = supplier.get();
                if (credentials == null) {
                    return Futures.failed(new IllegalStateException("Provided RedisCredentials supplier returned null"));
                }
                return CompletableFuture.completedFuture(credentials);
            } catch (Exception e) {
                return Futures.failed(e);
            }
        };
    }

    /**
     * Some implementations of the {@link CredentialsProvider} may support streaming new credentials, based on some event that
     * originates outside the driver. In this case they should indicate that so the {@link RedisAuthenticationHandler} is able
     * to process these new credentials.
     *
     * @return whether the {@link CredentialsProvider} supports streaming credentials.
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * Subscribe to credential updates produced by this provider.
     * <p>
     * For implementations that support streaming credentials (as indicated by {@link #supportsStreaming()} returning
     * {@code true}), the {@code onNext} consumer is invoked whenever new credentials become available, typically as a result of
     * external events such as token renewal or rotation. The {@code onError} consumer is invoked when the provider observes a
     * failure while obtaining new credentials.
     * <p>
     * Replay semantics on subscription are defined by the implementation and callers should consult the concrete provider's
     * documentation. Subscribers must not assume any specific replay behaviour from this interface alone.
     * <p>
     * Implementations that do not support streaming credentials (where {@link #supportsStreaming()} returns {@code false})
     * throw an {@link UnsupportedOperationException} by default.
     *
     * @param onNext consumer invoked with each new {@link RedisCredentials} value, must not be {@code null}.
     * @param onError consumer invoked with errors observed while producing credentials, must not be {@code null}.
     * @return a {@link Subscription} that can be used to stop receiving updates.
     * @throws UnsupportedOperationException if the provider does not support streaming credentials.
     */
    default Subscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
        throw new UnsupportedOperationException("Streaming credentials are not supported by this provider.");
    }

    /**
     * Extension to {@link CredentialsProvider} that resolves credentials immediately without the need to defer the credential
     * resolution.
     */
    @FunctionalInterface
    interface ImmediateCredentialsProvider extends CredentialsProvider {

        @Override
        default CompletionStage<RedisCredentials> resolveCredentialsAsync() {
            try {
                RedisCredentials credentials = resolveCredentialsNow();
                if (credentials == null) {
                    return Futures.failed(new IllegalStateException("RedisCredentials resolved to null"));
                }
                return CompletableFuture.completedFuture(credentials);
            } catch (Exception e) {
                return Futures.failed(e);
            }
        }

        /**
         * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
         * {@code CredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation might
         * load credentials from an existing key management system, or load new credentials when credentials are rotated. If an
         * error occurs during the loading of credentials or credentials could not be found, a runtime exception will be raised.
         *
         * @return the resolved {@link RedisCredentials} that can be used to authorize a Redis connection.
         */
        RedisCredentials resolveCredentialsNow();

    }

}
