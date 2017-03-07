/*
 * Copyright 2011-2016 the original author or authors.
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceFactories;

/**
 * {@link List} of command outputs, possibly deeply nested.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class NestedMultiOutput<K, V> extends CommandOutput<K, V, List<Object>> {
    private final Deque<List<Object>> stack;
    private int depth;

    public NestedMultiOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        stack = LettuceFactories.newSpScQueue();
        depth = 0;
    }

    @Override
    public void set(long integer) {
        output.add(integer);
    }

    @Override
    public void set(ByteBuffer bytes) {
        output.add(bytes == null ? null : codec.decodeValue(bytes));
    }

    @Override
    public void complete(int depth) {
        if (depth > 0 && depth < this.depth) {
            output = stack.pop();
            this.depth--;
        }
    }

    @Override
    public void multi(int count) {
        List<Object> a = new ArrayList<>(count);
        output.add(a);
        stack.push(output);
        output = a;
        this.depth++;
    }
}
