/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk;

/**
 * Represents the result of the Redis <a href="https://redis.io/docs/latest/commands/topk.list/">TOPK.LIST</a> command.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class TopKListValue {

    private final String name;

    private final Integer count;

    public TopKListValue(String name, Integer count) {
        this.name = name;
        this.count = count;
    }

    /**
     * Returns the name of the value.
     *
     * @return the name of the value
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the count of the value.
     *
     * @return the count of the value or null if not requested
     */
    public Integer getCount() {
        return count;
    }

}
