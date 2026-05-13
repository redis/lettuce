/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

/**
 * Represents extended metadata about a Redis array, returned by the {@code ARINFO key FULL} command.
 * <p>
 * Extends {@link ArrayMetadata} with 5 additional per-slice statistics: dense/sparse slice counts and their average sizes.
 * <p>
 * Note: The {@code avg-dense-size}, {@code avg-dense-fill}, and {@code avg-sparse-size} fields are returned as bulk strings by
 * Redis (not integers), so they are represented as {@link String} here.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see ArrayMetadata
 * @see <a href="https://redis.io/docs/latest/commands/arinfo/">Redis Documentation: ARINFO</a>
 */
public class ArrayFullMetadata extends ArrayMetadata {

    private Long denseSlices;

    private Long sparseSlices;

    private String avgDenseSize;

    private String avgDenseFill;

    private String avgSparseSize;

    /**
     * Creates a new empty {@link ArrayFullMetadata} instance.
     */
    public ArrayFullMetadata() {
        // Default constructor
    }

    /**
     * Gets the number of dense slices in the array.
     *
     * @return the dense slice count, or {@code null} if not available
     */
    public Long getDenseSlices() {
        return denseSlices;
    }

    public void setDenseSlices(Long denseSlices) {
        this.denseSlices = denseSlices;
    }

    /**
     * Gets the number of sparse slices in the array.
     *
     * @return the sparse slice count, or {@code null} if not available
     */
    public Long getSparseSlices() {
        return sparseSlices;
    }

    public void setSparseSlices(Long sparseSlices) {
        this.sparseSlices = sparseSlices;
    }

    /**
     * Gets the average size of dense slices.
     * <p>
     * Returned as a string because Redis sends this as a bulk string (e.g. {@code "0"}, {@code "4.5"}).
     *
     * @return the average dense slice size as a string, or {@code null} if not available
     */
    public String getAvgDenseSize() {
        return avgDenseSize;
    }

    public void setAvgDenseSize(String avgDenseSize) {
        this.avgDenseSize = avgDenseSize;
    }

    /**
     * Gets the average fill ratio of dense slices.
     * <p>
     * Returned as a string because Redis sends this as a bulk string.
     *
     * @return the average dense fill as a string, or {@code null} if not available
     */
    public String getAvgDenseFill() {
        return avgDenseFill;
    }

    public void setAvgDenseFill(String avgDenseFill) {
        this.avgDenseFill = avgDenseFill;
    }

    /**
     * Gets the average size of sparse slices.
     * <p>
     * Returned as a string because Redis sends this as a bulk string.
     *
     * @return the average sparse slice size as a string, or {@code null} if not available
     */
    public String getAvgSparseSize() {
        return avgSparseSize;
    }

    public void setAvgSparseSize(String avgSparseSize) {
        this.avgSparseSize = avgSparseSize;
    }

}
