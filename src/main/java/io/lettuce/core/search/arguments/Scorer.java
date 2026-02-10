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
 * Scoring configuration for text search. Specifies the scoring algorithm.
 * <p>
 * Note: Parameter support will be added in a future release when the Redis server supports it.
 * </p>
 *
 * <h3>Example:</h3>
 *
 * <pre>
 * {@code
 * Scorer.of(ScoringFunction.BM25)
 * // Output: SCORER BM25
 *
 * // Using factory methods
 * Scorer.bm25()
 * Scorer.tfidf()
 * }
 * </pre>
 *
 * @author Aleksandar Todorov
 * @since 7.2
 * @see ScoringFunction
 */
@Experimental
public class Scorer {

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
     * Create a BM25 scorer.
     *
     * @return a new {@link Scorer} instance with BM25 algorithm
     */
    public static Scorer bm25() {
        return new Scorer(ScoringFunction.BM25);
    }

    /**
     * Create a TFIDF scorer.
     *
     * @return a new {@link Scorer} instance with TFIDF algorithm
     */
    public static Scorer tfidf() {
        return new Scorer(ScoringFunction.TF_IDF);
    }

    /**
     * Create a TFIDF with document normalization scorer.
     *
     * @return a new {@link Scorer} instance with TFIDF.DOCNORM algorithm
     */
    public static Scorer tfidfNormalized() {
        return new Scorer(ScoringFunction.TF_IDF_NORMALIZED);
    }

    /**
     * Create a DISMAX scorer.
     *
     * @return a new {@link Scorer} instance with DISMAX algorithm
     */
    public static Scorer dismax() {
        return new Scorer(ScoringFunction.DIS_MAX);
    }

    /**
     * Create a DOCSCORE scorer.
     *
     * @return a new {@link Scorer} instance with DOCSCORE algorithm
     */
    public static Scorer docscore() {
        return new Scorer(ScoringFunction.DOCUMENT_SCORE);
    }

    /**
     * Create a HAMMING scorer.
     *
     * @return a new {@link Scorer} instance with HAMMING algorithm
     */
    public static Scorer hamming() {
        return new Scorer(ScoringFunction.HAMMING_DISTANCE);
    }

    /**
     * Get the scoring algorithm.
     *
     * @return the scoring algorithm
     */
    public ScoringFunction getAlgorithm() {
        return algorithm;
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
