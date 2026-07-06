/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.util.Map;

/**
 * Represents the result of the Redis <a href="https://redis.io/docs/latest/commands/tdigest.info/">TDIGEST.INFO</a> command.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class TDigestInfoValue {

    private final Map<String, Object> rawInfo;

    private final Long compression;

    private final Long capacity;

    private final Long mergedNodes;

    private final Long unmergedNodes;

    private final Long mergedWeight;

    private final Long unmergedWeight;

    private final Long observations;

    private final Long totalCompressions;

    private final Long memoryUsage;

    /**
     * Creates a new {@link TDigestInfoValue}.
     *
     * @param rawInfo the map containing the values of the sketch as plain {@code Object}.
     */
    public TDigestInfoValue(Map<String, Object> rawInfo) {
        this.rawInfo = rawInfo;
        this.compression = (Long) rawInfo.get("Compression");
        this.capacity = (Long) rawInfo.get("Capacity");
        this.mergedNodes = (Long) rawInfo.get("Merged Nodes");
        this.unmergedNodes = (Long) rawInfo.get("Unmerged Nodes");
        this.mergedWeight = (Long) rawInfo.get("Merged Weight");
        this.unmergedWeight = (Long) rawInfo.get("Unmerged Weight");
        this.observations = (Long) rawInfo.get("Observations");
        this.totalCompressions = (Long) rawInfo.get("Total Compressions");
        this.memoryUsage = (Long) rawInfo.get("Memory Usage");
    }

    /**
     * Returns the raw info map returned by the Redis server.
     *
     * @return the raw info map returned by the Redis server
     */
    public Map<String, Object> getRawInfo() {
        return rawInfo;
    }

    /**
     * Returns the compression of the sketch.
     *
     * @return the compression of the sketch.
     */
    public Long getCompression() {
        return compression;
    }

    /**
     * Returns the size of the buffer.
     *
     * @return the size of the buffer.
     */
    public Long getCapacity() {
        return capacity;
    }

    /**
     * Returns the number of merged observations.
     *
     * @return the number of merged observations.
     */
    public Long getMergedNodes() {
        return mergedNodes;
    }

    /**
     * Returns the number of buffered nodes (uncompressed observations).
     *
     * @return the number of buffered nodes (uncompressed observations).
     */
    public Long getUnmergedNodes() {
        return unmergedNodes;
    }

    /**
     * Returns the weight of values of the merged nodes.
     *
     * @return the weight of values of the merged nodes.
     */
    public Long getMergedWeight() {
        return mergedWeight;
    }

    /**
     * Returns the weight of values of the unmerged nodes (uncompressed observations).
     *
     * @return the weight of values of the unmerged nodes (uncompressed observations).
     */
    public Long getUnmergedWeight() {
        return unmergedWeight;
    }

    /**
     * Returns the number of observations added to the sketch.
     *
     * @return the number of observations added to the sketch.
     */
    public Long getObservations() {
        return observations;
    }

    /**
     * Returns the number of times this sketch compressed data together.
     *
     * @return the number of times this sketch compressed data together.
     */
    public Long getTotalCompressions() {
        return totalCompressions;
    }

    /**
     * Returns the number of bytes allocated for the sketch.
     *
     * @return the number of bytes allocated for the sketch.
     */
    public Long getMemoryUsage() {
        return memoryUsage;
    }

}
