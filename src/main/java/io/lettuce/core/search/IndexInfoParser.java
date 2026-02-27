/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for FT.INFO command results that handles both RESP2 and RESP3 protocol responses.
 *
 * <p>
 * This parser automatically detects the Redis protocol version and switches between RESP2 and RESP3 parsing strategies.
 * </p>
 *
 * <p>
 * The result is an {@link IndexInfo} object containing index information and statistics. The structure includes various metrics
 * about the index such as number of documents, memory usage, indexing statistics, and field definitions.
 * </p>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Julien Ruaux
 * @since 6.8
 */
public class IndexInfoParser<K, V> implements ComplexDataParser<IndexInfo<V>> {

    private final RedisCodec<K, V> codec;

    // Top-level response keys
    private static final String KEY_INDEX_NAME = "index_name";

    private static final String KEY_INDEX_OPTIONS = "index_options";

    private static final String KEY_INDEX_DEFINITION = "index_definition";

    private static final String KEY_ATTRIBUTES = "attributes";

    private static final String KEY_NUM_DOCS = "num_docs";

    private static final String KEY_MAX_DOC_ID = "max_doc_id";

    private static final String KEY_NUM_TERMS = "num_terms";

    private static final String KEY_NUM_RECORDS = "num_records";

    private static final String KEY_GC_STATS = "gc_stats";

    private static final String KEY_CURSOR_STATS = "cursor_stats";

    private static final String KEY_DIALECT_STATS = "dialect_stats";

    private static final String KEY_INDEX_ERRORS = "Index Errors";

    private static final String KEY_FIELD_STATISTICS = "field statistics";

    // Index definition keys
    private static final String DEF_KEY_TYPE = "key_type";

    private static final String DEF_PREFIXES = "prefixes";

    private static final String DEF_FILTER = "filter";

    private static final String DEF_LANGUAGE_FIELD = "language_field";

    private static final String DEF_SCORE_FIELD = "score_field";

    private static final String DEF_PAYLOAD_FIELD = "payload_field";

    private static final String DEF_DEFAULT_SCORE = "default_score";

    private static final String DEF_DEFAULT_LANGUAGE = "default_language";

    // Field attribute keys
    private static final String ATTR_IDENTIFIER = "identifier";

    private static final String ATTR_ATTRIBUTE = "attribute";

    private static final String ATTR_TYPE = "type";

    private static final String ATTR_FLAGS = "flags";

    // Field flags
    private static final String FLAG_SORTABLE = "SORTABLE";

    private static final String FLAG_UNF = "UNF";

    private static final String FLAG_NOINDEX = "NOINDEX";

    private static final String FLAG_INDEXEMPTY = "INDEXEMPTY";

    private static final String FLAG_INDEXMISSING = "INDEXMISSING";

    private static final String FLAG_NOSTEM = "NOSTEM";

    private static final String FLAG_PHONETIC = "PHONETIC";

    private static final String FLAG_WITHSUFFIXTRIE = "WITHSUFFIXTRIE";

    private static final String FLAG_WEIGHT = "WEIGHT";

    private static final String FLAG_SEPARATOR = "SEPARATOR";

    private static final String FLAG_CASESENSITIVE = "CASESENSITIVE";

    private static final String FLAG_COORD_SYSTEM = "COORD_SYSTEM";

    private static final String FLAG_ALGORITHM = "ALGORITHM";

    // Index options
    private static final String OPT_NOOFFSETS = "NOOFFSETS";

    private static final String OPT_NOHL = "NOHL";

    private static final String OPT_NOFIELDS = "NOFIELDS";

    private static final String OPT_NOFREQS = "NOFREQS";

    private static final String OPT_MAXTEXTFIELDS = "MAXTEXTFIELDS";

    private static final String OPT_SKIPINITIALSCAN = "SKIPINITIALSCAN";

    // Field types
    private static final String TYPE_TEXT = "TEXT";

    private static final String TYPE_NUMERIC = "NUMERIC";

    private static final String TYPE_TAG = "TAG";

    private static final String TYPE_GEO = "GEO";

    private static final String TYPE_GEOSHAPE = "GEOSHAPE";

    private static final String TYPE_VECTOR = "VECTOR";

    // Error statistics keys
    private static final String ERR_INDEXING_FAILURES = "indexing failures";

    private static final String ERR_LAST_INDEXING_ERROR = "last indexing error";

    private static final String ERR_LAST_INDEXING_ERROR_KEY = "last indexing error key";

    // Size statistics keys
    private static final String SIZE_INVERTED_SZ_MB = "inverted_sz_mb";

    private static final String SIZE_VECTOR_INDEX_SZ_MB = "vector_index_sz_mb";

    private static final String SIZE_TOTAL_INVERTED_INDEX_BLOCKS = "total_inverted_index_blocks";

    private static final String SIZE_OFFSET_VECTORS_SZ_MB = "offset_vectors_sz_mb";

    private static final String SIZE_DOC_TABLE_SIZE_MB = "doc_table_size_mb";

    private static final String SIZE_SORTABLE_VALUES_SIZE_MB = "sortable_values_size_mb";

    private static final String SIZE_KEY_TABLE_SIZE_MB = "key_table_size_mb";

    private static final String SIZE_GEOSHAPES_SZ_MB = "geoshapes_sz_mb";

    private static final String SIZE_RECORDS_PER_DOC_AVG = "records_per_doc_avg";

    private static final String SIZE_BYTES_PER_RECORD_AVG = "bytes_per_record_avg";

    private static final String SIZE_OFFSETS_PER_TERM_AVG = "offsets_per_term_avg";

    private static final String SIZE_OFFSET_BITS_PER_RECORD_AVG = "offset_bits_per_record_avg";

    private static final String SIZE_TAG_OVERHEAD_SZ_MB = "tag_overhead_sz_mb";

    private static final String SIZE_TEXT_OVERHEAD_SZ_MB = "text_overhead_sz_mb";

    private static final String SIZE_TOTAL_INDEX_MEMORY_SZ_MB = "total_index_memory_sz_mb";

    // Indexing statistics keys
    private static final String IDX_HASH_INDEXING_FAILURES = "hash_indexing_failures";

    private static final String IDX_TOTAL_INDEXING_TIME = "total_indexing_time";

    private static final String IDX_INDEXING = "indexing";

    private static final String IDX_PERCENT_INDEXED = "percent_indexed";

    private static final String IDX_NUMBER_OF_USES = "number_of_uses";

    private static final String IDX_CLEANING = "cleaning";

    // GC statistics keys
    private static final String GC_BYTES_COLLECTED = "bytes_collected";

    private static final String GC_TOTAL_MS_RUN = "total_ms_run";

    private static final String GC_TOTAL_CYCLES = "total_cycles";

    private static final String GC_AVERAGE_CYCLE_TIME_MS = "average_cycle_time_ms";

    private static final String GC_LAST_RUN_TIME_MS = "last_run_time_ms";

    private static final String GC_NUMERIC_TREES_MISSED = "gc_numeric_trees_missed";

    private static final String GC_BLOCKS_DENIED = "gc_blocks_denied";

    // Cursor statistics keys
    private static final String CURSOR_GLOBAL_IDLE = "global_idle";

    private static final String CURSOR_GLOBAL_TOTAL = "global_total";

    private static final String CURSOR_INDEX_CAPACITY = "index_capacity";

    private static final String CURSOR_INDEX_TOTAL = "index_total";

    // Dialect statistics keys
    private static final String DIALECT_1 = "dialect_1";

    private static final String DIALECT_2 = "dialect_2";

    private static final String DIALECT_3 = "dialect_3";

    private static final String DIALECT_4 = "dialect_4";

    // Known field names for categorization
    private static final Set<String> SIZE_STATS = new HashSet<>(Arrays.asList(SIZE_INVERTED_SZ_MB, SIZE_VECTOR_INDEX_SZ_MB,
            SIZE_TOTAL_INVERTED_INDEX_BLOCKS, SIZE_OFFSET_VECTORS_SZ_MB, SIZE_DOC_TABLE_SIZE_MB, SIZE_SORTABLE_VALUES_SIZE_MB,
            SIZE_KEY_TABLE_SIZE_MB, SIZE_GEOSHAPES_SZ_MB, SIZE_RECORDS_PER_DOC_AVG, SIZE_BYTES_PER_RECORD_AVG,
            SIZE_OFFSETS_PER_TERM_AVG, SIZE_OFFSET_BITS_PER_RECORD_AVG, SIZE_TAG_OVERHEAD_SZ_MB, SIZE_TEXT_OVERHEAD_SZ_MB,
            SIZE_TOTAL_INDEX_MEMORY_SZ_MB));

    private static final Set<String> INDEXING_STATS = new HashSet<>(Arrays.asList(IDX_HASH_INDEXING_FAILURES,
            IDX_TOTAL_INDEXING_TIME, IDX_INDEXING, IDX_PERCENT_INDEXED, IDX_NUMBER_OF_USES, IDX_CLEANING));

    public IndexInfoParser(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "Codec must not be null");
        this.codec = codec;
    }

    /**
     * Parse the FT.INFO response data, automatically detecting RESP2 vs RESP3 format.
     *
     * @param data the response data from Redis
     * @return an IndexInfo object containing index information and statistics
     */
    @Override
    public IndexInfo<V> parse(ComplexData data) {
        if (data == null) {
            return new IndexInfo<>();
        }

        if (data.isList()) {
            return parseResp2(data);
        }

        return parseResp3(data);
    }

    /**
     * Parse FT.INFO response in RESP2 format (array-based with alternating key-value pairs).
     */
    private IndexInfo<V> parseResp2(ComplexData data) {
        List<Object> infoArray = data.getDynamicList();
        IndexInfo<V> indexInfo = new IndexInfo<>();
        Map<String, Object> sizeStats = new HashMap<>();
        Map<String, Object> indexingStats = new HashMap<>();
        Map<String, Object> gcStats = new HashMap<>();
        Map<String, Object> cursorStats = new HashMap<>();
        Map<String, Object> dialectStats = new HashMap<>();
        Map<String, Object> indexErrors = new HashMap<>();

        // RESP2: Parse alternating key-value pairs
        for (int i = 0; i < infoArray.size(); i += 2) {
            if (i + 1 >= infoArray.size()) {
                break; // Incomplete pair, skip
            }

            String key = decodeStringAsString(infoArray.get(i));
            Object value = infoArray.get(i + 1);
            populateIndexInfo(indexInfo, key, value, sizeStats, indexingStats, gcStats, cursorStats, dialectStats, indexErrors);
        }

        buildStatisticsObjects(indexInfo, sizeStats, indexingStats, gcStats, cursorStats, dialectStats, indexErrors);
        return indexInfo;
    }

    /**
     * Parse FT.INFO response in RESP3 format (native map structure).
     */
    private IndexInfo<V> parseResp3(ComplexData data) {
        Map<Object, Object> rawMap = data.getDynamicMap();
        IndexInfo<V> indexInfo = new IndexInfo<>();
        Map<String, Object> sizeStats = new HashMap<>();
        Map<String, Object> indexingStats = new HashMap<>();
        Map<String, Object> gcStats = new HashMap<>();
        Map<String, Object> cursorStats = new HashMap<>();
        Map<String, Object> dialectStats = new HashMap<>();
        Map<String, Object> indexErrors = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            String key = decodeStringAsString(entry.getKey());
            Object value = entry.getValue();
            populateIndexInfo(indexInfo, key, value, sizeStats, indexingStats, gcStats, cursorStats, dialectStats, indexErrors);
        }

        buildStatisticsObjects(indexInfo, sizeStats, indexingStats, gcStats, cursorStats, dialectStats, indexErrors);
        return indexInfo;
    }

    /**
     * Populate the IndexInfo object based on the key-value pair.
     */
    private void populateIndexInfo(IndexInfo<V> indexInfo, String key, Object value, Map<String, Object> sizeStats,
            Map<String, Object> indexingStats, Map<String, Object> gcStats, Map<String, Object> cursorStats,
            Map<String, Object> dialectStats, Map<String, Object> indexErrors) {
        switch (key) {
            case KEY_INDEX_NAME:
                indexInfo.setIndexName(decodeStringAsString(value));
                break;
            case KEY_INDEX_OPTIONS:
                parseIndexOptions(indexInfo, value);
                break;
            case KEY_INDEX_DEFINITION:
                parseIndexDefinition(indexInfo, value);
                break;
            case KEY_ATTRIBUTES:
                parseAttributes(indexInfo, value);
                break;
            case KEY_NUM_DOCS:
                indexInfo.setNumDocs(parseLong(value));
                break;
            case KEY_MAX_DOC_ID:
                indexInfo.setMaxDocId(parseLong(value));
                break;
            case KEY_NUM_TERMS:
                indexInfo.setNumTerms(parseLong(value));
                break;
            case KEY_NUM_RECORDS:
                indexInfo.setNumRecords(parseLong(value));
                break;
            case KEY_GC_STATS:
                parseGcStats(gcStats, value);
                break;
            case KEY_CURSOR_STATS:
                parseCursorStats(cursorStats, value);
                break;
            case KEY_DIALECT_STATS:
                parseDialectStats(dialectStats, value);
                break;
            case KEY_INDEX_ERRORS:
                parseIndexErrors(indexErrors, value);
                break;
            case KEY_FIELD_STATISTICS:
                parseFieldStatistics(indexInfo, value);
                break;
            default:
                // Categorize statistics fields
                if (SIZE_STATS.contains(key)) {
                    sizeStats.put(key, parseValue(value));
                } else if (INDEXING_STATS.contains(key)) {
                    indexingStats.put(key, parseValue(value));
                } else {
                    // Store unrecognized fields for forward compatibility
                    indexInfo.putAdditionalField(key, parseValue(value));
                }
                break;
        }
    }

    private void parseIndexOptions(IndexInfo<V> indexInfo, Object value) {
        List<Object> options = parseListValue(value);
        for (Object option : options) {
            String optionStr = decodeStringAsString(option).toUpperCase();
            switch (optionStr) {
                case OPT_NOOFFSETS:
                    indexInfo.setNoOffsets();
                    break;
                case OPT_NOHL:
                    indexInfo.setNoHighlight();
                    break;
                case OPT_NOFIELDS:
                    indexInfo.setNoFields();
                    break;
                case OPT_NOFREQS:
                    indexInfo.setNoFrequency();
                    break;
                case OPT_MAXTEXTFIELDS:
                    indexInfo.setMaxTextFields();
                    break;
                case OPT_SKIPINITIALSCAN:
                    indexInfo.setSkipInitialScan();
                    break;
                // Ignore unknown options
            }
        }
    }

    private void parseIndexDefinition(IndexInfo<V> indexInfo, Object value) {
        Map<String, Object> defMap = new LinkedHashMap<>();
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        // Don't call parseValue here - keep raw values for metadata fields
                        defMap.put(decodeStringAsString(list.get(i)), list.get(i + 1));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    // Don't call parseValue here - keep raw values for metadata fields
                    defMap.put(decodeStringAsString(entry.getKey()), entry.getValue());
                }
            }
        }

        // Create IndexDefinition from the map
        IndexInfo.IndexDefinition<V> indexDefinition = new IndexInfo.IndexDefinition<>();

        String keyTypeStr = getString(defMap, DEF_KEY_TYPE);
        if (keyTypeStr != null) {
            try {
                indexDefinition.setKeyType(IndexInfo.IndexDefinition.TargetType.valueOf(keyTypeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Unknown key type, leave as null
            }
        }

        Object prefixesObj = defMap.get(DEF_PREFIXES);
        if (prefixesObj instanceof ComplexData) {
            ComplexData prefixesData = (ComplexData) prefixesObj;
            List<Object> prefixesList = prefixesData.getDynamicList();
            for (Object prefix : prefixesList) {
                indexDefinition.addPrefix(decodeValue(prefix));
            }
        } else if (prefixesObj instanceof List) {
            for (Object prefix : (List<?>) prefixesObj) {
                indexDefinition.addPrefix(decodeValue(prefix));
            }
        }

        V filter = getValue(defMap, DEF_FILTER);
        if (filter != null) {
            indexDefinition.setFilter(filter);
        }

        V languageField = getValue(defMap, DEF_LANGUAGE_FIELD);
        if (languageField != null) {
            indexDefinition.setLanguageField(languageField);
        }

        V scoreField = getValue(defMap, DEF_SCORE_FIELD);
        if (scoreField != null) {
            indexDefinition.setScoreField(scoreField);
        }

        V payloadField = getValue(defMap, DEF_PAYLOAD_FIELD);
        if (payloadField != null) {
            indexDefinition.setPayloadField(payloadField);
        }

        Double defaultScore = getDouble(defMap, DEF_DEFAULT_SCORE);
        if (defaultScore != null) {
            indexDefinition.setDefaultScore(defaultScore);
        }

        V defaultLanguage = getValue(defMap, DEF_DEFAULT_LANGUAGE);
        if (defaultLanguage != null) {
            indexDefinition.setDefaultLanguage(defaultLanguage);
        }

        // Collect additional fields
        Map<String, Object> additionalFields = new LinkedHashMap<>(defMap);
        additionalFields.remove(DEF_KEY_TYPE);
        additionalFields.remove(DEF_PREFIXES);
        additionalFields.remove(DEF_FILTER);
        additionalFields.remove(DEF_LANGUAGE_FIELD);
        additionalFields.remove(DEF_SCORE_FIELD);
        additionalFields.remove(DEF_PAYLOAD_FIELD);
        additionalFields.remove(DEF_DEFAULT_SCORE);
        additionalFields.remove(DEF_DEFAULT_LANGUAGE);

        for (Map.Entry<String, Object> entry : additionalFields.entrySet()) {
            indexDefinition.putAdditionalField(entry.getKey(), entry.getValue());
        }

        indexInfo.setIndexDefinition(indexDefinition);
    }

    private void parseAttributes(IndexInfo<V> indexInfo, Object value) {
        List<Object> attributesList = parseListValue(value);
        for (Object attr : attributesList) {
            Map<String, Object> attributeMap = new LinkedHashMap<>();
            if (attr instanceof List) {
                // Handle case where parseListValue already parsed the ComplexData into a List
                // RESP2: alternating key-value pairs
                List<?> list = (List<?>) attr;
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        String key = decodeStringAsString(list.get(i));
                        Object val = list.get(i + 1);
                        // Handle special case where SORTABLE has a flag as its value (e.g., SORTABLE NOSTEM)
                        // In this case, we need to add both SORTABLE and the flag (NOSTEM) as separate keys
                        if (FLAG_SORTABLE.equals(key)) {
                            attributeMap.put(key, val);
                            String valStr = decodeStringAsString(val);
                            // Check if the value is a known flag
                            if (isFieldFlag(valStr)) {
                                attributeMap.put(valStr, valStr);
                            }
                        } else {
                            attributeMap.put(key, val);
                        }
                    }
                }
            } else if (attr instanceof Map) {
                // Handle case where parseListValue already parsed the map
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) attr;
                attributeMap.putAll(map);
            } else if (attr instanceof ComplexData) {
                ComplexData data = (ComplexData) attr;
                if (data.isList()) {
                    // RESP2: alternating key-value pairs
                    List<Object> list = data.getDynamicList();
                    for (int i = 0; i < list.size(); i += 2) {
                        if (i + 1 < list.size()) {
                            // Don't call parseValue - keep raw values for metadata fields
                            String key = decodeStringAsString(list.get(i));
                            Object val = list.get(i + 1);
                            // Handle special case where SORTABLE has a flag as its value
                            if (FLAG_SORTABLE.equals(key)) {
                                attributeMap.put(key, val);
                                String valStr = decodeStringAsString(val);
                                if (isFieldFlag(valStr)) {
                                    attributeMap.put(valStr, valStr);
                                }
                            } else {
                                attributeMap.put(key, val);
                            }
                        }
                    }
                } else if (data.isMap()) {
                    // RESP3: map
                    Map<Object, Object> map = data.getDynamicMap();
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        // Don't call parseValue - keep raw values for metadata fields
                        attributeMap.put(decodeStringAsString(entry.getKey()), entry.getValue());
                    }
                }
            }
            IndexInfo.Field<V> field = createFieldFromMap(attributeMap);
            if (field != null) {
                indexInfo.addField(field);
            }
        }
    }

    /**
     * Check if a string is a known field flag.
     */
    private boolean isFieldFlag(String str) {
        return FLAG_NOSTEM.equals(str) || FLAG_UNF.equals(str) || FLAG_NOINDEX.equals(str) || FLAG_PHONETIC.equals(str)
                || FLAG_WITHSUFFIXTRIE.equals(str) || FLAG_INDEXEMPTY.equals(str) || FLAG_INDEXMISSING.equals(str);
    }

    /**
     * Creates a Field object from the attribute map based on the field type.
     *
     * @param attributeMap the map containing field attributes
     * @return the appropriate Field subclass instance, or null if type is missing
     */
    private IndexInfo.Field<V> createFieldFromMap(Map<String, Object> attributeMap) {
        // Extract common fields
        V identifier = getValue(attributeMap, ATTR_IDENTIFIER);
        V attribute = getValue(attributeMap, ATTR_ATTRIBUTE);
        String type = getString(attributeMap, ATTR_TYPE);

        if (identifier == null || type == null) {
            return null; // Invalid field definition
        }

        // Extract common boolean flags
        boolean sortable = getBoolean(attributeMap, FLAG_SORTABLE);
        boolean unNormalizedForm = getBoolean(attributeMap, FLAG_UNF);
        boolean noIndex = getBoolean(attributeMap, FLAG_NOINDEX);
        boolean indexEmpty = getBoolean(attributeMap, FLAG_INDEXEMPTY);
        boolean indexMissing = getBoolean(attributeMap, FLAG_INDEXMISSING);

        // Collect additional fields not explicitly mapped
        Map<String, Object> additionalFields = new LinkedHashMap<>(attributeMap);
        additionalFields.remove(ATTR_IDENTIFIER);
        additionalFields.remove(ATTR_ATTRIBUTE);
        additionalFields.remove(ATTR_TYPE);
        additionalFields.remove(FLAG_SORTABLE);
        additionalFields.remove(FLAG_UNF);
        additionalFields.remove(FLAG_NOINDEX);
        additionalFields.remove(FLAG_INDEXEMPTY);
        additionalFields.remove(FLAG_INDEXMISSING);

        // Create the appropriate Field subclass based on type
        switch (type.toUpperCase()) {
            case TYPE_TEXT:
                return createTextField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            case TYPE_NUMERIC:
                return new IndexInfo.NumericField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                        indexMissing, additionalFields);
            case TYPE_TAG:
                return createTagField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            case TYPE_GEO:
                return new IndexInfo.GeoField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                        indexMissing, additionalFields);
            case TYPE_GEOSHAPE:
                return createGeoshapeField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            case TYPE_VECTOR:
                return createVectorField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            default:
                // Unknown field type - store all data in additionalFields
                additionalFields.put(ATTR_TYPE, type);
                return new IndexInfo.NumericField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                        indexMissing, additionalFields);
        }
    }

    private IndexInfo.TextField<V> createTextField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm,
            boolean noIndex, boolean indexEmpty, boolean indexMissing, Map<String, Object> attributeMap,
            Map<String, Object> additionalFields) {
        Double weight = parseDouble(attributeMap.get(FLAG_WEIGHT));
        boolean noStem = getBoolean(attributeMap, FLAG_NOSTEM);
        String phonetic = getString(attributeMap, FLAG_PHONETIC);
        boolean withSuffixTrie = getBoolean(attributeMap, FLAG_WITHSUFFIXTRIE);

        additionalFields.remove(FLAG_WEIGHT);
        additionalFields.remove(FLAG_NOSTEM);
        additionalFields.remove(FLAG_PHONETIC);
        additionalFields.remove(FLAG_WITHSUFFIXTRIE);

        return new IndexInfo.TextField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                weight, noStem, phonetic, withSuffixTrie, additionalFields);
    }

    private IndexInfo.TagField<V> createTagField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm,
            boolean noIndex, boolean indexEmpty, boolean indexMissing, Map<String, Object> attributeMap,
            Map<String, Object> additionalFields) {
        String separator = getString(attributeMap, FLAG_SEPARATOR);
        boolean caseSensitive = getBoolean(attributeMap, FLAG_CASESENSITIVE);
        boolean withSuffixTrie = getBoolean(attributeMap, FLAG_WITHSUFFIXTRIE);

        additionalFields.remove(FLAG_SEPARATOR);
        additionalFields.remove(FLAG_CASESENSITIVE);
        additionalFields.remove(FLAG_WITHSUFFIXTRIE);

        return new IndexInfo.TagField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                separator, caseSensitive, withSuffixTrie, additionalFields);
    }

    private IndexInfo.GeoshapeField<V> createGeoshapeField(V identifier, V attribute, boolean sortable,
            boolean unNormalizedForm, boolean noIndex, boolean indexEmpty, boolean indexMissing,
            Map<String, Object> attributeMap, Map<String, Object> additionalFields) {
        String coordinateSystemStr = getString(attributeMap, FLAG_COORD_SYSTEM);
        IndexInfo.GeoshapeField.CoordinateSystem coordinateSystem = null;
        if (coordinateSystemStr != null) {
            try {
                coordinateSystem = IndexInfo.GeoshapeField.CoordinateSystem.valueOf(coordinateSystemStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Unknown coordinate system, leave as null
            }
        }

        additionalFields.remove(FLAG_COORD_SYSTEM);

        return new IndexInfo.GeoshapeField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                indexMissing, coordinateSystem, additionalFields);
    }

    private IndexInfo.VectorField<V> createVectorField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm,
            boolean noIndex, boolean indexEmpty, boolean indexMissing, Map<String, Object> attributeMap,
            Map<String, Object> additionalFields) {
        String algorithmStr = getString(attributeMap, FLAG_ALGORITHM);
        IndexInfo.VectorField.Algorithm algorithm = null;
        if (algorithmStr != null) {
            try {
                // Handle SVS-VAMANA special case
                String algName = algorithmStr.toUpperCase().replace("-", "_");
                algorithm = IndexInfo.VectorField.Algorithm.valueOf(algName);
            } catch (IllegalArgumentException e) {
                // Unknown algorithm, leave as null
            }
        }

        // Vector attributes are stored in a nested map
        Map<String, Object> vectorAttributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
            String key = entry.getKey();
            // Skip common fields and algorithm
            if (!key.equals(ATTR_IDENTIFIER) && !key.equals(ATTR_ATTRIBUTE) && !key.equals(ATTR_TYPE)
                    && !key.equals(FLAG_ALGORITHM) && !key.equals(FLAG_SORTABLE) && !key.equals(FLAG_UNF)
                    && !key.equals(FLAG_NOINDEX) && !key.equals(FLAG_INDEXEMPTY) && !key.equals(FLAG_INDEXMISSING)) {
                vectorAttributes.put(key, entry.getValue());
            }
        }

        additionalFields.remove(FLAG_ALGORITHM);
        // Remove vector attributes from additionalFields as they're in vectorAttributes
        for (String key : vectorAttributes.keySet()) {
            additionalFields.remove(key);
        }

        return new IndexInfo.VectorField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                algorithm, vectorAttributes, additionalFields);
    }

    private void parseGcStats(Map<String, Object> gcStats, Object value) {
        parseKeyValuePairs(value, gcStats, true);
    }

    private void parseCursorStats(Map<String, Object> cursorStats, Object value) {
        parseKeyValuePairs(value, cursorStats, true);
    }

    private void parseDialectStats(Map<String, Object> dialectStats, Object value) {
        parseKeyValuePairs(value, dialectStats, false);
    }

    private void parseIndexErrors(Map<String, Object> indexErrors, Object value) {
        parseKeyValuePairs(value, indexErrors, true);
    }

    /**
     * Parse key-value pairs from ComplexData into a map. Handles both RESP2 (alternating key-value list) and RESP3 (native map)
     * formats.
     *
     * @param value the ComplexData to parse
     * @param targetMap the map to populate with parsed key-value pairs
     * @param parseValues if true, values are parsed using parseValue(); if false, values are parsed as Long
     */
    private void parseKeyValuePairs(Object value, Map<String, Object> targetMap, boolean parseValues) {
        if (!(value instanceof ComplexData)) {
            return;
        }
        ComplexData data = (ComplexData) value;
        if (data.isList()) {
            // RESP2: alternating key-value pairs
            List<Object> list = data.getDynamicList();
            for (int i = 0; i < list.size(); i += 2) {
                if (i + 1 < list.size()) {
                    String key = decodeStringAsString(list.get(i));
                    Object parsedValue = parseValues ? parseValue(list.get(i + 1)) : parseLong(list.get(i + 1));
                    targetMap.put(key, parsedValue);
                }
            }
        } else if (data.isMap()) {
            // RESP3: map
            Map<Object, Object> map = data.getDynamicMap();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                String key = decodeStringAsString(entry.getKey());
                Object parsedValue = parseValues ? parseValue(entry.getValue()) : parseLong(entry.getValue());
                targetMap.put(key, parsedValue);
            }
        }
    }

    private void parseFieldStatistics(IndexInfo<V> indexInfo, Object value) {
        if (!(value instanceof ComplexData)) {
            return;
        }
        ComplexData fieldStatsData = (ComplexData) value;
        List<Object> fieldStatsList = fieldStatsData.getDynamicList();

        for (Object fieldStat : fieldStatsList) {
            Map<String, Object> fieldStatMap = new LinkedHashMap<>();
            if (fieldStat instanceof ComplexData) {
                ComplexData data = (ComplexData) fieldStat;
                if (data.isList()) {
                    // RESP2: alternating key-value pairs
                    List<Object> list = data.getDynamicList();
                    for (int i = 0; i < list.size(); i += 2) {
                        if (i + 1 < list.size()) {
                            fieldStatMap.put(decodeStringAsString(list.get(i)), parseValue(list.get(i + 1)));
                        }
                    }
                } else if (data.isMap()) {
                    // RESP3: map
                    Map<Object, Object> map = data.getDynamicMap();
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        fieldStatMap.put(decodeStringAsString(entry.getKey()), parseValue(entry.getValue()));
                    }
                }
            }

            // Build FieldErrorStatistics from the map
            IndexInfo.FieldErrorStatistics fieldErrorStats = new IndexInfo.FieldErrorStatistics();

            String identifier = getString(fieldStatMap, ATTR_IDENTIFIER);
            if (identifier != null) {
                fieldErrorStats.setIdentifier(identifier);
            }

            String attribute = getString(fieldStatMap, ATTR_ATTRIBUTE);
            if (attribute != null) {
                fieldErrorStats.setAttribute(attribute);
            }

            Object indexErrorsObj = fieldStatMap.get(KEY_INDEX_ERRORS);
            if (indexErrorsObj != null) {
                Map<String, Object> errorsMap = new LinkedHashMap<>();
                if (indexErrorsObj instanceof ComplexData) {
                    ComplexData errorsData = (ComplexData) indexErrorsObj;
                    if (errorsData.isList()) {
                        List<Object> errorsList = errorsData.getDynamicList();
                        for (int i = 0; i < errorsList.size(); i += 2) {
                            if (i + 1 < errorsList.size()) {
                                errorsMap.put(decodeStringAsString(errorsList.get(i)), parseValue(errorsList.get(i + 1)));
                            }
                        }
                    } else if (errorsData.isMap()) {
                        Map<Object, Object> errorsRawMap = errorsData.getDynamicMap();
                        for (Map.Entry<Object, Object> entry : errorsRawMap.entrySet()) {
                            errorsMap.put(decodeStringAsString(entry.getKey()), parseValue(entry.getValue()));
                        }
                    }
                } else if (indexErrorsObj instanceof Map) {
                    // Already parsed to Map by parseValue() (RESP3)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsedMap = (Map<String, Object>) indexErrorsObj;
                    errorsMap.putAll(parsedMap);
                } else if (indexErrorsObj instanceof List) {
                    // Already parsed to List by parseValue() (RESP2 alternating key-value pairs)
                    @SuppressWarnings("unchecked")
                    List<Object> errorsList = (List<Object>) indexErrorsObj;
                    for (int i = 0; i < errorsList.size(); i += 2) {
                        if (i + 1 < errorsList.size()) {
                            errorsMap.put(decodeStringAsString(errorsList.get(i)), errorsList.get(i + 1));
                        }
                    }
                }
                IndexInfo.ErrorStatistics errors = buildErrorStatistics(errorsMap);
                fieldErrorStats.setErrors(errors);
            }

            indexInfo.addFieldStatistic(fieldErrorStats);
        }
    }

    /**
     * Parse a value which can be a simple type, list, or nested map.
     */
    private Object parseValue(Object value) {
        if (value instanceof ByteBuffer) {
            return decodeValue(value);
        } else if (value instanceof ComplexData) {
            ComplexData complexData = (ComplexData) value;
            if (complexData.isList()) {
                return parseList(complexData);
            } else if (complexData.isMap()) {
                return parseMap(complexData);
            }
        } else if (value instanceof Number) {
            return value;
        }
        return value;
    }

    /**
     * Parse a list value.
     */
    private List<Object> parseList(ComplexData data) {
        List<Object> list = data.getDynamicList();
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(parseValue(item));
        }
        return result;
    }

    private List<Object> parseListValue(Object value) {
        if (value instanceof ComplexData) {
            return parseList((ComplexData) value);
        }
        return new ArrayList<>();
    }

    /**
     * Parse a map value.
     */
    private Map<String, Object> parseMap(ComplexData data) {
        Map<Object, Object> rawMap = data.getDynamicMap();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            String key = decodeStringAsString(entry.getKey());
            Object value = parseValue(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    /**
     * Build strongly-typed statistics objects from the collected maps.
     */
    private void buildStatisticsObjects(IndexInfo<V> indexInfo, Map<String, Object> sizeMap, Map<String, Object> indexingMap,
            Map<String, Object> gcMap, Map<String, Object> cursorMap, Map<String, Object> dialectMap,
            Map<String, Object> indexErrorsMap) {
        // Build SizeStatistics
        if (!sizeMap.isEmpty()) {
            IndexInfo.SizeStatistics sizeStats = indexInfo.getSizeStats();
            sizeStats.setInvertedSizeMb(parseDouble(sizeMap.get(SIZE_INVERTED_SZ_MB)));
            sizeStats.setVectorIndexSizeMb(parseDouble(sizeMap.get(SIZE_VECTOR_INDEX_SZ_MB)));
            sizeStats.setTotalInvertedIndexBlocks(parseLong(sizeMap.get(SIZE_TOTAL_INVERTED_INDEX_BLOCKS)));
            sizeStats.setOffsetVectorsSizeMb(parseDouble(sizeMap.get(SIZE_OFFSET_VECTORS_SZ_MB)));
            sizeStats.setDocTableSizeMb(parseDouble(sizeMap.get(SIZE_DOC_TABLE_SIZE_MB)));
            sizeStats.setSortableValuesSizeMb(parseDouble(sizeMap.get(SIZE_SORTABLE_VALUES_SIZE_MB)));
            sizeStats.setKeyTableSizeMb(parseDouble(sizeMap.get(SIZE_KEY_TABLE_SIZE_MB)));
            sizeStats.setGeoshapesSizeMb(parseDouble(sizeMap.get(SIZE_GEOSHAPES_SZ_MB)));
            sizeStats.setRecordsPerDocAvg(parseDouble(sizeMap.get(SIZE_RECORDS_PER_DOC_AVG)));
            sizeStats.setBytesPerRecordAvg(parseDouble(sizeMap.get(SIZE_BYTES_PER_RECORD_AVG)));
            sizeStats.setOffsetsPerTermAvg(parseDouble(sizeMap.get(SIZE_OFFSETS_PER_TERM_AVG)));
            sizeStats.setOffsetBitsPerRecordAvg(parseDouble(sizeMap.get(SIZE_OFFSET_BITS_PER_RECORD_AVG)));
            sizeStats.setTagOverheadSizeMb(parseDouble(sizeMap.get(SIZE_TAG_OVERHEAD_SZ_MB)));
            sizeStats.setTextOverheadSizeMb(parseDouble(sizeMap.get(SIZE_TEXT_OVERHEAD_SZ_MB)));
            sizeStats.setTotalIndexMemorySizeMb(parseDouble(sizeMap.get(SIZE_TOTAL_INDEX_MEMORY_SZ_MB)));
        }

        // Build IndexingStatistics
        if (!indexingMap.isEmpty()) {
            IndexInfo.IndexingStatistics indexingStats = indexInfo.getIndexingStats();
            indexingStats.setHashIndexingFailures(parseLong(indexingMap.get(IDX_HASH_INDEXING_FAILURES)));
            indexingStats.setTotalIndexingTime(parseDouble(indexingMap.get(IDX_TOTAL_INDEXING_TIME)));
            indexingStats.setIndexing(parseBoolean(indexingMap.get(IDX_INDEXING)));
            indexingStats.setPercentIndexed(parseDouble(indexingMap.get(IDX_PERCENT_INDEXED)));
            indexingStats.setNumberOfUses(parseLong(indexingMap.get(IDX_NUMBER_OF_USES)));
            indexingStats.setCleaning(parseBoolean(indexingMap.get(IDX_CLEANING)));
        }

        // Build GcStatistics
        if (!gcMap.isEmpty()) {
            IndexInfo.GcStatistics gcStats = indexInfo.getGcStats();
            gcStats.setBytesCollected(parseLong(gcMap.get(GC_BYTES_COLLECTED)));
            gcStats.setTotalMsRun(parseDouble(gcMap.get(GC_TOTAL_MS_RUN)));
            gcStats.setTotalCycles(parseLong(gcMap.get(GC_TOTAL_CYCLES)));
            gcStats.setAverageCycleTimeMs(parseDouble(gcMap.get(GC_AVERAGE_CYCLE_TIME_MS)));
            gcStats.setLastRunTimeMs(parseDouble(gcMap.get(GC_LAST_RUN_TIME_MS)));
            gcStats.setGcNumericTreesMissed(parseLong(gcMap.get(GC_NUMERIC_TREES_MISSED)));
            gcStats.setGcBlocksDenied(parseLong(gcMap.get(GC_BLOCKS_DENIED)));
        }

        // Build CursorStatistics
        if (!cursorMap.isEmpty()) {
            IndexInfo.CursorStatistics cursorStats = indexInfo.getCursorStats();
            cursorStats.setGlobalIdle(parseLong(cursorMap.get(CURSOR_GLOBAL_IDLE)));
            cursorStats.setGlobalTotal(parseLong(cursorMap.get(CURSOR_GLOBAL_TOTAL)));
            cursorStats.setIndexCapacity(parseLong(cursorMap.get(CURSOR_INDEX_CAPACITY)));
            cursorStats.setIndexTotal(parseLong(cursorMap.get(CURSOR_INDEX_TOTAL)));
        }

        // Build DialectStatistics
        if (!dialectMap.isEmpty()) {
            IndexInfo.DialectStatistics dialectStats = indexInfo.getDialectStats();
            dialectStats.setDialect1(parseLong(dialectMap.get(DIALECT_1)));
            dialectStats.setDialect2(parseLong(dialectMap.get(DIALECT_2)));
            dialectStats.setDialect3(parseLong(dialectMap.get(DIALECT_3)));
            dialectStats.setDialect4(parseLong(dialectMap.get(DIALECT_4)));
        }

        // Build ErrorStatistics for index errors
        if (!indexErrorsMap.isEmpty()) {
            IndexInfo.ErrorStatistics errorStats = buildErrorStatistics(indexErrorsMap);
            indexInfo.setIndexErrors(errorStats);
        }
    }

    /**
     * Build ErrorStatistics from a map.
     */
    private IndexInfo.ErrorStatistics buildErrorStatistics(Map<String, Object> errorsMap) {
        IndexInfo.ErrorStatistics errorStats = new IndexInfo.ErrorStatistics();
        errorStats.setIndexingFailures(parseLong(errorsMap.get(ERR_INDEXING_FAILURES)));
        String lastError = parseString(errorsMap.get(ERR_LAST_INDEXING_ERROR));
        if (lastError != null) {
            errorStats.setLastIndexingError(lastError);
        }
        String lastErrorKey = parseString(errorsMap.get(ERR_LAST_INDEXING_ERROR_KEY));
        if (lastErrorKey != null) {
            errorStats.setLastIndexingErrorKey(lastErrorKey);
        }
        return errorStats;
    }

    /**
     * Parse a Long value from various types.
     */
    private long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof ByteBuffer) {
            String str = decodeStringAsString(value);
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Parse a Double value from various types.
     */
    private double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof ByteBuffer) {
            String str = decodeStringAsString(value);
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Parse a Boolean value from various types. Redis returns 0/1 for boolean values.
     */
    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof ByteBuffer) {
            String str = decodeStringAsString(value);
            try {
                return Integer.parseInt(str) != 0;
            } catch (NumberFormatException e) {
                return Boolean.parseBoolean(str);
            }
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value) != 0;
            } catch (NumberFormatException e) {
                return Boolean.parseBoolean((String) value);
            }
        }
        return false;
    }

    /**
     * Parse a String value from various types.
     */
    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return decodeStringAsString(value);
    }

    /**
     * Decode a ByteBuffer or other object to type V.
     */
    @SuppressWarnings("unchecked")
    private V decodeValue(Object obj) {
        if (obj instanceof ByteBuffer) {
            return codec.decodeValue((ByteBuffer) obj);
        }
        return (V) obj;
    }

    /**
     * Decode a ByteBuffer or other object to a String. This is used for keys and metadata that should always be strings,
     * regardless of the codec.
     */
    private String decodeStringAsString(Object obj) {
        if (obj instanceof ByteBuffer) {
            return StringCodec.UTF8.decodeValue((ByteBuffer) obj);
        }
        if (obj instanceof byte[]) {
            return new String((byte[]) obj, StandardCharsets.UTF_8);
        }
        return obj != null ? obj.toString() : null;
    }

    /**
     * Get a value of type V from the map.
     */
    private V getValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        // Decode ByteBuffer to type V
        return decodeValue(value);
    }

    /**
     * Get a String value from the map.
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        // Decode ByteBuffer to string
        return decodeStringAsString(value);
    }

    /**
     * Get a Double value from the map.
     */
    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        // Decode ByteBuffer to string and parse as double
        String str = decodeStringAsString(value);
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get a boolean value from the map. Returns true if the key exists (regardless of value). Also checks if the key is in the
     * "flags" list.
     */
    private boolean getBoolean(Map<String, Object> map, String key) {
        // Check if key exists directly in the map
        if (map.containsKey(key)) {
            return true;
        }
        // Check if key is in the "flags" list
        Object flags = map.get(ATTR_FLAGS);
        if (flags instanceof List) {
            List<?> flagsList = (List<?>) flags;
            for (Object flag : flagsList) {
                String flagStr = null;
                if (flag instanceof String) {
                    flagStr = (String) flag;
                } else {
                    flagStr = decodeStringAsString(flag);
                }
                if (key.equals(flagStr)) {
                    return true;
                }
            }
        }
        return false;
    }

}
