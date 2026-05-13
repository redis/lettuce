/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.array.ArrayFullMetadata;

import java.util.List;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/arinfo/">ARINFO key FULL</a> command output.
 * <p>
 * Converts the response from the Redis ARINFO FULL command into an {@link ArrayFullMetadata} object containing all 12 fields (7
 * base + 5 extended per-slice stats).
 * <p>
 * Note: The {@code avg-dense-size}, {@code avg-dense-fill}, and {@code avg-sparse-size} fields are bulk strings on the wire,
 * not integers.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class ArrayFullMetadataParser implements ComplexDataParser<ArrayFullMetadata> {

    public static final ArrayFullMetadataParser INSTANCE = new ArrayFullMetadataParser();

    // Extended field names (base fields reuse constants from ArrayMetadataParser)
    private static final String DENSE_SLICES = "dense-slices";

    private static final String SPARSE_SLICES = "sparse-slices";

    private static final String AVG_DENSE_SIZE = "avg-dense-size";

    private static final String AVG_DENSE_FILL = "avg-dense-fill";

    private static final String AVG_SPARSE_SIZE = "avg-sparse-size";

    private ArrayFullMetadataParser() {
    }

    @Override
    public ArrayFullMetadata parse(ComplexData dynamicData) {
        List<Object> data = ArrayMetadataParser.verifyStructure(dynamicData, "ARINFO FULL");

        if (data == null) {
            return null;
        }

        ArrayFullMetadata metadata = new ArrayFullMetadata();

        // Populate the 7 base fields
        ArrayMetadataParser.populateBaseFields(metadata, data);

        // Populate the 5 extended fields
        for (int i = 0; i < data.size(); i += 2) {
            if (i + 1 >= data.size()) {
                break;
            }

            String key = data.get(i).toString();
            Object value = data.get(i + 1);

            switch (key) {
                case DENSE_SLICES:
                    metadata.setDenseSlices(ArrayMetadataParser.parseLong(value));
                    break;
                case SPARSE_SLICES:
                    metadata.setSparseSlices(ArrayMetadataParser.parseLong(value));
                    break;
                case AVG_DENSE_SIZE:
                    metadata.setAvgDenseSize(value != null ? value.toString() : null);
                    break;
                case AVG_DENSE_FILL:
                    metadata.setAvgDenseFill(value != null ? value.toString() : null);
                    break;
                case AVG_SPARSE_SIZE:
                    metadata.setAvgSparseSize(value != null ? value.toString() : null);
                    break;
            }
        }

        return metadata;
    }

}
