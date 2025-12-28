/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the information and statistics returned by the FT.INFO command for a RediSearch index.
 * <p>
 * This class encapsulates comprehensive details about a search index including its configuration, schema definition, memory
 * usage, indexing progress, and performance metrics. The information is organized into several categories:
 * </p>
 * <ul>
 * <li><strong>General information:</strong> index name, options, definition, schema attributes, document counts</li>
 * <li><strong>Size statistics:</strong> memory usage for various index components (inverted index, vectors, documents,
 * etc.)</li>
 * <li><strong>Indexing statistics:</strong> indexing progress, failures, and timing information</li>
 * <li><strong>Garbage collection:</strong> GC cycles, bytes collected, and timing metrics</li>
 * <li><strong>Cursor statistics:</strong> information about active cursors for pagination</li>
 * <li><strong>Dialect statistics:</strong> usage counts for different query dialects</li>
 * <li><strong>Error statistics:</strong> indexing failures and errors per field</li>
 * </ul>
 *
 * @author Julien Ruaux
 * @since 6.8
 * @see <a href="https://redis.io/docs/latest/commands/ft.info/">FT.INFO</a>
 */
public class IndexInfo {

    private String indexName;

    private final List<String> indexOptions = new ArrayList<>();

    private final Map<String, Object> indexDefinition = new HashMap<>();

    private final List<Map<String, Object>> attributes = new ArrayList<>();

    private Long numDocs;

    private Long maxDocId;

    private Long numTerms;

    private Long numRecords;

    private final Map<String, Object> sizeStatistics = new HashMap<>();

    private final Map<String, Object> indexingStatistics = new HashMap<>();

    private final Map<String, Object> gcStatistics = new HashMap<>();

    private final Map<String, Object> cursorStatistics = new HashMap<>();

    private final Map<String, Long> dialectStatistics = new HashMap<>();

    private final Map<String, Object> indexErrors = new HashMap<>();

    private final List<Map<String, Object>> fieldStatistics = new ArrayList<>();

    /**
     * Creates a new empty IndexInfo instance.
     */
    public IndexInfo() {
    }

    /**
     * Gets the index name.
     *
     * @return the index name
     */
    public String getIndexName() {
        return indexName;
    }

    void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Gets the index options that were specified during index creation (e.g., FILTER, LANGUAGE, etc.).
     *
     * @return an unmodifiable list of index options
     */
    public List<String> getIndexOptions() {
        return Collections.unmodifiableList(indexOptions);
    }

    void addIndexOption(String option) {
        this.indexOptions.add(option);
    }

    /**
     * Gets the index definition including key_type (HASH or JSON), prefixes, and default_score.
     *
     * @return an unmodifiable map of index definition properties
     */
    public Map<String, Object> getIndexDefinition() {
        return Collections.unmodifiableMap(indexDefinition);
    }

    void putIndexDefinition(String key, Object value) {
        this.indexDefinition.put(key, value);
    }

    /**
     * Gets the index schema attributes (field names, types, and attributes).
     *
     * @return an unmodifiable list of attribute maps
     */
    public List<Map<String, Object>> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    void addAttribute(Map<String, Object> attribute) {
        this.attributes.add(attribute);
    }

    /**
     * Gets the number of documents in the index.
     *
     * @return the number of documents
     */
    public Long getNumDocs() {
        return numDocs;
    }

    void setNumDocs(Long numDocs) {
        this.numDocs = numDocs;
    }

    /**
     * Gets the maximum document ID.
     *
     * @return the maximum document ID
     */
    public Long getMaxDocId() {
        return maxDocId;
    }

    void setMaxDocId(Long maxDocId) {
        this.maxDocId = maxDocId;
    }

    /**
     * Gets the number of distinct terms in the index.
     *
     * @return the number of distinct terms
     */
    public Long getNumTerms() {
        return numTerms;
    }

    void setNumTerms(Long numTerms) {
        this.numTerms = numTerms;
    }

    /**
     * Gets the total number of records in the index.
     *
     * @return the total number of records
     */
    public Long getNumRecords() {
        return numRecords;
    }

    void setNumRecords(Long numRecords) {
        this.numRecords = numRecords;
    }

    /**
     * Gets size statistics including memory usage for various index components.
     * <p>
     * Common keys include:
     * </p>
     * <ul>
     * <li>inverted_sz_mb - Memory used by the inverted index</li>
     * <li>vector_index_sz_mb - Memory used by vector indexes</li>
     * <li>doc_table_size_mb - Memory used by the document table</li>
     * <li>sortable_values_size_mb - Memory used by sortable values</li>
     * <li>key_table_size_mb - Memory used by the key table</li>
     * <li>geoshapes_sz_mb - Memory used by GEO-related fields</li>
     * <li>records_per_doc_avg - Average records per document</li>
     * <li>bytes_per_record_avg - Average bytes per record</li>
     * </ul>
     *
     * @return an unmodifiable map of size statistics
     */
    public Map<String, Object> getSizeStatistics() {
        return Collections.unmodifiableMap(sizeStatistics);
    }

    void putSizeStatistic(String key, Object value) {
        this.sizeStatistics.put(key, value);
    }

    /**
     * Gets indexing-related statistics including progress, failures, and timing.
     * <p>
     * Common keys include:
     * </p>
     * <ul>
     * <li>hash_indexing_failures - Number of indexing failures</li>
     * <li>total_indexing_time - Total time spent indexing (ms)</li>
     * <li>indexing - Whether indexing is in progress (0 or 1)</li>
     * <li>percent_indexed - Percentage of index completed (0-1)</li>
     * <li>number_of_uses - Number of times the index has been used</li>
     * <li>cleaning - Whether index deletion is in progress (0 or 1)</li>
     * </ul>
     *
     * @return an unmodifiable map of indexing statistics
     */
    public Map<String, Object> getIndexingStatistics() {
        return Collections.unmodifiableMap(indexingStatistics);
    }

    void putIndexingStatistic(String key, Object value) {
        this.indexingStatistics.put(key, value);
    }

    /**
     * Gets garbage collection statistics.
     * <p>
     * Common keys include:
     * </p>
     * <ul>
     * <li>bytes_collected - Bytes collected during GC</li>
     * <li>total_ms_run - Total GC time (ms)</li>
     * <li>total_cycles - Total GC cycles</li>
     * <li>average_cycle_time_ms - Average GC cycle time (ms)</li>
     * <li>last_run_time_ms - Last GC run time (ms)</li>
     * <li>gc_numeric_trees_missed - Numeric tree nodes missed during GC</li>
     * <li>gc_blocks_denied - Blocks skipped during GC</li>
     * </ul>
     *
     * @return an unmodifiable map of GC statistics
     */
    public Map<String, Object> getGcStatistics() {
        return Collections.unmodifiableMap(gcStatistics);
    }

    void putGcStatistic(String key, Object value) {
        this.gcStatistics.put(key, value);
    }

    /**
     * Gets cursor statistics for pagination.
     * <p>
     * Common keys include:
     * </p>
     * <ul>
     * <li>global_idle - Number of idle cursors in the system</li>
     * <li>global_total - Total number of cursors in the system</li>
     * <li>index_capacity - Maximum cursors allowed per index</li>
     * <li>index_total - Total cursors open on this index</li>
     * </ul>
     *
     * @return an unmodifiable map of cursor statistics
     */
    public Map<String, Object> getCursorStatistics() {
        return Collections.unmodifiableMap(cursorStatistics);
    }

    void putCursorStatistic(String key, Object value) {
        this.cursorStatistics.put(key, value);
    }

    /**
     * Gets dialect usage statistics showing how many times each query dialect (1-4) was used.
     *
     * @return an unmodifiable map of dialect usage counts
     */
    public Map<String, Long> getDialectStatistics() {
        return Collections.unmodifiableMap(dialectStatistics);
    }

    void putDialectStatistic(String key, Long value) {
        this.dialectStatistics.put(key, value);
    }

    /**
     * Gets index-level error statistics.
     * <p>
     * Common keys include:
     * </p>
     * <ul>
     * <li>indexing failures - Number of indexing failures</li>
     * <li>last indexing error - Description of last error</li>
     * <li>last indexing error key - Key that caused last error</li>
     * </ul>
     *
     * @return an unmodifiable map of index error statistics
     */
    public Map<String, Object> getIndexErrors() {
        return Collections.unmodifiableMap(indexErrors);
    }

    void putIndexError(String key, Object value) {
        this.indexErrors.put(key, value);
    }

    /**
     * Gets per-field error statistics.
     * <p>
     * Each entry contains field information and error statistics for that field.
     * </p>
     *
     * @return an unmodifiable list of field statistics maps
     */
    public List<Map<String, Object>> getFieldStatistics() {
        return Collections.unmodifiableList(fieldStatistics);
    }

    void addFieldStatistic(Map<String, Object> fieldStatistic) {
        this.fieldStatistics.add(fieldStatistic);
    }

    @Override
    public String toString() {
        return "IndexInfo{" + "indexName='" + indexName + '\'' + ", numDocs=" + numDocs + ", maxDocId=" + maxDocId
                + ", numTerms=" + numTerms + ", numRecords=" + numRecords + '}';
    }

}
