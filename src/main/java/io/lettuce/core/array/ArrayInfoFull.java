/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import java.util.Collections;
import java.util.Map;

import io.lettuce.core.annotations.Experimental;

/**
 * Immutable representation of extended metadata about a Redis array, returned by the {@code ARINFO key FULL} command.
 * <p>
 * Extends the base fields from {@link ArrayInfo} with per-slice statistics. Known fields are available via typed getters. The
 * raw server map is accessible via {@link #getInfo()} so callers can read fields added in future server versions without
 * requiring a client release.
 * <p>
 * Note: The {@code avg-dense-size}, {@code avg-dense-fill}, and {@code avg-sparse-size} fields are returned as bulk strings by
 * Redis (not integers), so they are represented as {@link String} here.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see ArrayInfo
 * @see <a href="https://redis.io/docs/latest/commands/arinfo/">Redis Documentation: ARINFO</a>
 */
@Experimental
public class ArrayInfoFull extends ArrayInfo {

    public static final String DENSE_SLICES = "dense-slices";

    public static final String SPARSE_SLICES = "sparse-slices";

    public static final String AVG_DENSE_SIZE = "avg-dense-size";

    public static final String AVG_DENSE_FILL = "avg-dense-fill";

    public static final String AVG_SPARSE_SIZE = "avg-sparse-size";

    private final Long denseSlices;

    private final Long sparseSlices;

    private final String avgDenseSize;

    private final String avgDenseFill;

    private final String avgSparseSize;

    /**
     * Creates a new {@link ArrayInfoFull} from the raw server response map.
     *
     * @param map the raw key-value pairs from the ARINFO FULL response.
     */
    public ArrayInfoFull(Map<String, Object> map) {
        super(map);
        this.denseSlices = parseLong(map.get(DENSE_SLICES));
        this.sparseSlices = parseLong(map.get(SPARSE_SLICES));
        this.avgDenseSize = map.get(AVG_DENSE_SIZE) != null ? map.get(AVG_DENSE_SIZE).toString() : null;
        this.avgDenseFill = map.get(AVG_DENSE_FILL) != null ? map.get(AVG_DENSE_FILL).toString() : null;
        this.avgSparseSize = map.get(AVG_SPARSE_SIZE) != null ? map.get(AVG_SPARSE_SIZE).toString() : null;
    }

    /**
     * @return the number of dense slices, or {@code null} if not available.
     */
    public Long getDenseSlices() {
        return denseSlices;
    }

    /**
     * @return the number of sparse slices, or {@code null} if not available.
     */
    public Long getSparseSlices() {
        return sparseSlices;
    }

    /**
     * @return the average size of dense slices as a string, or {@code null} if not available.
     */
    public String getAvgDenseSize() {
        return avgDenseSize;
    }

    /**
     * @return the average fill ratio of dense slices as a string, or {@code null} if not available.
     */
    public String getAvgDenseFill() {
        return avgDenseFill;
    }

    /**
     * @return the average size of sparse slices as a string, or {@code null} if not available.
     */
    public String getAvgSparseSize() {
        return avgSparseSize;
    }

}
