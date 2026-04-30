/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Arguments for the FT.SUGADD command.
 * <p>
 * This class provides a builder pattern for constructing arguments for adding suggestions to an auto-complete dictionary. The
 * FT.SUGADD command adds a suggestion string to an auto-complete suggestion dictionary with a specified score.
 * </p>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SugAddArgs<K, V> {

    private boolean incr;

    private V payload;

    /**
     * Builder entry points for {@link SugAddArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SugAddArgs} setting {@literal INCR}.
         *
         * @return new {@link SugAddArgs} with {@literal INCR} set.
         * @see SugAddArgs#incr()
         */
        public static <K, V> SugAddArgs<K, V> incr() {
            return new SugAddArgs<K, V>().incr();
        }

        /**
         * Creates new {@link SugAddArgs} setting {@literal PAYLOAD}.
         *
         * @param payload the payload to save with the suggestion.
         * @return new {@link SugAddArgs} with {@literal PAYLOAD} set.
         * @see SugAddArgs#payload(Object)
         */
        public static <K, V> SugAddArgs<K, V> payload(V payload) {
            return new SugAddArgs<K, V>().payload(payload);
        }

    }

    /**
     * Increment the existing entry of the suggestion by the given score, instead of replacing the score. This is useful for
     * updating the dictionary based on user queries in real time.
     *
     * @return {@code this} {@link SugAddArgs}.
     */
    public SugAddArgs<K, V> incr() {
        this.incr = true;
        return this;
    }

    /**
     * Save an extra payload with the suggestion, that can be fetched by adding the WITHPAYLOADS argument to FT.SUGGET.
     *
     * @param payload the payload to save with the suggestion.
     * @return {@code this} {@link SugAddArgs}.
     */
    public SugAddArgs<K, V> payload(V payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Builds the arguments and appends them to the {@link CommandArgs}.
     *
     * @param args the command arguments to append to.
     */
    public void build(CommandArgs<K, V> args) {
        if (incr) {
            args.add("INCR");
        }

        if (payload != null) {
            args.add("PAYLOAD").addValue(payload);
        }
    }

}
