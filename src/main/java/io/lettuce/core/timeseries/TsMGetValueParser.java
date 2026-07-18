/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/commands/ts.mget/">TS.MGET</a> command output.
 * <p>
 * The top-level container differs between protocols: RESP2 returns an array of {@code [key, labels, sample]} triples, while
 * RESP3 returns a native map of key to a {@code [labels, sample]} pair. This parser branches on {@link ComplexData#isMap()} to
 * normalize both into the same {@link List} of {@link TsMGetValue}.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Gyumin Hwang
 * @since 7.7
 */
public final class TsMGetValueParser<K, V> implements ComplexDataParser<List<TsMGetValue<K>>> {

    private final RedisCodec<K, V> codec;

    public TsMGetValueParser(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    @Override
    public List<TsMGetValue<K>> parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing TS.MGET: data must not be null");
        }

        List<TsMGetValue<K>> result = new ArrayList<>();
        if (data.isMap()) {
            // RESP3: key -> [labels, sample]
            for (Map.Entry<Object, Object> entry : data.getDynamicMap().entrySet()) {
                K key = codec.decodeKey((ByteBuffer) entry.getKey());
                List<Object> valueList = ((ComplexData) entry.getValue()).getDynamicList();
                result.add(buildValue(key, valueList.get(0), valueList.get(1)));
            }
        } else {
            // RESP2: [key, labels, sample]
            for (Object entryObj : data.getDynamicList()) {
                List<Object> entry = ((ComplexData) entryObj).getDynamicList();
                K key = codec.decodeKey((ByteBuffer) entry.get(0));
                result.add(buildValue(key, entry.get(1), entry.get(2)));
            }
        }
        return result;
    }

    private TsMGetValue<K> buildValue(K key, Object labelsRaw, Object sampleRaw) {
        return new TsMGetValue<>(key, TsLabelsParser.decode(labelsRaw), decodeSample(sampleRaw));
    }

    private TsSample decodeSample(Object rawSample) {
        return rawSample == null ? null : TsSampleParser.decode((ComplexData) rawSample);
    }

}
