/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

import java.nio.ByteBuffer;
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
public class IndexInfoParser<K, V> implements ComplexDataParser<IndexInfo> {

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
    public IndexInfo parse(ComplexData data) {
        if (data == null) {
            return new IndexInfo();
        }

        if (data.isList()) {
            return parseResp2(data);
        }

        return parseResp3(data);
    }

    /**
     * Parse FT.INFO response in RESP2 format (array-based with alternating key-value pairs).
     */
    private IndexInfo parseResp2(ComplexData data) {
        List<Object> infoArray = data.getDynamicList();
        IndexInfo indexInfo = new IndexInfo();

        // RESP2: Parse alternating key-value pairs
        for (int i = 0; i < infoArray.size(); i += 2) {
            if (i + 1 >= infoArray.size()) {
                break; // Incomplete pair, skip
            }

            String key = decodeString(infoArray.get(i));
            Object value = infoArray.get(i + 1);
            populateIndexInfo(indexInfo, key, value);
        }

        return indexInfo;
    }

    /**
     * Parse FT.INFO response in RESP3 format (native map structure).
     */
    private IndexInfo parseResp3(ComplexData data) {
        Map<Object, Object> rawMap = data.getDynamicMap();
        IndexInfo indexInfo = new IndexInfo();

        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            String key = decodeString(entry.getKey());
            Object value = entry.getValue();
            populateIndexInfo(indexInfo, key, value);
        }

        return indexInfo;
    }

    /**
     * Populate the IndexInfo object based on the key-value pair.
     */
    private void populateIndexInfo(IndexInfo indexInfo, String key, Object value) {
        switch (key) {
            case "index_name":
                indexInfo.setIndexName(decodeString(value));
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
                parseGcStats(indexInfo, value);
                break;
            case "cursor_stats":
                parseCursorStats(indexInfo, value);
                break;
            case "dialect_stats":
                parseDialectStats(indexInfo, value);
                break;
            case "Index Errors":
                parseIndexErrors(indexInfo, value);
                break;
            case "field statistics":
                parseFieldStatistics(indexInfo, value);
                break;
            default:
                // Categorize remaining fields
                if (SIZE_STATS.contains(key)) {
                    indexInfo.putSizeStatistic(key, parseValue(value));
                } else if (INDEXING_STATS.contains(key)) {
                    indexInfo.putIndexingStatistic(key, parseValue(value));
                }
                break;
        }
    }

    private void parseIndexOptions(IndexInfo indexInfo, Object value) {
        List<Object> options = parseListValue(value);
        for (Object option : options) {
            indexInfo.addIndexOption(decodeString(option));
        }
    }

    private void parseIndexDefinition(IndexInfo indexInfo, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        indexInfo.putIndexDefinition(decodeString(list.get(i)), parseValue(list.get(i + 1)));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    indexInfo.putIndexDefinition(decodeString(entry.getKey()), parseValue(entry.getValue()));
                }
            }
        }
    }

    private void parseAttributes(IndexInfo indexInfo, Object value) {
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
                            attributeMap.put(decodeString(list.get(i)), parseValue(list.get(i + 1)));
                        }
                    }
                } else if (data.isMap()) {
                    // RESP3: map
                    Map<Object, Object> map = data.getDynamicMap();
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        attributeMap.put(decodeString(entry.getKey()), parseValue(entry.getValue()));
                    }
                }
            }
            indexInfo.addAttribute(attributeMap);
        }
    }

    private void parseGcStats(IndexInfo indexInfo, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        indexInfo.putGcStatistic(decodeString(list.get(i)), parseValue(list.get(i + 1)));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    indexInfo.putGcStatistic(decodeString(entry.getKey()), parseValue(entry.getValue()));
                }
            }
        }
    }

    private void parseCursorStats(IndexInfo indexInfo, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        indexInfo.putCursorStatistic(decodeString(list.get(i)), parseValue(list.get(i + 1)));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    indexInfo.putCursorStatistic(decodeString(entry.getKey()), parseValue(entry.getValue()));
                }
            }
        }
    }

    private void parseDialectStats(IndexInfo indexInfo, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        indexInfo.putDialectStatistic(decodeString(list.get(i)), parseLong(list.get(i + 1)));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    indexInfo.putDialectStatistic(decodeString(entry.getKey()), parseLong(entry.getValue()));
                }
            }
        }
    }

    private void parseIndexErrors(IndexInfo indexInfo, Object value) {
        if (value instanceof ComplexData) {
            ComplexData data = (ComplexData) value;
            if (data.isList()) {
                // RESP2: alternating key-value pairs
                List<Object> list = data.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    if (i + 1 < list.size()) {
                        indexInfo.putIndexError(decodeString(list.get(i)), parseValue(list.get(i + 1)));
                    }
                }
            } else if (data.isMap()) {
                // RESP3: map
                Map<Object, Object> map = data.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    indexInfo.putIndexError(decodeString(entry.getKey()), parseValue(entry.getValue()));
                }
            }
        }
    }

    private void parseFieldStatistics(IndexInfo indexInfo, Object value) {
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
                            fieldStatMap.put(decodeString(list.get(i)), parseValue(list.get(i + 1)));
                        }
                    }
                } else if (data.isMap()) {
                    // RESP3: map
                    Map<Object, Object> map = data.getDynamicMap();
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        fieldStatMap.put(decodeString(entry.getKey()), parseValue(entry.getValue()));
                    }
                }
            }
            indexInfo.addFieldStatistic(fieldStatMap);
        }
    }

    /**
     * Parse a value which can be a simple type, list, or nested map.
     */
    private Object parseValue(Object value) {
        if (value instanceof ByteBuffer) {
            return decodeString(value);
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
            String key = decodeString(entry.getKey());
            Object value = parseValue(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    /**
     * Parse a Long value from various types.
     */
    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof ByteBuffer) {
            String str = decodeString(value);
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Decode a ByteBuffer or other object to a String.
     */
    private String decodeString(Object obj) {
        if (obj instanceof ByteBuffer) {
            return codec.decodeValue((ByteBuffer) obj).toString();
        }
        return obj != null ? obj.toString() : null;
    }

}
