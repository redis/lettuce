/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for the reply of the Redis <a href="https://redis.io/commands/ts.mget/">TS.MGET</a> command. RESP2 sends an array of
 * {@code [key, labels, sample]}, RESP3 a map of key to {@code [labels, sample]}; both produce equal {@link TsMGetValue} lists,
 * in the order of the reply.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public final class TsMGetValueParser<K, V> implements ComplexDataParser<List<TsMGetValue<K>>> {

    private final RedisCodec<K, V> codec;

    /**
     * Create a new parser that decodes keys with {@code codec}.
     *
     * @param codec the codec of the connection, must not be {@code null}.
     * @throws IllegalArgumentException if {@code codec} is {@code null}.
     */
    public TsMGetValueParser(RedisCodec<K, V> codec) {

        LettuceAssert.notNull(codec, "RedisCodec must not be null");

        this.codec = codec;
    }

    /**
     * Parse a TS.MGET reply.
     *
     * @param data the reply, can be {@code null} for an empty reply.
     * @return the values, never {@code null}.
     */
    @Override
    public List<TsMGetValue<K>> parse(ComplexData data) {

        List<TsMGetValue<K>> result = new ArrayList<>();

        if (data == null) {
            return result;
        }

        if (data.isMap()) {
            data.getDynamicMap().forEach((key, value) -> {
                List<Object> entry = ((ComplexData) value).getDynamicList();
                result.add(new TsMGetValue<>(TsParserSupport.key(codec, key), TsParserSupport.labels(entry.get(0)),
                        TsSampleParser.sample(entry.size() > 1 ? entry.get(1) : null)));
            });
            return result;
        }

        for (Object value : data.getDynamicList()) {
            List<Object> entry = ((ComplexData) value).getDynamicList();
            result.add(new TsMGetValue<>(TsParserSupport.key(codec, entry.get(0)), TsParserSupport.labels(entry.get(1)),
                    TsSampleParser.sample(entry.size() > 2 ? entry.get(2) : null)));
        }

        return result;
    }

}
