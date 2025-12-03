/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Arguments for the COMBINE clause in FT.HYBRID command. Defines how text search scores and vector similarity scores are
 * combined into a final ranking score using RRF (Reciprocal Rank Fusion) or LINEAR strategies.
 *
 * @param <K> Key type
 * @author Aleksandar Todorov
 * @since 7.2
 * @see <a href="https://redis.io/docs/latest/commands/ft.hybrid/">FT.HYBRID</a>
 * @see RRF
 * @see Linear
 */
@Experimental
public class CombineArgs<K> {

    private final CombineMethod<K> method;

    private K scoreAlias;

    private CombineArgs(CombineMethod<K> method) {
        this.method = method;
    }

    /**
     * Create CombineArgs with the specified combine method.
     *
     * @param method the combine method (RRF or Linear)
     * @param <K> Key type
     * @return CombineArgs instance
     */
    public static <K> CombineArgs<K> of(CombineMethod<K> method) {
        return new CombineArgs<>(method);
    }

    /**
     * Set an alias for the combined score field using YIELD_SCORE_AS.
     *
     * @param alias the field name to use for the combined score
     * @return this instance
     */
    public CombineArgs<K> as(K alias) {
        LettuceAssert.notNull(alias, "Alias must not be null");
        this.scoreAlias = alias;
        return this;
    }

    /**
     * Interface for combine methods (RRF or LINEAR).
     *
     * @param <K> Key type
     */
    public interface CombineMethod<K> {

        /**
         * Build the combine method arguments.
         *
         * @param args the {@link CommandArgs} to append to
         * @param <V> value type
         */
        <V> void build(CommandArgs<K, V> args);

    }

    /**
     * Reciprocal Rank Fusion (RRF) combine method. Combines rankings from text and vector search using reciprocal rank fusion.
     * <p>
     * RRF formula: score = 1 / (CONSTANT + rank_in_window)
     * </p>
     * <p>
     * Default parameters: WINDOW=20, CONSTANT=60
     * </p>
     *
     * @param <K> Key type
     */
    public static class RRF<K> implements CombineMethod<K> {

        private Integer window;

        private Double constant;

        /**
         * Set the WINDOW parameter - number of top results to consider from each ranking.
         *
         * @param window number of top results (default: 20)
         * @return this instance
         */
        public RRF<K> window(int window) {
            LettuceAssert.isTrue(window > 0, "Window must be positive");
            this.window = window;
            return this;
        }

        /**
         * Set the CONSTANT parameter - constant added to rank to prevent division by zero.
         *
         * @param constant constant value (default: 60)
         * @return this instance
         */
        public RRF<K> constant(double constant) {
            LettuceAssert.isTrue(constant > 0, "Constant must be positive");
            this.constant = constant;
            return this;
        }

        @Override
        public <V> void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.RRF);
            // Count of total items (not pairs): WINDOW, value, CONSTANT, value = 4 items
            int itemCount = 0;
            if (window != null) {
                itemCount += 2; // WINDOW + value
            }
            if (constant != null) {
                itemCount += 2; // CONSTANT + value
            }

            // Always add count, even if 0
            args.add(itemCount);
            if (window != null) {
                args.add(CommandKeyword.WINDOW);
                args.add(window);
            }
            if (constant != null) {
                args.add(CommandKeyword.CONSTANT);
                args.add(constant);
            }
        }

    }

    /**
     * Linear combine method. Combines text and vector scores using weighted average.
     * <p>
     * Formula: combined_score = ALPHA * text_score + BETA * vector_score
     * </p>
     * <p>
     * Default parameters: ALPHA=0.3, BETA=0.7
     * </p>
     *
     * @param <K> Key type
     */
    public static class Linear<K> implements CombineMethod<K> {

        private Double alpha;

        private Double beta;

        /**
         * Set the ALPHA parameter - weight for text search score.
         *
         * @param alpha weight for text score (default: 0.3)
         * @return this instance
         */
        public Linear<K> alpha(double alpha) {
            LettuceAssert.isTrue(alpha >= 0, "Alpha must be non-negative");
            this.alpha = alpha;
            return this;
        }

        /**
         * Set the BETA parameter - weight for vector search score.
         *
         * @param beta weight for vector score (default: 0.7)
         * @return this instance
         */
        public Linear<K> beta(double beta) {
            LettuceAssert.isTrue(beta >= 0, "Beta must be non-negative");
            this.beta = beta;
            return this;
        }

        @Override
        public <V> void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.LINEAR);
            // Count of total items (not pairs): ALPHA, value, BETA, value = 4 items
            int itemCount = 0;
            if (alpha != null) {
                itemCount += 2; // ALPHA + value
            }
            if (beta != null) {
                itemCount += 2; // BETA + value
            }

            // Always add count, even if 0
            args.add(itemCount);
            if (alpha != null) {
                args.add(CommandKeyword.ALPHA);
                args.add(alpha);
            }
            if (beta != null) {
                args.add(CommandKeyword.BETA);
                args.add(beta);
            }
        }

    }

    /**
     * Build the COMBINE clause arguments.
     *
     * @param args the {@link CommandArgs} to append to
     * @param <V> value type
     */
    public <V> void build(CommandArgs<K, V> args) {
        method.build(args);

        // YIELD_SCORE_AS for COMBINE
        if (scoreAlias != null) {
            args.add(CommandKeyword.YIELD_SCORE_AS);
            args.addKey(scoreAlias);
        }
    }

}
