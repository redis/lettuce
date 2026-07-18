/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/commands/ts.info/">TS.INFO</a> command output, including the {@code DEBUG}
 * modifier.
 * <p>
 * The RESP2 and RESP3 replies of {@code TS.INFO} differ in shape (flat key-value array vs. native map, and 4- vs 3-element
 * {@code rules} tuples), but this parser normalizes both into the same {@link TsInfoValue}.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Gyumin Hwang
 * @since 7.7
 */
public final class TsInfoValueParser<K, V> implements ComplexDataParser<TsInfoValue<K>> {

    private final RedisCodec<K, V> codec;

    public TsInfoValueParser(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    @Override
    public TsInfoValue<K> parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing TS.INFO: data must not be null");
        }

        Map<String, Object> info = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : data.getDynamicMap().entrySet()) {
            info.put(decodeUtf8(entry.getKey()), entry.getValue());
        }

        info.put("chunkType", decodeUtf8(info.get("chunkType")));
        info.put("duplicatePolicy", decodeUtf8(info.get("duplicatePolicy")));
        info.put("sourceKey", decodeKey(info.get("sourceKey")));
        info.put("labels", TsLabelsParser.decode(info.get("labels")));
        info.put("rules", decodeRules(info.get("rules")));
        info.put("ignoreMaxValDiff", toNullableDouble(info.get("ignoreMaxValDiff")));
        info.put("keySelfName", decodeKey(info.get("keySelfName")));
        info.put("Chunks", decodeChunks(info.get("Chunks")));

        return new TsInfoValue<>(info);
    }

    private List<TsInfoValue.Rule<K>> decodeRules(Object rawRules) {
        List<TsInfoValue.Rule<K>> rules = new ArrayList<>();
        if (rawRules == null) {
            return rules;
        }

        ComplexData rulesData = (ComplexData) rawRules;
        if (rulesData.isMap()) {
            // RESP3: destKey is the map key, value is a 3-element [bucketDuration, aggType, timestampAlignment] tuple
            for (Map.Entry<Object, Object> entry : rulesData.getDynamicMap().entrySet()) {
                K destKey = codec.decodeKey((ByteBuffer) entry.getKey());
                List<Object> tuple = ((ComplexData) entry.getValue()).getDynamicList();
                rules.add(buildRule(destKey, tuple.get(0), tuple.get(1), tuple.get(2)));
            }
        } else {
            // RESP2: each rule is a 4-element [destKey, bucketDuration, aggType, timestampAlignment] tuple
            for (Object ruleObj : rulesData.getDynamicList()) {
                List<Object> tuple = ((ComplexData) ruleObj).getDynamicList();
                K destKey = codec.decodeKey((ByteBuffer) tuple.get(0));
                rules.add(buildRule(destKey, tuple.get(1), tuple.get(2), tuple.get(3)));
            }
        }
        return rules;
    }

    private TsInfoValue.Rule<K> buildRule(K destKey, Object bucketDuration, Object aggregationType, Object timestampAlignment) {
        return new TsInfoValue.Rule<>(destKey, ((Number) bucketDuration).longValue(), decodeAggregationType(aggregationType),
                ((Number) timestampAlignment).longValue());
    }

    private TsAggregationType decodeAggregationType(Object value) {
        String raw = decodeUtf8(value);
        if (raw == null) {
            return null;
        }
        try {
            return TsAggregationType.valueOf(raw.replace('.', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<TsInfoValue.Chunk> decodeChunks(Object rawChunks) {
        if (rawChunks == null) {
            return null;
        }

        List<TsInfoValue.Chunk> chunks = new ArrayList<>();
        for (Object chunkObj : ((ComplexData) rawChunks).getDynamicList()) {
            Map<String, Object> chunk = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : ((ComplexData) chunkObj).getDynamicMap().entrySet()) {
                chunk.put(decodeUtf8(entry.getKey()), entry.getValue());
            }
            chunks.add(new TsInfoValue.Chunk(((Number) chunk.get("startTimestamp")).longValue(),
                    ((Number) chunk.get("endTimestamp")).longValue(), ((Number) chunk.get("samples")).longValue(),
                    ((Number) chunk.get("size")).longValue(), toNullableDouble(chunk.get("bytesPerSample"))));
        }
        return chunks;
    }

    private K decodeKey(Object value) {
        if (value == null) {
            return null;
        }
        return codec.decodeKey((ByteBuffer) value);
    }

    private static Double toNullableDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.valueOf(StringCodec.UTF8.decodeValue((ByteBuffer) value));
    }

    private static String decodeUtf8(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return StringCodec.UTF8.decodeValue((ByteBuffer) value);
    }

}
