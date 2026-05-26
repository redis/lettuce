/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.array.ArrayInfo;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/arinfo/">ARINFO</a> command output.
 * <p>
 * Converts the response from the Redis ARINFO command (without FULL) into an {@link ArrayInfo} object. All key-value pairs are
 * collected into a map and passed to the {@link ArrayInfo} constructor, which extracts known fields and provides raw map access
 * for future-proofing.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class ArrayInfoParser implements ComplexDataParser<ArrayInfo> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(ArrayInfoParser.class);

    public static final ArrayInfoParser INSTANCE = new ArrayInfoParser();

    private ArrayInfoParser() {
    }

    @Override
    public ArrayInfo parse(ComplexData dynamicData) {
        List<Object> data = verifyStructure(dynamicData, "ARINFO");

        if (data == null) {
            return null;
        }

        Map<String, Object> map = toMap(data);
        return new ArrayInfo(map);
    }

    /**
     * Converts a flat list of alternating key-value pairs into a map.
     */
    static Map<String, Object> toMap(List<Object> data) {
        Map<String, Object> map = new LinkedHashMap<>(data.size() / 2);
        for (int i = 0; i < data.size(); i += 2) {
            if (i + 1 >= data.size()) {
                break;
            }
            map.put(data.get(i).toString(), data.get(i + 1));
        }
        return map;
    }

    static List<Object> verifyStructure(ComplexData output, String commandName) {
        if (output == null) {
            LOG.warn("Failed while parsing {}: output must not be null", commandName);
            return null;
        }

        List<Object> data;
        try {
            data = output.getDynamicList();
        } catch (UnsupportedOperationException e) {
            try {
                Map<Object, Object> map = output.getDynamicMap();
                data = new ArrayList<>(map.size() * 2);
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    data.add(entry.getKey());
                    data.add(entry.getValue());
                }
            } catch (UnsupportedOperationException ex) {
                LOG.warn("Failed while parsing {}: output must be a list or a map", commandName, ex);
                return new ArrayList<>();
            }
        }

        if (data == null || data.isEmpty()) {
            LOG.warn("Failed while parsing {}: data must not be null or empty", commandName);
            return new ArrayList<>();
        }

        if (data.size() % 2 != 0) {
            LOG.warn("Failed while parsing {}: data must contain key-value pairs", commandName);
            return new ArrayList<>();
        }

        return data;
    }

}
