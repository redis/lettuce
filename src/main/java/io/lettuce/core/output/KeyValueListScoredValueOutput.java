/*
 * Copyright 2023-2024 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.KeyValue;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;

/**
 * Output for multiple [B]ZMPOP result items.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.3
 */
public class KeyValueListScoredValueOutput<K, V> extends CommandOutput<K, V, KeyValue<K, List<ScoredValue<V>>>> {

    private K key;

    private V value;

    private double score;

    private List<ScoredValue<V>> items = new ArrayList<>();

    public KeyValueListScoredValueOutput(RedisCodec<K, V> codec, K defaultKey) {
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
            score = parseDouble(decodeAscii(bytes));
        }
    }

    @Override
    public void set(double number) {

        if (value != null) {
            score = number;
        }
    }

    @Override
    public void complete(int depth) {

        if (depth == 2) {
            items.add(ScoredValue.just(score, value));
            value = null;
        }

        if (depth == 0) {
            if (key != null) {
                output = KeyValue.just(key, items);
            }
        }
    }

}
