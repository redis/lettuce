/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.nio.ByteBuffer;
import java.util.List;

import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for the scan-dump command output of the RedisBloom module, such as
 * <a href="https://redis.io/docs/latest/commands/bf.scandump/">BF.SCANDUMP</a> and
 * <a href="https://redis.io/docs/latest/commands/cf.scandump/">CF.SCANDUMP</a>.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public final class ScanDumpValueParser implements ComplexDataParser<ScanDumpValue> {

    public static final ScanDumpValueParser INSTANCE = new ScanDumpValueParser();

    private ScanDumpValueParser() {
    }

    @Override
    public ScanDumpValue parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing SCANDUMP: data must not be null");
        }
        List<Object> raw = data.getDynamicList();
        if (raw == null || raw.size() != 2) {
            throw new IllegalArgumentException("Failed parsing SCANDUMP: data must be a list of two elements");
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
        return new ScanDumpValue(iterator, dataBytes);
    }

}
