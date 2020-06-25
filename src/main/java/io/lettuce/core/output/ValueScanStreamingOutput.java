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

import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * Streaming API for multiple Values. You can implement this interface in order to receive a call to {@code onValue} on every
 * key.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ValueScanStreamingOutput<K, V> extends ScanOutput<K, V, StreamScanCursor> {

    private final ValueStreamingChannel<V> channel;

    public ValueScanStreamingOutput(RedisCodec<K, V> codec, ValueStreamingChannel<V> channel) {
        super(codec, new StreamScanCursor());
        this.channel = channel;
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        channel.onValue(bytes == null ? null : codec.decodeValue(bytes));
        output.setCount(output.getCount() + 1);
    }

}
