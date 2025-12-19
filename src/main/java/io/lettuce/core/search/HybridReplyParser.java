/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Parser for {@code FT.HYBRID} responses. Handles both RESP2 and RESP3 protocol formats.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 */
@Experimental
public class HybridReplyParser<K, V> implements ComplexDataParser<HybridReply<K, V>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(HybridReplyParser.class);

    private final RedisCodec<K, V> codec;

    private final ByteBuffer TOTAL_RESULTS_KEY = StringCodec.UTF8.encodeKey("total_results");

    private final ByteBuffer EXECUTION_TIME_KEY = StringCodec.UTF8.encodeKey("execution_time");

    private final ByteBuffer WARNINGS_KEY = StringCodec.UTF8.encodeKey("warnings");

    private final ByteBuffer RESULTS_KEY = StringCodec.UTF8.encodeKey("results");

    public HybridReplyParser(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    @Override
    public HybridReply<K, V> parse(ComplexData data) {
        try {
            HybridReply<K, V> hybridReply = new HybridReply<>();

            if (data.isList()) {
                parseResp2(data, hybridReply);
            } else {
                parseResp3(data, hybridReply);
            }

            return hybridReply;
        } catch (Exception e) {
            LOG.warn("Unable to parse FT.HYBRID result from Redis", e);
            return new HybridReply<>();
        }
    }

    private void parseResp2(ComplexData data, HybridReply<K, V> reply) {
        List<Object> list = data.getDynamicList();
        if (list == null || list.isEmpty()) {
            return;
        }
        parseResp2(list, reply);
    }

    private void parseResp2(List<Object> list, HybridReply<K, V> reply) {
        // RESP2 format: ["key1", value1, "key2", value2, ...]
        // Parse as key-value pairs
        for (int i = 0; i + 1 < list.size(); i += 2) {
            Object keyObj = list.get(i);
            Object valueObj = list.get(i + 1);

            if (!(keyObj instanceof ByteBuffer)) {
                continue;
            }

            ByteBuffer keyBuffer = (ByteBuffer) keyObj;

            if (keyBuffer.equals(TOTAL_RESULTS_KEY)) {
                if (valueObj instanceof Long) {
                    reply.setTotalResults((Long) valueObj);
                }
            } else if (keyBuffer.equals(EXECUTION_TIME_KEY)) {
                if (valueObj instanceof ByteBuffer) {
                    try {
                        String asString = StringCodec.UTF8.decodeKey((ByteBuffer) valueObj);
                        reply.setExecutionTime(Double.parseDouble(asString));
                    } catch (NumberFormatException ignore) {
                        // leave default
                    }
                } else if (valueObj instanceof Double) {
                    reply.setExecutionTime((Double) valueObj);
                }
            } else if (keyBuffer.equals(WARNINGS_KEY)) {
                if (valueObj instanceof ComplexData) {
                    ComplexData warningData = (ComplexData) valueObj;
                    List<Object> warnList = warningData.getDynamicList();
                    if (warnList != null) {
                        for (Object o : warnList) {
                            if (o instanceof ByteBuffer) {
                                reply.addWarning(codec.decodeValue((ByteBuffer) o));
                            }
                        }
                    }
                }
            } else if (keyBuffer.equals(RESULTS_KEY)) {
                if (valueObj instanceof ComplexData) {
                    ComplexData resultsData = (ComplexData) valueObj;
                    List<Object> resultsList = resultsData.getDynamicList();
                    if (resultsList != null) {
                        for (Object resultObj : resultsList) {
                            if (resultObj instanceof ComplexData) {
                                HybridReply.Result<K, V> result = new HybridReply.Result<>();
                                addFieldsFromComplexData((ComplexData) resultObj, result);
                                reply.addResult(result);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseResp3(ComplexData data, HybridReply<K, V> reply) {
        Map<Object, Object> resultsMap = data.getDynamicMap();
        if (resultsMap == null || resultsMap.isEmpty()) {
            return;
        }

        Object total = resultsMap.get(TOTAL_RESULTS_KEY);
        if (total instanceof Long) {
            reply.setTotalResults((Long) total);
        }

        Object executionTime = resultsMap.get(EXECUTION_TIME_KEY);
        if (executionTime instanceof Double) {
            reply.setExecutionTime((Double) executionTime);
        } else if (executionTime instanceof ByteBuffer) {
            try {
                String asString = StringCodec.UTF8.decodeKey((ByteBuffer) executionTime);
                reply.setExecutionTime(Double.parseDouble(asString));
            } catch (NumberFormatException ignore) {
                // leave default
            }
        }

        Object warnings = resultsMap.get(WARNINGS_KEY);
        if (warnings instanceof ComplexData) {
            ComplexData warningData = (ComplexData) warnings;
            List<Object> warnList = warningData.getDynamicList();
            if (warnList != null) {
                for (Object o : warnList) {
                    if (o instanceof ByteBuffer) {
                        reply.addWarning(codec.decodeValue((ByteBuffer) o));
                    }
                }
            }
        }

        Object resultsObj = resultsMap.get(RESULTS_KEY);
        if (!(resultsObj instanceof ComplexData)) {
            return;
        }

        ComplexData results = (ComplexData) resultsObj;
        List<Object> rawResults = results.getDynamicList();
        if (rawResults == null || rawResults.isEmpty()) {
            return;
        }

        for (Object raw : rawResults) {
            if (!(raw instanceof ComplexData)) {
                continue;
            }

            ComplexData resultData = (ComplexData) raw;
            HybridReply.Result<K, V> result = parseResultEntry(resultData);
            reply.addResult(result);
        }
    }

    private HybridReply.Result<K, V> parseResultEntry(ComplexData resultData) {
        Map<Object, Object> entryMap;
        try {
            entryMap = resultData.getDynamicMap();
        } catch (UnsupportedOperationException e) {
            entryMap = null;
        }

        HybridReply.Result<K, V> result = new HybridReply.Result<>();

        if (entryMap != null && !entryMap.isEmpty()) {
            entryMap.forEach((key, value) -> {
                if (!(key instanceof ByteBuffer) || !(value instanceof ByteBuffer)) {
                    return;
                }

                K fieldKey = codec.decodeKey((ByteBuffer) key);
                V fieldValue = codec.decodeValue((ByteBuffer) value);
                result.addField(fieldKey, fieldValue);
            });
        } else {
            addFieldsFromComplexData(resultData, result);
        }

        return result;
    }

    private void addFieldsFromComplexData(ComplexData data, HybridReply.Result<K, V> result) {
        Map<Object, Object> map;
        try {
            map = data.getDynamicMap();
        } catch (UnsupportedOperationException e) {
            map = null;
        }

        if (map != null && !map.isEmpty()) {
            map.forEach((k, v) -> {
                if (!(k instanceof ByteBuffer) || !(v instanceof ByteBuffer)) {
                    return;
                }
                K decodedKey = codec.decodeKey((ByteBuffer) k);
                V decodedValue = codec.decodeValue((ByteBuffer) v);
                result.addField(decodedKey, decodedValue);
            });
            return;
        }

        List<Object> list = data.getDynamicList();
        if (list == null || list.isEmpty()) {
            return;
        }

        for (int i = 0; i + 1 < list.size(); i += 2) {
            Object k = list.get(i);
            Object v = list.get(i + 1);
            if (!(k instanceof ByteBuffer) || !(v instanceof ByteBuffer)) {
                continue;
            }
            K decodedKey = codec.decodeKey((ByteBuffer) k);
            V decodedValue = codec.decodeValue((ByteBuffer) v);
            result.addField(decodedKey, decodedValue);
        }
    }

}
