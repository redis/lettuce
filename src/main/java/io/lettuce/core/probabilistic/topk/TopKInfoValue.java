/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk;

import java.util.Map;

/**
 * Represents the result of the Redis <a href="https://redis.io/docs/latest/commands/topk.info/">TOPK.INFO</a> command.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class TopKInfoValue {

    private final Map<String, Object> rawInfo;

    private final Long k;

    private final Long width;

    private final Long depth;

    private final Double decay;

    public TopKInfoValue(Map<String, Object> rawInfo) {
        this.rawInfo = rawInfo;
        this.k = (Long) rawInfo.get("k");
        this.width = (Long) rawInfo.get("width");
        this.depth = (Long) rawInfo.get("depth");
        this.decay = (Double) rawInfo.get("decay");
    }

    /**
     * Returns the raw info map returned by the Redis server.
     *
     * @return the raw info map
     */
    public Map<String, Object> getRawInfo() {
        return rawInfo;
    }

    /**
     * Returns the number of top items to keep.
     *
     * @return the number of top items to keep
     */
    public Long getK() {
        return k;
    }

    /**
     * Returns the number of counters kept per item.
     *
     * @return the number of counters kept per item
     */
    public Long getWidth() {
        return width;
    }

    /**
     * Returns the number of counters per item.
     *
     * @return the number of counters per item
     */
    public Long getDepth() {
        return depth;
    }

    /**
     * Returns the probability of an item to be retained.
     *
     * @return the probability of an item to be retained
     */
    public Double getDecay() {
        return decay;
    }

}
