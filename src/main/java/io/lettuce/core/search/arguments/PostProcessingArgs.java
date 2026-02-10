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

import java.util.ArrayList;
import java.util.List;

/**
 * Argument list builder for FT.HYBRID and FT.AGGREGATE post-processing operations. Operations are applied in user-specified
 * order after the COMBINE clause.
 *
 * <h3>Basic Usage:</h3>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     PostProcessingArgs<String, String> args = PostProcessingArgs.<String, String> builder().load("@price", "@category")
 *             .addOperation(GroupBy.of("@category").reduce(Reducer.of(ReduceFunction.COUNT).as("total")))
 *             .addOperation(Apply.of("@price * 0.9", "discounted_price"))
 *             .addOperation(SortBy.of("@discounted_price", SortDirection.DESC))
 *             .addOperation(Filter.of("@discounted_price > 100")).build();
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
     * Ordered list of pipeline operations (GROUPBY, SORTBY, APPLY, FILTER). These operations are applied in the order specified
     * by the user.
     */
    private final List<PostProcessingOperation<K, ?>> postProcessingOperations = new ArrayList<>();

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
         *
         * @param fields the field identifiers
         * @return this builder
         */
        @SafeVarargs
        public final Builder<K, V> load(K... fields) {
            LettuceAssert.notNull(fields, "Fields must not be null");
            for (K field : fields) {
                LettuceAssert.notNull(field, "Field must not be null");
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
         *
         * @return this builder
         */
        public Builder<K, V> loadAll() {
            instance.loadAll = true;
            return this;
        }

        /**
         * Add a post-processing operation to the pipeline.
         * <p>
         * Operations are applied in the order they are added. Supported operations include:
         * </p>
         * <ul>
         * <li>{@link GroupBy} - Group results by properties with optional reducers</li>
         * <li>{@link Apply} - Apply expressions to create computed fields</li>
         * <li>{@link SortBy} - Sort results by properties</li>
         * <li>{@link Filter} - Filter results by expressions</li>
         * </ul>
         *
         * <h3>Example Usage:</h3>
         *
         * <pre>
         * {@code
         * PostProcessingArgs.<String, String> builder()
         *     .addOperation(GroupBy.of("@category").reduce(Reducer.of(ReduceFunction.COUNT).as("total")))
         *     .addOperation(Apply.of("@price * 0.9", "discounted_price"))
         *     .addOperation(SortBy.of("@discounted_price", SortDirection.DESC))
         *     .addOperation(Filter.of("@discounted_price > 100"))
         *     .build();
         * }
         * </pre>
         *
         * @param operation the operation to add (GroupBy, Apply, SortBy, Filter, etc.)
         * @return this builder
         */
        public Builder<K, V> addOperation(PostProcessingOperation<K, ?> operation) {
            LettuceAssert.notNull(operation, "Operation must not be null");
            instance.postProcessingOperations.add(operation);
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
     * LOAD is built first, then post-processing operations are built in user-specified order.
     * </p>
     *
     * @param args the {@link CommandArgs} to append to
     */
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.LOAD);
        if (loadAll) {
            // LOAD *
            args.add("*");
        } else {
            args.add(loadFields.size()); // Count prefix required
            loadFields.forEach(args::addKey);
        }

        for (PostProcessingOperation<K, ?> operation : postProcessingOperations) {
            // Cast is safe because all operations can build with CommandArgs<K, V>
            @SuppressWarnings("unchecked")
            PostProcessingOperation<K, V> typedOperation = (PostProcessingOperation<K, V>) operation;
            typedOperation.build(args);
        }
    }

}
