// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandKeyword.*;
import static com.lambdaworks.redis.protocol.CommandType.GET;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;

/**
 * Argument list builder for the redis <a href="http://redis.io/commands/sort">SORT</a> command. Static import the methods from
 * {@link Builder} and chain the method calls: {@code by("weight_*").desc().limit(0, 2)}.
 * 
 * @author Will Glozer
 */
public class SortArgs implements CompositeArgument {

    private String by;
    private Limit limit = Limit.unlimited();
    private List<String> get;
    private CommandKeyword order;
    private boolean alpha;

    /**
     * Static builder methods.
     */
    public static class Builder {
        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static SortArgs by(String pattern) {
            return new SortArgs().by(pattern);
        }

        public static SortArgs limit(long offset, long count) {
            return new SortArgs().limit(offset, count);
        }

        public static SortArgs get(String pattern) {
            return new SortArgs().get(pattern);
        }

        public static SortArgs asc() {
            return new SortArgs().asc();
        }

        public static SortArgs desc() {
            return new SortArgs().desc();
        }

        public static SortArgs alpha() {
            return new SortArgs().alpha();
        }
    }

    public SortArgs by(String pattern) {
        by = pattern;
        return this;
    }

    public SortArgs limit(long offset, long count) {
        return limit(Limit.create(offset, count));
    }

    public SortArgs limit(Limit limit) {

        LettuceAssert.notNull(limit, "Limit must not be null");

        this.limit = limit;
        return this;
    }

    public SortArgs get(String pattern) {
        if (get == null) {
            get = new ArrayList<>();
        }
        get.add(pattern);
        return this;
    }

    public SortArgs asc() {
        order = ASC;
        return this;
    }

    public SortArgs desc() {
        order = DESC;
        return this;
    }

    public SortArgs alpha() {
        alpha = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (by != null) {
            args.add(BY);
            args.add(by);
        }

        if (get != null) {
            for (String pattern : get) {
                args.add(GET);
                args.add(pattern);
            }
        }

        if (limit != null && limit.isLimited()) {
            args.add(LIMIT);
            args.add(limit.getOffset());
            args.add(limit.getCount());
        }

        if (order != null) {
            args.add(order);
        }

        if (alpha) {
            args.add(ALPHA);
        }

    }

    <K, V> void build(CommandArgs<K, V> args, K store) {

        build(args);

        if (store != null) {
            args.add(STORE);
            args.addKey(store);
        }
    }
}
