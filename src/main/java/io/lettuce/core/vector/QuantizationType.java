/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.vector;

import io.lettuce.core.protocol.CommandKeyword;

/**
 * Enumeration of quantization types used for storing vectors in Redis Vector Sets.
 * <p>
 * Quantization affects how vectors are stored and impacts memory usage, performance, and recall quality. Different quantization
 * types offer different trade-offs between memory usage, search speed, and accuracy.
 *
 * @author Tihomir Mateev
 * @since 6.7
 * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/performance/#quantization-effects">Redis
 *      Documentation: Quantization Effects</a>
 */
public enum QuantizationType {

    /**
     * No quantization. Vectors are stored as 32-bit floating point values.
     * <p>
     * This provides the highest precision but uses the most memory (4 bytes per dimension). Use this when precision is critical
     * and memory usage is not a concern.
     * <p>
     * Corresponds to the {@code NOQUANT} option in Redis commands.
     */
    NO_QUANTIZATION(CommandKeyword.NOQUANT.toString()),

    /**
     * Binary quantization. Vectors are stored as binary values (1 bit per dimension).
     * <p>
     * This is the most memory-efficient option (1 bit per dimension) and provides the fastest search, but has the lowest recall
     * quality. Use this when memory usage and speed are critical, and some loss in recall quality is acceptable.
     * <p>
     * Corresponds to the {@code BIN} option in Redis commands.
     */
    BINARY(CommandKeyword.BIN.toString()),

    /**
     * Signed 8-bit quantization. Vectors are stored as 8-bit integers.
     * <p>
     * This is the default quantization type and provides a good balance between memory usage (1 byte per dimension) and recall
     * quality. Use this for most use cases.
     * <p>
     * Corresponds to the {@code Q8} option in Redis commands.
     */
    Q8(CommandKeyword.Q8.toString());

    private final String keyword;

    /**
     * Creates a new quantization type with the specified keyword.
     *
     * @param keyword the command keyword string used in Redis commands
     */
    QuantizationType(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Gets the command keyword string used in Redis commands for this quantization type.
     *
     * @return the command keyword string
     */
    public String getKeyword() {
        return keyword;
    }

}
