/*
 * Copyright 2024, Redis Ltd. and Contributors
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
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.output.data.DynamicAggregateData;
import io.lettuce.core.output.data.ArrayAggregateData;
import io.lettuce.core.output.data.MapAggregateData;
import io.lettuce.core.output.data.SetAggregateData;

import java.nio.ByteBuffer;
import java.util.Deque;

/**
 * An implementation of the {@link CommandOutput} that heuristically attempts to parse any response the Redis server could
 * provide, leaving out the user to provide the knowledge of how this data is to be processed
 * <p>
 * The Redis server could return, besides the simple types such as boolean, long, String, etc. also aggregations of the former,
 * inside aggregate data structures such as arrays, maps and sets, defined in the specification for both the
 * <a href="https://github.com/redis/redis-specifications/blob/master/protocol/RESP2.md">RESP2</a> and
 * <a href="https://github.com/redis/redis-specifications/blob/master/protocol/RESP3.md">RESP3</a> protocol.
 * <p>
 * Commands typically result in simple types, however some of the commands could return complex nested structures. A simple
 * solution to parse such a structure is to have a dynamic data type and leave the user to parse the result to a domain object.
 * If there is some breach of contract then the code consuming the driver could simply stop using the provided parser and start
 * parsing the new dynamic data itself, without having to change the version of the library. This allows a certain degree of
 * stability against change. Consult the members of the {@link io.lettuce.core.api.parsers} package for details on how aggregate
 * Objects could be parsed.
 * <p>
 * The {@link DynamicAggregateOutput} is supposed to be compatible with all Redis commands.
 *
 * @see DynamicAggregateData
 * @author Tihomir Mateev
 * @since 7.0
 */
public class DynamicAggregateOutput<K, V> extends CommandOutput<K, V, DynamicAggregateData> {

    private final Deque<DynamicAggregateData> dataStack;

    /**
     * Constructs a new instance of the {@link DynamicAggregateOutput}
     * 
     * @param codec the {@link RedisCodec} to be applied
     */
    public DynamicAggregateOutput(RedisCodec<K, V> codec) {
        super(codec, null);
        dataStack = LettuceFactories.newSpScQueue();
    }

    @Override
    public void set(long integer) {
        if (output == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "Integer value should have been preceded by some sort of aggregation.");
        }

        output.store(integer);
    }

    @Override
    public void set(double number) {
        if (output == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "Double value should have been preceded by some sort of aggregation.");
        }

        output.store(number);
    }

    @Override
    public void set(boolean value) {
        if (output == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "Double value should have been preceded by some sort of aggregation.");
        }

        output.store(value);
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (output == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "ByteBuffer value should have been preceded by some sort of aggregation.");
        }

        output.storeObject(bytes == null ? null : codec.decodeValue(bytes));
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        if (output == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "String value should have been preceded by some sort of aggregation.");
        }

        output.store(bytes == null ? null : StringCodec.UTF8.decodeValue(bytes));
    }

    @Override
    public void complete(int depth) {
        if (!dataStack.isEmpty() && depth == dataStack.size()) {
            output = dataStack.pop();
        }
    }

    private void multi(DynamicAggregateData data) {
        // if there is no data set, then we are at the root object
        if (output == null) {
            output = data;
            return;
        }

        // otherwise we need to nest the provided structure
        output.storeObject(data);
        dataStack.push(output);
        output = data;
    }

    @Override
    public void multiSet(int count) {
        SetAggregateData dynamicData = new SetAggregateData(count);
        multi(dynamicData);
    }

    @Override
    public void multiArray(int count) {
        ArrayAggregateData dynamicData = new ArrayAggregateData(count);
        multi(dynamicData);
    }

    @Override
    public void multiMap(int count) {
        MapAggregateData dynamicData = new MapAggregateData(count);
        multi(dynamicData);
    }

}
