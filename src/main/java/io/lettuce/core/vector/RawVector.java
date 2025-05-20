/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.vector;

import java.nio.ByteBuffer;

public class RawVector {

    private QuantizationType type;

    private ByteBuffer vector;

    private Double beforeNormalization;

    private Double quantizationRange;

    public RawVector(QuantizationType type, ByteBuffer vector, Double beforeNormalization, Double quantizationRange) {
        this.type = type;
        this.vector = vector;
        this.beforeNormalization = beforeNormalization;
        this.quantizationRange = quantizationRange;
    }

    public RawVector() {

    }

    public QuantizationType getType() {
        return type;
    }

    public ByteBuffer getVector() {
        return vector;
    }

    public Double beforeNormalization() {
        return beforeNormalization;
    }

    public Double getQuantizationRange() {
        return quantizationRange;
    }

}
