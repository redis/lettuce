/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import java.util.Optional;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Arguments for the SEARCH clause in FT.HYBRID command. Configures text search query, scoring function, and score aliasing.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Aleksandar Todorov
 * @since 7.2
 * @see ScoringFunction
 * @see Scorer
 */
@Experimental
public class HybridSearchArgs<K, V> {

    private final V query;

    private final Scorer scorer;

    private final K scoreAlias;

    private HybridSearchArgs(Builder<K, V> builder) {
        this.query = builder.query;
        this.scorer = builder.scorer;
        this.scoreAlias = builder.scoreAlias;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public V getQuery() {
        return query;
    }

    public Optional<Scorer> getScorer() {
        return Optional.ofNullable(scorer);
    }

    public Optional<K> getScoreAlias() {
        return Optional.ofNullable(scoreAlias);
    }

    public static class Builder<K, V> {

        private V query;

        private Scorer scorer;

        private K scoreAlias;

        /**
         * Set the text search query.
         *
         * @param query the search query
         * @return this builder
         */
        public Builder<K, V> query(V query) {
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
        public Builder<K, V> scorer(Scorer scorer) {
            LettuceAssert.notNull(scorer, "Scorer must not be null");
            this.scorer = scorer;
            return this;
        }

        /**
         * Set an alias for the text search score field.
         *
         * @param alias the field name to use for the search score
         * @return this builder
         */
        public Builder<K, V> scoreAlias(K alias) {
            LettuceAssert.notNull(alias, "Score alias must not be null");
            this.scoreAlias = alias;
            return this;
        }

        /**
         * Build the {@link HybridSearchArgs} instance.
         *
         * @return the configured arguments
         */
        public HybridSearchArgs<K, V> build() {
            LettuceAssert.notNull(query, "Query must not be null");
            return new HybridSearchArgs<>(this);
        }

    }

    /**
     * Build the SEARCH clause arguments.
     *
     * @param args the {@link CommandArgs} to append to
     */
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.SEARCH);
        args.addValue(query);

        // SCORER inside SEARCH
        if (scorer != null) {
            scorer.build(args);
        }

        // YIELD_SCORE_AS for SEARCH
        if (scoreAlias != null) {
            args.add(CommandKeyword.YIELD_SCORE_AS);
            args.addKey(scoreAlias);
        }
    }

    /**
     * Scoring configuration for text search. Specifies the scoring algorithm.
     * <p>
     * Note: Parameter support will be added in a future release when the Redis server supports it.
     * </p>
     *
     * <h3>Example:</h3>
     *
     * <pre>
     * Scorer.of(ScoringFunction.BM25)
     * // Output: SCORER BM25
     * </pre>
     *
     * @author Aleksandar Todorov
     * @since 7.2
     * @see ScoringFunction
     */
    public static class Scorer {

        private final ScoringFunction algorithm;

        private Scorer(ScoringFunction algorithm) {
            this.algorithm = algorithm;
        }

        /**
         * Create a scorer with the specified algorithm.
         *
         * @param algorithm the scoring algorithm
         * @return a new {@link Scorer} instance
         */
        public static Scorer of(ScoringFunction algorithm) {
            LettuceAssert.notNull(algorithm, "Algorithm must not be null");
            return new Scorer(algorithm);
        }

        /**
         * Build the SCORER arguments.
         *
         * @param args the {@link CommandArgs} to append to
         * @param <K> key type
         * @param <V> value type
         */
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.SCORER);
            args.add(algorithm.toString());
        }

    }

}
