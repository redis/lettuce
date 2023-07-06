/*
 * Copyright 2023 the original author or authors.
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

import static java.lang.Double.*;

import java.nio.ByteBuffer;

import io.lettuce.core.KeyValue;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;

/**
 * Output for a single [B]ZMPOP result.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.3
 */
public class KeyValueOfScoredValueOutput<K, V> extends CommandOutput<K, V, KeyValue<K, ScoredValue<V>>> {

    private K key;

    private V value;

    public KeyValueOfScoredValueOutput(RedisCodec<K, V> codec, K defaultKey) {
        super(codec, KeyValue.empty(defaultKey));
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes != null) {
            if (key == null) {
                key = codec.decodeKey(bytes);
                return;
            }

            if (value == null) {
                value = codec.decodeValue(bytes);
                return;
            }

            output = KeyValue.just(key, ScoredValue.just(parseDouble(decodeAscii(bytes)), value));
        }
    }

    @Override
    public void set(double number) {

        if (key != null && value != null) {
            output = KeyValue.just(key, ScoredValue.just(number, value));
        }
    }

}
