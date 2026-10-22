/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for the reply of the Redis <a href="https://redis.io/commands/ts.info/">TS.INFO</a> command, with or without
 * {@code DEBUG}. RESP2 and RESP3 replies produce equal {@link TsInfoValue} instances.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public final class TsInfoValueParser<K, V> implements ComplexDataParser<TsInfoValue<K>> {

    private final RedisCodec<K, V> codec;

    /**
     * Create a new parser that decodes keys with {@code codec}.
     *
     * @param codec the codec of the connection, must not be {@code null}.
     * @throws IllegalArgumentException if {@code codec} is {@code null}.
     */
    public TsInfoValueParser(RedisCodec<K, V> codec) {

        LettuceAssert.notNull(codec, "RedisCodec must not be null");

        this.codec = codec;
    }

    /**
     * Parse a TS.INFO reply.
     *
     * @param data the reply, must not be {@code null}.
     * @return the parsed information.
     * @throws IllegalArgumentException if {@code data} is {@code null}.
     */
    @Override
    public TsInfoValue<K> parse(ComplexData data) {

        LettuceAssert.notNull(data, "Failed parsing TS.INFO: data must not be null");

        Map<String, Object> fields = TsParserSupport.fields(data);

        Map<String, Object> rawInfo = new LinkedHashMap<>();
        fields.forEach((name, value) -> rawInfo.put(name, TsParserSupport.plain(value)));

        String chunkType = TsParserSupport.string(fields.get("chunkType"));
        String duplicatePolicy = TsParserSupport.string(fields.get("duplicatePolicy"));
        Object chunks = fields.containsKey("Chunks") ? fields.get("Chunks") : fields.get("chunks");

        return new TsInfoValue<>(TsParserSupport.longOrZero(fields.get("totalSamples")),
                TsParserSupport.longOrZero(fields.get("memoryUsage")), TsParserSupport.longOrZero(fields.get("firstTimestamp")),
                TsParserSupport.longOrZero(fields.get("lastTimestamp")),
                TsParserSupport.longOrZero(fields.get("retentionTime")), TsParserSupport.longOrZero(fields.get("chunkCount")),
                TsParserSupport.longOrZero(fields.get("chunkSize")), isEmpty(chunkType) ? null : TsEncoding.of(chunkType),
                isEmpty(duplicatePolicy) ? null : TsDuplicatePolicy.of(duplicatePolicy),
                TsParserSupport.labels(fields.get("labels")), TsParserSupport.key(codec, fields.get("sourceKey")),
                rules(fields.get("rules")), TsParserSupport.longValue(fields.get("ignoreMaxTimeDiff")),
                TsParserSupport.doubleValue(fields.get("ignoreMaxValDiff")),
                TsParserSupport.key(codec, fields.get("keySelfName")), chunks(chunks), rawInfo);
    }

    // RESP2: array of [destKey, bucketDuration, aggregation, alignment]; RESP3: map destKey -> [bucketDuration, aggregation,
    // alignment]
    private List<TsInfoValue.Rule<K>> rules(Object value) {

        List<TsInfoValue.Rule<K>> rules = new ArrayList<>();

        if (!(value instanceof ComplexData)) {
            return rules;
        }

        ComplexData data = (ComplexData) value;

        if (data.isMap()) {
            data.getDynamicMap().forEach((destination, rule) -> {
                List<Object> fields = ((ComplexData) rule).getDynamicList();
                rules.add(rule(destination, fields.get(0), fields.get(1), fields.size() > 2 ? fields.get(2) : null));
            });
            return rules;
        }

        for (Object rule : data.getDynamicList()) {
            List<Object> fields = ((ComplexData) rule).getDynamicList();
            rules.add(rule(fields.get(0), fields.get(1), fields.get(2), fields.size() > 3 ? fields.get(3) : null));
        }

        return rules;
    }

    private TsInfoValue.Rule<K> rule(Object destination, Object bucketDuration, Object aggregation, Object alignment) {
        return new TsInfoValue.Rule<>(TsParserSupport.key(codec, destination), TsParserSupport.longOrZero(bucketDuration),
                TsAggregation.of(TsParserSupport.string(aggregation)), TsParserSupport.longOrZero(alignment));
    }

    // RESP2: array of flat name/value arrays; RESP3: array of maps
    private static List<TsInfoValue.Chunk> chunks(Object value) {

        if (!(value instanceof ComplexData)) {
            return null;
        }

        List<TsInfoValue.Chunk> chunks = new ArrayList<>();

        for (Object chunk : ((ComplexData) value).getDynamicList()) {

            Map<String, Object> fields = TsParserSupport.fields((ComplexData) chunk);
            Double bytesPerSample = TsParserSupport.doubleValue(fields.get("bytesPerSample"));

            chunks.add(new TsInfoValue.Chunk(TsParserSupport.longOrZero(fields.get("startTimestamp")),
                    TsParserSupport.longOrZero(fields.get("endTimestamp")), TsParserSupport.longOrZero(fields.get("samples")),
                    TsParserSupport.longOrZero(fields.get("size")), bytesPerSample == null ? 0 : bytesPerSample));
        }

        return chunks;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

}
