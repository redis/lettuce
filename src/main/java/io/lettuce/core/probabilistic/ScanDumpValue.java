/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

/**
 * Value object for the scan-dump commands of the RedisBloom module, such as
 * <a href="https://redis.io/docs/latest/commands/bf.scandump/">BF.SCANDUMP</a> and
 * <a href="https://redis.io/docs/latest/commands/cf.scandump/">CF.SCANDUMP</a>.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class ScanDumpValue {

    private final long iterator;

    private final byte[] data;

    public ScanDumpValue(long iterator, byte[] data) {
        this.iterator = iterator;
        this.data = data;
    }

    /**
     * Returns the iterator value.
     *
     * @return the iterator value
     */
    public long getIterator() {
        return iterator;
    }

    /**
     * Returns the data.
     *
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

}
