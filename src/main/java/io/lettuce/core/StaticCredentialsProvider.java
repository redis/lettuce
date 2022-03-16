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

import reactor.core.publisher.Mono;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Static implementation of {@link RedisCredentialsProvider}.
 *
 * @author Mark Paluch
 * @since 6.2
 */
public class StaticCredentialsProvider
        implements RedisCredentialsProvider, RedisCredentialsProvider.ImmediateRedisCredentialsProvider {

    private final RedisCredentials credentials;

    private final Mono<RedisCredentials> mono;

    /**
     * Create a static {@link StaticCredentialsProvider} object from {@code username} and {@code password}.
     *
     * @param username can be {@code null}.
     * @param password can be {@code null}.
     */
    public StaticCredentialsProvider(String username, char[] password) {
        this(RedisCredentials.just(username, password));
    }

    /**
     * Create a static {@link StaticCredentialsProvider} object from {@link RedisCredentials}. The snapshot of the given
     * credentials is used to create a static representation to avoid credentials changes if the {@link RedisCredentials} change
     * over time.
     *
     * @param credentials must not be {@code null}.
     */
    public StaticCredentialsProvider(RedisCredentials credentials) {

        LettuceAssert.notNull(credentials, "RedisCredentials must not be null");

        this.credentials = RedisCredentials.just(credentials.getUsername(), credentials.getPassword());
        this.mono = Mono.just(credentials);
    }

    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        return mono;
    }

    @Override
    public RedisCredentials resolveCredentialsNow() {
        return this.credentials;
    }

}
