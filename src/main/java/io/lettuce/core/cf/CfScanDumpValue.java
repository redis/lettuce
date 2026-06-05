/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cf;

/**
 * Value object for the Redis <a href="https://redis.io/docs/latest/commands/cf.scandump/">CF.SCANDUMP</a> command.
 *
 * @author HwangRock
 * @since 7.7
 */
public class CfScanDumpValue {

    private final long iterator;

    private final byte[] data;

    public CfScanDumpValue(long iterator, byte[] data) {
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
