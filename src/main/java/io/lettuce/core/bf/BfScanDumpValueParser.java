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

import java.nio.ByteBuffer;
import java.util.List;

import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/bf.scandump/">BF.SCANDUMP</a> command output.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public final class BfScanDumpValueParser implements ComplexDataParser<BfScanDumpValue> {

    public static final BfScanDumpValueParser INSTANCE = new BfScanDumpValueParser();

    private BfScanDumpValueParser() {
    }

    @Override
    public BfScanDumpValue parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing BF.SCANDUMP: data must not be null");
        }
        List<Object> raw = data.getDynamicList();
        if (raw == null || raw.size() != 2) {
            throw new IllegalArgumentException("Failed parsing BF.SCANDUMP: data must be a list of two elements");
        }
        long iterator = ((Number) raw.get(0)).longValue();
        ByteBuffer dataByteBuffer = (ByteBuffer) raw.get(1);
        byte[] dataBytes;
        if (dataByteBuffer == null) {
            dataBytes = new byte[0];
        } else {
            dataBytes = new byte[dataByteBuffer.remaining()];
            dataByteBuffer.get(dataBytes);
        }
        return new BfScanDumpValue(iterator, dataBytes);
    }

}
