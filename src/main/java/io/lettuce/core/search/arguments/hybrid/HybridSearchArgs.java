/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments.hybrid;

import java.util.Optional;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.search.aggregateutils.Scorer;
import io.lettuce.core.search.arguments.ScoringFunction;

/**
 * Arguments for the SEARCH clause in FT.HYBRID command. Configures text search query, scoring function, and score aliasing.
 *
 * @author Aleksandar Todorov
 * @since 7.5
 * @see ScoringFunction
 * @see Scorer
 */
@Experimental
public class HybridSearchArgs {

    private final String query;

    private final Scorer scorer;

    private final String scoreAlias;

    private HybridSearchArgs(Builder builder) {
        this.query = builder.query;
        this.scorer = builder.scorer;
        this.scoreAlias = builder.scoreAlias;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getQuery() {
        return query;
    }

    public Optional<Scorer> getScorer() {
        return Optional.ofNullable(scorer);
    }

    public Optional<String> getScoreAlias() {
        return Optional.ofNullable(scoreAlias);
    }

    public static class Builder {

        private String query;

        private Scorer scorer;

        private String scoreAlias;

        /**
         * Set the text search query.
         *
         * @param query the search query
         * @return this builder
         */
        public Builder query(String query) {
            LettuceAssert.notNull(query, "Query must not be null");
            this.query = query;
            return this;
        }

        /**
         * Set the scoring algorithm with optional parameters.
         *
         * @param scorer the scorer to use
         * @return this builder
         */
        public Builder scorer(Scorer scorer) {
            LettuceAssert.notNull(scorer, "Scorer must not be null");
            this.scorer = scorer;
            return this;
        }

        /**
         * Set an alias for the text search score field (YIELD_SCORE_AS).
         *
         * @param alias the label to assign to the combined fusion score
         * @return this builder
         */
        public Builder scoreAlias(String alias) {
            LettuceAssert.notNull(alias, "Score alias must not be null");
            this.scoreAlias = alias;
            return this;
        }

        /**
         * Build the {@link HybridSearchArgs} instance.
         *
         * @return the configured arguments
         */
        public HybridSearchArgs build() {
            LettuceAssert.notNull(query, "Query must not be null");
            return new HybridSearchArgs(this);
        }

    }

    /**
     * Build the SEARCH clause arguments.
     *
     * @param args the {@link CommandArgs} to append to
     */
    public void build(CommandArgs<?, ?> args) {
        args.add(CommandKeyword.SEARCH);
        args.add(query);

        // SCORER inside SEARCH
        if (scorer != null) {
            scorer.build(args);
        }

        // YIELD_SCORE_AS for SEARCH
        if (scoreAlias != null) {
            args.add(CommandKeyword.YIELD_SCORE_AS);
            args.add(scoreAlias);
        }
    }

}
