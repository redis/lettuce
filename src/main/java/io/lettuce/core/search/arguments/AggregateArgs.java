/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.search.AggregationReply;

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
 *     AggregateArgs args = AggregateArgs.builder().groupBy("category").reduce(Reducer.count().as("count"))
 *             .sortBy("count", SortDirection.DESC).build();
 *     SearchReply<String> result = redis.ftAggregate("myindex", "*", args);
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
 *     AggregateArgs args = AggregateArgs.builder().load("price", "quantity", "category")
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
 * @since 6.8
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/commands/ft.aggregate/">FT.AGGREGATE</a>
 * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/">Redis
 *      Aggregations Guide</a>
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class AggregateArgs {

    private Optional<Boolean> verbatim = Optional.empty();

    private final List<LoadField> loadFields = new ArrayList<>();

    private Optional<Duration> timeout = Optional.empty();

    /**
     * Ordered list of pipeline operations (GROUPBY, SORTBY, APPLY, FILTER). These operations must be applied in the order
     * specified by the user.
     */
    private final List<PipelineOperation> pipelineOperations = new ArrayList<>();

    private Optional<WithCursor> withCursor = Optional.empty();

    private final Map<String, Object> params = new HashMap<>();

    private Optional<String> scorer = Optional.empty();

    private Optional<Boolean> addScores = Optional.empty();

    private QueryDialects dialect = QueryDialects.DIALECT2;

    /**
     * Creates a new {@link AggregateArgs} instance.
     *
     * @return new instance of {@link AggregateArgs}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AggregateArgs}.
     *
     */
    public static class Builder {

        private final AggregateArgs args = new AggregateArgs();

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
        public Builder verbatim() {
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
        public Builder load(String field) {
            args.loadFields.add(new LoadField(field, null));
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
        public Builder load(String field, String alias) {
            args.loadFields.add(new LoadField(field, alias));
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
        public Builder loadAll() {
            args.loadFields.add(new LoadField(null, null)); // Special case for *
            return this;
        }

        /**
         * Set timeout for the aggregate operation.
         *
         * @param timeout the timeout duration
         * @return the builder.
         */
        public Builder timeout(Duration timeout) {
            args.timeout = Optional.of(timeout);
            return this;
        }

        /**
         * Add a GROUPBY clause.
         *
         * @param groupBy the group by specification
         * @return the builder.
         */
        public Builder groupBy(GroupBy groupBy) {
            args.pipelineOperations.add(groupBy);
            return this;
        }

        /**
         * Add a SORTBY clause.
         *
         * @param sortBy the sort by specification
         * @return the builder.
         */
        public Builder sortBy(SortBy sortBy) {
            args.pipelineOperations.add(sortBy);
            return this;
        }

        /**
         * Add an APPLY clause.
         *
         * @param apply the apply specification
         * @return the builder.
         */
        public Builder apply(Apply apply) {
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
        public Builder limit(long offset, long num) {
            args.pipelineOperations.add(new Limit(offset, num));
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
        public Builder filter(String filter) {
            args.pipelineOperations.add(new Filter(filter));
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
         * Use {@link io.lettuce.core.api.sync.RediSearchCommands#ftCursorread(String, AggregationReply.Cursor, int)} and
         * {@link io.lettuce.core.api.sync.RediSearchCommands#ftCursordel(String, AggregationReply.Cursor)} to iterate through
         * and manage the cursor.
         * </p>
         *
         * @param withCursor the cursor specification with count and optional idle timeout
         * @return the builder.
         */
        public Builder withCursor(WithCursor withCursor) {
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
        public Builder param(String name, String value) {
            args.params.put(name, value);
            return this;
        }

        /**
         * Add a binary parameter for parameterized queries.
         *
         * <p>
         * Defines a binary value parameter that can be referenced in the query using {@code $name}. The value bypasses the
         * connection's value codec, which is useful for passing vector blobs (e.g. KNN {@code $BLOB}) over a non-binary
         * connection.
         * </p>
         *
         * <p>
         * <strong>Note:</strong> To use PARAMS, set DIALECT to 2 or greater.
         * </p>
         *
         * @param name the parameter name (referenced as $name in query)
         * @param value the binary parameter value (e.g., vector data)
         * @return the builder.
         */
        public Builder param(String name, byte[] value) {
            args.params.put(name, value);
            return this;
        }

        /**
         * Set SCORER clause.
         *
         * @param scorer the scorer function
         * @return the builder.
         */
        public Builder scorer(String scorer) {
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
        public Builder addScores() {
            args.addScores = Optional.of(true);
            return this;
        }

        /**
         * Set the query dialect.
         *
         * @param dialect the query dialect
         * @return the builder.
         */
        public Builder dialect(QueryDialects dialect) {
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
        public final Builder groupBy(String... properties) {
            return groupBy(new GroupBy(Arrays.asList(properties)));
        }

        /**
         * Convenience method to add a SORTBY clause with a single property.
         *
         * @param property the property to sort by
         * @param direction the sort direction
         * @return the builder.
         */
        public Builder sortBy(String property, SortDirection direction) {
            return sortBy(new SortBy(Collections.singletonList(new SortProperty(property, direction))));
        }

        /**
         * Convenience method to add an APPLY clause.
         *
         * @param expression the expression to apply
         * @param name the result field name
         * @return the builder.
         */
        public Builder apply(String expression, String name) {
            return apply(new Apply(expression, name));
        }

        /**
         * Build the {@link AggregateArgs}.
         *
         * @return the built {@link AggregateArgs}.
         */
        public AggregateArgs build() {
            return args;
        }

    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<?, ?> args) {
        verbatim.ifPresent(v -> args.add(CommandKeyword.VERBATIM));

        // ADDSCORES is a query-level option and must be emitted before the result-processing pipeline
        // (GROUPBY/SORTBY/APPLY/...); the server rejects it if it appears after those clauses.
        addScores.ifPresent(v -> args.add(CommandKeyword.ADDSCORES));

        if (!loadFields.isEmpty()) {
            args.add(CommandKeyword.LOAD);
            if (loadFields.size() == 1 && loadFields.get(0).field == null) {
                // LOAD *
                args.add("*");
            } else {
                // Count the total number of arguments (field + optional AS + alias)
                int argCount = 0;
                for (LoadField loadField : loadFields) {
                    argCount++; // field
                    if (loadField.alias != null) {
                        argCount += 2; // AS + alias
                    }
                }
                args.add(argCount);
                for (LoadField loadField : loadFields) {
                    args.add(loadField.field);
                    if (loadField.alias != null) {
                        args.add(CommandKeyword.AS);
                        args.add(loadField.alias);
                    }
                }
            }
        }

        timeout.ifPresent(t -> {
            args.add(CommandKeyword.TIMEOUT);
            args.add(t.toMillis());
        });

        // Add pipeline operations in user-specified order
        for (PipelineOperation operation : pipelineOperations) {
            operation.build(args);
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
                args.add(key);
                if (value instanceof byte[]) {
                    args.add((byte[]) value);
                } else {
                    args.add((String) value);
                }
            });
        }

        scorer.ifPresent(s -> {
            args.add(CommandKeyword.SCORER);
            args.add(s);
        });

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
    public interface PipelineOperation {

        /**
         * Build the operation arguments into the command args.
         *
         * @param args the command args to build into
         */
        void build(CommandArgs<?, ?> args);

    }

    // Helper classes
    public static class LoadField {

        final String field;

        final String alias;

        LoadField(String field, String alias) {
            this.field = field;
            this.alias = alias;
        }

    }

    public static class Limit implements PipelineOperation {

        final long offset;

        final long num;

        Limit(long offset, long num) {
            this.offset = offset;
            this.num = num;
        }

        @Override
        public void build(CommandArgs<?, ?> args) {
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
     *     GroupBy groupBy = GroupBy.of("category").reduce(Reducer.count().as("item_count"));
     *
     *     // Group by multiple fields with multiple reducers
     *     GroupBy complexGroup = GroupBy.of("category", "brand").reduce(Reducer.count().as("count"))
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
     * <li><strong>COLLECT</strong> - Collect per-row field projections into an array of entries per group (experimental, see
     * {@link Reducer#collect()})</li>
     * </ul>
     *
     * <p>
     * <strong>Performance Note:</strong> Properties used in GROUPBY should be stored as SORTABLE in the index for optimal
     * performance.
     * </p>
     */
    public static class GroupBy implements PipelineOperation {

        private final List<String> properties;

        private final List<Reducer> reducers;

        public GroupBy(List<String> properties) {
            this.properties = new ArrayList<>(properties);
            this.reducers = new ArrayList<>();
        }

        public GroupBy reduce(Reducer reducer) {
            this.reducers.add(reducer);
            return this;
        }

        /**
         * Static factory method to create a GroupBy instance.
         *
         * @param properties the properties to group by
         * @return new GroupBy instance
         */
        @SafeVarargs
        public static GroupBy of(String... properties) {
            return new GroupBy(Arrays.asList(properties));
        }

        @Override
        public void build(CommandArgs<?, ?> args) {
            args.add(CommandKeyword.GROUPBY);
            args.add(properties.size());
            for (String property : properties) {
                // Add @ prefix if not already present
                String propertyStr = property.toString();
                if (!propertyStr.startsWith("@")) {
                    args.add("@" + propertyStr);
                } else {
                    args.add(propertyStr);
                }
            }

            for (Reducer reducer : reducers) {
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
     *     SortBy sortBy = SortBy.of("price", SortDirection.DESC);
     *
     *     // Sort with MAX optimization for top-N queries
     *     SortBy topN = SortBy.of("score", SortDirection.DESC).max(100) // Only sort top 100 results
     *             .withCount(); // Include accurate count
     *
     *     // Multiple sort criteria
     *     SortBy multiSort = SortBy.of(new SortProperty("category", SortDirection.ASC),
     *             new SortProperty("price", SortDirection.DESC));
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
    public static class SortBy implements PipelineOperation {

        private final List<SortProperty> properties;

        private Optional<Long> max = Optional.empty();

        private boolean withCount = false;

        public SortBy(List<SortProperty> properties) {
            this.properties = new ArrayList<>(properties);
        }

        public SortBy max(long max) {
            this.max = Optional.of(max);
            return this;
        }

        public SortBy withCount() {
            this.withCount = true;
            return this;
        }

        /**
         * Static factory method to create a SortBy instance with a single property.
         *
         * @param property the property to sort by
         * @param direction the sort direction
         * @return new SortBy instance
         */
        public static SortBy of(String property, SortDirection direction) {
            return new SortBy(Collections.singletonList(new SortProperty(property, direction)));
        }

        /**
         * Static factory method to create a SortBy instance with multiple properties.
         *
         * @param properties the properties to sort by
         * @return new SortBy instance
         */
        @SafeVarargs
        public static SortBy of(SortProperty... properties) {
            return new SortBy(Arrays.asList(properties));
        }

        @Override
        public void build(CommandArgs<?, ?> args) {
            args.add(CommandKeyword.SORTBY);
            // Count includes property + direction pairs
            args.add(properties.size() * 2L);
            for (SortProperty property : properties) {
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
     *     Apply totalValue = new Apply("@price * @quantity", "total_value");
     *
     *     // Mathematical operations
     *     Apply discount = new Apply("@price * 0.9", "discounted_price");
     *
     *     // String operations
     *     Apply fullName = new Apply("@first_name + ' ' + @last_name", "full_name");
     *
     *     // Date operations
     *     Apply dayOfWeek = new Apply("day(@timestamp)", "day");
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
    public static class Apply implements PipelineOperation {

        private final String expression;

        private final String name;

        public Apply(String expression, String name) {
            this.expression = expression;
            this.name = name;
        }

        @Override
        public void build(CommandArgs<?, ?> args) {
            args.add(CommandKeyword.APPLY);
            args.add(expression);
            args.add(CommandKeyword.AS);
            args.add(name);
        }

        /**
         * Static factory method to create an Apply instance with a single name and expression pair.
         *
         * @param name the name of the expression
         * @param expression the expression to apply
         * @return new Apply instance
         */
        public static Apply of(String expression, String name) {
            return new Apply(expression, name);
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
     *     Reducer count = Reducer.count().as("item_count");
     *
     *     // Sum numeric values
     *     Reducer totalSales = Reducer.sum("@sales").as("total_sales");
     *
     *     // Calculate average
     *     Reducer avgPrice = Reducer.avg("@price").as("average_price");
     *
     *     // Find extremes
     *     Reducer maxScore = Reducer.max("@score").as("highest_score");
     *     Reducer minPrice = Reducer.min("@price").as("lowest_price");
     *
     *     // Count distinct values
     *     Reducer uniqueUsers = Reducer.countDistinct("@user_id").as("unique_users");
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
    public static class Reducer {

        private final String function;

        private final List<String> args;

        private Optional<String> alias = Optional.empty();

        public Reducer(String function, List<String> args) {
            this.function = function;
            this.args = new ArrayList<>(args);
        }

        public Reducer as(String alias) {
            this.alias = Optional.of(alias);
            return this;
        }

        /**
         * Static factory method to create a COUNT reducer.
         *
         * @return new COUNT Reducer instance
         */
        public static Reducer count() {
            return new Reducer("COUNT", Collections.emptyList());
        }

        /**
         * Static factory method to create a SUM reducer.
         *
         * @param field the field to sum
         * @return new SUM Reducer instance
         */
        public static Reducer sum(String field) {
            return new Reducer("SUM", Collections.singletonList(field));
        }

        /**
         * Static factory method to create an AVG reducer.
         *
         * @param field the field to average
         * @return new AVG Reducer instance
         */
        public static Reducer avg(String field) {
            return new Reducer("AVG", Collections.singletonList(field));
        }

        /**
         * Static factory method to create a MIN reducer.
         *
         * @param field the field to find minimum
         * @return new MIN Reducer instance
         */
        public static Reducer min(String field) {
            return new Reducer("MIN", Collections.singletonList(field));
        }

        /**
         * Static factory method to create a MAX reducer.
         *
         * @param field the field to find maximum
         * @return new MAX Reducer instance
         */
        public static Reducer max(String field) {
            return new Reducer("MAX", Collections.singletonList(field));
        }

        /**
         * Static factory method to create a COUNT_DISTINCT reducer.
         *
         * @param field the field to count distinct values
         * @return new COUNT_DISTINCT Reducer instance
         */
        public static Reducer countDistinct(String field) {
            return new Reducer("COUNT_DISTINCT", Collections.singletonList(field));
        }

        /**
         * Static factory method to create a {@code COLLECT} reducer.
         *
         * <p>
         * {@code COLLECT} gathers per-document projections within a {@code GROUPBY} group and returns them as an array of
         * per-entry maps under the reducer alias, optionally sorted and bounded. Configure the projected fields via
         * {@link CollectReducer#fields(String[]) fields(...)} or {@link CollectReducer#fieldsAll() fieldsAll()}, then
         * optionally chain {@link CollectReducer#sortBy(SortProperty[]) sortBy(...)} and
         * {@link CollectReducer#limit(long, long) limit(...)} before calling {@link Reducer#as(String) as(...)}.
         * </p>
         *
         * <p>
         * The collected column is a {@link io.lettuce.core.search.FieldValue} of kind
         * {@link io.lettuce.core.search.FieldValue.Kind#ARRAY} with one element per collected entry; read the entries via
         * {@link io.lettuce.core.search.FieldValue#asList() asList()} and each entry via
         * {@link io.lettuce.core.search.FieldValue#asMap() asMap()}, which normalizes the protocol-specific entry shape.
         * </p>
         *
         * <p>
         * <strong>Experimental.</strong> Both the underlying Redis Search feature and this API may change. {@code COLLECT} is
         * gated behind {@code search-enable-unstable-features}; enable it on the server (for example via
         * {@code CONFIG SET search-enable-unstable-features yes}) before issuing aggregations that use this reducer, otherwise
         * the server replies with an error.
         * </p>
         *
         * @return new {@link CollectReducer} instance
         * @see CollectReducer
         */
        @Experimental
        public static CollectReducer collect() {
            return new CollectReducer();
        }

        public void build(CommandArgs<?, ?> args) {
            args.add(CommandKeyword.REDUCE);
            args.add(function);
            args.add(this.args.size());
            for (String arg : this.args) {
                args.add(arg.toString());
            }

            alias.ifPresent(a -> {
                args.add(CommandKeyword.AS);
                args.add(a);
            });
        }

    }

    /**
     * Represents a {@code REDUCE COLLECT} clause in an aggregation pipeline.
     *
     * <p>
     * Within each {@code GROUPBY} group, {@code COLLECT} projects a chosen set of fields from every row and returns them as an
     * array of per-entry maps under the reducer alias, optionally sorted and bounded. It targets grouped-reporting workflows
     * where the caller needs the representative rows of each group in a single aggregation query. The grammar produced by this
     * builder is:
     * </p>
     *
     * <pre>
     * {@code
     * REDUCE COLLECT <narg>
     *     FIELDS ( * | <num_fields> <@field> [<@field> ...] )
     *     [SORTBY <narg> <@field> [ASC|DESC] [<@field> [ASC|DESC] ...]]
     *     [LIMIT <offset> <count>]
     *   [AS <alias>]
     * }
     * </pre>
     *
     * <p>
     * Field and sort-key names are referenced with an {@code @} prefix on the wire (the builder adds it automatically when it
     * is missing). The output map keys are the bare names. {@code FIELDS *} projects whatever the pipeline has materialized at
     * the {@code COLLECT} stage; it does not implicitly fetch the full document.
     * </p>
     *
     * <p>
     * The collected column is a {@link io.lettuce.core.search.FieldValue} of kind
     * {@link io.lettuce.core.search.FieldValue.Kind#ARRAY} with one element per collected entry; read the entries via
     * {@link io.lettuce.core.search.FieldValue#asList() asList()} and each entry via
     * {@link io.lettuce.core.search.FieldValue#asMap() asMap()}, which normalizes the protocol-specific entry shape (RESP3
     * returns each entry as a map, RESP2 as a flat key/value array).
     * </p>
     *
     * <p>
     * The number of collected entries per group is always bounded by the server: {@code SORTBY} without an explicit
     * {@code LIMIT} returns at most 10 entries per group, and without either clause collection is capped by the
     * {@code search-max-aggregate-results} configuration. Supply an explicit {@link #limit(long, long) limit(...)} to control
     * the bound.
     * </p>
     *
     * <p>
     * <strong>Experimental.</strong> Both the underlying Redis Search feature and this API may change. {@code COLLECT} is gated
     * behind {@code search-enable-unstable-features}; enable it on the server before issuing aggregations that use this
     * reducer.
     * </p>
     *
     * @see Reducer#collect()
     * @since 7.7
     */
    @Experimental
    public static class CollectReducer extends Reducer {

        private boolean allFields = false;

        private final List<String> fields = new ArrayList<>();

        private final List<SortProperty> sortProperties = new ArrayList<>();

        private Optional<Long> limitOffset = Optional.empty();

        private Optional<Long> limitCount = Optional.empty();

        private Optional<String> alias = Optional.empty();

        CollectReducer() {
            super("COLLECT", Collections.emptyList());
        }

        /**
         * Project the named fields for every document in the group. Names may be supplied with or without a leading {@code @};
         * the builder normalizes each to a single {@code @<name>} on the wire. Use {@code @__key} or ordinary document field
         * names.
         *
         * <p>
         * Mutually exclusive with {@link #fieldsAll()}. May be called multiple times to append further fields.
         * </p>
         *
         * @param fields the fields to project
         * @return {@code this} for chaining
         */
        public CollectReducer fields(String... fields) {
            if (this.allFields) {
                throw new IllegalStateException("REDUCE COLLECT cannot mix FIELDS * with explicit field names");
            }
            Collections.addAll(this.fields, fields);
            return this;
        }

        /**
         * Project every field present in the pipeline at the {@code COLLECT} stage ({@code FIELDS *}).
         *
         * <p>
         * Per the COLLECT specification, {@code *} does not trigger an implicit load — fields must already be in the pipeline
         * (typically via {@code LOAD *} or because they are grouping keys / reducer aliases). Mutually exclusive with
         * {@link #fields(String[])}.
         * </p>
         *
         * @return {@code this} for chaining
         */
        public CollectReducer fieldsAll() {
            if (!this.fields.isEmpty()) {
                throw new IllegalStateException("REDUCE COLLECT cannot mix FIELDS * with explicit field names");
            }
            this.allFields = true;
            return this;
        }

        /**
         * In-group sort by one or more properties. May be called multiple times to append further sort keys.
         *
         * <p>
         * <strong>Note:</strong> when {@code SORTBY} is supplied without an explicit {@link #limit(long, long) limit(...)}, the
         * server applies a default limit of 10 entries per group. Supply an explicit limit to collect more sorted entries.
         * Without {@code SORTBY}, entry order is unspecified and collection is capped by the server's
         * {@code search-max-aggregate-results} configuration.
         * </p>
         *
         * @param properties the sort properties
         * @return {@code this} for chaining
         */
        public CollectReducer sortBy(SortProperty... properties) {
            Collections.addAll(this.sortProperties, properties);
            return this;
        }

        /**
         * Convenience for {@code sortBy(new SortProperty(field, SortDirection.ASC))}.
         *
         * @param field the field to sort by ascending
         * @return {@code this} for chaining
         */
        public CollectReducer sortByAsc(String field) {
            this.sortProperties.add(new SortProperty(field, SortDirection.ASC));
            return this;
        }

        /**
         * Convenience for {@code sortBy(new SortProperty(field, SortDirection.DESC))}.
         *
         * @param field the field to sort by descending
         * @return {@code this} for chaining
         */
        public CollectReducer sortByDesc(String field) {
            this.sortProperties.add(new SortProperty(field, SortDirection.DESC));
            return this;
        }

        /**
         * Bound the output per group to the first {@code count} entries (offset 0).
         *
         * @param count the maximum number of entries per group
         * @return {@code this} for chaining
         */
        public CollectReducer limit(long count) {
            return limit(0, count);
        }

        /**
         * Bound the output per group to {@code count} entries starting at {@code offset}.
         *
         * @param offset the number of entries to skip
         * @param count the maximum number of entries to return
         * @return {@code this} for chaining
         */
        public CollectReducer limit(long offset, long count) {
            if (offset < 0 || count < 0) {
                throw new IllegalArgumentException("LIMIT offset and count must be non-negative");
            }
            this.limitOffset = Optional.of(offset);
            this.limitCount = Optional.of(count);
            return this;
        }

        @Override
        public CollectReducer as(String alias) {
            this.alias = Optional.of(alias);
            return this;
        }

        @Override
        public void build(CommandArgs<?, ?> args) {
            if (!allFields && fields.isEmpty()) {
                throw new IllegalStateException("REDUCE COLLECT requires either fields(...) or fieldsAll() to be configured");
            }

            args.add(CommandKeyword.REDUCE);
            args.add("COLLECT");
            args.add(argCount());

            args.add(CommandKeyword.FIELDS);
            if (allFields) {
                args.add("*");
            } else {
                args.add(fields.size());
                for (String field : fields) {
                    args.add(withAtPrefix(field));
                }
            }

            if (!sortProperties.isEmpty()) {
                args.add(CommandKeyword.SORTBY);
                args.add(sortProperties.size() * 2L);
                for (SortProperty property : sortProperties) {
                    args.add(withAtPrefix(property.property));
                    args.add(property.direction.name());
                }
            }

            if (limitOffset.isPresent()) {
                args.add(CommandKeyword.LIMIT);
                args.add(limitOffset.get());
                args.add(limitCount.get());
            }

            alias.ifPresent(a -> {
                args.add(CommandKeyword.AS);
                args.add(a);
            });
        }

        /**
         * Computes {@code <narg>} as the number of reducer argument tokens (the {@code FIELDS}, {@code SORTBY}, and
         * {@code LIMIT} clauses), excluding the trailing {@code AS <alias>}.
         */
        private long argCount() {
            long count = allFields ? 2 : 2 + fields.size();
            if (!sortProperties.isEmpty()) {
                count += 2 + sortProperties.size() * 2L;
            }
            if (limitOffset.isPresent()) {
                count += 3;
            }
            return count;
        }

        private static String withAtPrefix(String name) {
            return name.startsWith("@") ? name : "@" + name;
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
    public static class Filter implements PipelineOperation {

        private final String expression;

        public Filter(String expression) {
            this.expression = expression;
        }

        @Override
        public void build(CommandArgs<?, ?> args) {
            args.add(CommandKeyword.FILTER);
            args.add(expression);
        }

    }

    /**
     * Represents a sort property with direction.
     */
    public static class SortProperty {

        final String property;

        final SortDirection direction;

        public SortProperty(String property, SortDirection direction) {
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
