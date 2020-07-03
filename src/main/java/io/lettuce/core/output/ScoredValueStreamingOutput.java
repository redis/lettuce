/*
 * Copyright 2011-2020 the original author or authors.
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

import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;

/**
 * Streaming-Output of of values and their associated scores. Returns the count of all values (including null).
 *
 * @author Mark Paluch
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class ScoredValueStreamingOutput<K, V> extends CommandOutput<K, V, Long> {

    private V value;

    private final ScoredValueStreamingChannel<V> channel;

    public ScoredValueStreamingOutput(RedisCodec<K, V> codec, ScoredValueStreamingChannel<V> channel) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = LettuceStrings.toDouble(decodeAscii(bytes));
        set(score);
    }

    @Override
    public void set(double number) {

        channel.onValue(ScoredValue.fromNullable(number, value));
        value = null;
        output = output.longValue() + 1;
    }

}
