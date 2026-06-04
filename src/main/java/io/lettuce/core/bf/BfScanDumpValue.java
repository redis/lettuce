/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.bf;

/**
 * Value object for the Redis <a href="https://redis.io/docs/latest/commands/bf.scandump/">BF.SCANDUMP</a> command.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class BfScanDumpValue {

    private final long iterator;

    private final byte[] data;

    public BfScanDumpValue(long iterator, byte[] data) {
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
