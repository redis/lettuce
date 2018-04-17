/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Argument list builder for the new redis <a href="http://redis.io/commands/migrate">MIGRATE</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code copy().auth("foobar")}.
 *
 * @author Mark Paluch
 */
public class MigrateArgs<K> implements CompositeArgument {

    private boolean copy = false;
    private boolean replace = false;
    List<K> keys = new ArrayList<>();
    private char[] password;

    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        public static <K> MigrateArgs<K> copy() {
            return new MigrateArgs<K>().copy();
        }

        public static <K> MigrateArgs<K> replace() {
            return new MigrateArgs<K>().replace();
        }

        public static <K> MigrateArgs<K> key(K key) {
            return new MigrateArgs<K>().key(key);
        }

        public static <K> MigrateArgs<K> auth(CharSequence password) {
            return new MigrateArgs<K>().auth(password);
        }

        public static <K> MigrateArgs<K> auth(char[] password) {
            return new MigrateArgs<K>().auth(password);
        }

        @SafeVarargs
        public static <K> MigrateArgs<K> keys(K... keys) {
            return new MigrateArgs<K>().keys(keys);
        }

        public static <K> MigrateArgs<K> keys(Iterable<K> keys) {
            return new MigrateArgs<K>().keys(keys);
        }

    }

    public MigrateArgs<K> copy() {
        this.copy = true;
        return this;
    }

    public MigrateArgs<K> replace() {
        this.replace = true;
        return this;
    }

    public MigrateArgs<K> key(K key) {
        LettuceAssert.notNull(key, "Key must not be null");
        this.keys.add(key);
        return this;
    }

    @SafeVarargs
    public final MigrateArgs<K> keys(K... keys) {
        LettuceAssert.notEmpty(keys, "Keys must not be empty");
        this.keys.addAll(Arrays.asList(keys));
        return this;
    }

    public MigrateArgs<K> keys(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys must not be null");
        for (K key : keys) {
            this.keys.add(key);
        }
        return this;
    }

    /**
     * Set {@literal AUTH} {@code password} option.
     *
     * @param password must not be {@literal null}.
     * @return {@code this} {@link MigrateArgs}.
     * @since 4.4.5
     */
    public MigrateArgs<K> auth(CharSequence password) {

        LettuceAssert.notNull(password, "Password must not be null");

        char[] chars = new char[password.length()];

        for (int i = 0; i < password.length(); i++) {
            chars[i] = password.charAt(i);
        }

        this.password = chars;
        return this;
    }

    /**
     * Set {@literal AUTH} {@code password} option.
     *
     * @param password must not be {@literal null}.
     * @return {@code this} {@link MigrateArgs}.
     * @since 4.4.5
     */
    public MigrateArgs<K> auth(char[] password) {

        LettuceAssert.notNull(password, "Password must not be null");

        this.password = Arrays.copyOf(password, password.length);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <K, V> void build(CommandArgs<K, V> args) {

        if (copy) {
            args.add(CommandKeyword.COPY);
        }

        if (replace) {
            args.add(CommandKeyword.REPLACE);
        }

        if (password != null) {
            args.add(CommandType.AUTH).add(password);
        }

        if (keys.size() > 1) {
            args.add(CommandType.KEYS);
            args.addKeys((List<K>) keys);
        }
    }
}
