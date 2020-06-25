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
package io.lettuce.core.pubsub;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.CommandOutput;

/**
 * One element of the Redis pub/sub stream. May be a message or notification of subscription details.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Result type.
 * @author Will Glozer
 */
public class PubSubOutput<K, V, T> extends CommandOutput<K, V, T> {

    public enum Type {
        message, pmessage, psubscribe, punsubscribe, subscribe, unsubscribe
    }

    private Type type;

    private K channel;

    private K pattern;

    private long count;

    private boolean completed;

    public PubSubOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    public Type type() {
        return type;
    }

    public K channel() {
        return channel;
    }

    public K pattern() {
        return pattern;
    }

    public long count() {
        return count;
    }

    @Override
    @SuppressWarnings({ "fallthrough", "unchecked" })
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            return;
        }

        if (type == null) {
            type = Type.valueOf(decodeAscii(bytes));
            return;
        }

        handleOutput(bytes);
    }

    @SuppressWarnings("unchecked")
    private void handleOutput(ByteBuffer bytes) {
        switch (type) {
            case pmessage:
                if (pattern == null) {
                    pattern = codec.decodeKey(bytes);
                    break;
                }
            case message:
                if (channel == null) {
                    channel = codec.decodeKey(bytes);
                    break;
                }
                output = (T) codec.decodeValue(bytes);
                completed = true;
                break;
            case psubscribe:
            case punsubscribe:
                pattern = codec.decodeKey(bytes);
                break;
            case subscribe:
            case unsubscribe:
                channel = codec.decodeKey(bytes);
                break;
            default:
                throw new UnsupportedOperationException("Operation " + type + " not supported");
        }
    }

    @Override
    public void set(long integer) {
        count = integer;
        // count comes last in (p)(un)subscribe ack.
        completed = true;
    }

    boolean isCompleted() {
        return completed;
    }

}
