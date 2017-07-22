/*
 * Copyright 2011-2017 the original author or authors.
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

import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.ScoredValueScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link com.lambdaworks.redis.ScoredValueScanCursor} for scan cursor output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ScoredValueScanOutput<K, V> extends ScanOutput<K, V, ScoredValueScanCursor<V>> {

    private V value;

    public ScoredValueScanOutput(RedisCodec<K, V> codec) {
        super(codec, new ScoredValueScanCursor<>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {

        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = LettuceStrings.toDouble(decodeAscii(bytes));
        output.getValues().add(new ScoredValue<>(score, value));
        value = null;
    }
}
