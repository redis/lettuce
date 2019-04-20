/*
 * Copyright 2018-2019 the original author or authors.
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

import io.lettuce.core.LettuceStrings;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;

/**
 * A single {@link ScoredValue}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.1
 */
public class ScoredValueOutput<K, V> extends CommandOutput<K, V, ScoredValue<V>> {

    private V value;

    public ScoredValueOutput(RedisCodec<K, V> codec) {
        super(codec, ScoredValue.empty());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            return;
        }

        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = LettuceStrings.toDouble(decodeAscii(bytes));
        set(score);
    }

    @Override
    public void set(double number) {
        output = ScoredValue.just(number, value);
        value = null;
    }
}
