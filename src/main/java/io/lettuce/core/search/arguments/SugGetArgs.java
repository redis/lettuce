/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Arguments for the FT.SUGGET command.
 * <p>
 * This class provides a builder pattern for constructing arguments for getting completion suggestions from an auto-complete dictionary.
 * The FT.SUGGET command retrieves completion suggestions for a prefix from an auto-complete suggestion dictionary.
 * </p>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SugGetArgs<K, V> {

    private boolean fuzzy;

    private boolean withScores;

    private boolean withPayloads;

    private Long max;

    /**
     * Builder entry points for {@link SugGetArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SugGetArgs} setting {@literal FUZZY}.
         *
         * @return new {@link SugGetArgs} with {@literal FUZZY} set.
         * @see SugGetArgs#fuzzy()
         */
        public static <K, V> SugGetArgs<K, V> fuzzy() {
            return new SugGetArgs<K, V>().fuzzy();
        }

        /**
         * Creates new {@link SugGetArgs} setting {@literal WITHSCORES}.
         *
         * @return new {@link SugGetArgs} with {@literal WITHSCORES} set.
         * @see SugGetArgs#withScores()
         */
        public static <K, V> SugGetArgs<K, V> withScores() {
            return new SugGetArgs<K, V>().withScores();
        }

        /**
         * Creates new {@link SugGetArgs} setting {@literal WITHPAYLOADS}.
         *
         * @return new {@link SugGetArgs} with {@literal WITHPAYLOADS} set.
         * @see SugGetArgs#withPayloads()
         */
        public static <K, V> SugGetArgs<K, V> withPayloads() {
            return new SugGetArgs<K, V>().withPayloads();
        }

        /**
         * Creates new {@link SugGetArgs} setting {@literal MAX}.
         *
         * @param max the maximum number of suggestions to return.
         * @return new {@link SugGetArgs} with {@literal MAX} set.
         * @see SugGetArgs#max(long)
         */
        public static <K, V> SugGetArgs<K, V> max(long max) {
            return new SugGetArgs<K, V>().max(max);
        }

    }

    /**
     * Perform a fuzzy prefix search, including prefixes at Levenshtein distance of 1 from the prefix sent.
     *
     * @return {@code this} {@link SugGetArgs}.
     */
    public SugGetArgs<K, V> fuzzy() {
        this.fuzzy = true;
        return this;
    }

    /**
     * Also return the score of each suggestion. This can be used to merge results from multiple instances.
     *
     * @return {@code this} {@link SugGetArgs}.
     */
    public SugGetArgs<K, V> withScores() {
        this.withScores = true;
        return this;
    }

    /**
     * Return optional payloads saved along with the suggestions. If no payload is present for an entry, it returns a null reply.
     *
     * @return {@code this} {@link SugGetArgs}.
     */
    public SugGetArgs<K, V> withPayloads() {
        this.withPayloads = true;
        return this;
    }

    /**
     * Limit the results to a maximum of {@code max} suggestions (default: 5).
     *
     * @param max the maximum number of suggestions to return.
     * @return {@code this} {@link SugGetArgs}.
     */
    public SugGetArgs<K, V> max(long max) {
        this.max = max;
        return this;
    }

    /**
     * Check if WITHSCORES option is enabled.
     *
     * @return {@code true} if WITHSCORES is enabled
     */
    public boolean isWithScores() {
        return withScores;
    }

    /**
     * Check if WITHPAYLOADS option is enabled.
     *
     * @return {@code true} if WITHPAYLOADS is enabled
     */
    public boolean isWithPayloads() {
        return withPayloads;
    }

    /**
     * Builds the arguments and appends them to the {@link CommandArgs}.
     *
     * @param args the command arguments to append to.
     */
    public void build(CommandArgs<K, V> args) {
        if (fuzzy) {
            args.add("FUZZY");
        }

        if (withScores) {
            args.add("WITHSCORES");
        }

        if (withPayloads) {
            args.add("WITHPAYLOADS");
        }

        if (max != null) {
            args.add("MAX").add(max);
        }
    }

}
