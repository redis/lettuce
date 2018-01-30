/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link java.util.List} of objects and lists to support dynamic nested structures (List with mixed content of values and
 * sublists).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ArrayOutput<K, V> extends CommandOutput<K, V, List<Object>> {

    private boolean initialized;
    private Deque<Integer> counts = new ArrayDeque<Integer>();
    private Deque<List<Object>> stack = new ArrayDeque<List<Object>>();

    public ArrayOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (bytes != null) {
            V value = codec.decodeValue(bytes);
            stack.peek().add(value);
        }
    }

    @Override
    public void set(long integer) {
        stack.peek().add(integer);
    }

    @Override
    public void complete(int depth) {
        if (counts.isEmpty()) {
            return;
        }

        if (depth == stack.size()) {
            if (stack.peek().size() == counts.peek()) {
                List<Object> pop = stack.pop();
                counts.pop();
                if (!stack.isEmpty()) {
                    stack.peek().add(pop);
                }
            }
        }
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }

        if (stack.isEmpty()) {
            stack.push(output);
        } else {
            stack.push(OutputFactory.newList(count));

        }
        counts.push(count);
    }
}
