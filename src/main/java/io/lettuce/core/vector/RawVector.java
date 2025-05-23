/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.vector;

import java.nio.ByteBuffer;

/**
 * Represents a raw vector returned by the Redis VEMB command with the RAW option.
 * <p>
 * This class encapsulates the binary representation of a vector along with metadata about its
 * quantization type, normalization factor, and quantization range. The raw binary format
 * allows for efficient storage and processing of vector data.
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
 * @see <a href="https://redis.io/docs/latest/commands/vemb/">Redis Documentation: VEMB</a>
 */
public class RawVector {

    /**
     * The quantization type used for storing the vector.
     * <p>
     * This indicates how the vector data is encoded in the binary blob.
     */
    private QuantizationType type;

    /**
     * The raw binary data of the vector.
     * <p>
     * The format of this data depends on the quantization type:
     * <ul>
     *   <li>For {@code NOQUANT}, it contains 32-bit floating point values</li>
     *   <li>For {@code Q8}, it contains 8-bit integer values</li>
     *   <li>For {@code BINARY}, it contains binary values (1 bit per dimension)</li>
     * </ul>
     */
    private ByteBuffer vector;

    /**
     * The L2 norm of the vector before normalization.
     * <p>
     * This value represents the original magnitude of the vector before it was normalized
     * for storage in the vector set. It can be used to reconstruct the original vector.
     */
    private Double beforeNormalization;

    /**
     * The quantization range used for Q8 quantization.
     * <p>
     * This value is only present for vectors with Q8 quantization and is used to convert
     * between the 8-bit integer representation and the original floating point values.
     * <p>
     * The formula to convert from quantized to original values is:
     * original = quantized * (quantizationRange / 127.0)
     */
    private Double quantizationRange;

    /**
     * Creates a new raw vector with the specified properties.
     *
     * @param type the quantization type used for storing the vector
     * @param vector the raw binary data of the vector
     * @param beforeNormalization the L2 norm of the vector before normalization
     * @param quantizationRange the quantization range (only for Q8 quantization)
     */
    public RawVector(QuantizationType type, ByteBuffer vector, Double beforeNormalization, Double quantizationRange) {
        this.type = type;
        this.vector = vector;
        this.beforeNormalization = beforeNormalization;
        this.quantizationRange = quantizationRange;
    }

    /**
     * Creates a new empty raw vector.
     * <p>
     * This constructor creates an empty raw vector object. The fields will be populated
     * when the object is used to parse the response from a Redis VEMB command with the RAW option.
     */
    public RawVector() {
        // Default constructor
    }

    /**
     * Gets the quantization type used for storing the vector.
     *
     * @return the quantization type, or {@code null} if not available
     */
    public QuantizationType getType() {
        return type;
    }

    /**
     * Gets the raw binary data of the vector.
     * <p>
     * The format of this data depends on the quantization type.
     *
     * @return the raw binary data of the vector, or {@code null} if not available
     */
    public ByteBuffer getVector() {
        return vector;
    }

    /**
     * Gets the L2 norm of the vector before normalization.
     * <p>
     * This value represents the original magnitude of the vector before it was normalized
     * for storage in the vector set.
     *
     * @return the L2 norm of the vector before normalization, or {@code null} if not available
     */
    public Double beforeNormalization() {
        return beforeNormalization;
    }

    /**
     * Gets the quantization range used for Q8 quantization.
     * <p>
     * This value is only present for vectors with Q8 quantization and is used to convert
     * between the 8-bit integer representation and the original floating point values.
     *
     * @return the quantization range, or {@code null} if not available or not using Q8 quantization
     */
    public Double getQuantizationRange() {
        return quantizationRange;
    }

}
