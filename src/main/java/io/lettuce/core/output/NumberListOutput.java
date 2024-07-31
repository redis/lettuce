/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
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

import io.lettuce.core.codec.RedisCodec;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link List} of Number output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.5
 */
public class NumberListOutput<K, V> extends CommandOutput<K, V, List<Number>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(NumberListOutput.class);

    private boolean initialized;

    public NumberListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
    }

    @Override
    public void set(ByteBuffer bytes) {
        output.add(bytes != null ? parseNumber(bytes) : null);
    }

    @Override
    public void set(double number) {
        output.add(number);
    }

    @Override
    public void set(long integer) {
        output.add(integer);
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        output.add(bytes != null ? parseNumber(bytes) : null);
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    private Number parseNumber(ByteBuffer bytes) {
        Number result = 0;
        try {
            result = NumberFormat.getNumberInstance().parse(decodeAscii(bytes));
        } catch (ParseException e) {
            LOG.warn("Failed to parse " + bytes, e);
        }

        return result;
    }

}
