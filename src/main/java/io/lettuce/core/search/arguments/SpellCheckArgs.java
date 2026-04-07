/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/ft.spellcheck/">FT.SPELLCHECK</a> command.
 * Static import methods are available.
 * <p>
 * {@link SpellCheckArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SpellCheckArgs {

    private Long distance;

    private Long dialect;

    private final List<TermsClause> termsClauses = new ArrayList<>();

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
        public static SpellCheckArgs distance(long distance) {
            return new SpellCheckArgs().distance(distance);
        }

        /**
         * Creates new {@link SpellCheckArgs} setting {@literal DIALECT}.
         *
         * @return new {@link SpellCheckArgs} with {@literal DIALECT} set.
         * @see SpellCheckArgs#dialect(long)
         */
        public static SpellCheckArgs dialect(long dialect) {
            return new SpellCheckArgs().dialect(dialect);
        }

        /**
         * Creates new {@link SpellCheckArgs} setting {@literal TERMS INCLUDE}.
         *
         * @return new {@link SpellCheckArgs} with {@literal TERMS INCLUDE} set.
         * @see SpellCheckArgs#termsInclude(String, String[])
         */
        public static SpellCheckArgs termsInclude(String dictionary, String... terms) {
            return new SpellCheckArgs().termsInclude(dictionary, terms);
        }

        /**
         * Creates new {@link SpellCheckArgs} setting {@literal TERMS EXCLUDE}.
         *
         * @return new {@link SpellCheckArgs} with {@literal TERMS EXCLUDE} set.
         * @see SpellCheckArgs#termsExclude(String, String[])
         */
        public static SpellCheckArgs termsExclude(String dictionary, String... terms) {
            return new SpellCheckArgs().termsExclude(dictionary, terms);
        }

    }

    /**
     * Set maximum Levenshtein distance for spelling suggestions (default: 1, max: 4).
     *
     * @param distance the maximum distance.
     * @return {@code this} {@link SpellCheckArgs}.
     */
    public SpellCheckArgs distance(long distance) {
        this.distance = distance;
        return this;
    }

    /**
     * Set the dialect version under which to execute the query.
     *
     * @param dialect the dialect version.
     * @return {@code this} {@link SpellCheckArgs}.
     */
    public SpellCheckArgs dialect(long dialect) {
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
    public SpellCheckArgs termsInclude(String dictionary, String... terms) {
        this.termsClauses.add(new TermsClause(TermsClause.Type.INCLUDE, dictionary, terms));
        return this;
    }

    /**
     * Exclude terms from a custom dictionary from spelling suggestions.
     *
     * @param dictionary the dictionary name.
     * @param terms optional terms to exclude from the dictionary.
     * @return {@code this} {@link SpellCheckArgs}.
     */
    public SpellCheckArgs termsExclude(String dictionary, String... terms) {
        this.termsClauses.add(new TermsClause(TermsClause.Type.EXCLUDE, dictionary, terms));
        return this;
    }

    /**
     * Builds the arguments and appends them to the {@link CommandArgs}.
     *
     * @param args the command arguments to append to.
     */
    public void build(CommandArgs<?, ?> args) {
        if (distance != null) {
            args.add(CommandKeyword.DISTANCE).add(distance);
        }

        for (TermsClause clause : termsClauses) {
            clause.build(args);
        }

        if (dialect != null) {
            args.add(CommandKeyword.DIALECT).add(dialect);
        }
    }

    /**
     * Represents a TERMS clause (INCLUDE or EXCLUDE).
     */
    private static class TermsClause {

        enum Type {
            INCLUDE, EXCLUDE
        }

        private final Type type;

        private final String dictionary;

        private final String[] terms;

        TermsClause(Type type, String dictionary, String... terms) {
            this.type = type;
            this.dictionary = dictionary;
            this.terms = terms;
        }

        void build(CommandArgs<?, ?> args) {
            args.add(CommandKeyword.TERMS).add(type.name()).add(dictionary);
            if (terms != null) {
                for (String term : terms) {
                    args.add(term);
                }
            }
        }

    }

}
