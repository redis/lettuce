/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;

import java.util.List;

/**
 * ${intent} for RediSearch functionality
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/search/">RediSearch</a>
 * @since 6.8
 */
public interface RediSearchCommands<K, V> {

    /**
     * Create a new search index with the given name and field definitions using default settings.
     *
     * <p>
     * This command creates a new search index that enables full-text search, filtering, and aggregation capabilities on Redis
     * data structures. The index will use default settings for data type (HASH), key prefixes (all keys), and other
     * configuration options.
     * </p>
     *
     * <p>
     * <strong>Time complexity:</strong> O(K) at creation where K is the number of fields, O(N) if scanning the keyspace is
     * triggered, where N is the number of keys in the keyspace
     * </p>
     *
     * @param index the index name, as a key
     * @param fieldArgs the {@link FieldArgs} list defining the searchable fields and their types
     * @return {@code "OK"} if the index was created successfully
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     * @see CreateArgs
     * @see FieldArgs
     * @see #ftCreate(Object, CreateArgs, List)
     * @see #ftDropindex(Object)
     */
    @Experimental
    String ftCreate(K index, List<FieldArgs<K>> fieldArgs);

    /**
     * Create a new search index with the given name, custom configuration, and field definitions.
     *
     * <p>
     * This command creates a new search index with advanced configuration options that control how the index behaves, what data
     * it indexes, and how it processes documents. This variant provides full control over index creation parameters.
     * </p>
     *
     * <p>
     * The {@link CreateArgs} parameter allows you to specify:
     * </p>
     * <ul>
     * <li><strong>Data type:</strong> HASH (default) or JSON documents</li>
     * <li><strong>Key prefixes:</strong> Which keys to index based on prefix patterns</li>
     * <li><strong>Filters:</strong> Conditional indexing based on field values</li>
     * <li><strong>Language settings:</strong> Default language and language field for stemming</li>
     * <li><strong>Performance options:</strong> NOOFFSETS, NOHL, NOFIELDS, NOFREQS for memory optimization</li>
     * <li><strong>Temporary indexes:</strong> Auto-expiring indexes for short-term use</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(K) at creation where K is the number of fields, O(N) if scanning the keyspace is
     * triggered, where N is the number of keys in the keyspace
     * </p>
     *
     * @param index the index name, as a key
     * @param arguments the index {@link CreateArgs} containing configuration options
     * @param fieldArgs the {@link FieldArgs} list defining the searchable fields and their types
     * @return {@code "OK"} if the index was created successfully
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     * @see CreateArgs
     * @see FieldArgs
     * @see #ftCreate(Object, List)
     * @see #ftDropindex(Object)
     */
    @Experimental
    String ftCreate(K index, CreateArgs<K, V> arguments, List<FieldArgs<K>> fieldArgs);

    /**
     * Add an alias to a search index.
     *
     * <p>
     * This command creates an alias that points to an existing search index, allowing applications to reference the index by an
     * alternative name. Aliases provide a level of indirection that enables transparent index management and migration
     * strategies.
     * </p>
     *
     * <p>
     * Key features and use cases:
     * </p>
     * <ul>
     * <li><strong>Index abstraction:</strong> Applications can use stable alias names while underlying indexes change</li>
     * <li><strong>Blue-green deployments:</strong> Switch traffic between old and new indexes seamlessly</li>
     * <li><strong>A/B testing:</strong> Route different application instances to different indexes</li>
     * <li><strong>Maintenance windows:</strong> Redirect queries during index rebuilds or migrations</li>
     * </ul>
     *
     * <p>
     * <strong>Important notes:</strong>
     * </p>
     * <ul>
     * <li>An index can have multiple aliases, but an alias can only point to one index</li>
     * <li>Aliases cannot reference other aliases (no alias chaining)</li>
     * <li>If the alias already exists, this command will fail with an error</li>
     * <li>Use {@link #ftAliasupdate(Object, Object)} to reassign an existing alias</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1)
     * </p>
     *
     * @param alias the alias name to create
     * @param index the target index name that the alias will point to
     * @return {@code "OK"} if the alias was successfully created
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.aliasadd/">FT.ALIASADD</a>
     * @see #ftAliasupdate(Object, Object)
     * @see #ftAliasdel(Object)
     */
    @Experimental
    String ftAliasadd(K alias, K index);

    /**
     * Update an existing alias to point to a different search index.
     *
     * <p>
     * This command updates an existing alias to point to a different index, or creates the alias if it doesn't exist. Unlike
     * {@link #ftAliasadd(Object, Object)}, this command will succeed even if the alias already exists, making it useful for
     * atomic alias updates during index migrations.
     * </p>
     *
     * <p>
     * Key features and use cases:
     * </p>
     * <ul>
     * <li><strong>Atomic updates:</strong> Change alias target without downtime</li>
     * <li><strong>Index migration:</strong> Seamlessly switch from old to new index versions</li>
     * <li><strong>Rollback capability:</strong> Quickly revert to previous index if issues arise</li>
     * <li><strong>Blue-green deployments:</strong> Switch production traffic between index versions</li>
     * </ul>
     *
     * <p>
     * <strong>Important notes:</strong>
     * </p>
     * <ul>
     * <li>If the alias doesn't exist, it will be created (same as {@code ftAliasadd})</li>
     * <li>If the alias exists, it will be updated to point to the new index</li>
     * <li>The previous index association is removed automatically</li>
     * <li>This operation is atomic - no intermediate state where alias is undefined</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1)
     * </p>
     *
     * @param alias the alias name to update or create
     * @param index the target index name that the alias will point to
     * @return {@code "OK"} if the alias was successfully updated
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.aliasupdate/">FT.ALIASUPDATE</a>
     * @see #ftAliasadd(Object, Object)
     * @see #ftAliasdel(Object)
     */
    @Experimental
    String ftAliasupdate(K alias, K index);

    /**
     * Remove an alias from a search index.
     *
     * <p>
     * This command removes an existing alias, breaking the association between the alias name and its target index. The
     * underlying index remains unchanged and accessible by its original name.
     * </p>
     *
     * <p>
     * Key features and use cases:
     * </p>
     * <ul>
     * <li><strong>Cleanup:</strong> Remove unused or obsolete aliases</li>
     * <li><strong>Security:</strong> Revoke access to indexes through specific alias names</li>
     * <li><strong>Maintenance:</strong> Temporarily disable access during maintenance windows</li>
     * <li><strong>Resource management:</strong> Clean up aliases before index deletion</li>
     * </ul>
     *
     * <p>
     * <strong>Important notes:</strong>
     * </p>
     * <ul>
     * <li>Only the alias is removed - the target index is not affected</li>
     * <li>If the alias doesn't exist, this command will fail with an error</li>
     * <li>Applications using the alias will receive errors after deletion</li>
     * <li>Consider using {@link #ftAliasupdate(Object, Object)} to redirect before deletion</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1)
     * </p>
     *
     * @param alias the alias name to remove
     * @return {@code "OK"} if the alias was successfully removed
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.aliasdel/">FT.ALIASDEL</a>
     * @see #ftAliasadd(Object, Object)
     * @see #ftAliasupdate(Object, Object)
     */
    @Experimental
    String ftAliasdel(K alias);

    /**
     * Add new attributes to an existing search index.
     *
     * <p>
     * This command allows you to extend an existing search index by adding new searchable fields without recreating the entire
     * index. The new attributes will be applied to future document updates and can optionally be applied to existing documents
     * through reindexing.
     * </p>
     *
     * <p>
     * Key features and considerations:
     * </p>
     * <ul>
     * <li><strong>Non-destructive:</strong> Existing index structure and data remain intact</li>
     * <li><strong>Incremental indexing:</strong> New fields are indexed as documents are updated</li>
     * <li><strong>Reindexing control:</strong> Option to skip initial scan for performance</li>
     * <li><strong>Field limitations:</strong> Text field limits may apply based on index creation options</li>
     * </ul>
     *
     * <p>
     * <strong>Important notes:</strong>
     * </p>
     * <ul>
     * <li>If the index was created without {@code MAXTEXTFIELDS}, you may be limited to 32 total text attributes</li>
     * <li>New attributes are only indexed for documents that are updated after the ALTER command</li>
     * <li>Use {@code SKIPINITIALSCAN} to avoid scanning existing documents if immediate indexing is not required</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(N) where N is the number of keys in the keyspace if initial scan is performed, O(1)
     * if {@code SKIPINITIALSCAN} is used
     * </p>
     *
     * @param index the index name, as a key
     * @param skipInitialScan if {@code true}, skip scanning and indexing existing documents; if {@code false}, scan and index
     *        existing documents with the new attributes
     * @param fieldArgs the {@link FieldArgs} list defining the new searchable fields and their types to add
     * @return {@code "OK"} if the index was successfully altered
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.alter/">FT.ALTER</a>
     * @see FieldArgs
     * @see #ftCreate(Object, List)
     * @see #ftCreate(Object, CreateArgs, List)
     */
    @Experimental
    String ftAlter(K index, boolean skipInitialScan, List<FieldArgs<K>> fieldArgs);

    /**
     * Add new attributes to an existing search index.
     *
     * <p>
     * This command allows you to extend an existing search index by adding new searchable fields without recreating the entire
     * index. The new attributes will be applied to future document updates and can optionally be applied to existing documents
     * through reindexing.
     * </p>
     *
     * <p>
     * Key features and considerations:
     * </p>
     * <ul>
     * <li><strong>Non-destructive:</strong> Existing index structure and data remain intact</li>
     * <li><strong>Incremental indexing:</strong> New fields are indexed as documents are updated</li>
     * <li><strong>Reindexing control:</strong> Option to skip initial scan for performance</li>
     * <li><strong>Field limitations:</strong> Text field limits may apply based on index creation options</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(N) where N is the number of keys in the keyspace if initial scan is performed
     * </p>
     *
     * @param index the index name, as a key
     * @param fieldArgs the {@link FieldArgs} list defining the new searchable fields and their types to add
     * @return {@code "OK"} if the index was successfully altered
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.alter/">FT.ALTER</a>
     * @see FieldArgs
     * @see #ftCreate(Object, List)
     * @see #ftCreate(Object, CreateArgs, List)
     */
    @Experimental
    String ftAlter(K index, List<FieldArgs<K>> fieldArgs);

    /**
     * Return a distinct set of values indexed in a Tag field.
     *
     * <p>
     * This command retrieves all unique values that have been indexed in a specific Tag field within a search index. It's
     * particularly useful for discovering the range of values available in categorical fields such as cities, categories,
     * status values, or any other enumerated data.
     * </p>
     *
     * <p>
     * Key features and use cases:
     * </p>
     * <ul>
     * <li><strong>Data exploration:</strong> Discover all possible values in a tag field</li>
     * <li><strong>Filter building:</strong> Populate dropdown lists or filter options in applications</li>
     * <li><strong>Data validation:</strong> Verify expected values are present in the index</li>
     * <li><strong>Analytics:</strong> Understand the distribution of categorical data</li>
     * </ul>
     *
     * <p>
     * <strong>Important limitations:</strong>
     * </p>
     * <ul>
     * <li>Only works with Tag fields defined in the index schema</li>
     * <li>No paging or sorting is provided - all values are returned at once</li>
     * <li>Tags are not alphabetically sorted in the response</li>
     * <li>Returned strings are lowercase with whitespaces removed</li>
     * <li>Performance scales with the number of unique values (O(N) complexity)</li>
     * </ul>
     *
     * <p>
     * <strong>Example usage scenarios:</strong>
     * </p>
     * <ul>
     * <li>Retrieving all available product categories for an e-commerce filter</li>
     * <li>Getting all city names indexed for location-based searches</li>
     * <li>Listing all status values (active, inactive, pending) for administrative interfaces</li>
     * <li>Discovering all tags or labels applied to content</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(N) where N is the number of distinct values in the tag field
     * </p>
     *
     * @param index the index name containing the tag field
     * @param fieldName the name of the Tag field defined in the index schema
     * @return a list of all distinct values indexed in the specified tag field. The list contains the raw tag values as they
     *         were indexed (lowercase, whitespace removed).
     * @since 7.0
     * @see <a href="https://redis.io/docs/latest/commands/ft.tagvals/">FT.TAGVALS</a>
     * @see #ftCreate(Object, List)
     * @see #ftCreate(Object, CreateArgs, List)
     */
    @Experimental
    List<V> ftTagvals(K index, K fieldName);

    /**
     * Drop a search index without deleting the associated documents.
     *
     * <p>
     * This command removes the search index and all its associated metadata, but preserves the original documents (hashes or
     * JSON objects) that were indexed. This is the safe default behavior that allows you to recreate the index later without
     * losing data.
     * </p>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1)
     * </p>
     *
     * @param index the index name, as a key
     * @return {@code "OK"} if the index was successfully dropped
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.dropindex/">FT.DROPINDEX</a>
     * @see #ftDropindex(Object, boolean)
     * @see #ftCreate(Object, List)
     */
    @Experimental
    String ftDropindex(K index);

    /**
     * Drop a search index with optional document deletion.
     *
     * <p>
     * This command removes the search index and optionally deletes all associated documents. When {@code deleteDocuments} is
     * {@code true}, this operation becomes destructive and will permanently remove both the index and all indexed documents
     * from Redis.
     * </p>
     *
     * <p>
     * <strong>Asynchronous Behavior:</strong> If an index creation is still running ({@link #ftCreate(Object, List)} is running
     * asynchronously), only the document hashes that have already been indexed are deleted. Documents that are queued for
     * indexing but not yet processed will remain in the database.
     * </p>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1) or O(N) if documents are deleted, where N is the number of keys in the keyspace
     * </p>
     *
     * @param index the index name, as a key
     * @param deleteDocuments if {@code true}, delete the indexed documents as well; if {@code false}, preserve documents
     * @return {@code "OK"} if the index was successfully dropped
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.dropindex/">FT.DROPINDEX</a>
     * @see #ftDropindex(Object)
     * @see #ftCreate(Object, List)
     */
    @Experimental
    String ftDropindex(K index, boolean deleteDocuments);

    /**
     * Search the index with a textual query using default search options.
     *
     * <p>
     * This command performs a full-text search on the specified index using the provided query string. It returns matching
     * documents with their content and metadata. This is the basic search variant that uses default search behavior without
     * additional filtering, sorting, or result customization.
     * </p>
     *
     * <p>
     * The query follows RediSearch query syntax, supporting:
     * </p>
     * <ul>
     * <li><strong>Simple text search:</strong> {@code "hello world"} - searches for documents containing both terms</li>
     * <li><strong>Field-specific search:</strong> {@code "@title:redis"} - searches within specific fields</li>
     * <li><strong>Boolean operators:</strong> {@code "redis AND search"} or {@code "redis | search"}</li>
     * <li><strong>Phrase search:</strong> {@code "\"exact phrase\""} - searches for exact phrase matches</li>
     * <li><strong>Wildcard search:</strong> {@code "redi*"} - prefix matching</li>
     * <li><strong>Numeric ranges:</strong> {@code "@price:[100 200]"} - numeric field filtering</li>
     * <li><strong>Geographic search:</strong> {@code "@location:[lon lat radius unit]"} - geo-spatial queries</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(N) where N is the number of results in the result set
     * </p>
     *
     * @param index the index name, as a key
     * @param query the query string following RediSearch query syntax
     * @return the result of the search command containing matching documents, see {@link SearchReply}
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.search/">FT.SEARCH</a>
     * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/query/">Query syntax</a>
     * @see SearchReply
     * @see SearchArgs
     * @see #ftSearch(Object, Object, SearchArgs)
     */
    @Experimental
    SearchReply<K, V> ftSearch(K index, V query);

    /**
     * Search the index with a textual query using advanced search options and filters.
     *
     * <p>
     * This command performs a full-text search on the specified index with advanced configuration options provided through
     * {@link SearchArgs}. This variant allows fine-grained control over search behavior, result formatting, filtering, sorting,
     * and pagination.
     * </p>
     *
     * <p>
     * The {@link SearchArgs} parameter enables you to specify:
     * </p>
     * <ul>
     * <li><strong>Result options:</strong> NOCONTENT, WITHSCORES, WITHPAYLOADS, WITHSORTKEYS</li>
     * <li><strong>Query behavior:</strong> VERBATIM (no stemming), NOSTOPWORDS</li>
     * <li><strong>Filtering:</strong> Numeric filters, geo filters, field filters</li>
     * <li><strong>Result customization:</strong> RETURN specific fields, SUMMARIZE, HIGHLIGHT</li>
     * <li><strong>Sorting and pagination:</strong> SORTBY, LIMIT offset and count</li>
     * <li><strong>Performance options:</strong> TIMEOUT, SLOP, INORDER</li>
     * <li><strong>Language and scoring:</strong> LANGUAGE, SCORER, EXPLAINSCORE</li>
     * </ul>
     *
     * <h3>Performance Considerations:</h3>
     * <ul>
     * <li>Use NOCONTENT when you only need document IDs</li>
     * <li>Specify RETURN fields to limit data transfer</li>
     * <li>Use SORTABLE fields for efficient sorting</li>
     * <li>Apply filters to reduce result set size</li>
     * <li>Use LIMIT for pagination to avoid large result sets</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(N) where N is the number of results in the result set. Complexity varies based on
     * query type, filters, and sorting requirements.
     * </p>
     *
     * @param index the index name, as a key
     * @param query the query string following RediSearch query syntax
     * @param args the search arguments containing advanced options and filters
     * @return the result of the search command containing matching documents and metadata, see {@link SearchReply}
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.search/">FT.SEARCH</a>
     * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/query/">Query syntax</a>
     * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/">Advanced concepts</a>
     * @see SearchReply
     * @see SearchArgs
     * @see #ftSearch(Object, Object)
     */
    @Experimental
    SearchReply<K, V> ftSearch(K index, V query, SearchArgs<K, V> args);

    /**
     * Run a search query on an index and perform basic aggregate transformations using default options.
     *
     * <p>
     * This command executes a search query and applies aggregation operations to transform and analyze the results. Unlike
     * {@link #ftSearch(Object, Object)}, which returns individual documents, FT.AGGREGATE processes the result set through a
     * pipeline of transformations to produce analytical insights, summaries, and computed values.
     * </p>
     *
     * <p>
     * This basic variant uses default aggregation behavior without additional pipeline operations. For advanced aggregations
     * with grouping, sorting, filtering, and custom transformations, use {@link #ftAggregate(Object, Object, AggregateArgs)}.
     * </p>
     *
     * <p>
     * Common use cases for aggregations include:
     * </p>
     * <ul>
     * <li><strong>Analytics:</strong> Count documents, calculate averages, find min/max values</li>
     * <li><strong>Reporting:</strong> Group data by categories, time periods, or geographic regions</li>
     * <li><strong>Data transformation:</strong> Apply mathematical functions, format dates, extract values</li>
     * <li><strong>Performance optimization:</strong> Process large datasets server-side instead of client-side</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1) base complexity, but depends on the query and number of results processed
     * </p>
     *
     * @param index the index name, as a key
     * @param query the base filtering query that retrieves documents for aggregation
     * @return the result of the aggregate command containing processed results, see {@link SearchReply}
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.aggregate/">FT.AGGREGATE</a>
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/">Aggregations</a>
     * @see SearchReply
     * @see AggregateArgs
     * @see #ftAggregate(Object, Object, AggregateArgs)
     */
    @Experimental
    AggregationReply<K, V> ftAggregate(K index, V query);

    /**
     * Run a search query on an index and perform advanced aggregate transformations with a processing pipeline.
     *
     * <p>
     * This command executes a search query and applies a sophisticated aggregation pipeline to transform, group, sort, and
     * analyze the results. The {@link AggregateArgs} parameter defines a series of operations that process the data
     * server-side, enabling powerful analytics and data transformation capabilities directly within Redis.
     * </p>
     *
     * <p>
     * The aggregation pipeline supports the following operations:
     * </p>
     * <ul>
     * <li><strong>LOAD:</strong> Load specific document attributes for processing</li>
     * <li><strong>GROUPBY:</strong> Group results by one or more properties</li>
     * <li><strong>REDUCE:</strong> Apply reduction functions (COUNT, SUM, AVG, MIN, MAX, etc.)</li>
     * <li><strong>SORTBY:</strong> Sort results by specified properties</li>
     * <li><strong>APPLY:</strong> Apply mathematical expressions and transformations</li>
     * <li><strong>FILTER:</strong> Filter results based on computed values</li>
     * <li><strong>LIMIT:</strong> Paginate results efficiently</li>
     * <li><strong>WITHCURSOR:</strong> Enable cursor-based pagination for large result sets</li>
     * </ul>
     *
     * <h3>Performance Considerations:</h3>
     * <ul>
     * <li>Use SORTABLE fields for efficient grouping and sorting operations</li>
     * <li>Apply filters early in the pipeline to reduce processing overhead</li>
     * <li>Use WITHCURSOR for large result sets to avoid memory issues</li>
     * <li>Load only necessary attributes to minimize data transfer</li>
     * <li>Consider using LIMIT to restrict result set size</li>
     * </ul>
     *
     * <p>
     * <strong>Time complexity:</strong> Non-deterministic, depends on the query and aggregation operations performed. Generally
     * linear to the number of results processed through the pipeline.
     * </p>
     *
     * @param index the index name, as a key
     * @param query the base filtering query that retrieves documents for aggregation
     * @param args the aggregate arguments defining the processing pipeline and operations
     * @return the result of the aggregate command containing processed and transformed results, see {@link SearchReply}
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.aggregate/">FT.AGGREGATE</a>
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/">Aggregations</a>
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/#cursor-api">Cursor
     *      API</a>
     * @see SearchReply
     * @see AggregateArgs
     * @see #ftAggregate(Object, Object)
     * @see #ftCursorread(Object, long)
     */
    @Experimental
    AggregationReply<K, V> ftAggregate(K index, V query, AggregateArgs<K, V> args);

    /**
     * Read next results from an existing cursor.
     *
     * <p>
     * This command is used to read the next batch of results from a cursor created by
     * {@link #ftAggregate(Object, Object, AggregateArgs)} with the {@code WITHCURSOR} option. Cursors provide an efficient way
     * to iterate through large result sets without loading all results into memory at once.
     * </p>
     *
     * <p>
     * The {@code count} parameter overrides the {@code COUNT} value specified in the original {@code FT.AGGREGATE} command,
     * allowing you to control the batch size for this specific read operation.
     * </p>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1)
     * </p>
     *
     * @param index the index name, as a key
     * @param cursorId the cursor id obtained from a previous {@code FT.AGGREGATE} or {@code FT.CURSOR READ} command
     * @param count the number of results to read. This parameter overrides the {@code COUNT} specified in {@code FT.AGGREGATE}
     * @return the result of the cursor read command containing the next batch of results and potentially a new cursor id, see
     *         {@link SearchReply}
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.cursor-read/">FT.CURSOR READ</a>
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/#cursor-api">Cursor
     *      API</a>
     * @see SearchReply
     * @see #ftAggregate(Object, Object, AggregateArgs)
     */
    @Experimental
    AggregationReply<K, V> ftCursorread(K index, long cursorId, int count);

    /**
     * Read next results from an existing cursor using the default batch size.
     *
     * <p>
     * This command is used to read the next batch of results from a cursor created by
     * {@link #ftAggregate(Object, Object, AggregateArgs)} with the {@code WITHCURSOR} option. This variant uses the default
     * batch size that was specified in the original {@code FT.AGGREGATE} command's {@code WITHCURSOR} clause.
     * </p>
     *
     * <p>
     * Cursors provide an efficient way to iterate through large result sets without loading all results into memory at once.
     * When the cursor is exhausted (no more results), the returned {@link SearchReply} will have a cursor id of 0.
     * </p>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1)
     * </p>
     *
     * @param index the index name, as a key
     * @param cursorId the cursor id obtained from a previous {@code FT.AGGREGATE} or {@code FT.CURSOR READ} command
     * @return the result of the cursor read command containing the next batch of results and potentially a new cursor id, see
     *         {@link SearchReply}
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.cursor-read/">FT.CURSOR READ</a>
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/#cursor-api">Cursor
     *      API</a>
     * @see SearchReply
     * @see #ftAggregate(Object, Object, AggregateArgs)
     */
    @Experimental
    AggregationReply<K, V> ftCursorread(K index, long cursorId);

    /**
     * Delete a cursor and free its associated resources.
     *
     * <p>
     * This command is used to explicitly delete a cursor created by {@link #ftAggregate(Object, Object, AggregateArgs)} with
     * the {@code WITHCURSOR} option. Deleting a cursor frees up server resources and should be done when you no longer need to
     * read more results from the cursor.
     * </p>
     *
     * <p>
     * <strong>Important:</strong> Cursors have a default timeout and will be automatically deleted by Redis if not accessed
     * within the timeout period. However, it's good practice to explicitly delete cursors when you're finished with them to
     * free up resources immediately.
     * </p>
     *
     * <p>
     * Once a cursor is deleted, any subsequent attempts to read from it using {@link #ftCursorread(Object, long)} or
     * {@link #ftCursorread(Object, long, int)} will result in an error.
     * </p>
     *
     * <p>
     * <strong>Time complexity:</strong> O(1)
     * </p>
     *
     * @param index the index name, as a key
     * @param cursorId the cursor id obtained from a previous {@code FT.AGGREGATE} or {@code FT.CURSOR READ} command
     * @return {@code "OK"} if the cursor was successfully deleted
     * @since 6.8
     * @see <a href="https://redis.io/docs/latest/commands/ft.cursor-del/">FT.CURSOR DEL</a>
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/aggregations/#cursor-api">Cursor
     *      API</a>
     * @see #ftAggregate(Object, Object, AggregateArgs)
     * @see #ftCursorread(Object, long)
     * @see #ftCursorread(Object, long, int)
     */
    @Experimental
    String ftCursordel(K index, long cursorId);

}
