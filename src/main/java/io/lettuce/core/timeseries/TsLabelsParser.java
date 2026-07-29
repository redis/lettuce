/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;

/**
 * Decodes the {@code labels} field shared by the {@code TS.INFO} and {@code TS.MGET} command outputs.
 * <p>
 * The wire shape differs between protocols: RESP3 returns a native map, while RESP2 returns a nested array of
 * {@code [key, value]} pairs, not a flat key-value array. Calling {@link ComplexData#getDynamicMap()} directly on the RESP2
 * shape is incorrect: its odd/even pairing heuristic treats each nested pair as a single element, silently dropping a single
 * label or mis-pairing multiple labels. This class branches on {@link ComplexData#isMap()} to normalize both shapes into the
 * same {@link Map}.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
final class TsLabelsParser {

    private TsLabelsParser() {
    }

    /**
     * Decodes the {@code labels} field into a {@code key -> value} map.
     *
     * @param rawLabels the raw {@link ComplexData} value stored under the {@code labels} key, or {@code null} if absent.
     * @return the decoded labels, or an empty {@link Map} if {@code rawLabels} is {@code null}.
     */
    static Map<String, String> decode(Object rawLabels) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (rawLabels == null) {
            return labels;
        }

        ComplexData labelsData = (ComplexData) rawLabels;
        if (labelsData.isMap()) {
            // RESP3: native map, key -> value directly
            for (Map.Entry<Object, Object> entry : labelsData.getDynamicMap().entrySet()) {
                labels.put(decodeUtf8(entry.getKey()), decodeUtf8(entry.getValue()));
            }
        } else {
            // RESP2: nested array of [key, value] pairs
            for (Object pairObj : labelsData.getDynamicList()) {
                List<Object> pair = ((ComplexData) pairObj).getDynamicList();
                labels.put(decodeUtf8(pair.get(0)), decodeUtf8(pair.get(1)));
            }
        }
        return labels;
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
