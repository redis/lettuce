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

    long aggregationGroups = 1;

    List<SearchReply<K, V>> replies = new ArrayList<>();

    /**
     * Optional Cursor metadata of the shard that created/owns the cursor. Present only when running in cluster mode, WITHCURSOR
     * was used, and the server created a cursor (cursorId > 0).
     */
    private Cursor cursor;

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
     * Returns the optional Cursor metadata for pagination and (in cluster) sticky routing, if applicable.
     *
     * <p>
     * When {@code WITHCURSOR} is used and Redis returns a cursor, this method yields a {@link Cursor} containing the cursor id.
     * In cluster mode, the cursor may additionally carry the node id on which the cursor resides for sticky FT.CURSOR READ/DEL
     * routing.
     * </p>
     *
     * @return an {@link Optional} with {@link Cursor} when a cursor was returned; otherwise an empty Optional.
     */
    public Optional<Cursor> getCursor() {
        return Optional.ofNullable(cursor);
    }

    /**
     * Set the {@link Cursor} metadata carried by this reply. Intended for post-parse stamping of node affinity (cluster mode)
     * or attaching the server-returned cursor id (standalone).
     */
    public void setCursor(Cursor cursor) {
        if (this.cursor == null) {
            this.cursor = cursor;
        }
    }

    void setGroupCount(long value) {
        this.aggregationGroups = value;
    }

    void addSearchReply(SearchReply<K, V> searchReply) {
        this.replies.add(searchReply);
    }

    /**
     * Lightweight cursor handle containing the server-assigned cursor id and optional node id (cluster sticky routing).
     */
    public static class Cursor {

        private final long cursorId;

        private String nodeId;

        private Cursor(long cursorId, String nodeId) {
            this.cursorId = cursorId;
            this.nodeId = nodeId;
        }

        /** Create a new cursor handle with the given cursor id and optional creating node id. */
        public static Cursor of(long cursorId, String nodeId) {
            return new Cursor(cursorId, nodeId);
        }

        /** Return the server-assigned cursor id for continued reads/deletes. */
        public long getCursorId() {
            return cursorId;
        }

        /** Return the node id that created this cursor, if known (cluster routing). */
        public Optional<String> getNodeId() {
            return Optional.ofNullable(nodeId);
        }

        /** Set the node id for this cursor (used for cluster-sticky routing). */
        public void setNodeId(String nodeId) {
            if (this.nodeId == null) {
                this.nodeId = nodeId;
            }
        }

    }

}
