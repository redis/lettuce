/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for {@code SORTBY} clause.
 *
 * @param <K> Key type.
 * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/sorting/">Sorting</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
public class SortByArgs<K> {

    private K attribute;

    private boolean isDescending;

    private boolean withCount;

    /**
     * Used to build a new instance of the {@link SortByArgs}.
     *
     * @return a {@link SortByArgs.Builder} that provides the option to build up a new instance of the {@link SearchArgs}
     * @param <K> the key type
     */
    public static <K> SortByArgs.Builder<K> builder() {
        return new SortByArgs.Builder<>();
    }

    /**
     * Builder for {@link SortByArgs}.
     * <p>
     * As a final step the {@link SortByArgs.Builder#build()} method needs to be executed to create the final {@link SortByArgs}
     * instance.
     *
     * @param <K> the key type
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     */
    public static class Builder<K> {

        private final SortByArgs<K> sortByArgs = new SortByArgs<>();

        /**
         * Add an attribute to sort by.
         *
         * @param attribute the attribute to sort by
         * @return the instance of the current {@link SortByArgs.Builder} for the purpose of method chaining
         */
        public SortByArgs.Builder<K> attribute(K attribute) {
            sortByArgs.attribute = attribute;
            return this;
        }

        /**
         * Sort in descending order. Default is ascending.
         *
         * @return the instance of the current {@link SortByArgs.Builder} for the purpose of method chaining
         */
        public SortByArgs.Builder<K> descending() {
            sortByArgs.isDescending = true;
            return this;
        }

        /**
         * Include the accurate counts for the query results with sorting. Default is disabled.
         *
         * @return the instance of the current {@link SortByArgs.Builder} for the purpose of method chaining
         */
        public SortByArgs.Builder<K> withCount() {
            sortByArgs.withCount = true;
            return this;
        }

        /**
         * Build the {@link SortByArgs}.
         *
         * @return the {@link SortByArgs}
         */
        public SortByArgs<K> build() {
            return sortByArgs;
        }

    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<K, ?> args) {
        args.add(CommandKeyword.SORTBY).addKey(attribute);

        if (this.isDescending) {
            args.add(CommandKeyword.DESC);
        }

        if (this.withCount) {
            args.add(CommandKeyword.WITHCOUNT);
        }
    }

}
