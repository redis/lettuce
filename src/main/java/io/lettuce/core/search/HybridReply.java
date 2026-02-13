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
import java.util.List;
import java.util.Map;

/**
 * Represents the results of an {@code FT.HYBRID} command. Contains total result count, execution time, warnings, and a list of
 * result field maps.
 * <p>
 * Each result is a {@link Map} of field names to values. The document key is available under the reserved field name
 * {@code __key} when returning individual documents. Score information (text score, vector distance, combined score) is
 * included when using {@code YIELD_SCORE_AS} in the query.
 * </p>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 */
@Experimental
public class HybridReply<K, V> {

    private long totalResults;

    private double executionTime;

    private final List<Map<K, V>> results;

    private final List<V> warnings = new ArrayList<>();

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
     * @return an unmodifiable view of all results returned by the command. Each result is a map of field names to values.
     */
    public List<Map<K, V>> getResults() {
        return Collections.unmodifiableList(results);
    }

    /**
     * Add a new result entry.
     *
     * @param result the result map to add
     */
    public void addResult(Map<K, V> result) {
        this.results.add(result);
    }

    /**
     * @return a read-only view of all warnings reported by the server
     */
    public List<V> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Add a warning message.
     *
     * @param warning the warning to add
     */
    public void addWarning(V warning) {
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

}
