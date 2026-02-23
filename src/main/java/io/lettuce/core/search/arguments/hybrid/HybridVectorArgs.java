/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments.hybrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Arguments for the VSIM clause in FT.HYBRID command. Configures vector similarity search including field, vector data, search
 * method (KNN or RANGE), filters, and score aliasing.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Aleksandar Todorov
 * @since 7.2
 * @see VectorSearchMethod
 * @see Knn
 * @see Range
 */
@Experimental
public class HybridVectorArgs<K, V> {

    private final K fieldName;

    private final V vectorData;

    private final VectorSearchMethod method;

    private final List<String> filters;

    private final K scoreAlias;

    private HybridVectorArgs(Builder<K, V> builder) {
        this.fieldName = builder.fieldName;
        this.vectorData = builder.vectorData;
        this.method = builder.method;
        this.filters = new ArrayList<>(builder.filters);
        this.scoreAlias = builder.scoreAlias;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public K getFieldName() {
        return fieldName;
    }

    public V getVectorData() {
        return vectorData;
    }

    public Optional<VectorSearchMethod> getMethod() {
        return Optional.ofNullable(method);
    }

    public List<String> getFilters() {
        return filters;
    }

    public Optional<K> getScoreAlias() {
        return Optional.ofNullable(scoreAlias);
    }

    public static class Builder<K, V> {

        private K fieldName;

        private V vectorData;

        private VectorSearchMethod method;

        private final List<String> filters = new ArrayList<>();

        private K scoreAlias;

        /**
         * Set the vector field name.
         *
         * @param fieldName the field name (typically prefixed with '@')
         * @return this builder
         */
        public Builder<K, V> field(K fieldName) {
            LettuceAssert.notNull(fieldName, "Field name must not be null");
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Set the query vector parameter reference.
         * <p>
         * This should be a parameter name (e.g., "$vec") that references a vector passed via
         * {@link HybridArgs.Builder#param(Object, byte[])}. The actual binary vector data must be provided in PARAMS.
         * </p>
         *
         * @param vectorData the parameter reference (e.g., "$vec")
         * @return this builder
         */
        public Builder<K, V> vector(V vectorData) {
            LettuceAssert.notNull(vectorData, "Vector data must not be null");
            this.vectorData = vectorData;
            return this;
        }

        /**
         * Set the vector search method (KNN or RANGE).
         *
         * @param method the search method
         * @return this builder
         * @see Knn
         * @see Range
         */
        public Builder<K, V> method(VectorSearchMethod method) {
            LettuceAssert.notNull(method, "Vector search method must not be null");
            this.method = method;
            return this;
        }

        /**
         * Add a FILTER expression for pre-filtering documents before vector scoring.
         *
         * @param expression the filter expression (e.g., "@brand:{apple|samsung}")
         * @return this builder
         */
        public Builder<K, V> filter(String expression) {
            LettuceAssert.notNull(expression, "Filter expression must not be null");
            this.filters.add(expression);
            return this;
        }

        /**
         * Set an alias for the vector distance field (normalized vector score).
         *
         * @param alias the field name to use for the normalized vector distance
         * @return this builder
         */
        public Builder<K, V> scoreAlias(K alias) {
            LettuceAssert.notNull(alias, "Score alias must not be null");
            this.scoreAlias = alias;
            return this;
        }

        /**
         * Build the {@link HybridVectorArgs} instance.
         *
         * @return the configured arguments
         */
        public HybridVectorArgs<K, V> build() {
            LettuceAssert.notNull(fieldName, "Field name must not be null");
            LettuceAssert.notNull(vectorData, "Vector data must not be null");
            return new HybridVectorArgs<>(this);
        }

    }

    /**
     * Build the VSIM clause arguments.
     *
     * @param args the {@link CommandArgs} to append to
     */
    public void build(CommandArgs<K, V> args) {
        args.add(CommandType.VSIM);
        args.addKey(fieldName);
        args.addValue(vectorData);

        // Vector search method (KNN or RANGE) - optional
        if (method != null) {
            method.build(args);
        }

        // FILTER inside VSIM
        for (String filter : filters) {
            args.add(CommandKeyword.FILTER);
            args.add(filter);
        }

        // YIELD_SCORE_AS for VSIM (normalized vector distance)
        if (scoreAlias != null) {
            args.add(CommandKeyword.YIELD_SCORE_AS);
            args.addKey(scoreAlias);
        }
    }

    /**
     * Interface for vector search methods used in VSIM clause.
     * <p>
     * Defines how vector similarity search is performed - either by finding K nearest neighbors (KNN) or by finding all vectors
     * within a certain distance range.
     * </p>
     */
    public interface VectorSearchMethod {

        /**
         * Build the method arguments into the command.
         *
         * @param args command arguments
         * @param <K> key type
         * @param <V> value type
         */
        <K, V> void build(CommandArgs<K, V> args);

    }

    /**
     * KNN (K-Nearest Neighbors) search method implementation.
     * <p>
     * Finds the K most similar vectors to the query vector. This is the default and most common vector search method.
     * </p>
     */
    public static class Knn implements VectorSearchMethod {

        private final int k;

        private Integer efRuntime;

        /**
         * Creates a new KNN search method.
         *
         * @param k number of nearest neighbors to return
         */
        private Knn(int k) {
            LettuceAssert.isTrue(k > 0, "K must be positive");
            this.k = k;
        }

        /**
         * Static factory method to create a KNN search method.
         *
         * @param k number of nearest neighbors to return
         * @return new KNN search method
         */
        public static Knn of(int k) {
            return new Knn(k);
        }

        /**
         * Set the EF_RUNTIME parameter for HNSW search.
         * <p>
         * The EF_RUNTIME parameter controls the size of the dynamic candidate list during HNSW search. Higher values improve
         * recall but increase search time.
         * </p>
         *
         * @param efRuntime size of the dynamic candidate list for HNSW algorithm
         * @return this KNN search method
         */
        public Knn efRuntime(int efRuntime) {
            LettuceAssert.isTrue(efRuntime > 0, "EF_RUNTIME must be positive");
            this.efRuntime = efRuntime;
            return this;
        }

        @Override
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.KNN);
            // Count of total items: K + value, optionally EF_RUNTIME + value
            int itemCount = efRuntime != null ? 4 : 2;
            args.add(itemCount);
            args.add(CommandKeyword.K);
            args.add(k);
            if (efRuntime != null) {
                args.add(CommandKeyword.EF_RUNTIME);
                args.add(efRuntime);
            }
        }

    }

    /**
     * Range-based search method implementation.
     * <p>
     * Finds all vectors within a specified distance radius from the query vector.
     * </p>
     */
    public static class Range implements VectorSearchMethod {

        private final double radius;

        private Double epsilon;

        /**
         * Creates a new Range search method.
         *
         * @param radius maximum distance from query vector
         */
        private Range(double radius) {
            LettuceAssert.isTrue(radius > 0, "Radius must be positive");
            this.radius = radius;
        }

        /**
         * Static factory method to create a Range search method.
         *
         * @param radius maximum distance from query vector
         * @return new Range search method
         */
        public static Range of(double radius) {
            return new Range(radius);
        }

        /**
         * Set the epsilon parameter for distance calculation tolerance.
         * <p>
         * The epsilon parameter provides a tolerance for the distance calculation.
         * </p>
         *
         * @param epsilon tolerance for distance calculation
         * @return this Range search method
         */
        public Range epsilon(double epsilon) {
            LettuceAssert.isTrue(epsilon > 0, "Epsilon must be positive");
            this.epsilon = epsilon;
            return this;
        }

        @Override
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.RANGE);
            // Count of key-value pairs: 1 for RADIUS, +1 if EPSILON is present
            int pairCount = epsilon != null ? 4 : 2;
            args.add(pairCount);
            args.add(CommandKeyword.RADIUS);
            args.add(radius);
            if (epsilon != null) {
                args.add(CommandKeyword.EPSILON);
                args.add(epsilon);
            }
        }

    }

}
