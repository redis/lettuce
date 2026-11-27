/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.array.ArrayInfoFull;

import java.util.List;
import java.util.Map;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/arinfo/">ARINFO key FULL</a> command output.
 * <p>
 * Converts the response from the Redis ARINFO FULL command into an {@link ArrayInfoFull} object. All key-value pairs are
 * collected into a map and passed to the {@link ArrayInfoFull} constructor, which extracts known fields (base + extended
 * per-slice stats) and provides raw map access for future-proofing.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class ArrayInfoFullParser implements ComplexDataParser<ArrayInfoFull> {

    public static final ArrayInfoFullParser INSTANCE = new ArrayInfoFullParser();

    private ArrayInfoFullParser() {
    }

    @Override
    public ArrayInfoFull parse(ComplexData dynamicData) {
        List<Object> data = ArrayInfoParser.verifyStructure(dynamicData, "ARINFO FULL");

        if (data == null) {
            return null;
        }

        Map<String, Object> map = ArrayInfoParser.toMap(data);
        return new ArrayInfoFull(map);
    }

}
