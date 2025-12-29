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

    // Known field names for categorization
    private static final Set<String> SIZE_STATS = new HashSet<>(Arrays.asList("inverted_sz_mb", "vector_index_sz_mb",
            "total_inverted_index_blocks", "offset_vectors_sz_mb", "doc_table_size_mb", "sortable_values_size_mb",
            "key_table_size_mb", "geoshapes_sz_mb", "records_per_doc_avg", "bytes_per_record_avg", "offsets_per_term_avg",
            "offset_bits_per_record_avg", "tag_overhead_sz_mb", "text_overhead_sz_mb", "total_index_memory_sz_mb"));

    private static final Set<String> INDEXING_STATS = new HashSet<>(Arrays.asList("hash_indexing_failures",
            "total_indexing_time", "indexing", "percent_indexed", "number_of_uses", "cleaning"));

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
            case "index_name":
                indexInfo.setIndexName(decodeStringAsString(value));
                break;
            case "index_options":
                parseIndexOptions(indexInfo, value);
                break;
            case "index_definition":
                parseIndexDefinition(indexInfo, value);
                break;
            case "attributes":
                parseAttributes(indexInfo, value);
                break;
            case "num_docs":
                indexInfo.setNumDocs(parseLong(value));
                break;
            case "max_doc_id":
                indexInfo.setMaxDocId(parseLong(value));
                break;
            case "num_terms":
                indexInfo.setNumTerms(parseLong(value));
                break;
            case "num_records":
                indexInfo.setNumRecords(parseLong(value));
                break;
            case "gc_stats":
                parseGcStats(gcStats, value);
                break;
            case "cursor_stats":
                parseCursorStats(cursorStats, value);
                break;
            case "dialect_stats":
                parseDialectStats(dialectStats, value);
                break;
            case "Index Errors":
                parseIndexErrors(indexErrors, value);
                break;
            case "field statistics":
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
                case "NOOFFSETS":
                    indexInfo.setNoOffsets(true);
                    break;
                case "NOHL":
                    indexInfo.setNoHighlight(true);
                    break;
                case "NOFIELDS":
                    indexInfo.setNoFields(true);
                    break;
                case "NOFREQS":
                    indexInfo.setNoFrequency(true);
                    break;
                case "MAXTEXTFIELDS":
                    indexInfo.setMaxTextFields(true);
                    break;
                case "SKIPINITIALSCAN":
                    indexInfo.setSkipInitialScan(true);
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

        String keyTypeStr = getString(defMap, "key_type");
        if (keyTypeStr != null) {
            try {
                indexDefinition.setKeyType(IndexInfo.IndexDefinition.TargetType.valueOf(keyTypeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Unknown key type, leave as null
            }
        }

        Object prefixesObj = defMap.get("prefixes");
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

        V filter = getValue(defMap, "filter");
        if (filter != null) {
            indexDefinition.setFilter(filter);
        }

        V languageField = getValue(defMap, "language_field");
        if (languageField != null) {
            indexDefinition.setLanguageField(languageField);
        }

        V scoreField = getValue(defMap, "score_field");
        if (scoreField != null) {
            indexDefinition.setScoreField(scoreField);
        }

        V payloadField = getValue(defMap, "payload_field");
        if (payloadField != null) {
            indexDefinition.setPayloadField(payloadField);
        }

        Double defaultScore = getDouble(defMap, "default_score");
        if (defaultScore != null) {
            indexDefinition.setDefaultScore(defaultScore);
        }

        V defaultLanguage = getValue(defMap, "default_language");
        if (defaultLanguage != null) {
            indexDefinition.setDefaultLanguage(defaultLanguage);
        }

        // Collect additional fields
        Map<String, Object> additionalFields = new LinkedHashMap<>(defMap);
        additionalFields.remove("key_type");
        additionalFields.remove("prefixes");
        additionalFields.remove("filter");
        additionalFields.remove("language_field");
        additionalFields.remove("score_field");
        additionalFields.remove("payload_field");
        additionalFields.remove("default_score");
        additionalFields.remove("default_language");

        for (Map.Entry<String, Object> entry : additionalFields.entrySet()) {
            indexDefinition.putAdditionalField(entry.getKey(), entry.getValue());
        }

        indexInfo.setIndexDefinition(indexDefinition);
    }

    private void parseAttributes(IndexInfo<V> indexInfo, Object value) {
        List<Object> attributesList = parseListValue(value);
        for (Object attr : attributesList) {
            Map<String, Object> attributeMap = new LinkedHashMap<>();
            if (attr instanceof ComplexData) {
                ComplexData data = (ComplexData) attr;
                if (data.isList()) {
                    // RESP2: alternating key-value pairs
                    List<Object> list = data.getDynamicList();
                    for (int i = 0; i < list.size(); i += 2) {
                        if (i + 1 < list.size()) {
                            // Don't call parseValue - keep raw values for metadata fields
                            attributeMap.put(decodeStringAsString(list.get(i)), list.get(i + 1));
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
            } else if (attr instanceof Map) {
                // Handle case where parseListValue already parsed the map
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) attr;
                attributeMap.putAll(map);
            }
            IndexInfo.Field<V> field = createFieldFromMap(attributeMap);
            if (field != null) {
                indexInfo.addField(field);
            }
        }
    }

    /**
     * Creates a Field object from the attribute map based on the field type.
     *
     * @param attributeMap the map containing field attributes
     * @return the appropriate Field subclass instance, or null if type is missing
     */
    private IndexInfo.Field<V> createFieldFromMap(Map<String, Object> attributeMap) {
        // Extract common fields
        V identifier = getValue(attributeMap, "identifier");
        V attribute = getValue(attributeMap, "attribute");
        String type = getString(attributeMap, "type");

        if (identifier == null || type == null) {
            return null; // Invalid field definition
        }

        // Extract common boolean flags
        boolean sortable = getBoolean(attributeMap, "SORTABLE");
        boolean unNormalizedForm = getBoolean(attributeMap, "UNF");
        boolean noIndex = getBoolean(attributeMap, "NOINDEX");
        boolean indexEmpty = getBoolean(attributeMap, "INDEXEMPTY");
        boolean indexMissing = getBoolean(attributeMap, "INDEXMISSING");

        // Collect additional fields not explicitly mapped
        Map<String, Object> additionalFields = new LinkedHashMap<>(attributeMap);
        additionalFields.remove("identifier");
        additionalFields.remove("attribute");
        additionalFields.remove("type");
        additionalFields.remove("SORTABLE");
        additionalFields.remove("UNF");
        additionalFields.remove("NOINDEX");
        additionalFields.remove("INDEXEMPTY");
        additionalFields.remove("INDEXMISSING");

        // Create the appropriate Field subclass based on type
        switch (type.toUpperCase()) {
            case "TEXT":
                return createTextField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            case "NUMERIC":
                return new IndexInfo.NumericField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                        indexMissing, additionalFields);
            case "TAG":
                return createTagField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            case "GEO":
                return new IndexInfo.GeoField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                        indexMissing, additionalFields);
            case "GEOSHAPE":
                return createGeoshapeField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            case "VECTOR":
                return createVectorField(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                        attributeMap, additionalFields);
            default:
                // Unknown field type - store all data in additionalFields
                additionalFields.put("type", type);
                return new IndexInfo.NumericField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                        indexMissing, additionalFields);
        }
    }

    private IndexInfo.TextField<V> createTextField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm,
            boolean noIndex, boolean indexEmpty, boolean indexMissing, Map<String, Object> attributeMap,
            Map<String, Object> additionalFields) {
        Double weight = parseDouble(attributeMap.get("WEIGHT"));
        boolean noStem = getBoolean(attributeMap, "NOSTEM");
        String phonetic = getString(attributeMap, "PHONETIC");
        boolean withSuffixTrie = getBoolean(attributeMap, "WITHSUFFIXTRIE");

        additionalFields.remove("WEIGHT");
        additionalFields.remove("NOSTEM");
        additionalFields.remove("PHONETIC");
        additionalFields.remove("WITHSUFFIXTRIE");

        return new IndexInfo.TextField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                weight, noStem, phonetic, withSuffixTrie, additionalFields);
    }

    private IndexInfo.TagField<V> createTagField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm,
            boolean noIndex, boolean indexEmpty, boolean indexMissing, Map<String, Object> attributeMap,
            Map<String, Object> additionalFields) {
        String separator = getString(attributeMap, "SEPARATOR");
        boolean caseSensitive = getBoolean(attributeMap, "CASESENSITIVE");
        boolean withSuffixTrie = getBoolean(attributeMap, "WITHSUFFIXTRIE");

        additionalFields.remove("SEPARATOR");
        additionalFields.remove("CASESENSITIVE");
        additionalFields.remove("WITHSUFFIXTRIE");

        return new IndexInfo.TagField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                separator, caseSensitive, withSuffixTrie, additionalFields);
    }

    private IndexInfo.GeoshapeField<V> createGeoshapeField(V identifier, V attribute, boolean sortable,
            boolean unNormalizedForm, boolean noIndex, boolean indexEmpty, boolean indexMissing,
            Map<String, Object> attributeMap, Map<String, Object> additionalFields) {
        String coordinateSystemStr = getString(attributeMap, "COORD_SYSTEM");
        IndexInfo.GeoshapeField.CoordinateSystem coordinateSystem = null;
        if (coordinateSystemStr != null) {
            try {
                coordinateSystem = IndexInfo.GeoshapeField.CoordinateSystem.valueOf(coordinateSystemStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Unknown coordinate system, leave as null
            }
        }

        additionalFields.remove("COORD_SYSTEM");

        return new IndexInfo.GeoshapeField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty,
                indexMissing, coordinateSystem, additionalFields);
    }

    private IndexInfo.VectorField<V> createVectorField(V identifier, V attribute, boolean sortable, boolean unNormalizedForm,
            boolean noIndex, boolean indexEmpty, boolean indexMissing, Map<String, Object> attributeMap,
            Map<String, Object> additionalFields) {
        String algorithmStr = getString(attributeMap, "ALGORITHM");
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
            if (!key.equals("identifier") && !key.equals("attribute") && !key.equals("type") && !key.equals("ALGORITHM")
                    && !key.equals("SORTABLE") && !key.equals("UNF") && !key.equals("NOINDEX") && !key.equals("INDEXEMPTY")
                    && !key.equals("INDEXMISSING")) {
                vectorAttributes.put(key, entry.getValue());
            }
        }

        additionalFields.remove("ALGORITHM");
        // Remove vector attributes from additionalFields as they're in vectorAttributes
        for (String key : vectorAttributes.keySet()) {
            additionalFields.remove(key);
        }

        return new IndexInfo.VectorField<>(identifier, attribute, sortable, unNormalizedForm, noIndex, indexEmpty, indexMissing,
                algorithm, vectorAttributes, additionalFields);
    }

    private void parseGcStats(Map<String, Object> gcStats, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        String key = decodeStringAsString(list.get(i));
                        Object parsedValue = parseValue(list.get(i + 1));
                        gcStats.put(key, parsedValue);
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    String key = decodeStringAsString(entry.getKey());
                    Object parsedValue = parseValue(entry.getValue());
                    gcStats.put(key, parsedValue);
                }
            }
        }
    }

    private void parseCursorStats(Map<String, Object> cursorStats, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        String key = decodeStringAsString(list.get(i));
                        Object parsedValue = parseValue(list.get(i + 1));
                        cursorStats.put(key, parsedValue);
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    String key = decodeStringAsString(entry.getKey());
                    Object parsedValue = parseValue(entry.getValue());
                    cursorStats.put(key, parsedValue);
                }
            }
        }
    }

    private void parseDialectStats(Map<String, Object> dialectStats, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        dialectStats.put(decodeStringAsString(list.get(i)), parseLong(list.get(i + 1)));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    dialectStats.put(decodeStringAsString(entry.getKey()), parseLong(entry.getValue()));
                }
            }
        }
    }

    private void parseIndexErrors(Map<String, Object> indexErrors, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        indexErrors.put(decodeStringAsString(list.get(i)), parseValue(list.get(i + 1)));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    indexErrors.put(decodeStringAsString(entry.getKey()), parseValue(entry.getValue()));
                }
            }
        }
    }

    private void parseFieldStatistics(IndexInfo<V> indexInfo, Object value) {
        List<Object> fieldStatsList = parseListValue(value);
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

            String identifier = (String) fieldStatMap.get("identifier");
            if (identifier != null) {
                fieldErrorStats.setIdentifier(identifier);
            }

            String attribute = (String) fieldStatMap.get("attribute");
            if (attribute != null) {
                fieldErrorStats.setAttribute(attribute);
            }

            Object indexErrorsObj = fieldStatMap.get("Index Errors");
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
            sizeStats.setInvertedSizeMb(parseDouble(sizeMap.get("inverted_sz_mb")));
            sizeStats.setVectorIndexSizeMb(parseDouble(sizeMap.get("vector_index_sz_mb")));
            sizeStats.setTotalInvertedIndexBlocks(parseLong(sizeMap.get("total_inverted_index_blocks")));
            sizeStats.setOffsetVectorsSizeMb(parseDouble(sizeMap.get("offset_vectors_sz_mb")));
            sizeStats.setDocTableSizeMb(parseDouble(sizeMap.get("doc_table_size_mb")));
            sizeStats.setSortableValuesSizeMb(parseDouble(sizeMap.get("sortable_values_size_mb")));
            sizeStats.setKeyTableSizeMb(parseDouble(sizeMap.get("key_table_size_mb")));
            sizeStats.setGeoshapesSizeMb(parseDouble(sizeMap.get("geoshapes_sz_mb")));
            sizeStats.setRecordsPerDocAvg(parseDouble(sizeMap.get("records_per_doc_avg")));
            sizeStats.setBytesPerRecordAvg(parseDouble(sizeMap.get("bytes_per_record_avg")));
            sizeStats.setOffsetsPerTermAvg(parseDouble(sizeMap.get("offsets_per_term_avg")));
            sizeStats.setOffsetBitsPerRecordAvg(parseDouble(sizeMap.get("offset_bits_per_record_avg")));
            sizeStats.setTagOverheadSizeMb(parseDouble(sizeMap.get("tag_overhead_sz_mb")));
            sizeStats.setTextOverheadSizeMb(parseDouble(sizeMap.get("text_overhead_sz_mb")));
            sizeStats.setTotalIndexMemorySizeMb(parseDouble(sizeMap.get("total_index_memory_sz_mb")));
        }

        // Build IndexingStatistics
        if (!indexingMap.isEmpty()) {
            IndexInfo.IndexingStatistics indexingStats = indexInfo.getIndexingStats();
            indexingStats.setHashIndexingFailures(parseLong(indexingMap.get("hash_indexing_failures")));
            indexingStats.setTotalIndexingTime(parseDouble(indexingMap.get("total_indexing_time")));
            indexingStats.setIndexing(parseBoolean(indexingMap.get("indexing")));
            indexingStats.setPercentIndexed(parseDouble(indexingMap.get("percent_indexed")));
            indexingStats.setNumberOfUses(parseLong(indexingMap.get("number_of_uses")));
            indexingStats.setCleaning(parseBoolean(indexingMap.get("cleaning")));
        }

        // Build GcStatistics
        if (!gcMap.isEmpty()) {
            IndexInfo.GcStatistics gcStats = indexInfo.getGcStats();
            gcStats.setBytesCollected(parseLong(gcMap.get("bytes_collected")));
            gcStats.setTotalMsRun(parseDouble(gcMap.get("total_ms_run")));
            gcStats.setTotalCycles(parseLong(gcMap.get("total_cycles")));
            gcStats.setAverageCycleTimeMs(parseDouble(gcMap.get("average_cycle_time_ms")));
            gcStats.setLastRunTimeMs(parseDouble(gcMap.get("last_run_time_ms")));
            gcStats.setGcNumericTreesMissed(parseLong(gcMap.get("gc_numeric_trees_missed")));
            gcStats.setGcBlocksDenied(parseLong(gcMap.get("gc_blocks_denied")));
        }

        // Build CursorStatistics
        if (!cursorMap.isEmpty()) {
            IndexInfo.CursorStatistics cursorStats = indexInfo.getCursorStats();
            cursorStats.setGlobalIdle(parseLong(cursorMap.get("global_idle")));
            cursorStats.setGlobalTotal(parseLong(cursorMap.get("global_total")));
            cursorStats.setIndexCapacity(parseLong(cursorMap.get("index_capacity")));
            cursorStats.setIndexTotal(parseLong(cursorMap.get("index_total")));
        }

        // Build DialectStatistics
        if (!dialectMap.isEmpty()) {
            IndexInfo.DialectStatistics dialectStats = indexInfo.getDialectStats();
            dialectStats.setDialect1(parseLong(dialectMap.get("dialect_1")));
            dialectStats.setDialect2(parseLong(dialectMap.get("dialect_2")));
            dialectStats.setDialect3(parseLong(dialectMap.get("dialect_3")));
            dialectStats.setDialect4(parseLong(dialectMap.get("dialect_4")));
        }

        // Build ErrorStatistics for index errors
        if (!indexErrorsMap.isEmpty()) {
            IndexInfo.ErrorStatistics errorStats = indexInfo.getIndexErrors();
            errorStats.setIndexingFailures(parseLong(indexErrorsMap.get("indexing failures")));
            String lastError = parseString(indexErrorsMap.get("last indexing error"));
            if (lastError != null) {
                errorStats.setLastIndexingError(lastError);
            }
            String lastErrorKey = parseString(indexErrorsMap.get("last indexing error key"));
            if (lastErrorKey != null) {
                errorStats.setLastIndexingErrorKey(lastErrorKey);
            }
        }
    }

    /**
     * Build ErrorStatistics from a map.
     */
    private IndexInfo.ErrorStatistics buildErrorStatistics(Map<String, Object> errorsMap) {
        IndexInfo.ErrorStatistics errorStats = new IndexInfo.ErrorStatistics();
        errorStats.setIndexingFailures(parseLong(errorsMap.get("indexing failures")));
        String lastError = parseString(errorsMap.get("last indexing error"));
        if (lastError != null) {
            errorStats.setLastIndexingError(lastError);
        }
        String lastErrorKey = parseString(errorsMap.get("last indexing error key"));
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
     * Encode a String to type K using the codec.
     */
    private K encodeKey(String str) {
        if (str == null) {
            return null;
        }
        ByteBuffer encoded = StringCodec.UTF8.encodeKey(str);
        return codec.decodeKey(encoded);
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
        Object flags = map.get("flags");
        if (flags instanceof List) {
            List<?> flagsList = (List<?>) flags;
            for (Object flag : flagsList) {
                if (flag instanceof String && key.equals(flag)) {
                    return true;
                }
            }
        }
        return false;
    }

}
