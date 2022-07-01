/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.KeyValue;
import io.lettuce.core.codec.RedisCodec;

/**
 * Key-value pair output holding a list of values.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.2
 */
public class KeyValueValueListOutput<K, V> extends CommandOutput<K, V, KeyValue<K, List<V>>> {

    private K key;

    private List<V> values = Collections.emptyList();

    public KeyValueValueListOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes != null) {
            if (key == null) {
                key = codec.decodeKey(bytes);
            } else {
                V value = codec.decodeValue(bytes);
                values.add(value);
            }
        }
    }

    @Override
    public void multi(int count) {
        values = OutputFactory.newList(count);
    }

    @Override
    public void complete(int depth) {
        if (depth == 0 && key != null) {
            output = KeyValue.just(key, values);
        }
    }

}
