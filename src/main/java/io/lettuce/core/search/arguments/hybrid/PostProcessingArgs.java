/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments.hybrid;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.search.aggregateutils.Apply;
import io.lettuce.core.search.aggregateutils.Filter;
import io.lettuce.core.search.aggregateutils.GroupBy;
import io.lettuce.core.search.aggregateutils.Limit;
import io.lettuce.core.search.aggregateutils.PostProcessingOperation;
import io.lettuce.core.search.aggregateutils.SortBy;

import java.util.ArrayList;
import java.util.List;

/**
 * Argument list builder for FT.HYBRID and FT.AGGREGATE post-processing operations. Operations are applied in user-specified
 * order after the COMBINE clause.
 * <p>
 * The builder provides dedicated methods for each operation type with validation:
 * </p>
 * <ul>
 * <li>{@link Builder#groupBy(GroupBy)} - Only one GROUPBY allowed</li>
 * <li>{@link Builder#sortBy(SortBy)} - Only one SORTBY allowed</li>
 * <li>{@link Builder#limit(Limit)} - Only one LIMIT allowed</li>
 * <li>{@link Builder#apply(Apply)} - Multiple APPLY operations allowed</li>
 * <li>{@link Builder#filter(Filter)} - Only one FILTER allowed</li>
 * </ul>
 *
 * <h3>Basic Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     PostProcessingArgs<String, String> args = PostProcessingArgs.<String, String> builder().load("@price", "@category")
 *             .groupBy(GroupBy.of("@category").reduce(Reducers.count().as("total")))
 *             .apply(Apply.of("@price * 0.9", "discounted_price")).sortBy(SortBy.of("@discounted_price", SortDirection.DESC))
 *             .filter(Filter.of("@discounted_price > 100")).limit(Limit.of(0, 10)).build();
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see GroupBy
 * @see Apply
 * @see SortBy
 * @see Filter
 * @see Limit
 */
@Experimental
public class PostProcessingArgs<K, V> {

    private final List<K> loadFields = new ArrayList<>();

    private boolean loadAll = false;

    /**
     * Ordered list of pipeline operations (GROUPBY, SORTBY, APPLY, FILTER, LIMIT). These operations are applied in the order
     * specified by the user.
     */
    private final List<PostProcessingOperation<K, ?>> postProcessingOperations = new ArrayList<>();

    // Tracking flags for single-use operations
    private boolean hasGroupBy = false;

    private boolean hasSortBy = false;

    private boolean hasFilter = false;

    private boolean hasLimit = false;

    /**
     * @return a new {@link Builder} for {@link PostProcessingArgs}.
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link PostProcessingArgs}.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    public static class Builder<K, V> {

        private final PostProcessingArgs<K, V> instance = new PostProcessingArgs<>();

        /**
         * Request loading of document attributes.
         * <p>
         * Note: To load all attributes, use {@link #loadAll()} instead of passing {@code "*"} as a field. Passing {@code "*"}
         * as a field name will result in an {@link IllegalArgumentException}.
         * </p>
         *
         * @param fields the field identifiers (must not include {@code "*"})
         * @return this builder
         * @throws IllegalArgumentException if {@code "*"} is passed as a field name
         */
        @SafeVarargs
        public final Builder<K, V> load(K... fields) {
            LettuceAssert.notNull(fields, "Fields must not be null");
            for (K field : fields) {
                LettuceAssert.notNull(field, "Field must not be null");
                if ("*".equals(field)) {
                    throw new IllegalArgumentException("Use loadAll() instead of load(\"*\") to load all document attributes");
                }
                instance.loadFields.add(field);
            }
            return this;
        }

        /**
         * Load all document attributes.
         * <p>
         * Equivalent to using {@code LOAD *} in the Redis command. This loads all attributes from the source documents. Use
         * with caution as this can significantly impact performance when dealing with large documents or many results.
         * </p>
         * <p>
         * <b>Note:</b> This feature requires Redis OSS 8.6 or later.
         * </p>
         *
         * @return this builder
         * @since Redis OSS 8.6
         */
        public Builder<K, V> loadAll() {
            instance.loadAll = true;
            return this;
        }

        /**
         * Add a GROUPBY operation to the pipeline.
         * <p>
         * Groups results by one or more properties with optional reducer functions. Only one GROUPBY operation is allowed per
         * pipeline.
         * </p>
         *
         * @param groupBy the GROUPBY operation
         * @return this builder
         * @throws IllegalStateException if a GROUPBY operation has already been added
         */
        public Builder<K, V> groupBy(GroupBy<K, V> groupBy) {
            LettuceAssert.notNull(groupBy, "GroupBy must not be null");
            if (instance.hasGroupBy) {
                throw new IllegalStateException("GROUPBY operation has already been added. Only one GROUPBY is allowed.");
            }
            instance.hasGroupBy = true;
            instance.postProcessingOperations.add(groupBy);
            return this;
        }

        /**
         * Add a SORTBY operation to the pipeline.
         * <p>
         * Sorts results by one or more properties. Only one SORTBY operation is allowed per pipeline.
         * </p>
         *
         * @param sortBy the SORTBY operation
         * @return this builder
         * @throws IllegalStateException if a SORTBY operation has already been added
         */
        public Builder<K, V> sortBy(SortBy<K> sortBy) {
            LettuceAssert.notNull(sortBy, "SortBy must not be null");
            if (instance.hasSortBy) {
                throw new IllegalStateException("SORTBY operation has already been added. Only one SORTBY is allowed.");
            }
            instance.hasSortBy = true;
            instance.postProcessingOperations.add(sortBy);
            return this;
        }

        /**
         * Add a LIMIT operation to the pipeline.
         * <p>
         * Limits the number of results returned. Only one LIMIT operation is allowed per pipeline.
         * </p>
         *
         * @param limit the LIMIT operation
         * @return this builder
         * @throws IllegalStateException if a LIMIT operation has already been added
         */
        public Builder<K, V> limit(Limit limit) {
            LettuceAssert.notNull(limit, "Limit must not be null");
            if (instance.hasLimit) {
                throw new IllegalStateException("LIMIT operation has already been added. Only one LIMIT is allowed.");
            }
            instance.hasLimit = true;
            instance.postProcessingOperations.add(limit);
            return this;
        }

        /**
         * Add an APPLY operation to the pipeline.
         * <p>
         * Applies an expression to create a computed field. Multiple APPLY operations can be added.
         * </p>
         *
         * @param apply the APPLY operation
         * @return this builder
         */
        public Builder<K, V> apply(Apply<K, V> apply) {
            LettuceAssert.notNull(apply, "Apply must not be null");
            instance.postProcessingOperations.add(apply);
            return this;
        }

        /**
         * Add a FILTER operation to the pipeline.
         * <p>
         * Filters results based on an expression. Only one FILTER operation is allowed per pipeline.
         * </p>
         *
         * @param filter the FILTER operation
         * @return this builder
         * @throws IllegalStateException if a FILTER operation has already been added
         */
        public Builder<K, V> filter(Filter<K, V> filter) {
            LettuceAssert.notNull(filter, "Filter must not be null");
            if (instance.hasFilter) {
                throw new IllegalStateException("FILTER operation has already been added. Only one FILTER is allowed.");
            }
            instance.hasFilter = true;
            instance.postProcessingOperations.add(filter);
            return this;
        }

        /**
         * Build the {@link PostProcessingArgs}.
         *
         * @return the built {@link PostProcessingArgs}
         */
        public PostProcessingArgs<K, V> build() {
            return instance;
        }

    }

    /**
     * Build the post-processing command arguments.
     * <p>
     * LOAD is built first (if specified), then post-processing operations are built in user-specified order.
     * </p>
     *
     * @param args the {@link CommandArgs} to append to
     */
    public void build(CommandArgs<K, V> args) {
        // LOAD clause - only emit if loadAll or loadFields is specified
        if (loadAll) {
            // LOAD * (no count prefix for wildcard)
            args.add(CommandKeyword.LOAD);
            args.add("*");
        } else if (!loadFields.isEmpty()) {
            // LOAD count field [field ...]
            args.add(CommandKeyword.LOAD);
            args.add(loadFields.size());
            loadFields.forEach(args::addKey);
        }
        // No LOAD emitted if neither loadAll nor loadFields specified

        for (PostProcessingOperation<K, ?> operation : postProcessingOperations) {
            // Cast is safe because all operations can build with CommandArgs<K, V>
            @SuppressWarnings("unchecked")
            PostProcessingOperation<K, V> typedOperation = (PostProcessingOperation<K, V>) operation;
            typedOperation.build(args);
        }
    }

}
