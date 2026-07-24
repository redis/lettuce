/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.annotations.Experimental;
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
 * <p>
 * Field names are schema identifiers and are decoded as raw UTF-8; field values are kept as raw bytes so that binary content
 * (for example vector embeddings) survives the round-trip.
 *
 * @author Aleksandar Todorov
 * @since 7.2
 */
@Experimental
public class HybridReplyParser implements ComplexDataParser<HybridReply> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(HybridReplyParser.class);

    private final ByteBuffer TOTAL_RESULTS_KEY = StringCodec.UTF8.encodeKey("total_results");

    private final ByteBuffer EXECUTION_TIME_KEY = StringCodec.UTF8.encodeKey("execution_time");

    private final ByteBuffer WARNINGS_KEY = StringCodec.UTF8.encodeKey("warnings");

    private final ByteBuffer RESULTS_KEY = StringCodec.UTF8.encodeKey("results");

    @Override
    public HybridReply parse(ComplexData data) {
        try {
            HybridReply hybridReply = new HybridReply();

            if (data.isList()) {
                parseResp2(data, hybridReply);
            } else {
                parseResp3(data, hybridReply);
            }

            return hybridReply;
        } catch (Exception e) {
            LOG.warn("Unable to parse FT.HYBRID result from Redis", e);
            return new HybridReply();
        }
    }

    private static byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    private void parseResp2(ComplexData data, HybridReply reply) {
        List<Object> list = data.getDynamicList();
        if (list == null || list.isEmpty()) {
            return;
        }
        parseResp2(list, reply);
    }

    private void parseResp2(List<Object> list, HybridReply reply) {
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
                                reply.addWarning(StringCodec.UTF8.decodeValue((ByteBuffer) o));
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
                                HybridReply.HybridResult result = new HybridReply.HybridResult();
                                addFieldsFromComplexData((ComplexData) resultObj, result);
                                reply.addResult(result);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseResp3(ComplexData data, HybridReply reply) {
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
                        reply.addWarning(StringCodec.UTF8.decodeValue((ByteBuffer) o));
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
            HybridReply.HybridResult result = new HybridReply.HybridResult();
            addFieldsFromComplexData(resultData, result);
            reply.addResult(result);
        }
    }

    private void addFieldsFromComplexData(ComplexData data, HybridReply.HybridResult result) {
        Map<Object, Object> map;
        try {
            map = data.getDynamicMap();
        } catch (UnsupportedOperationException e) {
            map = null;
        }

        if (map != null && !map.isEmpty()) {
            map.forEach((k, v) -> {
                if (!(k instanceof ByteBuffer) || (v != null && !(v instanceof ByteBuffer))) {
                    return;
                }
                result.addField(StringCodec.UTF8.decodeKey((ByteBuffer) k), v == null ? null : toBytes((ByteBuffer) v));
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
            if (!(k instanceof ByteBuffer) || (v != null && !(v instanceof ByteBuffer))) {
                continue;
            }
            result.addField(StringCodec.UTF8.decodeKey((ByteBuffer) k), v == null ? null : toBytes((ByteBuffer) v));
        }
    }

}
