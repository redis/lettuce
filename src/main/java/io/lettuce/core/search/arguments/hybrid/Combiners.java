/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments.hybrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Factory class for creating {@link Combiner} instances.
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * {@code
 * // RRF with default parameters
 * Combiners.rrf()
 *
 * // RRF with custom window and constant
 * Combiners.rrf().window(10).constant(60)
 *
 * // Linear combination with weights
 * Combiners.linear().alpha(0.7).beta(0.3)
 *
 * // With score alias
 * Combiners.rrf().as("combined_score")
 * }
 * </pre>
 *
 * @author Aleksandar Todorov
 * @since 7.2
 * @see Combiner
 */
@Experimental
public final class Combiners {

    private Combiners() {
    }

    /**
     * Create an RRF (Reciprocal Rank Fusion) combiner.
     *
     * @param <K> Key type
     * @return a new RRF combiner
     */
    public static <K> RRF<K> rrf() {
        return new RRF<>();
    }

    /**
     * Create a Linear combination combiner.
     *
     * @param <K> Key type
     * @return a new Linear combiner
     */
    public static <K> Linear<K> linear() {
        return new Linear<>();
    }

    /**
     * RRF (Reciprocal Rank Fusion) combiner.
     * <p>
     * Combines rankings from text and vector search using reciprocal rank fusion. RRF formula: score = 1 / (CONSTANT +
     * rank_in_window)
     * </p>
     *
     * @param <K> Key type
     */
    public static class RRF<K> extends Combiner<K> {

        private Integer window;

        private Double constant;

        RRF() {
            super("RRF");
        }

        /**
         * Set the WINDOW parameter - number of top results to consider from each ranking.
         *
         * @param window number of top results
         * @return this RRF instance
         */
        public RRF<K> window(int window) {
            LettuceAssert.isTrue(window > 0, "Window must be positive");
            this.window = window;
            return this;
        }

        /**
         * Set the CONSTANT parameter - constant added to rank to prevent division by zero.
         *
         * @param constant constant value (typically 60)
         * @return this RRF instance
         */
        public RRF<K> constant(double constant) {
            LettuceAssert.isTrue(constant > 0, "Constant must be positive");
            this.constant = constant;
            return this;
        }

        @Override
        protected List<Object> getOwnArgs() {
            if (window == null && constant == null) {
                return Collections.emptyList();
            }
            List<Object> args = new ArrayList<>();
            if (window != null) {
                args.add(CommandKeyword.WINDOW);
                args.add(window);
            }
            if (constant != null) {
                args.add(CommandKeyword.CONSTANT);
                args.add(constant);
            }
            return args;
        }

    }

    /**
     * Linear combination combiner.
     * <p>
     * Combines text and vector scores using weighted average. Formula: combined_score = ALPHA * text_score + BETA *
     * vector_score
     * </p>
     *
     * @param <K> Key type
     */
    public static class Linear<K> extends Combiner<K> {

        private Integer window;

        private Double alpha;

        private Double beta;

        Linear() {
            super("LINEAR");
        }

        /**
         * Set the WINDOW parameter - number of top results to consider from each ranking.
         *
         * @param window number of top results
         * @return this Linear instance
         */
        public Linear<K> window(int window) {
            LettuceAssert.isTrue(window > 0, "Window must be positive");
            this.window = window;
            return this;
        }

        /**
         * Set the ALPHA parameter - weight for text search score.
         *
         * @param alpha weight for text score (0.0 to 1.0)
         * @return this Linear instance
         */
        public Linear<K> alpha(double alpha) {
            LettuceAssert.isTrue(alpha >= 0, "Alpha must be non-negative");
            this.alpha = alpha;
            return this;
        }

        /**
         * Set the BETA parameter - weight for vector search score.
         *
         * @param beta weight for vector score (0.0 to 1.0)
         * @return this Linear instance
         */
        public Linear<K> beta(double beta) {
            LettuceAssert.isTrue(beta >= 0, "Beta must be non-negative");
            this.beta = beta;
            return this;
        }

        @Override
        protected List<Object> getOwnArgs() {
            List<Object> args = new ArrayList<>();
            if (alpha != null) {
                args.add(CommandKeyword.ALPHA);
                args.add(alpha);
            }
            if (beta != null) {
                args.add(CommandKeyword.BETA);
                args.add(beta);
            }
            if (window != null) {
                args.add(CommandKeyword.WINDOW);
                args.add(window);
            }
            return args;
        }

    }

}
