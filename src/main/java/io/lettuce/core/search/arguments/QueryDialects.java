/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

/**
 * Enumeration of Redis Search query dialects that define the syntax and features available for search queries and aggregation
 * operations.
 *
 * <p>
 * Query dialects in Redis Search determine:
 * </p>
 * <ul>
 * <li>The syntax rules for query expressions</li>
 * <li>Available operators and functions</li>
 * <li>Field reference syntax and behavior</li>
 * <li>Compatibility with different Redis Search versions</li>
 * </ul>
 *
 * <p>
 * Each dialect represents a specific version of the query language with its own capabilities and syntax rules. Higher dialect
 * numbers generally include more features and improved functionality, but may not be compatible with older Redis Search
 * versions.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 * 
 * <pre>
 * 
 * {
 *     &#64;code
 *     SearchArgs<String, String> args = SearchArgs.<String, String> builder().dialect(QueryDialects.DIALECT2).build();
 * }
 * </pre>
 *
 * @author Redis Ltd.
 * @since 6.8
 * @see io.lettuce.core.search.arguments.SearchArgs
 * @see io.lettuce.core.search.arguments.AggregateArgs
 */
public enum QueryDialects {

    /**
     * Query dialect version 1 - the original Redis Search query syntax.
     *
     * <p>
     * Features and characteristics:
     * </p>
     * <ul>
     * <li>Basic query syntax with standard operators</li>
     * <li>Field references without @ prefix</li>
     * <li>Limited function support</li>
     * <li>Compatible with early Redis Search versions</li>
     * </ul>
     *
     * <p>
     * This dialect provides the most basic query functionality and is primarily maintained for backward compatibility with
     * older applications.
     * </p>
     */
    DIALECT1("1"),

    /**
     * Query dialect version 2 - enhanced query syntax with improved features.
     *
     * <p>
     * Features and characteristics:
     * </p>
     * <ul>
     * <li>Field references with @ prefix (e.g., @field_name)</li>
     * <li>Enhanced operator support</li>
     * <li>Improved aggregation functions</li>
     * <li>Better error handling and validation</li>
     * <li>Support for more complex expressions</li>
     * </ul>
     *
     * <p>
     * This is the recommended dialect for most applications as it provides a good balance of features and compatibility.
     * </p>
     */
    DIALECT2("2"),

    /**
     * Query dialect version 3 - advanced query syntax with extended capabilities.
     *
     * <p>
     * Features and characteristics:
     * </p>
     * <ul>
     * <li>All features from DIALECT2</li>
     * <li>Additional built-in functions</li>
     * <li>Enhanced aggregation capabilities</li>
     * <li>Improved performance optimizations</li>
     * <li>Extended operator set</li>
     * </ul>
     *
     * <p>
     * This dialect includes advanced features for complex query scenarios and is suitable for applications requiring
     * sophisticated search operations.
     * </p>
     */
    DIALECT3("3"),

    /**
     * Query dialect version 4 - latest query syntax with cutting-edge features.
     *
     * <p>
     * Features and characteristics:
     * </p>
     * <ul>
     * <li>All features from previous dialects</li>
     * <li>Latest query language enhancements</li>
     * <li>Newest built-in functions and operators</li>
     * <li>Advanced optimization features</li>
     * <li>Experimental or preview functionality</li>
     * </ul>
     *
     * <p>
     * This dialect provides access to the latest Redis Search features but may require newer Redis Search versions and could
     * include experimental functionality that might change in future releases.
     * </p>
     */
    DIALECT4("4");

    private final String dialect;

    /**
     * Creates a new QueryDialects enum constant with the specified dialect version string.
     *
     * <p>
     * This constructor is used internally to initialize each enum constant with its corresponding dialect version identifier
     * that will be sent to Redis Search.
     * </p>
     *
     * @param dialect the string representation of the dialect version (e.g., "1", "2", "3", "4"). Must not be {@code null} or
     *        empty.
     */
    QueryDialects(String dialect) {
        this.dialect = dialect;
    }

    /**
     * Returns the string representation of this query dialect.
     *
     * <p>
     * This method returns the dialect version identifier that is sent to Redis Search when executing queries or aggregations.
     * The returned string corresponds to the DIALECT parameter value used in Redis Search commands.
     * </p>
     *
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li>{@code QueryDialects.DIALECT1.toString()} returns {@code "1"}</li>
     * <li>{@code QueryDialects.DIALECT2.toString()} returns {@code "2"}</li>
     * <li>{@code QueryDialects.DIALECT3.toString()} returns {@code "3"}</li>
     * <li>{@code QueryDialects.DIALECT4.toString()} returns {@code "4"}</li>
     * </ul>
     *
     * @return the string representation of the dialect version, never {@code null} or empty
     */
    @Override
    public String toString() {
        return dialect;
    }

}
