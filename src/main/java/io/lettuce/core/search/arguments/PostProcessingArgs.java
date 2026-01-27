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
import io.lettuce.core.protocol.ProtocolKeyword;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        args.add(loadFields.size()); // Count prefix required
        loadFields.forEach(args::addKey);

        for (PostProcessingOperation<K, ?> operation : postProcessingOperations) {
            // Cast is safe because all operations can build with CommandArgs<K, V>
            @SuppressWarnings("unchecked")
            PostProcessingOperation<K, V> typedOperation = (PostProcessingOperation<K, V>) operation;
            typedOperation.build(args);
        }
    }

    /**
     * Interface for post-processing operations applied in user-specified order.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    public interface PostProcessingOperation<K, V> {

        /**
         * Build the operation arguments into the command args.
         *
         * @param args the command args to build into
         */
        void build(CommandArgs<K, V> args);

    }

    /**
     * GROUPBY post-processing operation. Groups results by one or more properties with reducer functions.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @see Reducer
     * @see ReduceFunction
     */
    public static class GroupBy<K, V> implements PostProcessingOperation<K, V> {

        private final List<K> properties;

        private final List<Reducer<K, V>> reducers;

        private GroupBy(List<K> properties) {
            this.properties = new ArrayList<>(properties);
            this.reducers = new ArrayList<>();
        }

        /**
         * Add a reducer to this GROUPBY operation.
         *
         * @param reducer the reducer to add
         * @return this GroupBy instance
         */
        public GroupBy<K, V> reduce(Reducer<K, V> reducer) {
            this.reducers.add(reducer);
            return this;
        }

        /**
         * Static factory method to create a GroupBy instance.
         *
         * @param properties the properties to group by
         * @param <K> Key type
         * @param <V> Value type
         * @return new GroupBy instance
         */
        @SafeVarargs
        public static <K, V> GroupBy<K, V> of(K... properties) {
            return new GroupBy<>(Arrays.asList(properties));
        }

        @Override
        public void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.GROUPBY);
            args.add(properties.size());
            for (K property : properties) {
                // Add @ prefix if not already present
                String propertyStr = property.toString();
                if (!propertyStr.startsWith("@")) {
                    args.add("@" + propertyStr);
                } else {
                    args.add(propertyStr);
                }
            }

            for (Reducer<K, V> reducer : reducers) {
                reducer.build(args);
            }
        }

    }

    /**
     * APPLY post-processing operation. Applies a 1-to-1 transformation expression on properties and stores the result as a new
     * property.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    public static class Apply<K, V> implements PostProcessingOperation<K, V> {

        private final V expression;

        private final K name;

        /**
         * Creates a new APPLY operation.
         *
         * @param expression the expression to apply
         * @param name the result field name
         */
        public Apply(V expression, K name) {
            this.expression = expression;
            this.name = name;
        }

        @Override
        public void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.APPLY);
            args.addValue(expression);
            args.add(CommandKeyword.AS);
            args.add(name.toString());
        }

        /**
         * Static factory method to create an Apply instance with a single name and expression pair.
         *
         * @param expression the expression to apply
         * @param name the name of the expression
         * @param <K> Key type
         * @param <V> Value type
         * @return new Apply instance
         */
        public static <K, V> Apply<K, V> of(V expression, K name) {
            return new Apply<>(expression, name);
        }

    }

    /**
     * SORTBY post-processing operation. Sorts results by one or more properties with optional MAX optimization for top-N
     * queries.
     *
     * @param <K> Key type.
     * @see SortProperty
     * @see SortDirection
     */
    public static class SortBy<K> implements PostProcessingOperation<K, Object> {

        private final List<SortProperty<K>> properties;

        /**
         * Creates a new SORTBY operation.
         *
         * @param properties the properties to sort by
         */
        public SortBy(List<SortProperty<K>> properties) {
            this.properties = new ArrayList<>(properties);
        }

        /**
         * Static factory method to create a SortBy instance with multiple properties.
         *
         * @param properties the properties to sort by
         * @param <K> Key type
         * @return new SortBy instance
         */
        @SafeVarargs
        public static <K> SortBy<K> of(SortProperty<K>... properties) {
            return new SortBy<>(Arrays.asList(properties));
        }

        @Override
        public void build(CommandArgs<K, Object> args) {
            args.add(CommandKeyword.SORTBY);
            // Count includes property + direction pairs
            args.add(properties.size() * 2L);
            for (SortProperty<K> property : properties) {
                // Add @ prefix if not already present
                String propertyStr = property.property.toString();
                if (!propertyStr.startsWith("@")) {
                    args.add("@" + propertyStr);
                } else {
                    args.add(propertyStr);
                }
                args.add(property.direction.name());
            }
        }

    }

    /**
     * FILTER post-processing operation. Filters results using predicate expressions on the current pipeline state.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    public static class Filter<K, V> implements PostProcessingOperation<K, V> {

        private final V expression;

        /**
         * Creates a new FILTER operation.
         *
         * @param expression the filter expression (e.g., "@price > 100", "@category == 'electronics'")
         */
        public Filter(V expression) {
            this.expression = expression;
        }

        @Override
        public void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.FILTER);
            args.addValue(expression);
        }

        /**
         * Static factory method to create a Filter instance.
         *
         * @param expression the filter expression
         * @param <K> Key type
         * @param <V> Value type
         * @return new Filter instance
         */
        public static <K, V> Filter<K, V> of(V expression) {
            return new Filter<>(expression);
        }

    }

    /**
     * LIMIT post-processing operation. Limits the number of results for pagination.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    public static class Limit<K, V> implements PostProcessingOperation<K, V> {

        final long offset;

        final long num;

        private Limit(long offset, long num) {
            this.offset = offset;
            this.num = num;
        }

        @Override
        public void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.LIMIT);
            args.add(offset);
            args.add(num);
        }

        /**
         * Static factory method to create a Limit instance.
         *
         * @param offset the zero-based starting index
         * @param num the maximum number of results to return
         * @param <K> Key type
         * @param <V> Value type
         * @return new Limit instance
         */
        public static <K, V> Limit<K, V> of(long offset, long num) {
            return new Limit<>(offset, num);
        }

    }

    /**
     * REDUCE function for GROUPBY operations. Performs aggregate operations on grouped results with optional aliasing.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @see ReduceFunction
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Reducer<K, V> {

        private final ReduceFunction function;

        private final List<V> args;

        private Optional<K> alias = Optional.empty();

        /**
         * Creates a new reducer.
         *
         * @param function the reducer function
         * @param args the arguments to the reducer function
         */
        private Reducer(ReduceFunction function, List<V> args) {
            this.function = function;
            this.args = new ArrayList<>(args);
        }

        /**
         * Static factory method to create a Reducer with a function and optional arguments.
         *
         * @param function the reducer function
         * @param args the arguments to the reducer function (optional, can be empty for COUNT)
         * @param <K> Key type
         * @param <V> Value type
         * @return new Reducer instance
         */
        @SafeVarargs
        public static <K, V> Reducer<K, V> of(ReduceFunction function, V... args) {
            LettuceAssert.notNull(function, "ReduceFunction must not be null");
            return new Reducer<>(function, args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
        }

        /**
         * Set an alias for the reducer result.
         *
         * @param alias the alias name
         * @return this reducer
         */
        public Reducer<K, V> as(K alias) {
            LettuceAssert.notNull(alias, "Alias must not be null");
            this.alias = Optional.of(alias);
            return this;
        }

        /**
         * Build the reducer into command args.
         *
         * @param args the command args to build into
         */
        public void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.REDUCE);
            args.add(function);
            args.add(this.args.size());
            for (V arg : this.args) {
                args.addValue(arg);
            }

            alias.ifPresent(a -> {
                args.add(CommandKeyword.AS);
                args.add(a.toString());
            });
        }

    }

    /**
     * Enumeration of REDUCE functions for GROUPBY operations.
     */
    public enum ReduceFunction implements ProtocolKeyword {

        /** Count the number of records in the group. */
        COUNT,

        /** Count unique values of a field. */
        COUNT_DISTINCT,

        /** Approximate count of unique values using HyperLogLog algorithm. */
        COUNT_DISTINCTISH,

        /** Sum all numeric values of a field. */
        SUM,

        /** Calculate the average of numeric values. */
        AVG,

        /** Find the minimum value. */
        MIN,

        /** Find the maximum value. */
        MAX,

        /** Calculate standard deviation. */
        STDDEV,

        /** Calculate quantile/percentile (e.g., median at 0.5). */
        QUANTILE,

        /** Collect all values into a list. */
        TOLIST,

        /** Get the first value in the group. */
        FIRST_VALUE,

        /** Random sampling from the group. */
        RANDOM_SAMPLE;

        private final byte[] bytes;

        ReduceFunction() {
            this.bytes = name().getBytes(StandardCharsets.US_ASCII);
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

    }

    /**
     * Represents a sort property with direction for SORTBY operations.
     *
     * @param <K> Key type.
     */
    public static class SortProperty<K> {

        final K property;

        final SortDirection direction;

        /**
         * Creates a new sort property.
         *
         * @param property the property to sort by
         * @param direction the sort direction
         */
        public SortProperty(K property, SortDirection direction) {
            this.property = property;
            this.direction = direction;
        }

        /**
         * Get the property to sort by.
         *
         * @return the property
         */
        public K getProperty() {
            return property;
        }

        /**
         * Get the sort direction.
         *
         * @return the sort direction
         */
        public SortDirection getDirection() {
            return direction;
        }

    }

    /**
     * Sort direction enumeration for SORTBY operations.
     */
    public enum SortDirection {

        /**
         * Ascending sort order.
         */
        ASC,

        /**
         * Descending sort order.
         */
        DESC

    }

}
