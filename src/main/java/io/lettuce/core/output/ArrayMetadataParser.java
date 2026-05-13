/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.array.ArrayMetadata;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/arinfo/">ARINFO</a> command output.
 * <p>
 * Converts the response from the Redis ARINFO command (without FULL) into an {@link ArrayMetadata} object containing 7
 * top-level fields.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class ArrayMetadataParser implements ComplexDataParser<ArrayMetadata> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(ArrayMetadataParser.class);

    public static final ArrayMetadataParser INSTANCE = new ArrayMetadataParser();

    // Field names in the ARINFO response
    static final String COUNT = "count";

    static final String LEN = "len";

    static final String NEXT_INSERT_INDEX = "next-insert-index";

    static final String SLICES = "slices";

    static final String DIRECTORY_SIZE = "directory-size";

    static final String SUPER_DIR_ENTRIES = "super-dir-entries";

    static final String SLICE_SIZE = "slice-size";

    private ArrayMetadataParser() {
    }

    @Override
    public ArrayMetadata parse(ComplexData dynamicData) {
        List<Object> data = verifyStructure(dynamicData, "ARINFO");

        if (data == null) {
            return null;
        }

        ArrayMetadata metadata = new ArrayMetadata();
        populateBaseFields(metadata, data);
        return metadata;
    }

    /**
     * Populates the 7 base fields shared between ARINFO and ARINFO FULL.
     */
    static void populateBaseFields(ArrayMetadata metadata, List<Object> data) {
        for (int i = 0; i < data.size(); i += 2) {
            if (i + 1 >= data.size()) {
                break;
            }

            String key = data.get(i).toString();
            Object value = data.get(i + 1);

            switch (key) {
                case COUNT:
                    metadata.setCount(parseLong(value));
                    break;
                case LEN:
                    metadata.setLen(parseLong(value));
                    break;
                case NEXT_INSERT_INDEX:
                    metadata.setNextInsertIndex(parseLong(value));
                    break;
                case SLICES:
                    metadata.setSlices(parseLong(value));
                    break;
                case DIRECTORY_SIZE:
                    metadata.setDirectorySize(parseLong(value));
                    break;
                case SUPER_DIR_ENTRIES:
                    metadata.setSuperDirEntries(parseLong(value));
                    break;
                case SLICE_SIZE:
                    metadata.setSliceSize(parseLong(value));
                    break;
            }
        }
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

    static Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
