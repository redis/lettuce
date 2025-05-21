/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.vector.QuantizationType;
import io.lettuce.core.vector.RawVector;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/vemb/">VEMB</a> command output with the RAW option.
 * <p>
 * This parser converts the response from the Redis VEMB command with the RAW option into a {@link RawVector} object,
 * which contains the raw binary vector data along with metadata such as quantization type, normalization factor,
 * and quantization range.
 * <p>
 * The VEMB command with RAW option returns the following data:
 * <ol>
 *   <li>The quantization type as a string: "int8" (Q8), "float32" (NOQUANT), or "binary" (BIN)</li>
 *   <li>A binary blob containing the raw vector data</li>
 *   <li>The L2 norm of the vector before normalization</li>
 *   <li>The quantization range (only for Q8 quantization)</li>
 * </ol>
 *
 * @author Tihomir Mateev
 * @since 6.7
 */
public class RawVectorParser implements ComplexDataParser<RawVector> {

    public static final RawVectorParser INSTANCE = new RawVectorParser();

    // Quantization type values in the VEMB RAW response
    private static final String INT8 = "int8";
    private static final String FLOAT32 = "float32";
    private static final String BINARY = "binary";

    /**
     * Utility constructor.
     */
    private RawVectorParser() {
    }

    /**
     * Parse the output of the Redis VEMB command with the RAW option and convert it to a {@link RawVector} object.
     * <p>
     * The VEMB command with RAW option returns an array with the following elements:
     * <ol>
     *   <li>The quantization type as a string</li>
     *   <li>A binary blob containing the raw vector data</li>
     *   <li>The L2 norm of the vector before normalization</li>
     *   <li>The quantization range (only for Q8 quantization)</li>
     * </ol>
     *
     * @param dynamicData output of VEMB command with RAW option
     * @return a {@link RawVector} instance containing the parsed information
     * @throws IllegalArgumentException if the input data is null, empty, or has an invalid format
     */
    @Override
    public RawVector parse(ComplexData dynamicData) {
        List<Object> data = verifyStructure(dynamicData);

        // Parse the quantization type
        String quantTypeStr = data.get(0).toString();
        QuantizationType quantType = parseQuantizationType(quantTypeStr);

        // Parse the raw vector data
        ByteBuffer vectorData = parseVectorData(data.get(1));

        // Parse the L2 norm
        Double beforeNormalization = parseDouble(data.get(2));

        // Parse the quantization range (only for Q8)
        Double quantizationRange = null;
        if (quantType == QuantizationType.Q8 && data.size() > 3) {
            quantizationRange = parseDouble(data.get(3));
        }

        return new RawVector(quantType, vectorData, beforeNormalization, quantizationRange);
    }

    /**
     * Verifies that the input data has the expected structure for a VEMB command with RAW option response.
     *
     * @param vembOutput the output from the VEMB command with RAW option
     * @return the list of elements from the VEMB command
     * @throws IllegalArgumentException if the input data is null, empty, or has an invalid format
     */
    private List<Object> verifyStructure(ComplexData vembOutput) {
        if (vembOutput == null) {
            throw new IllegalArgumentException("Failed while parsing VEMB RAW: vembOutput must not be null");
        }

        List<Object> data;
        try {
            data = vembOutput.getDynamicList();
        } catch (UnsupportedOperationException e) {
            throw new IllegalArgumentException("Failed while parsing VEMB RAW: vembOutput must be a list", e);
        }

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Failed while parsing VEMB RAW: data must not be null or empty");
        }

        // VEMB RAW returns at least 3 elements: quantization type, vector data, and L2 norm
        if (data.size() < 3) {
            throw new IllegalArgumentException("Failed while parsing VEMB RAW: data must contain at least 3 elements");
        }

        return data;
    }

    /**
     * Parses a value from the VEMB RAW response into a Double.
     *
     * @param value the value to parse
     * @return the parsed Double value, or null if the value cannot be parsed
     */
    private Double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                // Return null if the string cannot be parsed as a double
                return null;
            }
        }
        return null;
    }

    /**
     * Parses the raw vector data from the VEMB RAW response.
     *
     * @param value the raw vector data from the VEMB RAW response
     * @return a ByteBuffer containing the raw vector data
     * @throws IllegalArgumentException if the value is not a valid ByteBuffer or byte array
     */
    private ByteBuffer parseVectorData(Object value) {
        if (value instanceof ByteBuffer) {
            return (ByteBuffer) value;
        } else if (value instanceof byte[]) {
            return ByteBuffer.wrap((byte[]) value);
        } else if (value instanceof String) {
            // In case the binary data is returned as a string
            byte[] bytes = ((String) value).getBytes();
            return ByteBuffer.wrap(bytes);
        }
        throw new IllegalArgumentException("Failed while parsing VEMB RAW: vector data must be a binary blob");
    }

    /**
     * Maps a quantization type string from the VEMB RAW response to a {@link QuantizationType} enum value.
     *
     * @param quantTypeStr the quantization type string from the VEMB RAW response
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
