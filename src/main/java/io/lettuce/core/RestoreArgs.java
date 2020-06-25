/*
 * Copyright 2018-2020 the original author or authors.
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

import java.time.Duration;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/restore">RESTORE</a> command. Static import the methods
 * from {@link RestoreArgs.Builder} and call the methods: {@code ttl(…)} .
 * <p>
 * {@link RestoreArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class RestoreArgs {

    long ttl;

    boolean replace;

    /**
     * Builder entry points for {@link XAddArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link RestoreArgs} and set the TTL.
         *
         * @return new {@link RestoreArgs} with min idle time set.
         * @see RestoreArgs#ttl(long)
         */
        public static RestoreArgs ttl(long milliseconds) {
            return new RestoreArgs().ttl(milliseconds);
        }

        /**
         * Creates new {@link RestoreArgs} and set the minimum idle time.
         *
         * @return new {@link RestoreArgs} with min idle time set.
         * @see RestoreArgs#ttl(Duration)
         */
        public static RestoreArgs ttl(Duration ttl) {

            LettuceAssert.notNull(ttl, "Time to live must not be null");

            return ttl(ttl.toMillis());
        }

    }

    /**
     * Set TTL in {@code milliseconds} after restoring the key.
     *
     * @param milliseconds time to live.
     * @return {@code this}.
     */
    public RestoreArgs ttl(long milliseconds) {

        this.ttl = milliseconds;
        return this;
    }

    /**
     * Set TTL in {@code milliseconds} after restoring the key.
     *
     * @param ttl time to live.
     * @return {@code this}.
     */
    public RestoreArgs ttl(Duration ttl) {

        LettuceAssert.notNull(ttl, "Time to live must not be null");

        return ttl(ttl.toMillis());
    }

    /**
     * Replaces existing keys if the target key already exists.
     *
     * @return {@code this}.
     */
    public RestoreArgs replace() {
        return replace(true);
    }

    /**
     * Replaces existing keys if the target key already exists.
     *
     * @param replace {@code true} to enable replacing of existing keys.
     * @return {@code this}.
     */
    public RestoreArgs replace(boolean replace) {

        this.replace = replace;
        return this;
    }

}
