/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.time.Duration;
import java.util.*;
import java.util.Arrays;

/**
 * Argument list builder for {@code FT.AGGREGATE} command.
 *
 * <p>
 * FT.AGGREGATE runs a search query on an index and performs aggregate transformations on the results. It provides a powerful
 * aggregation pipeline that can group, sort, apply mathematical expressions, filter, and limit results in a single command.
 * </p>
 *
 * <h3>Basic Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Simple aggregation with grouping and counting
 *     AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().groupBy("category")
 *             .reduce(Reducer.count().as("count")).sortBy("count", SortDirection.DESC).build();
 *     SearchReply<String, String> result = redis.ftAggregate("myindex", "*", args);
 * }
 * </pre>
 *
 *
 * <h3>Advanced Pipeline Example:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Complex aggregation pipeline
 *     AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().load("price", "quantity", "category")
 *             .apply("@price * @quantity", "total_value").filter("@total_value > 100").groupBy("category")
 *             .reduce(Reducer.sum("@total_value").as("category_total")).reduce(Reducer.avg("@price").as("avg_price"))
 *             .sortBy("category_total", SortDirection.DESC).limit(0, 10).dialect(QueryDialects.DIALECT2).build();
 * }
 * </pre>
 *
 * <h3>Supported Operations:</h3>
 * <ul>
 * <li><strong>LOAD</strong> - Load document attributes from source documents</li>
 * <li><strong>GROUPBY</strong> - Group results by one or more properties with reducers</li>
 * <li><strong>SORTBY</strong> - Sort results by properties with ASC/DESC directions</li>
 * <li><strong>APPLY</strong> - Apply mathematical expressions to create computed fields</li>
 * <li><strong>FILTER</strong> - Filter results using predicate expressions</li>
 * <li><strong>LIMIT</strong> - Limit and paginate results</li>
 * <li><strong>WITHCURSOR</strong> - Use cursor-based pagination for large result sets</li>
 * </ul>
 *
 * <h3>Performance Considerations:</h3>
 * <ul>
 * <li>Attributes used in GROUPBY and SORTBY should be stored as SORTABLE for optimal performance</li>
 * <li>LOAD operations can hurt performance as they require HMGET operations on each record</li>
 * <li>Use SORTBY with MAX for efficient top-N queries</li>
 * <li>Consider using WITHCURSOR for large result sets to avoid memory issues</li>
 * </ul>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 6.8
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/commands/ft.aggregate/">FT.AGGREGATE</a>
 * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/">Redis
 *      Aggregations Guide</a>
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class AggregateArgs<K, V> {

    private Optional<Boolean> verbatim = Optional.empty();

    private final List<LoadField<K>> loadFields = new ArrayList<>();

    private Optional<Duration> timeout = Optional.empty();

    /**
     * Ordered list of pipeline operations (GROUPBY, SORTBY, APPLY, FILTER). These operations must be applied in the order
     * specified by the user.
     */
    private final List<PipelineOperation<K, ?>> pipelineOperations = new ArrayList<>();

    private Optional<WithCursor> withCursor = Optional.empty();

    private final Map<K, V> params = new HashMap<>();

    private Optional<V> scorer = Optional.empty();

    private Optional<Boolean> addScores = Optional.empty();

    private QueryDialects dialect = QueryDialects.DIALECT2;

    /**
     * Creates a new {@link AggregateArgs} instance.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @return new instance of {@link AggregateArgs}.
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link AggregateArgs}.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    public static class Builder<K, V> {

        private final AggregateArgs<K, V> args = new AggregateArgs<>();

        /**
         * Set VERBATIM flag - do not try to use stemming for query expansion.
         *
         * <p>
         * When set, the query terms are searched verbatim without attempting to use stemming for query expansion. This is
         * useful when you want exact matches for your search terms.
         * </p>
         *
         * @return the builder.
         */
        public Builder<K, V> verbatim() {
            args.verbatim = Optional.of(true);
            return this;
        }

        /**
         * Load document attributes from the source document.
         *
         * <p>
         * Loads the specified field from the source document. For hash documents, this is the field name. For JSON documents,
         * this can be a JSONPath expression.
         * </p>
         *
         * <p>
         * <strong>Performance Note:</strong> LOAD operations can significantly hurt performance as they require HMGET
         * operations on each processed record. Consider storing frequently accessed attributes as SORTABLE for better
         * performance.
         * </p>
         *
         * @param field the field identifier (field name for hashes, JSONPath for JSON)
         * @return the builder.
         */
        public Builder<K, V> load(K field) {
            args.loadFields.add(new LoadField<>(field, null));
            return this;
        }

        /**
         * Load document attributes from the source document with alias.
         *
         * <p>
         * Loads the specified field from the source document and assigns it an alias name for use in the aggregation pipeline.
         * The alias can be referenced in subsequent GROUPBY, SORTBY, APPLY, and FILTER operations.
         * </p>
         *
         * @param field the field identifier (field name for hashes, JSONPath for JSON)
         * @param alias the alias name to use in the result
         * @return the builder.
         */
        public Builder<K, V> load(K field, K alias) {
            args.loadFields.add(new LoadField<>(field, alias));
            return this;
        }

        /**
         * Load all document attributes.
         *
         * <p>
         * Equivalent to using {@code LOAD *} in the Redis command. This loads all attributes from the source documents. Use
         * with caution as this can significantly impact performance when dealing with large documents or many results.
         * </p>
         *
         * @return the builder.
         */
        public Builder<K, V> loadAll() {
            args.loadFields.add(new LoadField<>(null, null)); // Special case for *
            return this;
        }

        /**
         * Set timeout for the aggregate operation.
         *
         * @param timeout the timeout duration
         * @return the builder.
         */
        public Builder<K, V> timeout(Duration timeout) {
            args.timeout = Optional.of(timeout);
            return this;
        }

        /**
         * Add a GROUPBY clause.
         *
         * @param groupBy the group by specification
         * @return the builder.
         */
        public Builder<K, V> groupBy(GroupBy<K, V> groupBy) {
            args.pipelineOperations.add(groupBy);
            return this;
        }

        /**
         * Add a SORTBY clause.
         *
         * @param sortBy the sort by specification
         * @return the builder.
         */
        public Builder<K, V> sortBy(SortBy<K> sortBy) {
            args.pipelineOperations.add(sortBy);
            return this;
        }

        /**
         * Add an APPLY clause.
         *
         * @param apply the apply specification
         * @return the builder.
         */
        public Builder<K, V> apply(Apply<K, V> apply) {
            args.pipelineOperations.add(apply);
            return this;
        }

        /**
         * Set LIMIT clause for pagination.
         *
         * <p>
         * Limits the number of results to return just {@code num} results starting at index {@code offset} (zero-based). This
         * is useful for pagination of results.
         * </p>
         *
         * <p>
         * <strong>Performance Note:</strong> It is much more efficient to use {@code SORTBY ... MAX} if you are only interested
         * in limiting the output of a sort operation. Use LIMIT for pagination or when you need results without sorting.
         * </p>
         *
         * <h3>Example:</h3>
         *
         * <pre>
         * {@code
         * // Get results 50-100 of the top 100 results efficiently
         * .sortBy("score", SortDirection.DESC).max(100)
         * .limit(50, 50)
         * }
         * </pre>
         *
         * @param offset the zero-based starting index
         * @param num the maximum number of results to return
         * @return the builder.
         */
        public Builder<K, V> limit(long offset, long num) {
            args.pipelineOperations.add(new Limit<>(offset, num));
            return this;
        }

        /**
         * Add a FILTER clause for post-aggregation filtering.
         *
         * <p>
         * Filters the results using predicate expressions relating to values in each result. Filters are applied after the
         * query and relate to the current state of the pipeline. This allows filtering on computed fields created by APPLY
         * operations or reducer results.
         * </p>
         *
         * <h3>Example Usage:</h3>
         *
         * <pre>
         * {@code
         * // Filter by numeric comparison
         * .filter("@price > 100")
         *
         * // Filter by computed field
         * .apply("@price * @quantity", "total_value")
         * .filter("@total_value > 1000")
         *
         * // Filter by reducer result
         * .groupBy("category").reduce(Reducer.count().as("count"))
         * .filter("@count >= 5")
         * }
         * </pre>
         *
         * @param filter the filter expression (e.g., "@price > 100", "@category == 'electronics'")
         * @return the builder.
         */
        public Builder<K, V> filter(V filter) {
            args.pipelineOperations.add(new Filter<>(filter));
            return this;
        }

        /**
         * Set WITHCURSOR clause for cursor-based pagination.
         *
         * <p>
         * Enables cursor-based pagination as a quicker alternative to LIMIT for scanning through large result sets. This is
         * particularly useful when you need to process all results but want to avoid memory issues with very large datasets.
         * </p>
         *
         * <h3>Example Usage:</h3>
         *
         * <pre>
         * {@code
         * // Basic cursor with read size
         * .withCursor(WithCursor.of(1000L))
         *
         * // Cursor with read size and idle timeout
         * .withCursor(WithCursor.of(1000L, Duration.ofMinutes(5)))
         * }
         * </pre>
         *
         * <p>
         * Use {@link io.lettuce.core.api.RediSearchCommands#ftCursorread(Object, long)} and
         * {@link io.lettuce.core.api.RediSearchCommands#ftCursordel(Object, long)} to iterate through and manage the cursor.
         * </p>
         *
         * @param withCursor the cursor specification with count and optional idle timeout
         * @return the builder.
         */
        public Builder<K, V> withCursor(WithCursor withCursor) {
            args.withCursor = Optional.of(withCursor);
            return this;
        }

        /**
         * Add a parameter for parameterized queries.
         *
         * <p>
         * Defines a value parameter that can be referenced in the query using {@code $name}. Each parameter reference in the
         * search query is substituted by the corresponding parameter value. This is useful for dynamic queries and prevents
         * injection attacks.
         * </p>
         *
         * <p>
         * <strong>Note:</strong> To use PARAMS, set DIALECT to 2 or greater.
         * </p>
         *
         * <h3>Example Usage:</h3>
         *
         * <pre>
         * {@code
         * // Define parameters
         * AggregateArgs.builder()
         *     .param("category", "electronics")
         *     .param("min_price", "100")
         *     .dialect(QueryDialects.DIALECT2)
         *     .build();
         *
         * // Use in query: "@category:$category @price:[$min_price +inf]"
         * }
         * </pre>
         *
         * @param name the parameter name (referenced as $name in query)
         * @param value the parameter value
         * @return the builder.
         */
        public Builder<K, V> param(K name, V value) {
            args.params.put(name, value);
            return this;
        }

        /**
         * Set SCORER clause.
         *
         * @param scorer the scorer function
         * @return the builder.
         */
        public Builder<K, V> scorer(V scorer) {
            args.scorer = Optional.of(scorer);
            return this;
        }

        /**
         * Set ADDSCORES flag to expose full-text search scores.
         *
         * <p>
         * The ADDSCORES option exposes the full-text score values to the aggregation pipeline. You can then use
         * {@code @__score} in subsequent pipeline operations like SORTBY, APPLY, FILTER, and GROUPBY.
         * </p>
         *
         * <h3>Example Usage:</h3>
         *
         * <pre>
         * {@code
         * // Sort by search relevance score
         * AggregateArgs.builder()
         *     .addScores()
         *     .sortBy("__score", SortDirection.DESC)
         *     .build();
         *
         * // Filter by minimum score threshold
         * AggregateArgs.builder()
         *     .addScores()
         *     .filter("@__score > 0.5")
         *     .build();
         * }
         * </pre>
         *
         * @return the builder.
         */
        public Builder<K, V> addScores() {
            args.addScores = Optional.of(true);
            return this;
        }

        /**
         * Set the query dialect.
         *
         * @param dialect the query dialect
         * @return the builder.
         */
        public Builder<K, V> dialect(QueryDialects dialect) {
            args.dialect = dialect;
            return this;
        }

        /**
         * Convenience method to add a GROUPBY clause with properties.
         *
         * @param properties the properties to group by
         * @return the builder.
         */
        @SafeVarargs
        public final Builder<K, V> groupBy(K... properties) {
            return groupBy(new GroupBy<>(Arrays.asList(properties)));
        }

        /**
         * Convenience method to add a SORTBY clause with a single property.
         *
         * @param property the property to sort by
         * @param direction the sort direction
         * @return the builder.
         */
        public Builder<K, V> sortBy(K property, SortDirection direction) {
            return sortBy(new SortBy<>(Collections.singletonList(new SortProperty<>(property, direction))));
        }

        /**
         * Convenience method to add an APPLY clause.
         *
         * @param expression the expression to apply
         * @param name the result field name
         * @return the builder.
         */
        public Builder<K, V> apply(V expression, K name) {
            return apply(new Apply<>(expression, name));
        }

        /**
         * Build the {@link AggregateArgs}.
         *
         * @return the built {@link AggregateArgs}.
         */
        public AggregateArgs<K, V> build() {
            return args;
        }

    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<K, V> args) {
        verbatim.ifPresent(v -> args.add(CommandKeyword.VERBATIM));

        if (!loadFields.isEmpty()) {
            args.add(CommandKeyword.LOAD);
            if (loadFields.size() == 1 && loadFields.get(0).field == null) {
                // LOAD *
                args.add("*");
            } else {
                // Count the total number of arguments (field + optional AS + alias)
                int argCount = 0;
                for (LoadField<K> loadField : loadFields) {
                    argCount++; // field
                    if (loadField.alias != null) {
                        argCount += 2; // AS + alias
                    }
                }
                args.add(argCount);
                for (LoadField<K> loadField : loadFields) {
                    args.add(loadField.field.toString());
                    if (loadField.alias != null) {
                        args.add(CommandKeyword.AS);
                        args.add(loadField.alias.toString());
                    }
                }
            }
        }

        timeout.ifPresent(t -> {
            args.add(CommandKeyword.TIMEOUT);
            args.add(t.toMillis());
        });

        // Add pipeline operations in user-specified order
        for (PipelineOperation<K, ?> operation : pipelineOperations) {
            // Cast is safe because all operations can build with CommandArgs<K, V>
            @SuppressWarnings("unchecked")
            PipelineOperation<K, V> typedOperation = (PipelineOperation<K, V>) operation;
            typedOperation.build(args);
        }

        // Add WITHCURSOR clause
        withCursor.ifPresent(wc -> {
            args.add(CommandKeyword.WITHCURSOR);
            wc.count.ifPresent(c -> {
                args.add(CommandKeyword.COUNT);
                args.add(c);
            });
            wc.maxIdle.ifPresent(mi -> {
                args.add(CommandKeyword.MAXIDLE);
                args.add(mi.toMillis());
            });
        });

        if (!params.isEmpty()) {
            args.add(CommandKeyword.PARAMS);
            args.add(params.size() * 2L);
            params.forEach((key, value) -> {
                args.add(key.toString());
                args.addValue(value);
            });
        }

        scorer.ifPresent(s -> {
            args.add(CommandKeyword.SCORER);
            args.addValue(s);
        });

        addScores.ifPresent(v -> args.add(CommandKeyword.ADDSCORES));

        args.add(CommandKeyword.DIALECT);
        args.add(dialect.toString());
    }

    public Optional<WithCursor> getWithCursor() {
        return withCursor;
    }

    /**
     * Interface for pipeline operations that need to be applied in user-specified order. This includes GROUPBY, SORTBY, APPLY,
     * and FILTER operations.
     */
    public interface PipelineOperation<K, V> {

        /**
         * Build the operation arguments into the command args.
         * 
         * @param args the command args to build into
         */
        void build(CommandArgs<K, V> args);

    }

    // Helper classes
    public static class LoadField<K> {

        final K field;

        final K alias;

        LoadField(K field, K alias) {
            this.field = field;
            this.alias = alias;
        }

    }

    public static class Limit<K, V> implements PipelineOperation<K, V> {

        final long offset;

        final long num;

        Limit(long offset, long num) {
            this.offset = offset;
            this.num = num;
        }

        @Override
        public void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.LIMIT);
            args.add(offset);
            args.add(num);
        }

    }

    public static class WithCursor {

        final Optional<Long> count;

        final Optional<Duration> maxIdle;

        public WithCursor(Long count, Optional<Duration> maxIdle) {
            this.count = Optional.ofNullable(count);
            this.maxIdle = maxIdle;
        }

        /**
         * Static factory method to create an WithCursor instance with a single name and expression pair.
         *
         * @param count the name of the expression
         * @param maxIdle the expression to apply
         * @return new Apply instance
         */
        public static WithCursor of(Long count, Duration maxIdle) {
            return new WithCursor(count, Optional.of(maxIdle));
        }

        /**
         * Static factory method to create an WithCursor instance with a single name and expression pair.
         *
         * @param count the name of the expression
         * @return new Apply instance
         */
        public static WithCursor of(Long count) {
            return new WithCursor(count, Optional.empty());
        }

    }

    /**
     * Represents a GROUPBY clause in an aggregation pipeline.
     *
     * <p>
     * Groups the results in the pipeline based on one or more properties. Each group should have at least one reducer function
     * that handles the group entries, either counting them or performing multiple aggregate operations.
     * </p>
     *
     * <h3>Example Usage:</h3>
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     // Group by category and count items
     *     GroupBy<String, String> groupBy = GroupBy.of("category").reduce(Reducer.count().as("item_count"));
     *
     *     // Group by multiple fields with multiple reducers
     *     GroupBy<String, String> complexGroup = GroupBy.of("category", "brand").reduce(Reducer.count().as("count"))
     *             .reduce(Reducer.avg("@price").as("avg_price")).reduce(Reducer.sum("@quantity").as("total_quantity"));
     * }
     * </pre>
     *
     * <h3>Supported Reducers:</h3>
     * <ul>
     * <li><strong>COUNT</strong> - Count the number of records in each group</li>
     * <li><strong>SUM</strong> - Sum numeric values within each group</li>
     * <li><strong>AVG</strong> - Calculate average of numeric values</li>
     * <li><strong>MIN/MAX</strong> - Find minimum/maximum values</li>
     * <li><strong>COUNT_DISTINCT</strong> - Count distinct values</li>
     * </ul>
     *
     * <p>
     * <strong>Performance Note:</strong> Properties used in GROUPBY should be stored as SORTABLE in the index for optimal
     * performance.
     * </p>
     */
    public static class GroupBy<K, V> implements PipelineOperation<K, V> {

        private final List<K> properties;

        private final List<Reducer<K, V>> reducers;

        public GroupBy(List<K> properties) {
            this.properties = new ArrayList<>(properties);
            this.reducers = new ArrayList<>();
        }

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
     * Represents a SORTBY clause in an aggregation pipeline.
     *
     * <p>
     * Sorts the pipeline results up until the point of SORTBY, using a list of properties. By default, sorting is ascending,
     * but ASC or DESC can be specified for each property.
     * </p>
     *
     * <h3>Example Usage:</h3>
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     // Simple sort by single field
     *     SortBy<String> sortBy = SortBy.of("price", SortDirection.DESC);
     *
     *     // Sort with MAX optimization for top-N queries
     *     SortBy<String> topN = SortBy.of("score", SortDirection.DESC).max(100) // Only sort top 100 results
     *             .withCount(); // Include accurate count
     *
     *     // Multiple sort criteria
     *     SortBy<String> multiSort = SortBy.of(new SortProperty<>("category", SortDirection.ASC),
     *             new SortProperty<>("price", SortDirection.DESC));
     * }
     * </pre>
     *
     * <h3>Performance Optimizations:</h3>
     * <ul>
     * <li><strong>MAX</strong> - Optimizes sorting by only processing the top N results</li>
     * <li><strong>WITHCOUNT</strong> - Returns accurate counts but processes all results</li>
     * <li><strong>SORTABLE fields</strong> - Use SORTABLE attribute in index for best performance</li>
     * </ul>
     *
     * <p>
     * <strong>Performance Note:</strong> Use {@code max()} for efficient top-N queries instead of sorting all results and then
     * using LIMIT.
     * </p>
     */
    public static class SortBy<K> implements PipelineOperation<K, Object> {

        private final List<SortProperty<K>> properties;

        private Optional<Long> max = Optional.empty();

        private boolean withCount = false;

        public SortBy(List<SortProperty<K>> properties) {
            this.properties = new ArrayList<>(properties);
        }

        public SortBy<K> max(long max) {
            this.max = Optional.of(max);
            return this;
        }

        public SortBy<K> withCount() {
            this.withCount = true;
            return this;
        }

        /**
         * Static factory method to create a SortBy instance with a single property.
         *
         * @param property the property to sort by
         * @param direction the sort direction
         * @param <K> Key type
         * @return new SortBy instance
         */
        public static <K> SortBy<K> of(K property, SortDirection direction) {
            return new SortBy<>(Collections.singletonList(new SortProperty<>(property, direction)));
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

            max.ifPresent(m -> {
                args.add(CommandKeyword.MAX);
                args.add(m);
            });

            if (withCount) {
                args.add(CommandKeyword.WITHCOUNT);
            }
        }

    }

    /**
     * Represents an APPLY clause in an aggregation pipeline.
     *
     * <p>
     * Applies a 1-to-1 transformation on one or more properties and either stores the result as a new property down the
     * pipeline or replaces any property using this transformation. APPLY can perform arithmetic operations on numeric
     * properties or apply functions depending on property types.
     * </p>
     *
     * <h3>Example Usage:</h3>
     *
     * <pre>
     * 
     * {
     *     &#64;code
     *     // Calculate total value from price and quantity
     *     Apply<String, String> totalValue = new Apply<>("@price * @quantity", "total_value");
     *
     *     // Mathematical operations
     *     Apply<String, String> discount = new Apply<>("@price * 0.9", "discounted_price");
     *
     *     // String operations
     *     Apply<String, String> fullName = new Apply<>("@first_name + ' ' + @last_name", "full_name");
     *
     *     // Date operations
     *     Apply<String, String> dayOfWeek = new Apply<>("day(@timestamp)", "day");
     * }
     * </pre>
     *
     * <h3>Supported Operations:</h3>
     * <ul>
     * <li><strong>Arithmetic:</strong> +, -, *, /, %, ^</li>
     * <li><strong>Mathematical functions:</strong> sqrt(), log(), abs(), ceil(), floor()</li>
     * <li><strong>String functions:</strong> upper(), lower(), substr()</li>
     * <li><strong>Date functions:</strong> day(), hour(), minute(), month(), year()</li>
     * <li><strong>Geo functions:</strong> geodistance()</li>
     * </ul>
     *
     * <p>
     * The expression is evaluated dynamically for each record in the pipeline and the result is stored as a new property that
     * can be referenced by further operations.
     * </p>
     */
    public static class Apply<K, V> implements PipelineOperation<K, V> {

        private final V expression;

        private final K name;

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
         * @param name the name of the expression
         * @param expression the expression to apply
         * @param <K> Key type
         * @param <V> Value type
         * @return new Apply instance
         */
        public static <K, V> Apply<K, V> of(V expression, K name) {
            return new Apply<>(expression, name);
        }

    }

    /**
     * Represents a REDUCE function in a GROUPBY clause.
     *
     * <p>
     * Reducers handle group entries in a GROUPBY operation, performing aggregate operations like counting, summing, averaging,
     * or finding min/max values. Each reducer can have an optional alias using the AS keyword.
     * </p>
     *
     * <h3>Example Usage:</h3>
     *
     * <pre>
     * 
     * {
     *     &#64;code
     *     // Count items in each group
     *     Reducer<String, String> count = Reducer.count().as("item_count");
     *
     *     // Sum numeric values
     *     Reducer<String, String> totalSales = Reducer.sum("@sales").as("total_sales");
     *
     *     // Calculate average
     *     Reducer<String, String> avgPrice = Reducer.avg("@price").as("average_price");
     *
     *     // Find extremes
     *     Reducer<String, String> maxScore = Reducer.max("@score").as("highest_score");
     *     Reducer<String, String> minPrice = Reducer.min("@price").as("lowest_price");
     *
     *     // Count distinct values
     *     Reducer<String, String> uniqueUsers = Reducer.countDistinct("@user_id").as("unique_users");
     * }
     * </pre>
     *
     * <h3>Available Reducer Functions:</h3>
     * <ul>
     * <li><strong>COUNT</strong> - Count the number of records in the group</li>
     * <li><strong>SUM</strong> - Sum all numeric values of a field</li>
     * <li><strong>AVG</strong> - Calculate the average of numeric values</li>
     * <li><strong>MIN</strong> - Find the minimum value</li>
     * <li><strong>MAX</strong> - Find the maximum value</li>
     * <li><strong>COUNT_DISTINCT</strong> - Count unique values of a field</li>
     * </ul>
     *
     * <p>
     * If no alias is provided using {@code as()}, the resulting field name will be the function name combined with the field
     * name (e.g., "count_distinct(@user_id)").
     * </p>
     */
    public static class Reducer<K, V> {

        private final String function;

        private final List<V> args;

        private Optional<K> alias = Optional.empty();

        public Reducer(String function, List<V> args) {
            this.function = function;
            this.args = new ArrayList<>(args);
        }

        public Reducer<K, V> as(K alias) {
            this.alias = Optional.of(alias);
            return this;
        }

        /**
         * Static factory method to create a COUNT reducer.
         *
         * @param <K> Key type
         * @param <V> Value type
         * @return new COUNT Reducer instance
         */
        public static <K, V> Reducer<K, V> count() {
            return new Reducer<>("COUNT", Collections.emptyList());
        }

        /**
         * Static factory method to create a SUM reducer.
         *
         * @param field the field to sum
         * @param <K> Key type
         * @param <V> Value type
         * @return new SUM Reducer instance
         */
        public static <K, V> Reducer<K, V> sum(V field) {
            return new Reducer<>("SUM", Collections.singletonList(field));
        }

        /**
         * Static factory method to create an AVG reducer.
         *
         * @param field the field to average
         * @param <K> Key type
         * @param <V> Value type
         * @return new AVG Reducer instance
         */
        public static <K, V> Reducer<K, V> avg(V field) {
            return new Reducer<>("AVG", Collections.singletonList(field));
        }

        /**
         * Static factory method to create a MIN reducer.
         *
         * @param field the field to find minimum
         * @param <K> Key type
         * @param <V> Value type
         * @return new MIN Reducer instance
         */
        public static <K, V> Reducer<K, V> min(V field) {
            return new Reducer<>("MIN", Collections.singletonList(field));
        }

        /**
         * Static factory method to create a MAX reducer.
         *
         * @param field the field to find maximum
         * @param <K> Key type
         * @param <V> Value type
         * @return new MAX Reducer instance
         */
        public static <K, V> Reducer<K, V> max(V field) {
            return new Reducer<>("MAX", Collections.singletonList(field));
        }

        /**
         * Static factory method to create a COUNT_DISTINCT reducer.
         *
         * @param field the field to count distinct values
         * @param <K> Key type
         * @param <V> Value type
         * @return new COUNT_DISTINCT Reducer instance
         */
        public static <K, V> Reducer<K, V> countDistinct(V field) {
            return new Reducer<>("COUNT_DISTINCT", Collections.singletonList(field));
        }

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
     * Represents a FILTER clause in an aggregation pipeline.
     *
     * <p>
     * Filters the results using predicate expressions relating to values in each result. Filters are applied after the query
     * and relate to the current state of the pipeline. This allows filtering on computed fields created by APPLY operations or
     * reducer results.
     * </p>
     */
    public static class Filter<K, V> implements PipelineOperation<K, V> {

        private final V expression;

        public Filter(V expression) {
            this.expression = expression;
        }

        @Override
        public void build(CommandArgs<K, V> args) {
            args.add(CommandKeyword.FILTER);
            args.addValue(expression);
        }

    }

    /**
     * Represents a sort property with direction.
     */
    public static class SortProperty<K> {

        final K property;

        final SortDirection direction;

        public SortProperty(K property, SortDirection direction) {
            this.property = property;
            this.direction = direction;
        }

    }

    /**
     * Sort direction enumeration.
     */
    public enum SortDirection {
        ASC, DESC
    }

}
