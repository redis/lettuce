/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.annotations.Experimental;

import java.nio.charset.StandardCharsets;
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
     * Field values are stored as the raw bytes returned by the server. {@link #getFields()} exposes them decoded as UTF-8
     * {@link String}s, which suits textual and numeric fields; binary fields (for example vector embeddings loaded via
     * {@code LOAD}) should be read via {@link #getFieldBytes(String)}, which preserves the exact bytes.
     */
    public static class HybridResult {

        private final Map<String, byte[]> rawFields = new LinkedHashMap<>();

        private Map<String, String> fields;

        /**
         * Gets the result fields decoded as UTF-8 text.
         * <p>
         * Binary field values (for example vector embeddings) are not valid UTF-8; read those via
         * {@link #getFieldBytes(String)} instead.
         *
         * @return the result fields, or an empty map if not available
         */
        public Map<String, String> getFields() {
            if (fields == null) {
                Map<String, String> decoded = new LinkedHashMap<>(rawFields.size());
                rawFields.forEach(
                        (key, value) -> decoded.put(key, value == null ? null : new String(value, StandardCharsets.UTF_8)));
                fields = decoded;
            }
            return fields;
        }

        /**
         * Gets the raw bytes of a single result field, exactly as returned by the server. Use this accessor for binary fields
         * such as vector embeddings, where UTF-8 decoding would corrupt the value.
         *
         * @param name the field name
         * @return the raw field value, or {@code null} if the field is not present
         */
        public byte[] getFieldBytes(String name) {
            return rawFields.get(name);
        }

        /**
         * Adds a single result field.
         *
         * @param key the field name
         * @param value the raw field value
         */
        public void addField(String key, byte[] value) {
            this.rawFields.put(key, value);
            this.fields = null;
        }

    }

}
