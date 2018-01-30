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
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Argument list builder for the new redis <a href="http://redis.io/commands/migrate">MIGRATE</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 *
 * @author Mark Paluch
 */
public class MigrateArgs<K> implements CompositeArgument {

    private boolean copy = false;
    private boolean replace = false;
    List<K> keys = new ArrayList<>();

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
        for (K key : keys) {
            this.keys.add(key);
        }
        return this;
    }

    public MigrateArgs<K> keys(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys must not be null");
        for (K key : keys) {
            this.keys.add(key);
        }
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

        if (keys.size() > 1) {
            args.add(CommandType.KEYS);
            args.addKeys((List<K>) keys);
        }
    }
}
