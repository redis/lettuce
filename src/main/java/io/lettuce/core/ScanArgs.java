/*
 * Copyright 2011-2020 the original author or authors.
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

import static io.lettuce.core.protocol.CommandKeyword.COUNT;
import static io.lettuce.core.protocol.CommandKeyword.MATCH;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis scan commands ({@literal SCAN, HSCAN, SSCAN, ZSCAN}). Static import the methods from
 *
 * {@link Builder} and chain the method calls: {@code matches("weight_*").limit(0, 2)}.
 * <p>
 * {@link ScanArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @author Ge Jun
 * @since 3.0
 */
public class ScanArgs implements CompositeArgument {

    private Long count;

    private String match;

    private Charset charset;

    /**
     * Builder entry points for {@link ScanArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ScanArgs} with {@literal LIMIT} set.
         *
         * @param count number of elements to scan
         * @return new {@link ScanArgs} with {@literal LIMIT} set.
         * @see ScanArgs#limit(long)
         */
        public static ScanArgs limit(long count) {
            return new ScanArgs().limit(count);
        }

        /**
         * Creates new {@link ScanArgs} with {@literal MATCH} set.
         *
         * @param matches the filter.
         * @return new {@link ScanArgs} with {@literal MATCH} set.
         * @see ScanArgs#match(String)
         */
        public static ScanArgs matches(String matches) {
            return new ScanArgs().match(matches);
        }

    }

    /**
     * Set the match filter. Uses {@link StandardCharsets#UTF_8 UTF-8} to encode {@code match}.
     *
     * @param match the filter, must not be {@code null}.
     * @return {@literal this} {@link ScanArgs}.
     */
    public ScanArgs match(String match) {
        return match(match, StandardCharsets.UTF_8);
    }

    /**
     * Set the match filter along the given {@link Charset}.
     *
     * @param match the filter, must not be {@code null}.
     * @param charset the charset for match, must not be {@code null}.
     * @return {@literal this} {@link ScanArgs}.
     * @since 6.0
     */
    public ScanArgs match(String match, Charset charset) {

        LettuceAssert.notNull(match, "Match must not be null");
        LettuceAssert.notNull(charset, "Charset must not be null");

        this.match = match;
        this.charset = charset;
        return this;
    }

    /**
     * Limit the scan by count
     *
     * @param count number of elements to scan
     * @return {@literal this} {@link ScanArgs}.
     */
    public ScanArgs limit(long count) {

        this.count = count;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (match != null) {
            args.add(MATCH).add(match.getBytes(charset));
        }

        if (count != null) {
            args.add(COUNT).add(count);
        }
    }

}
