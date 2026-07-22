/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.annotations.Experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the results of an {@code FT.HYBRID} command. Contains total result count, execution time, warnings, and a list of
 * results.
 * <p>
 * Each result carries the returned fields. The document key is available under the reserved field name {@code __key} when
 * returning individual documents. Score information (text score, vector distance, combined score) is included when using
 * {@code YIELD_SCORE_AS} in the query.
 * </p>
 *
 * @author Aleksandar Todorov
 * @since 7.2
 */
@Experimental
public class HybridReply {

    private long totalResults;

    private double executionTime;

    private final List<HybridResult> results;

    private final List<String> warnings = new ArrayList<>();

    /**
     * Creates a new empty HybridReply instance.
     */
    public HybridReply() {
        this.totalResults = 0;
        this.executionTime = 0;
        this.results = new ArrayList<>();
    }

    /**
     * @return the total number of matching documents reported by the server
     */
    public long getTotalResults() {
        return totalResults;
    }

    /**
     * Set the total number of matching documents.
     *
     * @param totalResults the total number of results
     */
    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }

    /**
     * @return the execution time reported by the server in seconds (or {@code 0.0} if not available)
     */
    public double getExecutionTime() {
        return executionTime;
    }

    /**
     * Set the execution time reported by the server.
     *
     * @param executionTime execution time in seconds
     */
    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }

    /**
     * @return an unmodifiable view of all results returned by the command
     */
    public List<HybridResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    /**
     * Add a new result entry.
     *
     * @param result the result to add
     */
    public void addResult(HybridResult result) {
        this.results.add(result);
    }

    /**
     * @return a read-only view of all warnings reported by the server
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Add a warning message.
     *
     * @param warning the warning to add
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * @return the number of result entries
     */
    public int size() {
        return results.size();
    }

    /**
     * @return {@code true} if no results were returned
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * Represents a single {@code FT.HYBRID} result entry.
     * <p>
     * {@link #getFields()} maps each field name to a {@link FieldValue}, which retains the exact bytes returned by the server
     * and can be read as either text ({@link FieldValue#asString()}) or binary ({@link FieldValue#asBytes()}). This lets a
     * single result mix textual/numeric fields with binary fields such as vector embeddings loaded via {@code LOAD}, where
     * UTF-8 decoding would corrupt the value. The document key is available under the reserved field name {@code __key}.
     */
    public static class HybridResult {

        private final Map<String, FieldValue> fields = new LinkedHashMap<>();

        /**
         * Gets the result fields, mapping each field name to its {@link FieldValue}, in the order returned by the server. Read
         * each value as text via {@link FieldValue#asString()} or as raw bytes via {@link FieldValue#asBytes()}.
         *
         * @return an unmodifiable, ordered map of field name to {@link FieldValue}, or an empty map if not available
         */
        public Map<String, FieldValue> getFields() {
            return Collections.unmodifiableMap(fields);
        }

        /**
         * Adds a single result field.
         *
         * @param key the field name
         * @param value the raw field value
         */
        public void addField(String key, byte[] value) {
            this.fields.put(key, value == null ? FieldValue.NULL : FieldValue.of(value));
        }

    }

}
