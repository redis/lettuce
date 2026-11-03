/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.vector;

/**
 * Holder for a VSIM result entry that includes both the similarity score and the element attributes when WITHATTRIBS is
 * requested.
 */
public final class VSimScoreAttribs {

    private final double score;

    private final String attributes;

    public VSimScoreAttribs(double score, String attributes) {
        this.score = score;
        this.attributes = attributes;
    }

    public double getScore() {
        return score;
    }

    public String getAttributes() {
        return attributes;
    }

}
