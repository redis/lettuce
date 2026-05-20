/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.bf;

/**
 * Value object for the Redis <a href="https://redis.io/docs/latest/commands/bf.scandump/">BF.SCAN.DUMP</a> command.
 *
 * @author Yordan Tsintsov
 * @since 7.6
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
