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
import java.util.List;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of values and their associated scores.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class ScoredValueListOutput<K, V> extends CommandOutput<K, V, List<ScoredValue<V>>>
        implements StreamingOutput<ScoredValue<V>> {
    private V value;
    private Subscriber<ScoredValue<V>> subscriber;

    public ScoredValueListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = Double.parseDouble(decodeAscii(bytes));
        subscriber.onNext(ScoredValue.fromNullable(score, value));
        value = null;
    }

    @Override
    public void setSubscriber(Subscriber<ScoredValue<V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<ScoredValue<V>> getSubscriber() {
        return subscriber;
    }
}
