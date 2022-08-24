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

import io.lettuce.core.internal.LettuceStrings;

/**
 * Provides credentials to access a secured Redis service.
 *
 * @author Jon Iantosca
 * @author Mark Paluch
 * @since 6.2
 */
public interface RedisCredentials {

    /**
     * Retrieve the Redis user, used to identify the user interacting with Redis. Can be used with Redis 6 and newer server
     * versions.
     *
     * @return the user name. Can be {@code null} if not set.
     * @see #hasUsername()
     */
    String getUsername();

    /**
     * Return whether the username is configured.
     *
     * @return {@code true} if the username is configured; {@code false} otherwise.
     */
    boolean hasUsername();

    /**
     * Retrieve the Redis password, used to authenticate the user interacting with Redis.
     *
     * @return the password Can be {@code null} if not set.
     * @see #hasUsername()
     */
    char[] getPassword();

    /**
     * Return whether the password is configured.
     *
     * @return {@code true} if the password is configured; {@code false} otherwise
     */
    boolean hasPassword();

    /**
     * Create a static {@link RedisCredentials} object from {@code username} and {@code password}.
     *
     * @param username can be {@code null}
     * @param password can be {@code null}
     * @return the static {@link RedisCredentials} object from {@code username} and {@code password}
     */
    static RedisCredentials just(String username, CharSequence password) {
        return new StaticRedisCredentials(username, password == null ? null : LettuceStrings.toCharArray(password));
    }

    /**
     * Create a static {@link RedisCredentials} object from {@code username} and {@code password}.
     *
     * @param username can be {@code null}
     * @param password can be {@code null}
     * @return the static {@link RedisCredentials} object from {@code username} and {@code password}
     */
    static RedisCredentials just(String username, char[] password) {
        return new StaticRedisCredentials(username, password);
    }

}
