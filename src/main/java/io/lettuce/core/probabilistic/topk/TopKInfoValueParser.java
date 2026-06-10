/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/topk.info/">TOPK.INFO</a> command output.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public final class TopKInfoValueParser implements ComplexDataParser<TopKInfoValue> {

    public static final TopKInfoValueParser INSTANCE = new TopKInfoValueParser();

    private TopKInfoValueParser() {
    }

    @Override
    public TopKInfoValue parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing TOPK.INFO: data must not be null");
        }
        Map<Object, Object> raw = data.getDynamicMap();
        Map<String, Object> info = new LinkedHashMap<>(raw.size());
        for (Map.Entry<Object, Object> e : raw.entrySet()) {
            String k = StringCodec.UTF8.decodeKey((ByteBuffer) e.getKey());
            info.put(k, e.getValue());
        }
        return new TopKInfoValue(info);
    }

}
