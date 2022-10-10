/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Interface for loading {@link RedisCredentials} that are used for authentication. A commonly-used implementation is
 * {@link StaticCredentialsProvider} for a fixed set of credentials.
 * <p>
 * Credentials are requested by the driver after connecting to the server. Therefore, credential retrieval is subject to
 * complete within the connection creation timeout to avoid connection failures.
 *
 * @author Mark Paluch
 * @since 6.2
 */
@FunctionalInterface
public interface RedisCredentialsProvider {

    /**
     * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
     * {@code RedisCredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation
     * might load credentials from an existing key management system, or load new credentials when credentials are rotated. If
     * an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be raised.
     *
     * @return a {@link Mono} emitting {@link RedisCredentials} that can be used to authorize a Redis connection.
     */
    Mono<RedisCredentials> resolveCredentials();

    /**
     * Creates a new {@link RedisCredentialsProvider} from a given {@link Supplier}.
     *
     * @param supplier must not be {@code null}.
     * @return the {@link RedisCredentials} using credentials from {@link Supplier}.
     */
    static RedisCredentialsProvider from(Supplier<RedisCredentials> supplier) {

        LettuceAssert.notNull(supplier, "Supplier must not be null");

        return () -> Mono.fromSupplier(supplier);
    }

    /**
     * Extension to {@link RedisCredentialsProvider} that resolves credentials immediately without the need to defer the
     * credential resolution.
     */
    @FunctionalInterface
    interface ImmediateRedisCredentialsProvider extends RedisCredentialsProvider {

        @Override
        default Mono<RedisCredentials> resolveCredentials() {
            return Mono.just(resolveCredentialsNow());
        }

        /**
         * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
         * {@code RedisCredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation
         * might load credentials from an existing key management system, or load new credentials when credentials are rotated.
         * If an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be
         * raised.
         *
         * @return the resolved {@link RedisCredentials} that can be used to authorize a Redis connection.
         */
        RedisCredentials resolveCredentialsNow();

    }

}
