/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.util.Map;

/**
 * Represents the result of the Redis <a href="https://redis.io/docs/latest/commands/cf.info/">CF.INFO</a> command.
 *
 * @author HwangRock
 * @since 7.7
 */
public class CfInfoValue {

    private final Map<String, Object> rawInfo;

    private final Long size;

    private final Long numberOfBuckets;

    private final Long numberOfFilters;

    private final Long numberOfItemsInserted;

    private final Long numberOfItemsDeleted;

    private final Long bucketSize;

    private final Long expansionRate;

    private final Long maxIterations;

    public CfInfoValue(Map<String, Object> rawInfo) {
        this.rawInfo = rawInfo;
        this.size = (Long) rawInfo.get("Size");
        this.numberOfBuckets = (Long) rawInfo.get("Number of buckets");
        this.numberOfFilters = (Long) rawInfo.get("Number of filters");
        this.numberOfItemsInserted = (Long) rawInfo.get("Number of items inserted");
        this.numberOfItemsDeleted = (Long) rawInfo.get("Number of items deleted");
        this.bucketSize = (Long) rawInfo.get("Bucket size");
        this.expansionRate = (Long) rawInfo.get("Expansion rate");
        this.maxIterations = (Long) rawInfo.get("Max iterations");
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
     * Returns the current size of the filter in bytes.
     *
     * @return the current size of the filter in bytes
     */
    public Long getSize() {
        return size;
    }

    /**
     * Returns the number of buckets in the filter.
     *
     * @return the number of buckets in the filter
     */
    public Long getNumberOfBuckets() {
        return numberOfBuckets;
    }

    /**
     * Returns the number of sub-filters in the filter.
     *
     * @return the number of sub-filters in the filter
     */
    public Long getNumberOfFilters() {
        return numberOfFilters;
    }

    /**
     * Returns the number of items inserted into the filter.
     *
     * @return the number of items inserted into the filter
     */
    public Long getNumberOfItemsInserted() {
        return numberOfItemsInserted;
    }

    /**
     * Returns the number of items deleted from the filter.
     *
     * @return the number of items deleted from the filter
     */
    public Long getNumberOfItemsDeleted() {
        return numberOfItemsDeleted;
    }

    /**
     * Returns the bucket size of the filter.
     *
     * @return the bucket size of the filter
     */
    public Long getBucketSize() {
        return bucketSize;
    }

    /**
     * Returns the expansion rate of the filter.
     *
     * @return the expansion rate of the filter
     */
    public Long getExpansionRate() {
        return expansionRate;
    }

    /**
     * Returns the maximum number of iterations when inserting items into the filter.
     *
     * @return the maximum number of iterations when inserting items into the filter
     */
    public Long getMaxIterations() {
        return maxIterations;
    }

}
