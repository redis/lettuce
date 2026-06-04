/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.bf;

import java.util.Map;

/**
 * Represents the result of the Redis <a href="https://redis.io/docs/latest/commands/bf.info/">BF.INFO</a> command.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class BfInfoValue {

    private final Map<String, Object> rawInfo;

    private final Long capacity;

    private final Long size;

    private final Long numberOfFilters;

    private final Long numberOfItemsInserted;

    private final Long expansionRate;

    public BfInfoValue(Map<String, Object> rawInfo) {
        this.rawInfo = rawInfo;
        this.capacity = (Long) rawInfo.get("Capacity");
        this.size = (Long) rawInfo.get("Size");
        this.numberOfFilters = (Long) rawInfo.get("Number of filters");
        this.numberOfItemsInserted = (Long) rawInfo.get("Number of items inserted");
        this.expansionRate = (Long) rawInfo.get("Expansion rate");
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
     * Returns the maximum number of items that can be inserted into the filter.
     *
     * @return the maximum number of items that can be inserted into the filter
     */
    public Long getCapacity() {
        return capacity;
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
     * Returns the number of filters in the filter.
     *
     * @return the number of filters in the filter
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
     * Returns the expansion rate of the filter.
     *
     * @return the expansion rate of the filter
     */
    public Long getExpansionRate() {
        return expansionRate;
    }

}
