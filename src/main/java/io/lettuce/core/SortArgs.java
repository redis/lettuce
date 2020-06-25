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

import static io.lettuce.core.protocol.CommandKeyword.*;
import static io.lettuce.core.protocol.CommandType.GET;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/sort">SORT</a> command. Static import the methods from
 * {@link Builder} and chain the method calls: {@code by("weight_*").desc().limit(0, 2)}.
 * <p>
 * {@link ScanArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Will Glozer
 * @author Mark Paluch
 */
public class SortArgs implements CompositeArgument {

    private String by;

    private Limit limit = Limit.unlimited();

    private List<String> get;

    private CommandKeyword order;

    private boolean alpha;

    /**
     * Builder entry points for {@link SortArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SortArgs} setting {@literal PATTERN}.
         *
         * @param pattern must not be {@code null}.
         * @return new {@link SortArgs} with {@literal PATTERN} set.
         * @see SortArgs#by(String)
         */
        public static SortArgs by(String pattern) {
            return new SortArgs().by(pattern);
        }

        /**
         * Creates new {@link SortArgs} setting {@literal LIMIT}.
         *
         * @param offset
         * @param count
         * @return new {@link SortArgs} with {@literal LIMIT} set.
         * @see SortArgs#limit(long, long)
         */
        public static SortArgs limit(long offset, long count) {
            return new SortArgs().limit(offset, count);
        }

        /**
         * Creates new {@link SortArgs} setting {@literal GET}.
         *
         * @param pattern must not be {@code null}.
         * @return new {@link SortArgs} with {@literal GET} set.
         * @see SortArgs#by(String)
         */
        public static SortArgs get(String pattern) {
            return new SortArgs().get(pattern);
        }

        /**
         * Creates new {@link SortArgs} setting {@literal ASC}.
         *
         * @return new {@link SortArgs} with {@literal ASC} set.
         * @see SortArgs#asc()
         */
        public static SortArgs asc() {
            return new SortArgs().asc();
        }

        /**
         * Creates new {@link SortArgs} setting {@literal DESC}.
         *
         * @return new {@link SortArgs} with {@literal DESC} set.
         * @see SortArgs#desc()
         */
        public static SortArgs desc() {
            return new SortArgs().desc();
        }

        /**
         * Creates new {@link SortArgs} setting {@literal ALPHA}.
         *
         * @return new {@link SortArgs} with {@literal ALPHA} set.
         * @see SortArgs#alpha()
         */
        public static SortArgs alpha() {
            return new SortArgs().alpha();
        }

    }

    /**
     * Sort keys by an external list. Key names are obtained substituting the first occurrence of {@code *} with the actual
     * value of the element in the list.
     *
     * @param pattern key name pattern.
     * @return {@code this} {@link SortArgs}.
     */
    public SortArgs by(String pattern) {

        LettuceAssert.notNull(pattern, "Pattern must not be null");

        this.by = pattern;
        return this;
    }

    /**
     * Limit the number of returned elements.
     *
     * @param offset
     * @param count
     * @return {@code this} {@link SortArgs}.
     */
    public SortArgs limit(long offset, long count) {
        return limit(Limit.create(offset, count));
    }

    /**
     * Limit the number of returned elements.
     *
     * @param limit must not be {@code null}.
     * @return {@code this} {@link SortArgs}.
     */
    public SortArgs limit(Limit limit) {

        LettuceAssert.notNull(limit, "Limit must not be null");

        this.limit = limit;
        return this;
    }

    /**
     * Retrieve external keys during sort. {@literal GET} supports {@code #} and {@code *} wildcards.
     *
     * @param pattern must not be {@code null}.
     * @return {@code this} {@link SortArgs}.
     */
    public SortArgs get(String pattern) {

        LettuceAssert.notNull(pattern, "Pattern must not be null");

        if (get == null) {
            get = new ArrayList<>();
        }
        get.add(pattern);
        return this;
    }

    /**
     * Apply numeric sort in ascending order.
     *
     * @return {@code this} {@link SortArgs}.
     */
    public SortArgs asc() {
        order = ASC;
        return this;
    }

    /**
     * Apply numeric sort in descending order.
     *
     * @return {@code this} {@link SortArgs}.
     */
    public SortArgs desc() {
        order = DESC;
        return this;
    }

    /**
     * Apply lexicographically sort.
     *
     * @return {@code this} {@link SortArgs}.
     */
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
