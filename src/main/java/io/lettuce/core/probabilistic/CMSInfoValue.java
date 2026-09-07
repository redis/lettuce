/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.util.Map;

/**
 * Represents the result of the Redis <a href="https://redis.io/docs/latest/commands/cms.info/">CMS.INFO</a> command.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class CMSInfoValue {

    private final Map<String, Object> rawInfo;

    private final Long width;

    private final Long depth;

    private final Long count;

    /**
     * Creates a new {@link CMSInfoValue}.
     *
     * @param rawInfo the map containing the values of the sketch as plain {@code Object}.
     */
    public CMSInfoValue(Map<String, Object> rawInfo) {
        this.rawInfo = rawInfo;
        this.width = (Long) rawInfo.get("width");
        this.depth = (Long) rawInfo.get("depth");
        this.count = (Long) rawInfo.get("count");
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
     * Returns the width of the sketch.
     *
     * @return the width of the sketch.
     */
    public Long getWidth() {
        return width;
    }

    /**
     * Returns the depth of the sketch.
     *
     * @return the depth of the sketch.
     */
    public Long getDepth() {
        return depth;
    }

    /**
     * Returns the count of the sketch.
     *
     * @return the count of the sketch.
     */
    public Long getCount() {
        return count;
    }

}
