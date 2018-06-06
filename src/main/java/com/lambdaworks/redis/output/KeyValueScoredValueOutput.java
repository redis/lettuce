/*
 * Copyright 2018 the original author or authors.
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
package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.KeyValue;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link com.lambdaworks.redis.KeyValue} encapsulating {@link ScoredValue}. See {@code BZPOPMIN}/{@code BZPOPMAX} commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.5
 */
public class KeyValueScoredValueOutput<K, V> extends CommandOutput<K, V, KeyValue<K, ScoredValue<V>>> {

    private K key;
    private V value;

    public KeyValueScoredValueOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (key == null) {
            key = codec.decodeKey(bytes);
            return;
        }

        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = LettuceStrings.toDouble(decodeAscii(bytes));

        output = new KeyValue<>(key, new ScoredValue<>(score, value));
        key = null;
        value = null;
    }

}
