/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import reactor.util.annotation.Nullable;

/**
 * Represents the response from a Redis Search aggregation command (FT.AGGREGATE) or an (FT.CURSOR READ) command. This class
 * encapsulates the results of aggregation operations including grouped data, statistical computations, and cursor-based
 * pagination for large result sets.
 *
 * <p>
 * An aggregation reply contains:
 * </p>
 * <ul>
 * <li>The number of aggregation groups returned</li>
 * <li>A list of {@link SearchReply} objects, each representing an aggregation group or result set</li>
 * <li>An optional cursor ID for pagination when dealing with large aggregation results</li>
 * </ul>
 *
 * <p>
 * Aggregation operations can include:
 * </p>
 * <ul>
 * <li>Grouping by one or more fields</li>
 * <li>Statistical functions (COUNT, SUM, AVG, MIN, MAX, etc.)</li>
 * <li>Sorting and limiting results</li>
 * <li>Filtering and transformations</li>
 * </ul>
 *
 * <p>
 * For cursor-based pagination, when the result set is too large to return in a single response, Redis returns a cursor ID that
 * can be used with FT.CURSOR READ to retrieve subsequent pages.
 * </p>
 *
 * @param <K> the type of keys used in the aggregation results
 * @param <V> the type of values used in the aggregation results
 * @author Redis Ltd.
 * @since 6.8
 * @see SearchReply
 */
public class AggregationReply<K, V> {

    private static final long NO_CURSOR = -1;

    long aggregationGroups = 1;

    List<SearchReply<K, V>> replies = new ArrayList<>();

    long cursorId = NO_CURSOR;

    /**
     * Cluster nodeId of the shard that created/owns the cursor. Non-null only when running in cluster mode, WITHCURSOR was
     * used, and the server created a cursor (cursorId > 0).
     */
    @Nullable
    private String nodeId;

    /**
     * Creates a new empty AggregationReply. The reply is initialized with defaults.
     */
    public AggregationReply() {
    }

    /**
     * Returns the number of aggregation groups in this reply.
     *
     * <p>
     * This value represents:
     * </p>
     * <ul>
     * <li>For grouped aggregations: the number of distinct groups returned</li>
     * <li>For non-grouped aggregations: typically 1, representing the entire result set</li>
     * <li>For empty results: may be 0 or 1 depending on the aggregation type</li>
     * </ul>
     *
     * <p>
     * Note: This count may be different from {@code getReplies().size()} in some cases, particularly when dealing with
     * cursor-based pagination where not all groups are returned in a single response.
     * </p>
     *
     * @return the number of aggregation groups, typically a positive integer
     */
    public long getAggregationGroups() {
        return aggregationGroups;
    }

    /**
     * Returns the list of search replies containing the aggregation results.
     *
     * <p>
     * Each {@link SearchReply} in the list represents:
     * </p>
     * <ul>
     * <li>For grouped aggregations: one aggregation group with its computed values</li>
     * <li>For non-grouped aggregations: typically a single reply containing all results</li>
     * <li>For cursor-based results: the current page of results</li>
     * </ul>
     *
     * <p>
     * The structure of each {@link SearchReply} depends on the aggregation operations performed:
     * </p>
     * <ul>
     * <li>GROUP BY operations create separate replies for each group</li>
     * <li>REDUCE operations add computed fields to each reply</li>
     * <li>LOAD operations include specified fields in the results</li>
     * <li>SORTBY operations determine the order of replies</li>
     * </ul>
     *
     * <p>
     * The returned list is mutable and reflects the current state of the aggregation results. Modifying this list will affect
     * the aggregation reply.
     * </p>
     *
     * @return a mutable list of {@link SearchReply} objects containing the aggregation results. Never {@code null}, but may be
     *         empty if no results were found.
     */
    public List<SearchReply<K, V>> getReplies() {
        return replies;
    }

    /**
     * Returns the cursor ID for pagination, if applicable.
     *
     * <p>
     * The cursor ID is used for paginating through large aggregation result sets that cannot be returned in a single response.
     * When Redis returns a cursor ID, it indicates that there are more results available that can be retrieved using the
     * FT.CURSOR READ command.
     * </p>
     *
     * <p>
     * Cursor behavior:
     * </p>
     * <ul>
     * <li>Returns -1 (NO_CURSOR) when no pagination is needed or available</li>
     * <li>Returns a positive integer when more results are available</li>
     * <li>Returns 0 when this is the last page of a paginated result set</li>
     * </ul>
     *
     * <p>
     * To retrieve the next page of results, use the returned cursor ID with the FT.CURSOR READ command. Continue reading until
     * the cursor ID becomes 0, indicating the end of the result set.
     * </p>
     *
     * <p>
     * Note: Cursors have a timeout and will expire if not used within the configured time limit. Always check for cursor
     * expiration when implementing pagination.
     * </p>
     *
     * @return the cursor ID for pagination. Returns -1 if no cursor is available, 0 if this is the last page, or a positive
     *         integer if more results are available.
     */
    public long getCursorId() {
        return cursorId;
    }

    /** Cluster nodeId that created the cursor, if available. */
    public Optional<String> getNodeId() {
        return Optional.ofNullable(nodeId);
    }

    /**
     * Internal helper to stamp the creating node id onto this reply while keeping the setter package-private. Public for
     * cross-package usage; does not expose the field directly.
     */
    public static <K1, V1> AggregationReply<K1, V1> stampNodeId(AggregationReply<K1, V1> reply, @Nullable String nodeId) {
        reply.setNodeId(nodeId);
        return reply;
    }

    // Package-private setter: only in-package impls may stamp nodeId.
    void setNodeId(@Nullable String nodeId) {
        this.nodeId = nodeId;
    }

    void setGroupCount(long value) {
        this.aggregationGroups = value;
    }

    void setCursorId(long value) {
        this.cursorId = value;
    }

    void addSearchReply(SearchReply<K, V> searchReply) {
        this.replies.add(searchReply);
    }

}
