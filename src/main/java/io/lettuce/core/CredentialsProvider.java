/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.concurrent.CompletionStage;

import reactor.core.publisher.Mono;

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
@SuppressWarnings("deprecation")
public interface CredentialsProvider extends RedisCredentialsProvider {

    /**
     * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
     * {@code CredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation might
     * load credentials from an existing key management system, or load new credentials when credentials are rotated. If an
     * error occurs during the loading of credentials or credentials could not be found, the returned {@link CompletionStage}
     * completes exceptionally.
     *
     * @return a {@link CompletionStage} that completes with the {@link RedisCredentials} used to authorize a Redis connection.
     */
    @Override
    CompletionStage<RedisCredentials> resolveCredentialsAsync();

    /**
     * Returns the {@link RedisCredentials} from {@link #resolveCredentialsAsync()} as a {@link Mono}, satisfying the deprecated
     * {@link RedisCredentialsProvider#resolveCredentials()} contract. Implementations need not override this method.
     *
     * @return a {@link Mono} emitting the {@link RedisCredentials} from {@link #resolveCredentialsAsync()}.
     */
    @Override
    default Mono<RedisCredentials> resolveCredentials() {
        return Mono.fromCompletionStage(resolveCredentialsAsync());
    }

}
