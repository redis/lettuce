/*
 * Copyright 2011-2019 the original author or authors.
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
import java.util.Collections;
import java.util.Set;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link Set} of value output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class ValueSetOutput<K, V> extends CommandOutput<K, V, Set<V>> {

    private boolean initialized;

    public ValueSetOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptySet());
    }

    @Override
    public void set(ByteBuffer bytes) {

        // RESP 3 behavior
        if (bytes == null && !initialized) {
            return;
        }

        output.add(bytes == null ? null : codec.decodeValue(bytes));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newSet(count);
            initialized = true;
        }
    }
}
