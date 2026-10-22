/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for the reply of the Redis <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a> and
 * <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a> commands.
 * <p>
 * RESP2 sends an array of {@code [name, labels, samples]}; RESP3 a map of name to {@code [labels, metadata…, samples]}. The
 * parser takes the aggregations, the reducer and whether the request groups from the request, so the result is the same on both
 * protocols. Create one parser per command.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public final class TsSeriesParser<K, V> implements ComplexDataParser<List<TsSeries<K>>> {

    private static final String REDUCER_LABEL = "__reducer__";

    private static final String SOURCE_LABEL = "__source__";

    private final RedisCodec<K, V> codec;

    private final List<TsAggregation> aggregations;

    private final boolean grouped;

    private final TsReducer reducer;

    /**
     * Create a new parser.
     *
     * @param codec the codec of the connection, must not be {@code null}.
     * @param aggregations the aggregations of the request, must not be {@code null}; empty without an aggregation.
     * @param grouped {@code true} if the request has {@code GROUPBY}.
     * @param reducer the reducer of the request, can be {@code null} without {@code GROUPBY}.
     * @throws IllegalArgumentException if {@code codec} or {@code aggregations} is {@code null}.
     */
    public TsSeriesParser(RedisCodec<K, V> codec, List<TsAggregation> aggregations, boolean grouped, TsReducer reducer) {

        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        LettuceAssert.notNull(aggregations, "Aggregations must not be null");

        this.codec = codec;
        this.aggregations = aggregations;
        this.grouped = grouped;
        this.reducer = grouped ? reducer : null;
    }

    /**
     * Parse a TS.MRANGE or TS.MREVRANGE reply.
     *
     * @param data the reply, can be {@code null} for an empty reply.
     * @return the series in the order of the reply, never {@code null}.
     */
    @Override
    public List<TsSeries<K>> parse(ComplexData data) {

        List<TsSeries<K>> result = new ArrayList<>();

        if (data == null) {
            return result;
        }

        if (data.isMap()) {
            data.getDynamicMap().forEach((name, value) -> result.add(resp3(name, ((ComplexData) value).getDynamicList())));
            return result;
        }

        for (Object entry : data.getDynamicList()) {
            List<Object> fields = ((ComplexData) entry).getDynamicList();
            result.add(resp2(fields.get(0), fields.get(1), fields.get(2)));
        }

        return result;
    }

    // [name, labels, samples]; a group carries __reducer__ and __source__ labels with WITHLABELS
    private TsSeries<K> resp2(Object name, Object labels, Object samples) {

        List<K> sources = null;
        if (grouped && labels instanceof ComplexData && !((ComplexData) labels).isMap()) {
            for (Object pair : ((ComplexData) labels).getDynamicList()) {
                List<Object> nameAndValue = ((ComplexData) pair).getDynamicList();
                if (SOURCE_LABEL.equals(TsParserSupport.string(nameAndValue.get(0))) && nameAndValue.size() > 1) {
                    sources = splitSources(nameAndValue.get(1));
                }
            }
        }

        return series(name, labels, sources, samples);
    }

    // [labels, metadata maps (aggregators, reducers, sources), samples]
    private TsSeries<K> resp3(Object name, List<Object> value) {

        List<K> sources = null;

        for (int i = 1; i < value.size() - 1; i++) {

            if (!(value.get(i) instanceof ComplexData) || !((ComplexData) value.get(i)).isMap()) {
                continue;
            }

            for (Map.Entry<Object, Object> metadata : ((ComplexData) value.get(i)).getDynamicMap().entrySet()) {
                if ("sources".equals(TsParserSupport.string(metadata.getKey()))) {
                    sources = keys(metadata.getValue());
                }
            }
        }

        if (grouped && sources == null) {
            sources = Collections.emptyList();
        }

        return series(name, value.get(0), sources, value.size() > 1 ? value.get(value.size() - 1) : null);
    }

    private TsSeries<K> series(Object name, Object labels, List<K> sources, Object samples) {

        Map<String, String> decodedLabels = TsParserSupport.labels(labels);
        decodedLabels.remove(REDUCER_LABEL);
        decodedLabels.remove(SOURCE_LABEL);

        K key = grouped ? null : TsParserSupport.key(codec, name);
        String group = grouped ? TsParserSupport.string(name) : null;

        return new TsSeries<>(key, group, decodedLabels, aggregations, reducer, grouped ? sources : null,
                TsSampleParser.samples(samples));
    }

    private List<K> keys(Object value) {

        List<K> keys = new ArrayList<>();
        if (!(value instanceof ComplexData)) {
            return keys;
        }

        ComplexData data = (ComplexData) value;
        Iterable<Object> elements = data.isSet() ? data.getDynamicSet() : data.getDynamicList();
        for (Object element : elements) {
            keys.add(TsParserSupport.key(codec, element));
        }

        return keys;
    }

    // RESP2 __source__ is "k1,k2": split the raw bytes on ',' and decode each part as a key
    private List<K> splitSources(Object value) {

        List<K> keys = new ArrayList<>();
        if (!(value instanceof ByteBuffer)) {
            return keys;
        }

        ByteBuffer bytes = ((ByteBuffer) value).duplicate();
        int start = bytes.position();
        for (int i = bytes.position(); i <= bytes.limit(); i++) {
            if (i == bytes.limit() || bytes.get(i) == ',') {
                ByteBuffer part = bytes.duplicate();
                part.position(start);
                part.limit(i);
                if (part.hasRemaining()) {
                    keys.add(codec.decodeKey(part.slice()));
                }
                start = i + 1;
            }
        }

        return keys;
    }

}
