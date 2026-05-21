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
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/bf.info/">BF.INFO</a> command output.
 *
 * @author Yordan Tsintsov
 * @since 7.6
 */
public final class BfInfoValueParser implements ComplexDataParser<BfInfoValue> {

    public static final BfInfoValueParser INSTANCE = new BfInfoValueParser();

    private BfInfoValueParser() {
    }

    @Override
    public BfInfoValue parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing BF.INFO: data must not be null");
        }
        Map<Object, Object> raw = data.getDynamicMap();
        Map<String, Object> info = new LinkedHashMap<>(raw.size());
        for (Map.Entry<Object, Object> e : raw.entrySet()) {
            String k = StringCodec.UTF8.decodeKey((ByteBuffer) e.getKey());
            info.put(k, e.getValue());
        }
        return new BfInfoValue(info);
    }

}
