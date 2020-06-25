/*
 * Copyright 2018-2020 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 5.1
 */
public class StreamReadOutput<K, V> extends CommandOutput<K, V, List<StreamMessage<K, V>>>
        implements StreamingOutput<StreamMessage<K, V>> {

    private boolean initialized;

    private Subscriber<StreamMessage<K, V>> subscriber;

    private boolean skipStreamKeyReset = false;

    private K stream;

    private K key;

    private String id;

    private Map<K, V> body;

    public StreamReadOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (stream == null) {
            if (bytes == null) {
                return;
            }

            stream = codec.decodeKey(bytes);
            skipStreamKeyReset = true;
            return;
        }

        if (id == null) {
            id = decodeAscii(bytes);
            return;
        }

        if (key == null) {
            key = codec.decodeKey(bytes);
            return;
        }

        if (body == null) {
            body = new LinkedHashMap<>();
        }

        body.put(key, bytes == null ? null : codec.decodeValue(bytes));
        key = null;
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void complete(int depth) {

        if (depth == 3 && body != null) {
            subscriber.onNext(output, new StreamMessage<>(stream, id, body));
            key = null;
            body = null;
            id = null;
        }

        // RESP2/RESP3 compat
        if (depth == 2 && skipStreamKeyReset) {
            skipStreamKeyReset = false;
        }

        if (depth == 1) {
            if (skipStreamKeyReset) {
                skipStreamKeyReset = false;
            } else {
                stream = null;
            }
        }
    }

    @Override
    public void setSubscriber(Subscriber<StreamMessage<K, V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<StreamMessage<K, V>> getSubscriber() {
        return subscriber;
    }

}
