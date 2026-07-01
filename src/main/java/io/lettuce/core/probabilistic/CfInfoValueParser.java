/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/cf.info/">CF.INFO</a> command output.
 *
 * @author HwangRock
 * @since 7.7
 */
public final class CfInfoValueParser implements ComplexDataParser<CfInfoValue> {

    public static final CfInfoValueParser INSTANCE = new CfInfoValueParser();

    private CfInfoValueParser() {
    }

    @Override
    public CfInfoValue parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing CF.INFO: data must not be null");
        }
        Map<Object, Object> raw = data.getDynamicMap();
        Map<String, Object> info = new LinkedHashMap<>(raw.size());
        for (Map.Entry<Object, Object> e : raw.entrySet()) {
            String k = StringCodec.UTF8.decodeKey((ByteBuffer) e.getKey());
            info.put(k, e.getValue());
        }
        return new CfInfoValue(info);
    }

}
