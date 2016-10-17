package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * Argument list builder for the new redis <a href="http://redis.io/commands/migrate">MIGRATE</a> command. Static import
 * the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
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
        Iterator<K> iterator = keys.iterator();
        while (iterator.hasNext()) {
            this.keys.add(iterator.next());
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
            args.addKeys((Iterable<K>) keys);
        }

    }
}
