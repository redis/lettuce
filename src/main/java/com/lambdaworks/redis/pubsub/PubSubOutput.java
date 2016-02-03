// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.CommandOutput;

/**
 * One element of the redis pub/sub stream. May be a message or notification of subscription details.
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
    }
}
