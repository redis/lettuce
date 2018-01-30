/*
 * Copyright 2011-2018 the original author or authors.
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
import java.util.Iterator;

import io.lettuce.core.codec.RedisCodec;

/**
 * Streaming-Output of Key Value Pairs. Returns the count of all Key-Value pairs (including null).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class KeyValueStreamingOutput<K, V> extends CommandOutput<K, V, Long> {

    private Iterable<K> keys;
    private Iterator<K> keyIterator;
    private K key;
    private KeyValueStreamingChannel<K, V> channel;

    public KeyValueStreamingOutput(RedisCodec<K, V> codec, KeyValueStreamingChannel<K, V> channel) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
    }

    public KeyValueStreamingOutput(RedisCodec<K, V> codec, KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
        this.keys = keys;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (keys == null) {
            if (key == null) {
                key = codec.decodeKey(bytes);
                return;
            }
        } else {
            if (keyIterator == null) {
                keyIterator = keys.iterator();
            }
            key = keyIterator.next();
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);
        channel.onKeyValue(key, value);
        output = output.longValue() + 1;
        key = null;
    }
}
