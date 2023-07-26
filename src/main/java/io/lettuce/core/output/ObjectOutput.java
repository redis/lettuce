/*
 * Copyright 2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceFactories;

/**
 * RESP3-capable command output that represents the RESP3 response as RESP3 primitives including support for nesting. Decodes
 * simple strings through {@link StringCodec#UTF8}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.3.0
 */
public class ObjectOutput<K, V> extends CommandOutput<K, V, Object> {

    private final Deque<Object> stack;

    private Object current;

    private boolean hasCurrent = false;

    private int depth;

    private boolean initialized;

    public ObjectOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        stack = LettuceFactories.newSpScQueue();
        depth = 0;
    }

    @Override
    public void set(long integer) {

        if (!initialized) {
            output = new ArrayList<>();
        }

        setValue(integer);
    }

    @Override
    public void set(double number) {
        setValue(number);
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        setSingle(bytes);
    }

    @Override
    public void set(boolean value) {
        setValue(value);
    }

    @Override
    public void set(ByteBuffer bytes) {
        setValue(bytes == null ? null : codec.decodeValue(bytes));
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        setValue(bytes == null ? null : StringCodec.UTF8.decodeValue(bytes));
    }

    @SuppressWarnings("unchecked")
    private void setValue(Object value) {

        if (output != null) {

            if (output instanceof Collection) {
                ((Collection<Object>) output).add(value);
            } else if (output instanceof Map) {
                if (!hasCurrent) {
                    current = value;
                    hasCurrent = true;
                } else {
                    ((Map<Object, Object>) output).put(current, value);
                    current = null;
                    hasCurrent = false;
                }
            } else {
                throw new IllegalStateException(
                        String.format("Output %s is not a supported container type to append a response value", output));
            }
        } else {
            output = value;
        }
    }

    @Override
    public void complete(int depth) {
        if (depth > 0 && depth < this.depth) {
            stack.pop();
            output = stack.peek();
            this.depth--;
        }
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(Math.max(1, count));
            push(output);
            initialized = true;
        } else {
            List<Object> list = OutputFactory.newList(count);
            setValue(list);
            push(list);
            output = list;
        }

        this.depth++;
    }

    @Override
    public void multiMap(int count) {

        if (!initialized) {
            output = new LinkedHashMap<>(count);
            initialized = true;
            push(output);
        } else {

            Map<Object, Object> map = new LinkedHashMap<>(count);
            setValue(map);
            push(map);
            output = map;
        }

        this.depth++;
    }

    private void push(Object output) {
        stack.push(output);
    }

}
