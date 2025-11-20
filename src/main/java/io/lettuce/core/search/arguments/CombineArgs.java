/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Arguments for the COMBINE clause in FT.HYBRID command. Defines how text search scores and vector similarity scores are
 * combined into a final ranking score using RRF (Reciprocal Rank Fusion) or LINEAR strategies.
 *
 * @param <K> Key type
 * @author Aleksandar Todorov
 * @since 7.2
 * @see <a href="https://redis.io/docs/latest/commands/ft.hybrid/">FT.HYBRID</a>
 */
public class CombineArgs<K> {

    private final CombineMethod method;

    private final Integer window;

    private final Double constant;

    private final Double alpha;

    private final Double beta;

    private K scoreAlias;

    private CombineArgs(CombineMethod method, Integer window, Double constant, Double alpha, Double beta) {
        this.method = method;
        this.window = window;
        this.constant = constant;
        this.alpha = alpha;
        this.beta = beta;
    }

    private enum CombineMethod {
        RRF, LINEAR
    }

    /**
     * Create CombineArgs with Reciprocal Rank Fusion (RRF) combiner with default parameters.
     * <p>
     * RRF is the default combination method. It combines rankings from text and vector search using reciprocal rank fusion with
     * default WINDOW=20 and CONSTANT=60.
     * </p>
     * <p>
     * RRF formula: score = 1 / (CONSTANT + rank_in_window)
     * </p>
     *
     * @param <K> Key type
     * @return CombineArgs instance
     */
    public static <K> CombineArgs<K> rrf() {
        return new CombineArgs<>(CombineMethod.RRF, null, null, null, null);
    }

    /**
     * Create CombineArgs with Reciprocal Rank Fusion (RRF) combiner with custom window parameter.
     * <p>
     * RRF formula: score = 1 / (CONSTANT + rank_in_window)
     * </p>
     *
     * @param <K> Key type
     * @param window number of top results to consider from each ranking (default: 20)
     * @return CombineArgs instance
     */
    public static <K> CombineArgs<K> rrf(int window) {
        LettuceAssert.isTrue(window > 0, "Window must be positive");
        return new CombineArgs<>(CombineMethod.RRF, window, null, null, null);
    }

    /**
     * Create CombineArgs with Reciprocal Rank Fusion (RRF) combiner with custom parameters.
     * <p>
     * RRF formula: score = 1 / (CONSTANT + rank_in_window)
     * </p>
     *
     * @param <K> Key type
     * @param window number of top results to consider from each ranking (default: 20)
     * @param constant constant added to rank to prevent division by zero (default: 60)
     * @return CombineArgs instance
     */
    public static <K> CombineArgs<K> rrf(int window, double constant) {
        LettuceAssert.isTrue(window > 0, "Window must be positive");
        LettuceAssert.isTrue(constant > 0, "Constant must be positive");
        return new CombineArgs<>(CombineMethod.RRF, window, constant, null, null);
    }

    /**
     * Create CombineArgs with Linear combiner using default weights.
     * <p>
     * Combines text and vector scores using weighted average with default ALPHA=0.3 and BETA=0.7.
     * </p>
     * <p>
     * Formula: combined_score = ALPHA * text_score + BETA * vector_score
     * </p>
     *
     * @param <K> Key type
     * @return CombineArgs instance with defaults (alpha=0.3, beta=0.7)
     */
    public static <K> CombineArgs<K> linear() {
        return new CombineArgs<>(CombineMethod.LINEAR, null, null, null, null);
    }

    /**
     * Create CombineArgs with Linear combiner using custom alpha weight.
     * <p>
     * Formula: combined_score = alpha * text_score + beta * vector_score
     * </p>
     * <p>
     * Note: alpha + beta should typically equal 1.0 for normalized scores, but this is not enforced.
     * </p>
     *
     * @param <K> Key type
     * @param alpha weight for text search score (default: 0.3)
     * @return CombineArgs instance
     */
    public static <K> CombineArgs<K> linear(double alpha) {
        LettuceAssert.isTrue(alpha >= 0, "Alpha must be non-negative");
        return new CombineArgs<>(CombineMethod.LINEAR, null, null, alpha, null);
    }

    /**
     * Create CombineArgs with Linear combiner using custom weights.
     * <p>
     * Formula: combined_score = alpha * text_score + beta * vector_score
     * </p>
     * <p>
     * Note: alpha + beta should typically equal 1.0 for normalized scores, but this is not enforced.
     * </p>
     *
     * @param <K> Key type
     * @param alpha weight for text search score (default: 0.3)
     * @param beta weight for vector search score (default: 0.7)
     * @return CombineArgs instance
     */
    public static <K> CombineArgs<K> linear(double alpha, double beta) {
        LettuceAssert.isTrue(alpha >= 0, "Alpha must be non-negative");
        LettuceAssert.isTrue(beta >= 0, "Beta must be non-negative");
        return new CombineArgs<>(CombineMethod.LINEAR, null, null, alpha, beta);
    }

    /**
     * Set an alias for the combined score field.
     *
     * @param alias the field name to use for the combined score
     * @return this instance
     */
    public CombineArgs<K> scoreAlias(K alias) {
        LettuceAssert.notNull(alias, "Score alias must not be null");
        this.scoreAlias = alias;
        return this;
    }

    /**
     * Build the COMBINE clause arguments.
     *
     * @param args the {@link CommandArgs} to append to
     * @param <V> value type
     */
    public <V> void build(CommandArgs<K, V> args) {
        if (method == CombineMethod.RRF) {
            args.add("RRF");
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
                args.add("WINDOW");
                args.add(window);
            }
            if (constant != null) {
                args.add("CONSTANT");
                args.add(constant);
            }
        } else if (method == CombineMethod.LINEAR) {
            args.add("LINEAR");
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
                args.add("ALPHA");
                args.add(alpha);
            }
            if (beta != null) {
                args.add("BETA");
                args.add(beta);
            }
        }

        // YIELD_SCORE_AS for COMBINE
        if (scoreAlias != null) {
            args.add("YIELD_SCORE_AS");
            args.addKey(scoreAlias);
        }
    }

}
