/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.output;

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;

/**
 * Double output, may be null.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class DoubleOutput<K, V> extends CommandOutput<K, V, Double> {

    public DoubleOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        output = (bytes == null) ? null : parseDouble(decodeString(bytes));
    }

    @Override
    public void set(double number) {
        output = number;
    }

}
