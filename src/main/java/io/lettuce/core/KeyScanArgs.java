/*
 * Copyright 2020-2022 the original author or authors.
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

import static io.lettuce.core.protocol.CommandKeyword.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis {@code SCAN} command. Static import the methods from
 *
 * {@link KeyScanArgs.Builder} and chain the method calls: {@code type("string").limit(2)}.
 * <p>
 * {@link KeyScanArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mykola Makhin
 * @since 6.1
 */
public class KeyScanArgs extends ScanArgs {

    private String type;

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
         * @see KeyScanArgs#limit(long)
         */
        public static KeyScanArgs limit(long count) {
            return new KeyScanArgs().limit(count);
        }

        /**
         * Creates new {@link ScanArgs} with {@literal MATCH} set.
         *
         * @param matches the filter.
         * @return new {@link ScanArgs} with {@literal MATCH} set.
         * @see KeyScanArgs#match(String)
         */
        public static KeyScanArgs matches(String matches) {
            return new KeyScanArgs().match(matches);
        }

        /**
         * Creates new {@link ScanArgs} with {@literal TYPE} set.
         *
         * @param type the filter.
         * @return new {@link ScanArgs} with {@literal TYPE} set.
         * @see KeyScanArgs#type(String)
         */
        public static KeyScanArgs type(String type) {
            return new KeyScanArgs().type(type);
        }

    }

    /**
     * Return keys only of specified type.
     *
     * @param type of keys as returned by TYPE command
     * @return {@literal this} {@link KeyScanArgs}.
     */
    public KeyScanArgs type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Set the match filter. Uses {@link StandardCharsets#UTF_8 UTF-8} to encode {@code match}.
     *
     * @param match the filter, must not be {@code null}.
     * @return {@literal this} {@link ScanArgs}.
     */
    @Override
    public KeyScanArgs match(String match) {
        super.match(match);
        return this;
    }

    /**
     * Set the match filter along the given {@link Charset}.
     *
     * @param match the filter, must not be {@code null}.
     * @param charset the charset for match, must not be {@code null}.
     * @return {@literal this} {@link ScanArgs}.
     */
    @Override
    public KeyScanArgs match(String match, Charset charset) {
        super.match(match, charset);
        return this;
    }

    /**
     * Limit the scan by count
     *
     * @param count number of elements to scan
     * @return {@literal this} {@link ScanArgs}.
     */
    @Override
    public KeyScanArgs limit(long count) {
        super.limit(count);
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        super.build(args);
        if (type != null) {
            args.add(TYPE).add(type);
        }
    }

}
