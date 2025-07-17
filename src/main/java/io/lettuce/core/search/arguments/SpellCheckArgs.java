/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.search.arguments;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/ft.spellcheck/">FT.SPELLCHECK</a> command.
 * Static import methods are available.
 * <p>
 * {@link SpellCheckArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SpellCheckArgs<K, V> {

    private Long distance;

    private Long dialect;

    private final List<TermsClause<K, V>> termsClauses = new ArrayList<>();

    /**
     * Builder entry points for {@link SpellCheckArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SpellCheckArgs} setting {@literal DISTANCE}.
         *
         * @return new {@link SpellCheckArgs} with {@literal DISTANCE} set.
         * @see SpellCheckArgs#distance(long)
         */
        public static <K, V> SpellCheckArgs<K, V> distance(long distance) {
            return new SpellCheckArgs<K, V>().distance(distance);
        }

        /**
         * Creates new {@link SpellCheckArgs} setting {@literal DIALECT}.
         *
         * @return new {@link SpellCheckArgs} with {@literal DIALECT} set.
         * @see SpellCheckArgs#dialect(long)
         */
        public static <K, V> SpellCheckArgs<K, V> dialect(long dialect) {
            return new SpellCheckArgs<K, V>().dialect(dialect);
        }

        /**
         * Creates new {@link SpellCheckArgs} setting {@literal TERMS INCLUDE}.
         *
         * @return new {@link SpellCheckArgs} with {@literal TERMS INCLUDE} set.
         * @see SpellCheckArgs#termsInclude(Object, Object[])
         */
        @SafeVarargs
        public static <K, V> SpellCheckArgs<K, V> termsInclude(K dictionary, V... terms) {
            return new SpellCheckArgs<K, V>().termsInclude(dictionary, terms);
        }

        /**
         * Creates new {@link SpellCheckArgs} setting {@literal TERMS EXCLUDE}.
         *
         * @return new {@link SpellCheckArgs} with {@literal TERMS EXCLUDE} set.
         * @see SpellCheckArgs#termsExclude(Object, Object[])
         */
        @SafeVarargs
        public static <K, V> SpellCheckArgs<K, V> termsExclude(K dictionary, V... terms) {
            return new SpellCheckArgs<K, V>().termsExclude(dictionary, terms);
        }

    }

    /**
     * Set maximum Levenshtein distance for spelling suggestions (default: 1, max: 4).
     *
     * @param distance the maximum distance.
     * @return {@code this} {@link SpellCheckArgs}.
     */
    public SpellCheckArgs<K, V> distance(long distance) {
        this.distance = distance;
        return this;
    }

    /**
     * Set the dialect version under which to execute the query.
     *
     * @param dialect the dialect version.
     * @return {@code this} {@link SpellCheckArgs}.
     */
    public SpellCheckArgs<K, V> dialect(long dialect) {
        this.dialect = dialect;
        return this;
    }

    /**
     * Include terms from a custom dictionary as potential spelling suggestions.
     *
     * @param dictionary the dictionary name.
     * @param terms optional terms to include from the dictionary.
     * @return {@code this} {@link SpellCheckArgs}.
     */
    @SafeVarargs
    public final SpellCheckArgs<K, V> termsInclude(K dictionary, V... terms) {
        this.termsClauses.add(new TermsClause<>(TermsClause.Type.INCLUDE, dictionary, terms));
        return this;
    }

    /**
     * Exclude terms from a custom dictionary from spelling suggestions.
     *
     * @param dictionary the dictionary name.
     * @param terms optional terms to exclude from the dictionary.
     * @return {@code this} {@link SpellCheckArgs}.
     */
    @SafeVarargs
    public final SpellCheckArgs<K, V> termsExclude(K dictionary, V... terms) {
        this.termsClauses.add(new TermsClause<>(TermsClause.Type.EXCLUDE, dictionary, terms));
        return this;
    }

    /**
     * Builds the arguments and appends them to the {@link CommandArgs}.
     *
     * @param args the command arguments to append to.
     */
    public void build(CommandArgs<K, V> args) {
        if (distance != null) {
            args.add("DISTANCE").add(distance);
        }

        for (TermsClause<K, V> clause : termsClauses) {
            clause.build(args);
        }

        if (dialect != null) {
            args.add("DIALECT").add(dialect);
        }
    }

    /**
     * Represents a TERMS clause (INCLUDE or EXCLUDE).
     */
    private static class TermsClause<K, V> {

        enum Type {
            INCLUDE, EXCLUDE
        }

        private final Type type;

        private final K dictionary;

        private final V[] terms;

        @SafeVarargs
        TermsClause(Type type, K dictionary, V... terms) {
            this.type = type;
            this.dictionary = dictionary;
            this.terms = terms;
        }

        void build(CommandArgs<K, V> args) {
            args.add("TERMS").add(type.name()).addKey(dictionary);
            if (terms != null && terms.length > 0) {
                for (V term : terms) {
                    args.addValue(term);
                }
            }
        }

    }

}
