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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/migrate">MIGRATE</a> command. Static import the methods
 * from {@link Builder} and chain the method calls: {@code copy().auth("foobar")}.
 * <p>
 * {@link MigrateArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 */
public class MigrateArgs<K> implements CompositeArgument {

    private boolean copy = false;

    private boolean replace = false;

    List<K> keys = new ArrayList<>();

    private char[] password;

    /**
     * Builder entry points for {@link MigrateArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link MigrateArgs} and enabling {@literal COPY}.
         *
         * @return new {@link MigrateArgs} with {@literal COPY} enabled.
         * @see MigrateArgs#copy()
         */
        public static <K> MigrateArgs<K> copy() {
            return new MigrateArgs<K>().copy();
        }

        /**
         * Creates new {@link MigrateArgs} and enabling {@literal REPLACE}.
         *
         * @return new {@link MigrateArgs} with {@literal REPLACE} enabled.
         * @see MigrateArgs#replace()
         */
        public static <K> MigrateArgs<K> replace() {
            return new MigrateArgs<K>().replace();
        }

        /**
         * Creates new {@link MigrateArgs} setting a {@code key} to migrate.
         *
         * @param key must not be {@code null}.
         * @return new {@link MigrateArgs} for {@code key} to migrate.
         * @see MigrateArgs#key(Object)
         */
        public static <K> MigrateArgs<K> key(K key) {
            return new MigrateArgs<K>().key(key);
        }

        /**
         * Creates new {@link MigrateArgs} setting {@code keys} to migrate.
         *
         * @param keys must not be {@code null}.
         * @return new {@link MigrateArgs} for {@code keys} to migrate.
         * @see MigrateArgs#keys(Object[])
         */
        @SafeVarargs
        public static <K> MigrateArgs<K> keys(K... keys) {
            return new MigrateArgs<K>().keys(keys);
        }

        /**
         * Creates new {@link MigrateArgs} setting {@code keys} to migrate.
         *
         * @param keys must not be {@code null}.
         * @return new {@link MigrateArgs} for {@code keys} to migrate.
         * @see MigrateArgs#keys(Iterable)
         */
        public static <K> MigrateArgs<K> keys(Iterable<K> keys) {
            return new MigrateArgs<K>().keys(keys);
        }

        /**
         * Creates new {@link MigrateArgs} with {@code AUTH} (target authentication) enabled.
         *
         * @return new {@link MigrateArgs} with {@code AUTH} (target authentication) enabled.
         * @since 4.4.5
         * @see MigrateArgs#auth(CharSequence)
         */
        public static <K> MigrateArgs<K> auth(CharSequence password) {
            // TODO : implement auth(username,password) when https://github.com/antirez/redis/pull/7035 is fixed
            return new MigrateArgs<K>().auth(password);
        }

        /**
         * Creates new {@link MigrateArgs} with {@code AUTH} (target authentication) enabled.
         *
         * @return new {@link MigrateArgs} with {@code AUTH} (target authentication) enabled.
         * @since 4.4.5
         * @see MigrateArgs#auth(char[])
         */
        public static <K> MigrateArgs<K> auth(char[] password) {
            return new MigrateArgs<K>().auth(password);
        }

    }

    /**
     * Do not remove the key from the local instance by setting {@code COPY}.
     *
     * @return {@code this} {@link MigrateArgs}.
     */
    public MigrateArgs<K> copy() {
        this.copy = true;
        return this;
    }

    /**
     * Replace existing key on the remote instance by setting {@code REPLACE}.
     *
     * @return {@code this} {@link MigrateArgs}.
     */
    public MigrateArgs<K> replace() {
        this.replace = true;
        return this;
    }

    /**
     * Migrate a single {@code key}.
     *
     * @param key must not be {@code null}.
     * @return {@code this} {@link MigrateArgs}.
     */
    public MigrateArgs<K> key(K key) {

        LettuceAssert.notNull(key, "Key must not be null");

        this.keys.add(key);
        return this;
    }

    /**
     * Migrate one or more {@code keys}.
     *
     * @param keys must not be {@code null}.
     * @return {@code this} {@link MigrateArgs}.
     */
    @SafeVarargs
    public final MigrateArgs<K> keys(K... keys) {

        LettuceAssert.notEmpty(keys, "Keys must not be empty");

        this.keys.addAll(Arrays.asList(keys));
        return this;
    }

    /**
     * Migrate one or more {@code keys}.
     *
     * @param keys must not be {@code null}.
     * @return {@code this} {@link MigrateArgs}.
     */
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
     * @param password must not be {@code null}.
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
     * @param password must not be {@code null}.
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
