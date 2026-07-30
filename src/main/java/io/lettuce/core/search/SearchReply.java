/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.annotations.Experimental;

/**
 * Represents the results of a Redis FT.SEARCH command.
 * <p>
 * This class encapsulates the search results including the total count of matching documents and a list of individual search
 * result documents. Each document contains the document ID and optionally the document fields, score, payload, and sort keys
 * depending on the search arguments used.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 * @see <a href="https://redis.io/docs/latest/commands/ft.search/">FT.SEARCH</a>
 */
public class SearchReply<K, V> {

    private long count;

    private final List<SearchResult<K, V>> results;

    private Long cursorId;

    private final List<V> warnings = new ArrayList<>();

    /**
     * Creates a new empty SearchReply instance.
     */
    public SearchReply() {
        this.count = 0;
        this.results = new ArrayList<>();
        this.cursorId = null;
    }

    /**
     * Creates a new SearchReply instance with the specified count and results.
     *
     * @param count the total number of matching documents
     * @param results the list of search result documents
     */
    SearchReply(long count, List<SearchResult<K, V>> results) {
        this.count = count;
        this.results = new ArrayList<>(results);
        this.cursorId = null;
    }

    /**
     * Gets the total number of matching documents.
     * <p>
     * This represents the total count of documents that match the search query, which may be larger than the number of results
     * returned if LIMIT was used.
     *
     * @return the total number of matching documents
     */
    public long getCount() {
        return count;
    }

    /**
     * Sets the total number of matching documents.
     *
     * @param count the total number of matching documents
     */
    void setCount(long count) {
        this.count = count;
    }

    /**
     * Gets the list of search result documents.
     * <p>
     * Each result contains the document ID and optionally the document fields, score, payload, and sort keys depending on the
     * search arguments used.
     *
     * @return an unmodifiable list of search result documents
     */
    public List<SearchResult<K, V>> getResults() {
        return Collections.unmodifiableList(results);
    }

    /**
     * Adds a search result document to the results list.
     *
     * @param result the search result document to add
     */
    public void addResult(SearchResult<K, V> result) {
        this.results.add(result);
    }

    /**
     * Gets the number of search result documents returned.
     * <p>
     * This may be different from {@link #getCount()} if LIMIT was used in the search.
     *
     * @return the number of search result documents returned
     */
    public int size() {
        return results.size();
    }

    /**
     * Checks if the search results are empty.
     *
     * @return true if no search result documents were returned, false otherwise
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * Gets the cursor ID for paginated results.
     * <p>
     * This is only available when using cursor-based pagination with FT.AGGREGATE WITHCURSOR. A cursor ID of 0 indicates that
     * there are no more results to fetch.
     *
     * @return the cursor ID, or null if cursor-based pagination is not being used
     */
    public Long getCursorId() {
        return cursorId;
    }

    /**
     * @return a {@link List} of all the warnings generated during the execution of this search
     */
    public List<V> getWarnings() {
        return this.warnings;
    }

    /**
     * Sets the cursor ID for paginated results.
     *
     * @param cursorId the cursor ID
     */
    void setCursorId(Long cursorId) {
        this.cursorId = cursorId;
    }

    /**
     * Add a new warning to the list of warnings
     *
     * @param v the warning to add
     */
    void addWarning(V v) {
        this.warnings.add(v);
    }

    /**
     * Represents a single search result document.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    public static class SearchResult<K, V> {

        private final K id;

        private Double score;

        private V payload;

        private V sortKey;

        private final Map<K, V> fields = new HashMap<>();

        /**
         * Creates a new SearchResult with the specified document ID.
         *
         * @param id the document ID
         */
        public SearchResult(K id) {
            this.id = id;
        }

        public SearchResult() {
            this.id = null;
        }

        /**
         * Gets the document ID.
         *
         * @return the document ID
         */
        public K getId() {
            return id;
        }

        /**
         * Gets the document score.
         * <p>
         * This is only available if WITHSCORES was used in the search.
         *
         * @return the document score, or null if not available
         */
        public Double getScore() {
            return score;
        }

        /**
         * Sets the document score.
         *
         * @param score the document score
         */
        void setScore(Double score) {
            this.score = score;
        }

        /**
         * Gets the document payload.
         * <p>
         * This is only available if WITHPAYLOADS was used in the search.
         *
         * @return the document payload, or null if not available
         */
        public V getPayload() {
            return payload;
        }

        /**
         * Sets the document payload.
         *
         * @param payload the document payload
         */
        void setPayload(V payload) {
            this.payload = payload;
        }

        /**
         * Gets the sort key.
         * <p>
         * This is only available if WITHSORTKEYS was used in the search.
         *
         * @return the sort key, or null if not available
         */
        public V getSortKey() {
            return sortKey;
        }

        /**
         * Sets the sort key.
         *
         * @param sortKey the sort key
         */
        void setSortKey(V sortKey) {
            this.sortKey = sortKey;
        }

        /**
         * Gets the document fields.
         * <p>
         * This contains the field names and values of the document. If NOCONTENT was used in the search, this will be null or
         * empty.
         * <p>
         * Values are typically scalars of the codec's value type. Aggregation reducers that produce non-scalar columns (for
         * example {@code FT.AGGREGATE REDUCE COLLECT}) store nested {@link List}/{@link Map} structures in the value position
         * of this map; reading such a field through a typed reference (for example {@code String v = fields.get("top")}, or
         * iterating {@code fields.values()} with a typed loop variable) throws {@link ClassCastException}. Read those columns
         * as {@code Object}, or use {@link #getCollectedEntries(Object)} which also normalizes the protocol-specific shape of
         * collected entries.
         *
         * @return the document fields, or null if not available
         */
        public Map<K, V> getFields() {
            return fields;
        }

        /**
         * Gets the entries of a collected aggregation column in a protocol-independent shape.
         * <p>
         * The {@code FT.AGGREGATE REDUCE COLLECT} reducer produces a nested column value — an array with one element per
         * collected row — whose raw shape in {@link #getFields()} differs by protocol: RESP3 decodes each entry as a
         * {@link Map} while RESP2 decodes it as a flat key/value {@link List}. This accessor normalizes both shapes to a list
         * of maps, one map per collected entry, preserving the entry and field order returned by the server.
         * <p>
         * <strong>Experimental.</strong> The underlying {@code COLLECT} reducer is an experimental Redis feature; this method
         * may change together with it.
         *
         * @param field name of the collected column (the reducer alias), must not be {@code null}.
         * @return one map per collected entry, in server order; an empty list if the field is absent.
         * @throws IllegalArgumentException if the field does not hold a collected structure (for example a scalar column).
         * @since 7.7
         * @see io.lettuce.core.search.arguments.AggregateArgs.Reducer#collect()
         */
        @Experimental
        @SuppressWarnings("unchecked")
        public List<Map<K, V>> getCollectedEntries(K field) {
            Object value = fields.get(field);
            if (value == null) {
                return Collections.emptyList();
            }
            if (!(value instanceof List)) {
                throw new IllegalArgumentException(
                        "Field '" + field + "' does not hold a collected structure but " + value.getClass().getName());
            }

            List<Map<K, V>> entries = new ArrayList<>();
            for (Object entry : (List<Object>) value) {
                if (entry instanceof Map) {
                    entries.add((Map<K, V>) entry);
                } else if (entry instanceof List) {
                    List<Object> flat = (List<Object>) entry;
                    Map<K, V> entryMap = new LinkedHashMap<>();
                    for (int i = 0; i + 1 < flat.size(); i += 2) {
                        entryMap.put((K) flat.get(i), (V) flat.get(i + 1));
                    }
                    entries.add(entryMap);
                } else {
                    throw new IllegalArgumentException("Field '" + field + "' does not hold a collected structure, entry is "
                            + entry.getClass().getName());
                }
            }
            return entries;
        }

        /**
         * Adds all the provided fields
         *
         * @param fields the document fields
         */
        public void addFields(Map<K, V> fields) {
            this.fields.putAll(fields);
        }

        /**
         * Adds a single document field
         *
         * @param key the field name
         * @param value the field value
         */
        public void addFields(K key, V value) {
            this.fields.put(key, value);
        }

    }

}
