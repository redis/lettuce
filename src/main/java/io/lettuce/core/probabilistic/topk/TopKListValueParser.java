/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/topk.list/">TOPK.LIST</a> command output.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public final class TopKListValueParser implements ComplexDataParser<List<TopKListValue>> {

    public static final TopKListValueParser INSTANCE = new TopKListValueParser();

    private TopKListValueParser() {
    }

    @Override
    public List<TopKListValue> parse(ComplexData data) {

        if (data == null) {
            throw new IllegalArgumentException("Failed parsing TOPK.LIST: data must not be null");
        }

        List<Object> raw = data.getDynamicList();

        List<TopKListValue> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String name = StringCodec.UTF8.decodeKey((ByteBuffer) (raw.get(i)));

            Integer count = null;
            if (i + 1 < raw.size() && raw.get(i + 1) instanceof Number) {
                count = ((Number) raw.get(i + 1)).intValue();
                i++;
            }

            result.add(new TopKListValue(name, count));
        }

        return result;
    }

}
