/*
 * Copyright 2018-2022 the original author or authors.
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

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * A single {@link ScoredValue} with result as a {@code Long} value.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.3
 */
public class ScoredValueLongOutput<K, V> extends CommandOutput<K, V, ScoredValue<Long>> {

    private Long value;

    public ScoredValueLongOutput(RedisCodec<K, V> codec) {
        super(codec, ScoredValue.empty());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            return;
        }

        if (value == null) {
            value = Long.parseLong(StandardCharsets.UTF_8.decode(bytes).toString());
            return;
        }

        double score = LettuceStrings.toDouble(decodeAscii(bytes));
        set(score);
    }

    @Override
    public void set(long integer) {
        value = integer;
    }

    @Override
    public void set(double number) {
        output = ScoredValue.just(number, value);
        value = null;
    }
}
