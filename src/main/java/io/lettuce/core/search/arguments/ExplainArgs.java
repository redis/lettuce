/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/ft.explain/">FT.EXPLAIN</a> command.
 * Static import methods are available.
 * <p>
 * {@link ExplainArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class ExplainArgs<K, V> {

    private QueryDialects dialect = QueryDialects.DIALECT2;

    /**
     * Builder entry points for {@link ExplainArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ExplainArgs} setting {@literal DIALECT}.
         *
         * @return new {@link ExplainArgs} with {@literal DIALECT} set.
         * @see ExplainArgs#dialect(QueryDialects)
         */
        public static <K, V> ExplainArgs<K, V> dialect(QueryDialects dialect) {
            return new ExplainArgs<K, V>().dialect(dialect);
        }

    }

    /**
     * Set the dialect version under which to execute the query. If not specified, the query executes under the default dialect
     * version set during module initial loading or via FT.CONFIG SET command.
     *
     * @param dialect the dialect version.
     * @return {@code this} {@link ExplainArgs}.
     */
    public ExplainArgs<K, V> dialect(QueryDialects dialect) {
        this.dialect = dialect;
        return this;
    }

    /**
     * Builds the arguments and appends them to the {@link CommandArgs}.
     *
     * @param args the command arguments to append to.
     */
    public void build(CommandArgs<K, V> args) {
        if (dialect != null) {
            args.add("DIALECT").add(dialect.toString());
        }
    }

}
