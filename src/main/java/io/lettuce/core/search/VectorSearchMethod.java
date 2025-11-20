/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Vector search methods for FT.HYBRID VSIM clause.
 * <p>
 * Defines how vector similarity search is performed - either by finding K nearest neighbors (KNN) or by finding all vectors
 * within a certain distance range.
 * </p>
 *
 * @author Aleksandar Todorov
 * @since 6.8
 * @see <a href="https://redis.io/docs/latest/commands/ft.hybrid/">FT.HYBRID</a>
 */
public abstract class VectorSearchMethod {

    /**
     * K-Nearest Neighbors search method.
     * <p>
     * Finds the K most similar vectors to the query vector. This is the default and most common vector search method.
     * </p>
     *
     * @param k number of nearest neighbors to return
     * @return KNN search method
     */
    public static VectorSearchMethod knn(int k) {
        LettuceAssert.isTrue(k > 0, "K must be positive");
        return new KnnMethod(k, null);
    }

    /**
     * K-Nearest Neighbors search with EF_RUNTIME parameter.
     * <p>
     * The EF_RUNTIME parameter controls the size of the dynamic candidate list during HNSW search. Higher values improve recall
     * but increase search time.
     * </p>
     *
     * @param k number of nearest neighbors to return
     * @param efRuntime size of the dynamic candidate list for HNSW algorithm
     * @return KNN search method with EF_RUNTIME
     */
    public static VectorSearchMethod knn(int k, int efRuntime) {
        LettuceAssert.isTrue(k > 0, "K must be positive");
        LettuceAssert.isTrue(efRuntime > 0, "EF_RUNTIME must be positive");
        return new KnnMethod(k, efRuntime);
    }

    /**
     * Range-based vector search method.
     * <p>
     * Finds all vectors within a specified distance radius from the query vector.
     * </p>
     *
     * @param radius maximum distance from query vector
     * @return Range search method
     */
    public static VectorSearchMethod range(double radius) {
        LettuceAssert.isTrue(radius > 0, "Radius must be positive");
        return new RangeMethod(radius, null);
    }

    /**
     * Range-based vector search with epsilon parameter.
     * <p>
     * The epsilon parameter provides a tolerance for the distance calculation.
     * </p>
     *
     * @param radius maximum distance from query vector
     * @param epsilon tolerance for distance calculation
     * @return Range search method with epsilon
     */
    public static VectorSearchMethod range(double radius, double epsilon) {
        LettuceAssert.isTrue(radius > 0, "Radius must be positive");
        LettuceAssert.isTrue(epsilon > 0, "Epsilon must be positive");
        return new RangeMethod(radius, epsilon);
    }

    /**
     * Build the method arguments into the command.
     *
     * @param args command arguments
     * @param <K> key type
     * @param <V> value type
     */
    public abstract <K, V> void build(CommandArgs<K, V> args);

    /**
     * KNN (K-Nearest Neighbors) search method implementation.
     */
    static class KnnMethod extends VectorSearchMethod {

        private final int k;

        private final Integer efRuntime;

        KnnMethod(int k, Integer efRuntime) {
            this.k = k;
            this.efRuntime = efRuntime;
        }

        @Override
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.KNN);
            // Count of total items: K + value, optionally EF_RUNTIME + value
            int itemCount = efRuntime != null ? 4 : 2;
            args.add(itemCount);
            args.add("K");
            args.add(k);
            if (efRuntime != null) {
                args.add("EF_RUNTIME");
                args.add(efRuntime);
            }
        }

    }

    /**
     * Range-based search method implementation.
     */
    static class RangeMethod extends VectorSearchMethod {

        private final double radius;

        private final Double epsilon;

        RangeMethod(double radius, Double epsilon) {
            this.radius = radius;
            this.epsilon = epsilon;
        }

        @Override
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add("RANGE");
            // Count of key-value pairs: 1 for RADIUS, +1 if EPSILON is present
            int pairCount = epsilon != null ? 2 : 1;
            args.add(pairCount);
            args.add("RADIUS");
            args.add(radius);
            if (epsilon != null) {
                args.add("EPSILON");
                args.add(epsilon);
            }
        }

    }

}
