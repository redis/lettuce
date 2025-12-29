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
import java.util.LinkedHashMap;
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
 * @param <V> Value type.
 * @author Julien Ruaux
 * @see <a href="https://redis.io/docs/latest/commands/ft.info/">FT.INFO</a>
 * @since 6.8
 */
public class IndexInfo<V> {

    private String indexName;

    private boolean noOffsets;

    private boolean noHighlight;

    private boolean noFields;

    private boolean noFrequency;

    private boolean maxTextFields;

    private boolean skipInitialScan;

    private IndexDefinition<V> indexDefinition = new IndexDefinition<>();

    private final List<Field<V>> fields = new ArrayList<>();

    private long numDocs;

    private long maxDocId;

    private long numTerms;

    private long numRecords;

    // Strongly-typed statistics objects
    private SizeStatistics sizeStats = new SizeStatistics();

    private IndexingStatistics indexingStats = new IndexingStatistics();

    private GcStatistics gcStats = new GcStatistics();

    private CursorStatistics cursorStats = new CursorStatistics();

    private DialectStatistics dialectStats = new DialectStatistics();

    private ErrorStatistics indexErrors = new ErrorStatistics();

    private final List<FieldErrorStatistics> fieldStatistics = new ArrayList<>();

    private final Map<String, Object> additionalFields = new HashMap<>();

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
     * Returns whether the index was created with NOOFFSETS option. If set, term offsets are not stored for documents (saves
     * memory, does not allow exact searches or highlighting).
     *
     * @return {@code true} if NOOFFSETS is set
     */
    public boolean isNoOffsets() {
        return noOffsets;
    }

    void setNoOffsets(boolean noOffsets) {
        this.noOffsets = noOffsets;
    }

    /**
     * Returns whether the index was created with NOHL (no highlighting) option. Conserves storage space and memory by disabling
     * highlighting support. Also implied by NOOFFSETS.
     *
     * @return {@code true} if NOHL is set
     */
    public boolean isNoHighlight() {
        return noHighlight;
    }

    void setNoHighlight(boolean noHighlight) {
        this.noHighlight = noHighlight;
    }

    /**
     * Returns whether the index was created with NOFIELDS option. If set, field bits are not stored for each term (saves
     * memory, does not allow filtering by specific fields).
     *
     * @return {@code true} if NOFIELDS is set
     */
    public boolean isNoFields() {
        return noFields;
    }

    void setNoFields(boolean noFields) {
        this.noFields = noFields;
    }

    /**
     * Returns whether the index was created with NOFREQS option. If set, term frequencies are not saved in the index (saves
     * memory, does not allow sorting based on term frequencies).
     *
     * @return {@code true} if NOFREQS is set
     */
    public boolean isNoFrequency() {
        return noFrequency;
    }

    void setNoFrequency(boolean noFrequency) {
        this.noFrequency = noFrequency;
    }

    /**
     * Returns whether the index was created with MAXTEXTFIELDS option. Forces RediSearch to encode indexes as if there were
     * more than 32 text fields, which allows adding additional fields (beyond 32) using FT.ALTER.
     *
     * @return {@code true} if MAXTEXTFIELDS is set
     */
    public boolean isMaxTextFields() {
        return maxTextFields;
    }

    void setMaxTextFields(boolean maxTextFields) {
        this.maxTextFields = maxTextFields;
    }

    /**
     * Returns whether the index was created with SKIPINITIALSCAN option. If set, the index was not scanned for existing
     * documents in the keyspace upon creation.
     *
     * @return {@code true} if SKIPINITIALSCAN is set
     */
    public boolean isSkipInitialScan() {
        return skipInitialScan;
    }

    void setSkipInitialScan(boolean skipInitialScan) {
        this.skipInitialScan = skipInitialScan;
    }

    /**
     * Gets the index definition including key_type (HASH or JSON), prefixes, filters, and other configuration.
     *
     * @return the index definition (never null, but individual fields may be empty/null if not available)
     */
    public IndexDefinition<V> getIndexDefinition() {
        return indexDefinition;
    }

    void setIndexDefinition(IndexDefinition<V> indexDefinition) {
        this.indexDefinition = indexDefinition;
    }

    /**
     * Gets the index schema fields (field names, types, and attributes).
     *
     * @return an unmodifiable list of fields
     */
    public List<Field<V>> getFields() {
        return Collections.unmodifiableList(fields);
    }

    void addField(Field<V> field) {
        this.fields.add(field);
    }

    /**
     * Gets the number of documents in the index.
     *
     * @return the number of documents
     */
    public long getNumDocs() {
        return numDocs;
    }

    void setNumDocs(long numDocs) {
        this.numDocs = numDocs;
    }

    /**
     * Gets the maximum document ID.
     *
     * @return the maximum document ID
     */
    public long getMaxDocId() {
        return maxDocId;
    }

    void setMaxDocId(long maxDocId) {
        this.maxDocId = maxDocId;
    }

    /**
     * Gets the number of distinct terms in the index.
     *
     * @return the number of distinct terms
     */
    public long getNumTerms() {
        return numTerms;
    }

    void setNumTerms(long numTerms) {
        this.numTerms = numTerms;
    }

    /**
     * Gets the total number of records in the index.
     *
     * @return the total number of records
     */
    public long getNumRecords() {
        return numRecords;
    }

    void setNumRecords(long numRecords) {
        this.numRecords = numRecords;
    }

    /**
     * Gets dialect usage statistics showing how many times each query dialect (1-4) was used.
     *
     * @return dialect statistics (never null, but individual fields may be null if not available)
     */
    public DialectStatistics getDialectStats() {
        return dialectStats;
    }

    void setDialectStats(DialectStatistics dialectStats) {
        this.dialectStats = dialectStats;
    }

    /**
     * Gets index-level error statistics including indexing failures and last error information.
     *
     * @return error statistics (never null, but individual fields may be null if not available)
     */
    public ErrorStatistics getIndexErrors() {
        return indexErrors;
    }

    void setIndexErrors(ErrorStatistics indexErrors) {
        this.indexErrors = indexErrors;
    }

    /**
     * Gets per-field error statistics.
     * <p>
     * Each entry contains field information and error statistics for that field.
     * </p>
     *
     * @return an unmodifiable list of field error statistics
     */
    public List<FieldErrorStatistics> getFieldStatistics() {
        return Collections.unmodifiableList(fieldStatistics);
    }

    void addFieldStatistic(FieldErrorStatistics fieldStatistic) {
        this.fieldStatistics.add(fieldStatistic);
    }

    /**
     * Gets additional fields that were returned by FT.INFO but are not explicitly mapped to known properties.
     * <p>
     * This map captures any fields that are not recognized by the current version of the parser, making the implementation
     * forward-compatible with future Redis versions that may add new fields to the FT.INFO response.
     * </p>
     *
     * @return an unmodifiable map of additional fields
     */
    public Map<String, Object> getAdditionalFields() {
        return Collections.unmodifiableMap(additionalFields);
    }

    void putAdditionalField(String key, Object value) {
        this.additionalFields.put(key, value);
    }

    // ========== Strongly-Typed Statistics Accessors ==========

    /**
     * Gets size statistics including memory usage for various index components.
     *
     * @return size statistics (never null, but individual fields may be null if not available)
     */
    public SizeStatistics getSizeStats() {
        return sizeStats;
    }

    void setSizeStats(SizeStatistics sizeStats) {
        this.sizeStats = sizeStats;
    }

    /**
     * Gets indexing-related statistics including progress, failures, and timing.
     *
     * @return indexing statistics (never null, but individual fields may be null if not available)
     */
    public IndexingStatistics getIndexingStats() {
        return indexingStats;
    }

    void setIndexingStats(IndexingStatistics indexingStats) {
        this.indexingStats = indexingStats;
    }

    /**
     * Gets garbage collection statistics.
     *
     * @return GC statistics (never null, but individual fields may be null if not available)
     */
    public GcStatistics getGcStats() {
        return gcStats;
    }

    void setGcStats(GcStatistics gcStats) {
        this.gcStats = gcStats;
    }

    /**
     * Gets cursor statistics for pagination.
     *
     * @return cursor statistics (never null, but individual fields may be null if not available)
     */
    public CursorStatistics getCursorStats() {
        return cursorStats;
    }

    void setCursorStats(CursorStatistics cursorStats) {
        this.cursorStats = cursorStats;
    }

    @Override
    public String toString() {
        return "IndexInfo{" + "indexName='" + indexName + '\'' + ", numDocs=" + numDocs + ", maxDocId=" + maxDocId
                + ", numTerms=" + numTerms + ", numRecords=" + numRecords + '}';
    }

    // ========== Nested Classes ==========

    /**
     * Size statistics including memory usage for various index components.
     *
     * @since 6.8
     */
    public static class SizeStatistics {

        private double invertedSizeMb;

        private double vectorIndexSizeMb;

        private long totalInvertedIndexBlocks;

        private double offsetVectorsSizeMb;

        private double docTableSizeMb;

        private double sortableValuesSizeMb;

        private double keyTableSizeMb;

        private double geoshapesSizeMb;

        private double recordsPerDocAvg;

        private double bytesPerRecordAvg;

        private double offsetsPerTermAvg;

        private double offsetBitsPerRecordAvg;

        private double tagOverheadSizeMb;

        private double textOverheadSizeMb;

        private double totalIndexMemorySizeMb;

        SizeStatistics() {
        }

        /**
         * Gets the memory used by the inverted index in megabytes.
         *
         * @return the inverted index size in MB
         */
        public double getInvertedSizeMb() {
            return invertedSizeMb;
        }

        /**
         * Gets the memory used by vector indexes in megabytes.
         *
         * @return the vector index size in MB
         */
        public double getVectorIndexSizeMb() {
            return vectorIndexSizeMb;
        }

        /**
         * Gets the total number of blocks in the inverted index.
         *
         * @return the total inverted index blocks
         */
        public long getTotalInvertedIndexBlocks() {
            return totalInvertedIndexBlocks;
        }

        /**
         * Gets the memory used by offset vectors in megabytes.
         *
         * @return the offset vectors size in MB
         */
        public double getOffsetVectorsSizeMb() {
            return offsetVectorsSizeMb;
        }

        /**
         * Gets the memory used by the document table in megabytes.
         *
         * @return the document table size in MB
         */
        public double getDocTableSizeMb() {
            return docTableSizeMb;
        }

        /**
         * Gets the memory used by sortable values in megabytes.
         *
         * @return the sortable values size in MB
         */
        public double getSortableValuesSizeMb() {
            return sortableValuesSizeMb;
        }

        /**
         * Gets the memory used by the key table in megabytes.
         *
         * @return the key table size in MB
         */
        public double getKeyTableSizeMb() {
            return keyTableSizeMb;
        }

        /**
         * Gets the memory used by GEO-related fields in megabytes.
         *
         * @return the geoshapes size in MB
         */
        public double getGeoshapesSizeMb() {
            return geoshapesSizeMb;
        }

        /**
         * Gets the average number of records per document.
         *
         * @return the average records per document
         */
        public double getRecordsPerDocAvg() {
            return recordsPerDocAvg;
        }

        /**
         * Gets the average size of each record in bytes.
         *
         * @return the average bytes per record
         */
        public double getBytesPerRecordAvg() {
            return bytesPerRecordAvg;
        }

        /**
         * Gets the average number of offsets per term.
         *
         * @return the average offsets per term
         */
        public double getOffsetsPerTermAvg() {
            return offsetsPerTermAvg;
        }

        /**
         * Gets the average number of bits used for offsets per record.
         *
         * @return the average offset bits per record
         */
        public double getOffsetBitsPerRecordAvg() {
            return offsetBitsPerRecordAvg;
        }

        /**
         * Gets the size of TAG index structures used for optimizing performance in megabytes.
         *
         * @return the TAG overhead size in MB
         */
        public double getTagOverheadSizeMb() {
            return tagOverheadSizeMb;
        }

        /**
         * Gets the size of TEXT index structures used for optimizing performance in megabytes.
         *
         * @return the TEXT overhead size in MB
         */
        public double getTextOverheadSizeMb() {
            return textOverheadSizeMb;
        }

        /**
         * Gets the total memory consumed by all indexes in the database in megabytes.
         *
         * @return the total index memory size in MB
         */
        public double getTotalIndexMemorySizeMb() {
            return totalIndexMemorySizeMb;
        }

        void setInvertedSizeMb(double invertedSizeMb) {
            this.invertedSizeMb = invertedSizeMb;
        }

        void setVectorIndexSizeMb(double vectorIndexSizeMb) {
            this.vectorIndexSizeMb = vectorIndexSizeMb;
        }

        void setTotalInvertedIndexBlocks(long totalInvertedIndexBlocks) {
            this.totalInvertedIndexBlocks = totalInvertedIndexBlocks;
        }

        void setOffsetVectorsSizeMb(double offsetVectorsSizeMb) {
            this.offsetVectorsSizeMb = offsetVectorsSizeMb;
        }

        void setDocTableSizeMb(double docTableSizeMb) {
            this.docTableSizeMb = docTableSizeMb;
        }

        void setSortableValuesSizeMb(double sortableValuesSizeMb) {
            this.sortableValuesSizeMb = sortableValuesSizeMb;
        }

        void setKeyTableSizeMb(double keyTableSizeMb) {
            this.keyTableSizeMb = keyTableSizeMb;
        }

        void setGeoshapesSizeMb(double geoshapesSizeMb) {
            this.geoshapesSizeMb = geoshapesSizeMb;
        }

        void setRecordsPerDocAvg(double recordsPerDocAvg) {
            this.recordsPerDocAvg = recordsPerDocAvg;
        }

        void setBytesPerRecordAvg(double bytesPerRecordAvg) {
            this.bytesPerRecordAvg = bytesPerRecordAvg;
        }

        void setOffsetsPerTermAvg(double offsetsPerTermAvg) {
            this.offsetsPerTermAvg = offsetsPerTermAvg;
        }

        void setOffsetBitsPerRecordAvg(double offsetBitsPerRecordAvg) {
            this.offsetBitsPerRecordAvg = offsetBitsPerRecordAvg;
        }

        void setTagOverheadSizeMb(double tagOverheadSizeMb) {
            this.tagOverheadSizeMb = tagOverheadSizeMb;
        }

        void setTextOverheadSizeMb(double textOverheadSizeMb) {
            this.textOverheadSizeMb = textOverheadSizeMb;
        }

        void setTotalIndexMemorySizeMb(double totalIndexMemorySizeMb) {
            this.totalIndexMemorySizeMb = totalIndexMemorySizeMb;
        }

    }

    /**
     * Indexing-related statistics including progress, failures, and timing.
     *
     * @since 6.8
     */
    public static class IndexingStatistics {

        private long hashIndexingFailures;

        private double totalIndexingTime;

        private boolean indexing;

        private double percentIndexed;

        private long numberOfUses;

        private boolean cleaning;

        IndexingStatistics() {
        }

        /**
         * Gets the number of failures encountered during indexing.
         *
         * @return the number of indexing failures
         */
        public long getHashIndexingFailures() {
            return hashIndexingFailures;
        }

        /**
         * Gets the cumulative wall-clock time spent indexing documents in milliseconds.
         *
         * @return the total indexing time in ms
         */
        public double getTotalIndexingTime() {
            return totalIndexingTime;
        }

        /**
         * Returns whether the index is currently being generated.
         *
         * @return {@code true} if indexing is in progress, {@code false} otherwise
         */
        public boolean isIndexing() {
            return indexing;
        }

        /**
         * Gets the percentage of the index that has been successfully generated (1.0 means 100%).
         *
         * @return the percent indexed (0.0-1.0)
         */
        public double getPercentIndexed() {
            return percentIndexed;
        }

        /**
         * Gets the number of times the index has been used.
         *
         * @return the number of uses
         */
        public long getNumberOfUses() {
            return numberOfUses;
        }

        /**
         * Returns whether index deletion is in progress.
         *
         * @return {@code true} if cleaning is in progress, {@code false} otherwise
         */
        public boolean isCleaning() {
            return cleaning;
        }

        void setHashIndexingFailures(long hashIndexingFailures) {
            this.hashIndexingFailures = hashIndexingFailures;
        }

        void setTotalIndexingTime(double totalIndexingTime) {
            this.totalIndexingTime = totalIndexingTime;
        }

        void setIndexing(boolean indexing) {
            this.indexing = indexing;
        }

        void setPercentIndexed(double percentIndexed) {
            this.percentIndexed = percentIndexed;
        }

        void setNumberOfUses(long numberOfUses) {
            this.numberOfUses = numberOfUses;
        }

        void setCleaning(boolean cleaning) {
            this.cleaning = cleaning;
        }

    }

    /**
     * Garbage collection statistics.
     *
     * @since 6.8
     */
    public static class GcStatistics {

        private long bytesCollected;

        private double totalMsRun;

        private long totalCycles;

        private double averageCycleTimeMs;

        private double lastRunTimeMs;

        private long gcNumericTreesMissed;

        private long gcBlocksDenied;

        GcStatistics() {
        }

        /**
         * Gets the number of bytes collected during garbage collection.
         *
         * @return the bytes collected
         */
        public long getBytesCollected() {
            return bytesCollected;
        }

        /**
         * Gets the total time in milliseconds spent on garbage collection.
         *
         * @return the total GC time in ms
         */
        public double getTotalMsRun() {
            return totalMsRun;
        }

        /**
         * Gets the total number of garbage collection cycles.
         *
         * @return the total GC cycles
         */
        public long getTotalCycles() {
            return totalCycles;
        }

        /**
         * Gets the average time in milliseconds for each garbage collection cycle.
         *
         * @return the average GC cycle time in ms (may be NaN)
         */
        public double getAverageCycleTimeMs() {
            return averageCycleTimeMs;
        }

        /**
         * Gets the time in milliseconds taken by the last garbage collection run.
         *
         * @return the last GC run time in ms
         */
        public double getLastRunTimeMs() {
            return lastRunTimeMs;
        }

        /**
         * Gets the number of numeric tree nodes whose changes were discarded during garbage collection.
         *
         * @return the number of numeric trees missed
         */
        public long getGcNumericTreesMissed() {
            return gcNumericTreesMissed;
        }

        /**
         * Gets the number of blocks whose changes were discarded during garbage collection.
         *
         * @return the number of blocks denied
         */
        public long getGcBlocksDenied() {
            return gcBlocksDenied;
        }

        void setBytesCollected(long bytesCollected) {
            this.bytesCollected = bytesCollected;
        }

        void setTotalMsRun(double totalMsRun) {
            this.totalMsRun = totalMsRun;
        }

        void setTotalCycles(long totalCycles) {
            this.totalCycles = totalCycles;
        }

        void setAverageCycleTimeMs(double averageCycleTimeMs) {
            this.averageCycleTimeMs = averageCycleTimeMs;
        }

        void setLastRunTimeMs(double lastRunTimeMs) {
            this.lastRunTimeMs = lastRunTimeMs;
        }

        void setGcNumericTreesMissed(long gcNumericTreesMissed) {
            this.gcNumericTreesMissed = gcNumericTreesMissed;
        }

        void setGcBlocksDenied(long gcBlocksDenied) {
            this.gcBlocksDenied = gcBlocksDenied;
        }

    }

    /**
     * Cursor statistics for pagination.
     *
     * @since 6.8
     */
    public static class CursorStatistics {

        private long globalIdle;

        private long globalTotal;

        private long indexCapacity;

        private long indexTotal;

        CursorStatistics() {
        }

        /**
         * Gets the number of idle cursors in the system.
         *
         * @return the global idle cursor count
         */
        public long getGlobalIdle() {
            return globalIdle;
        }

        /**
         * Gets the total number of cursors in the system.
         *
         * @return the global total cursor count
         */
        public long getGlobalTotal() {
            return globalTotal;
        }

        /**
         * Gets the maximum number of cursors allowed per index.
         *
         * @return the index capacity
         */
        public long getIndexCapacity() {
            return indexCapacity;
        }

        /**
         * Gets the total number of cursors open on this index.
         *
         * @return the index total cursor count
         */
        public long getIndexTotal() {
            return indexTotal;
        }

        void setGlobalIdle(long globalIdle) {
            this.globalIdle = globalIdle;
        }

        void setGlobalTotal(long globalTotal) {
            this.globalTotal = globalTotal;
        }

        void setIndexCapacity(long indexCapacity) {
            this.indexCapacity = indexCapacity;
        }

        void setIndexTotal(long indexTotal) {
            this.indexTotal = indexTotal;
        }

    }

    /**
     * Dialect usage statistics showing how many times each query dialect (1-4) was used.
     *
     * @since 6.8
     */
    public static class DialectStatistics {

        private long dialect1;

        private long dialect2;

        private long dialect3;

        private long dialect4;

        DialectStatistics() {
        }

        /**
         * Gets the number of times dialect 1 was used.
         *
         * @return the dialect 1 usage count
         */
        public long getDialect1() {
            return dialect1;
        }

        void setDialect1(long dialect1) {
            this.dialect1 = dialect1;
        }

        /**
         * Gets the number of times dialect 2 was used.
         *
         * @return the dialect 2 usage count
         */
        public long getDialect2() {
            return dialect2;
        }

        void setDialect2(long dialect2) {
            this.dialect2 = dialect2;
        }

        /**
         * Gets the number of times dialect 3 was used.
         *
         * @return the dialect 3 usage count
         */
        public long getDialect3() {
            return dialect3;
        }

        void setDialect3(long dialect3) {
            this.dialect3 = dialect3;
        }

        /**
         * Gets the number of times dialect 4 was used.
         *
         * @return the dialect 4 usage count
         */
        public long getDialect4() {
            return dialect4;
        }

        void setDialect4(long dialect4) {
            this.dialect4 = dialect4;
        }

    }

    /**
     * Error statistics including indexing failures and last error information.
     *
     * @since 6.8
     */
    public static class ErrorStatistics {

        private long indexingFailures;

        private String lastIndexingError;

        private String lastIndexingErrorKey;

        ErrorStatistics() {
        }

        /**
         * Gets the number of indexing failures.
         *
         * @return the number of indexing failures
         */
        public long getIndexingFailures() {
            return indexingFailures;
        }

        void setIndexingFailures(long indexingFailures) {
            this.indexingFailures = indexingFailures;
        }

        /**
         * Gets the description of the last indexing error.
         *
         * @return the last indexing error message, or {@code null} if not available
         */
        public String getLastIndexingError() {
            return lastIndexingError;
        }

        void setLastIndexingError(String lastIndexingError) {
            this.lastIndexingError = lastIndexingError;
        }

        /**
         * Gets the key that caused the last indexing error.
         *
         * @return the last indexing error key, or {@code null} if not available
         */
        public String getLastIndexingErrorKey() {
            return lastIndexingErrorKey;
        }

        void setLastIndexingErrorKey(String lastIndexingErrorKey) {
            this.lastIndexingErrorKey = lastIndexingErrorKey;
        }

    }

    /**
     * Per-field error statistics containing field information and error details.
     *
     * @since 6.8
     */
    public static class FieldErrorStatistics {

        private String identifier;

        private String attribute;

        private ErrorStatistics errors;

        FieldErrorStatistics() {
        }

        /**
         * Gets the field identifier (e.g., JSON path or field name).
         *
         * @return the field identifier
         */
        public String getIdentifier() {
            return identifier;
        }

        void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Gets the attribute name used in queries.
         *
         * @return the attribute name, or {@code null} if not available
         */
        public String getAttribute() {
            return attribute;
        }

        void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        /**
         * Gets the error statistics for this field.
         *
         * @return the error statistics, or {@code null} if not available
         */
        public ErrorStatistics getErrors() {
            return errors;
        }

        void setErrors(ErrorStatistics errors) {
            this.errors = errors;
        }

    }

    /**
     * Represents the index definition returned by FT.INFO.
     * <p>
     * Contains information about how the index was created, including the key type, prefixes, filters, and other configuration
     * options. This mirrors the structure of {@link io.lettuce.core.search.arguments.CreateArgs} but is used for reading index
     * information rather than building commands.
     *
     * @param <V> Value type.
     * @see <a href="https://redis.io/docs/latest/commands/ft.info/">FT.INFO</a>
     * @since 6.8
     */
    public static class IndexDefinition<V> {

        /**
         * Possible target types for the index.
         */
        public enum TargetType {
            HASH, JSON
        }

        private TargetType keyType;

        private final List<V> prefixes = new ArrayList<>();

        private V filter;

        private V languageField;

        private V scoreField;

        private V payloadField;

        private double defaultScore;

        private V defaultLanguage;

        private final Map<String, Object> additionalFields = new LinkedHashMap<>();

        /**
         * Package-private constructor for IndexDefinition.
         */
        IndexDefinition() {
        }

        /**
         * Get the key type (HASH or JSON).
         *
         * @return the key type, or {@code null} if not available
         */
        public TargetType getKeyType() {
            return keyType;
        }

        void setKeyType(TargetType keyType) {
            this.keyType = keyType;
        }

        /**
         * Get the list of key prefixes that the index applies to.
         *
         * @return an unmodifiable list of prefixes
         */
        public List<V> getPrefixes() {
            return Collections.unmodifiableList(prefixes);
        }

        void addPrefix(V prefix) {
            this.prefixes.add(prefix);
        }

        /**
         * Get the filter expression used to select which keys to index.
         *
         * @return the filter expression, or {@code null} if not available
         */
        public V getFilter() {
            return filter;
        }

        void setFilter(V filter) {
            this.filter = filter;
        }

        /**
         * Get the language field name.
         *
         * @return the language field name, or {@code null} if not available
         */
        public V getLanguageField() {
            return languageField;
        }

        void setLanguageField(V languageField) {
            this.languageField = languageField;
        }

        /**
         * Get the score field name.
         *
         * @return the score field name, or {@code null} if not available
         */
        public V getScoreField() {
            return scoreField;
        }

        void setScoreField(V scoreField) {
            this.scoreField = scoreField;
        }

        /**
         * Get the payload field name.
         *
         * @return the payload field name, or {@code null} if not available
         */
        public V getPayloadField() {
            return payloadField;
        }

        void setPayloadField(V payloadField) {
            this.payloadField = payloadField;
        }

        /**
         * Get the default score for documents.
         *
         * @return the default score
         */
        public double getDefaultScore() {
            return defaultScore;
        }

        void setDefaultScore(double defaultScore) {
            this.defaultScore = defaultScore;
        }

        /**
         * Get the default language for text fields.
         *
         * @return the default language, or {@code null} if not available
         */
        public V getDefaultLanguage() {
            return defaultLanguage;
        }

        void setDefaultLanguage(V defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
        }

        /**
         * Get additional fields that are not explicitly mapped. This is useful for forward compatibility with future Redis
         * versions that may add new index definition properties.
         *
         * @return a map of additional properties
         */
        public Map<String, Object> getAdditionalFields() {
            return Collections.unmodifiableMap(additionalFields);
        }

        void putAdditionalField(String key, Object value) {
            this.additionalFields.put(key, value);
        }

    }

    /**
     * Base class for field information returned by FT.INFO.
     * <p>
     * Represents a field in the index schema with its type and configuration. This mirrors the structure of
     * {@link io.lettuce.core.search.arguments.FieldArgs} but is used for reading index information rather than building
     * commands.
     *
     * @param <V> Value type.
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">Field and
     *      type options</a>
     * @since 6.8
     */
    public abstract static class Field<V> {

        /**
         * Field type enumeration.
         */
        public enum FieldType {
            TEXT, NUMERIC, TAG, GEO, GEOSHAPE, VECTOR
        }

        private final V identifier;

        private final V attribute;

        private final FieldType type;

        private final boolean sortable;

        private final boolean unNormalizedForm;

        private final boolean noIndex;

        private final boolean indexEmpty;

        private final boolean indexMissing;

        private final Map<String, Object> additionalFields;

        /**
         * Constructor for Field.
         *
         * @param identifier the field identifier (e.g., JSON path or field name)
         * @param attribute the attribute name used in queries (may be null)
         * @param type the field type
         * @param sortable whether the field is sortable
         * @param unNormalizedForm whether unnormalized form is used
         * @param noIndex whether the field is not indexed
         * @param indexEmpty whether empty values are indexed
         * @param indexMissing whether missing values are indexed
         * @param additionalFields additional fields not explicitly mapped
         */
        protected Field(V identifier, V attribute, FieldType type, boolean sortable, boolean unNormalizedForm, boolean noIndex,
                boolean indexEmpty, boolean indexMissing, Map<String, Object> additionalFields) {
            this.identifier = identifier;
            this.attribute = attribute;
            this.type = type;
            this.sortable = sortable;
            this.unNormalizedForm = unNormalizedForm;
            this.noIndex = noIndex;
            this.indexEmpty = indexEmpty;
            this.indexMissing = indexMissing;
            this.additionalFields = new LinkedHashMap<>(additionalFields);
        }

        /**
         * Get the field identifier (e.g., JSON path or field name).
         *
         * @return the field identifier
         */
        public V getIdentifier() {
            return identifier;
        }

        /**
         * Get the attribute name used in queries.
         *
         * @return the attribute name, or null if not set
         */
        public V getAttribute() {
            return attribute;
        }

        /**
         * Get the field type.
         *
         * @return the field type
         */
        public FieldType getType() {
            return type;
        }

        /**
         * Check if the field is sortable.
         *
         * @return true if sortable
         */
        public boolean isSortable() {
            return sortable;
        }

        /**
         * Check if the field uses unnormalized form.
         *
         * @return true if unnormalized form
         */
        public boolean isUnNormalizedForm() {
            return unNormalizedForm;
        }

        /**
         * Check if the field is not indexed.
         *
         * @return true if not indexed
         */
        public boolean isNoIndex() {
            return noIndex;
        }

        /**
         * Check if the field indexes empty values.
         *
         * @return true if indexes empty values
         */
        public boolean isIndexEmpty() {
            return indexEmpty;
        }

        /**
         * Check if the field indexes missing values.
         *
         * @return true if indexes missing values
         */
        public boolean isIndexMissing() {
            return indexMissing;
        }

        /**
         * Get additional fields that are not explicitly mapped. This is useful for forward compatibility with future Redis
         * versions that may add new field properties.
         *
         * @return a map of additional properties
         */
        public Map<String, Object> getAdditionalFields() {
            return Collections.unmodifiableMap(additionalFields);
        }

    }

    /**
     * Represents a TEXT field in the index schema.
     * <p>
     * Text fields are specifically designed for storing human language text. When indexing text fields, Redis performs several
     * transformations to optimize search capabilities including lowercasing and tokenization.
     *
     * @param <V> Value type.
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#text-fields">Text
     *      Fields</a>
     * @since 6.8
     */
    public static class TextField<V> extends Field<V> {

        private final Double weight;

        private final boolean noStem;

        private final String phonetic;

        private final boolean withSuffixTrie;

        /**
         * Constructor for TextField.
         *
         * @param identifier the field identifier
         * @param attribute the attribute name (may be null)
         * @param sortable whether the field is sortable
         * @param unNormalizedForm whether unnormalized form is used
         * @param noIndex whether the field is not indexed
         * @param indexEmpty whether empty values are indexed
         * @param indexMissing whether missing values are indexed
         * @param weight the field weight (may be null)
         * @param noStem whether stemming is disabled
         * @param phonetic the phonetic matcher (may be null)
         * @param withSuffixTrie whether suffix trie is enabled
         * @param additionalFields additional fields not explicitly mapped
         */
        public TextField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm, boolean noIndex,
                boolean indexEmpty, boolean indexMissing, Double weight, boolean noStem, String phonetic,
                boolean withSuffixTrie, Map<String, Object> additionalFields) {
            super(identifier, attribute, FieldType.TEXT, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                    additionalFields);
            this.weight = weight;
            this.noStem = noStem;
            this.phonetic = phonetic;
            this.withSuffixTrie = withSuffixTrie;
        }

        /**
         * Get the field weight.
         *
         * @return the weight, or null if not set
         */
        public Double getWeight() {
            return weight;
        }

        /**
         * Check if stemming is disabled.
         *
         * @return true if stemming is disabled
         */
        public boolean isNoStem() {
            return noStem;
        }

        /**
         * Get the phonetic matcher.
         *
         * @return the phonetic matcher, or null if not set
         */
        public String getPhonetic() {
            return phonetic;
        }

        /**
         * Check if suffix trie is enabled.
         *
         * @return true if suffix trie is enabled
         */
        public boolean isWithSuffixTrie() {
            return withSuffixTrie;
        }

    }

    /**
     * Represents a NUMERIC field in the index schema.
     * <p>
     * Numeric fields are used to store non-textual, countable values. They can hold integer or floating-point values and
     * support range-based queries.
     *
     * @param <V> Value type.
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#numeric-fields">Numeric
     *      Fields</a>
     * @since 6.8
     */
    public static class NumericField<V> extends Field<V> {

        /**
         * Constructor for NumericField.
         *
         * @param identifier the field identifier
         * @param attribute the attribute name (may be null)
         * @param sortable whether the field is sortable
         * @param unNormalizedForm whether unnormalized form is used
         * @param noIndex whether the field is not indexed
         * @param indexEmpty whether empty values are indexed
         * @param indexMissing whether missing values are indexed
         * @param additionalFields additional fields not explicitly mapped
         */
        public NumericField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm, boolean noIndex,
                boolean indexEmpty, boolean indexMissing, Map<String, Object> additionalFields) {
            super(identifier, attribute, FieldType.NUMERIC, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                    additionalFields);
        }

    }

    /**
     * Represents a TAG field in the index schema.
     * <p>
     * Tag fields are used to store textual data that represents a collection of data tags or labels. Unlike text fields, tag
     * fields are stored as-is without tokenization or stemming.
     *
     * @param <V> Value type.
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#tag-fields">Tag
     *      Fields</a>
     * @since 6.8
     */
    public static class TagField<V> extends Field<V> {

        private final String separator;

        private final boolean caseSensitive;

        private final boolean withSuffixTrie;

        /**
         * Constructor for TagField.
         *
         * @param identifier the field identifier
         * @param attribute the attribute name (may be null)
         * @param sortable whether the field is sortable
         * @param unNormalizedForm whether unnormalized form is used
         * @param noIndex whether the field is not indexed
         * @param indexEmpty whether empty values are indexed
         * @param indexMissing whether missing values are indexed
         * @param separator the tag separator (may be null)
         * @param caseSensitive whether case-sensitive
         * @param withSuffixTrie whether suffix trie is enabled
         * @param additionalFields additional fields not explicitly mapped
         */
        public TagField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm, boolean noIndex,
                boolean indexEmpty, boolean indexMissing, String separator, boolean caseSensitive, boolean withSuffixTrie,
                Map<String, Object> additionalFields) {
            super(identifier, attribute, FieldType.TAG, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                    additionalFields);
            this.separator = separator;
            this.caseSensitive = caseSensitive;
            this.withSuffixTrie = withSuffixTrie;
        }

        /**
         * Get the tag separator.
         *
         * @return the separator, or null if not set
         */
        public String getSeparator() {
            return separator;
        }

        /**
         * Check if the field is case sensitive.
         *
         * @return true if case sensitive
         */
        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        /**
         * Check if suffix trie is enabled.
         *
         * @return true if suffix trie is enabled
         */
        public boolean isWithSuffixTrie() {
            return withSuffixTrie;
        }

    }

    /**
     * Represents a GEO field in the index schema.
     * <p>
     * Geo fields are used to store geographical coordinates such as longitude and latitude, enabling geospatial radius queries.
     *
     * @param <V> Value type.
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#geo-fields">Geo
     *      Fields</a>
     * @since 6.8
     */
    public static class GeoField<V> extends Field<V> {

        /**
         * Constructor for GeoField.
         *
         * @param identifier the field identifier
         * @param attribute the attribute name (may be null)
         * @param sortable whether the field is sortable
         * @param unNormalizedForm whether unnormalized form is used
         * @param noIndex whether the field is not indexed
         * @param indexEmpty whether empty values are indexed
         * @param indexMissing whether missing values are indexed
         * @param additionalFields additional fields not explicitly mapped
         */
        public GeoField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm, boolean noIndex,
                boolean indexEmpty, boolean indexMissing, Map<String, Object> additionalFields) {
            super(identifier, attribute, FieldType.GEO, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                    additionalFields);
        }

    }

    /**
     * Represents a GEOSHAPE field in the index schema.
     * <p>
     * Geoshape fields provide more advanced functionality than GEO fields, supporting both points and shapes with geographical
     * or Cartesian coordinates.
     *
     * @param <V> Value type.
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#geoshape-fields">Geoshape
     *      Fields</a>
     * @since 6.8
     */
    public static class GeoshapeField<V> extends Field<V> {

        /**
         * Coordinate system for geoshape fields.
         */
        public enum CoordinateSystem {
            /**
             * Cartesian (planar) coordinates.
             */
            FLAT,
            /**
             * Spherical (geographical) coordinates.
             */
            SPHERICAL
        }

        private final CoordinateSystem coordinateSystem;

        /**
         * Constructor for GeoshapeField.
         *
         * @param identifier the field identifier
         * @param attribute the attribute name (may be null)
         * @param sortable whether the field is sortable
         * @param unNormalizedForm whether unnormalized form is used
         * @param noIndex whether the field is not indexed
         * @param indexEmpty whether empty values are indexed
         * @param indexMissing whether missing values are indexed
         * @param coordinateSystem the coordinate system (may be null)
         * @param additionalFields additional fields not explicitly mapped
         */
        public GeoshapeField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm, boolean noIndex,
                boolean indexEmpty, boolean indexMissing, CoordinateSystem coordinateSystem,
                Map<String, Object> additionalFields) {
            super(identifier, attribute, FieldType.GEOSHAPE, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                    additionalFields);
            this.coordinateSystem = coordinateSystem;
        }

        /**
         * Get the coordinate system.
         *
         * @return the coordinate system, or null if not set
         */
        public CoordinateSystem getCoordinateSystem() {
            return coordinateSystem;
        }

    }

    /**
     * Represents a VECTOR field in the index schema.
     * <p>
     * Vector fields are floating-point vectors typically generated by external machine learning models, used for similarity
     * search.
     *
     * @param <V> Value type.
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#vector-fields">Vector
     *      Fields</a>
     * @since 6.8
     */
    public static class VectorField<V> extends Field<V> {

        /**
         * Vector similarity index algorithms.
         */
        public enum Algorithm {
            /**
             * Brute force algorithm.
             */
            FLAT,
            /**
             * Hierarchical, navigable, small world algorithm.
             */
            HNSW,
            /**
             * SVS-VAMANA algorithm for high-performance approximate vector search.
             *
             * @since Redis 8.2
             */
            SVS_VAMANA
        }

        private final Algorithm algorithm;

        private final Map<String, Object> attributes;

        /**
         * Constructor for VectorField.
         *
         * @param identifier the field identifier
         * @param attribute the attribute name (may be null)
         * @param sortable whether the field is sortable
         * @param unNormalizedForm whether unnormalized form is used
         * @param noIndex whether the field is not indexed
         * @param indexEmpty whether empty values are indexed
         * @param indexMissing whether missing values are indexed
         * @param algorithm the vector algorithm (may be null)
         * @param attributes vector-specific attributes (DIM, DISTANCE_METRIC, TYPE, etc.)
         * @param additionalFields additional fields not explicitly mapped
         */
        public VectorField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm, boolean noIndex,
                boolean indexEmpty, boolean indexMissing, Algorithm algorithm, Map<String, Object> attributes,
                Map<String, Object> additionalFields) {
            super(identifier, attribute, FieldType.VECTOR, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                    additionalFields);
            this.algorithm = algorithm;
            this.attributes = new LinkedHashMap<>(attributes);
        }

        /**
         * Get the vector algorithm.
         *
         * @return the algorithm, or null if not set
         */
        public Algorithm getAlgorithm() {
            return algorithm;
        }

        /**
         * Get the vector attributes (DIM, DISTANCE_METRIC, TYPE, etc.).
         *
         * @return a map of vector attributes
         */
        public Map<String, Object> getAttributes() {
            return Collections.unmodifiableMap(attributes);
        }

    }

}
