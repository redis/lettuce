/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.vector.QuantizationType;
import io.lettuce.core.vector.VectorMetadata;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/vinfo/">VINFO</a> command output.
 * <p>
 * This parser converts the response from the Redis VINFO command into a {@link VectorMetadata} object, which contains
 * information about a vector set including its dimensionality, quantization type, size, and other parameters related to the
 * HNSW graph structure.
 *
 * @author Tihomir Mateev
 * @since 6.7
 */
public class VectorMetadataParser implements ComplexDataParser<VectorMetadata> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(VectorMetadataParser.class);

    public static final VectorMetadataParser INSTANCE = new VectorMetadataParser();

    // Field names in the VINFO response
    private static final String ATTRIBUTES_COUNT = "attributes-count";

    private static final String SIZE = "size";

    private static final String MAX_NODES = "hnsw-m";

    private static final String VSET_UID = "vset-uid";

    private static final String QUANT_TYPE = "quant-type";

    private static final String MAX_LEVEL = "max-level";

    private static final String VECTOR_DIM = "vector-dim";

    private static final String MAX_NODE_UID = "hnsw-max-node-uid";

    private static final String PROJECTION_INPUT_DIM = "projection-input-dim";

    // Quant types
    private static final String INT8 = "int8";

    private static final String FLOAT32 = "float32";

    private static final String BINARY = "binary";

    /**
     * Utility constructor.
     */
    private VectorMetadataParser() {
    }

    /**
     * Parse the output of the Redis VINFO command and convert it to a {@link VectorMetadata} object.
     * <p>
     * The VINFO command returns an array of key-value pairs, where each pair consists of a field name followed by its value.
     * This method extracts the relevant fields and populates a {@link VectorMetadata} object with the corresponding values.
     *
     * @param dynamicData output of VINFO command
     * @return a {@link VectorMetadata} instance containing the parsed information
     * @throws IllegalArgumentException if the input data is null, empty, or has an invalid format
     */
    @Override
    public VectorMetadata parse(ComplexData dynamicData) {
        List<Object> data = verifyStructure(dynamicData);

        if (data == null) {
            // Valid response for the case when a vector set does not exist
            return null;
        }

        VectorMetadata metadata = new VectorMetadata();

        // Parse the array of key-value pairs
        for (int i = 0; i < data.size(); i += 2) {
            if (i + 1 >= data.size()) {
                break; // Avoid index out of bounds
            }

            String key = data.get(i).toString();
            Object value = data.get(i + 1);

            switch (key) {
                case ATTRIBUTES_COUNT:
                    metadata.setAttributesCount(parseInteger(value));
                    break;
                case SIZE:
                    metadata.setSize(parseInteger(value));
                    break;
                case QUANT_TYPE:
                    metadata.setType(parseQuantizationType(value.toString()));
                    break;
                case VECTOR_DIM:
                    metadata.setDimensionality(parseInteger(value));
                    break;
                case MAX_LEVEL:
                    metadata.maxLevel(parseInteger(value));
                    break;
                case MAX_NODES:
                    metadata.setMaxNodes(parseInteger(value));
                    break;
                case MAX_NODE_UID:
                    metadata.maxNodeUid(parseInteger(value));
                    break;
                case PROJECTION_INPUT_DIM:
                    metadata.setProjectionInputDim(parseInteger(value));
                    break;
                case VSET_UID:
                    metadata.setvSetUid(parseInteger(value));
                    break;
            }
        }

        return metadata;
    }

    /**
     * Verifies that the input data has the expected structure for a VINFO command response.
     *
     * @param vinfoOutput the output from the VINFO command
     * @return the list of key-value pairs from the VINFO command
     * @throws IllegalArgumentException if the input data is null, empty, or has an invalid format
     */
    private List<Object> verifyStructure(ComplexData vinfoOutput) {
        if (vinfoOutput == null) {
            LOG.warn("Failed while parsing VINFO: vinfoOutput must not be null");
            return null;
        }

        List<Object> data;
        try {
            data = vinfoOutput.getDynamicList();
        } catch (UnsupportedOperationException e) {
            try {
                Map<Object, Object> map = vinfoOutput.getDynamicMap();
                data = new ArrayList<>(map.size() * 2);
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    data.add(entry.getKey());
                    data.add(entry.getValue());
                }
            } catch (UnsupportedOperationException ex) {
                LOG.warn("Failed while parsing VINFO: vinfoOutput must be a list or a map", ex);
                return new ArrayList<>();
            }
        }

        if (data == null || data.isEmpty()) {
            LOG.warn("Failed while parsing VINFO: data must not be null or empty");
            return new ArrayList<>();
        }

        // VINFO returns an array of key-value pairs, so the size must be even
        if (data.size() % 2 != 0) {
            LOG.warn("Failed while parsing VINFO: data must contain key-value pairs");
            return new ArrayList<>();
        }

        return data;
    }

    /**
     * Parses a value from the VINFO response into an Integer.
     *
     * @param value the value to parse
     * @return the parsed Integer value, or null if the value cannot be parsed
     */
    private Integer parseInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                // Return null if the string cannot be parsed as an integer
                return null;
            }
        }
        return null;
    }

    /**
     * Maps a quantization type string from the VINFO response to a {@link QuantizationType} enum value.
     *
     * @param quantTypeStr the quantization type string from the VINFO response
     * @return the corresponding {@link QuantizationType} enum value, or null if the string does not match any known type
     */
    private QuantizationType parseQuantizationType(String quantTypeStr) {
        switch (quantTypeStr.toLowerCase()) {
            case INT8:
                return QuantizationType.Q8;
            case FLOAT32:
                return QuantizationType.NO_QUANTIZATION;
            case BINARY:
                return QuantizationType.BINARY;
            default:
                return null;
        }
    }

}
