/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.search.VectorSearchMethod;

/**
 * Arguments for the VSIM clause in FT.HYBRID command. Configures vector similarity search including field, vector data, search
 * method (KNN or RANGE), filters, and score aliasing.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Aleksandar Todorov
 * @since 7.2
 * @see VectorSearchMethod
 */
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
         * Set the query vector data.
         *
         * @param vectorData the vector data (byte array or other format)
         * @return this builder
         */
        public Builder<K, V> vector(V vectorData) {
            LettuceAssert.notNull(vectorData, "Vector data must not be null");
            this.vectorData = vectorData;
            return this;
        }

        /**
         * Set the query vector data as byte array.
         * <p>
         * This overload allows passing binary vector data (e.g., float arrays encoded as bytes) when using String codec.
         * </p>
         *
         * @param vectorData the vector data as byte array
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public Builder<K, V> vector(byte[] vectorData) {
            LettuceAssert.notNull(vectorData, "Vector data must not be null");
            this.vectorData = (V) vectorData;
            return this;
        }

        /**
         * Set the vector search method (KNN or RANGE).
         *
         * @param method the search method
         * @return this builder
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

        // Handle byte[] vectors specially (they're always binary data)
        if (vectorData instanceof byte[]) {
            args.add((byte[]) vectorData);
        } else {
            args.addValue((V) vectorData);
        }

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
            args.add("YIELD_SCORE_AS");
            args.addKey(scoreAlias);
        }
    }

}
