// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;

/**
 * One element of the redis pub/sub stream. May be a message or notification
 * of subscription details.
 *
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class PubSubOutput<V> extends CommandOutput<V> {
    enum Type { message, pmessage, psubscribe, punsubscribe, subscribe, unsubscribe }

    private Type type;
    private String channel;
    private String pattern;
    private long count;
    private V message;

    public PubSubOutput(RedisCodec<?, V> codec) {
        super(codec);
    }

    public Type type() {
        return type;
    }

    public String channel() {
        return channel;
    }

    public String pattern() {
        return pattern;
    }

    public long count() {
        return count;
    }

    @Override
    public V get() {
        return message;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ByteBuffer bytes) {
        if (type == null) {
            type = Type.valueOf(decodeAscii(bytes));
            return;
        }

        switch (type) {
            case pmessage:
                if (pattern == null) {
                    pattern = decodeAscii(bytes);
                    break;
                }
            case message:
                if (channel == null) {
                    channel = decodeAscii(bytes);
                    break;
                }
                message = (V) codec.decodeValue(bytes);
                break;
            case psubscribe:
            case punsubscribe:
                pattern = decodeAscii(bytes);
                break;
            case subscribe:
            case unsubscribe:
                channel = decodeAscii(bytes);
                break;
        }
    }

    @Override
    public void set(long integer) {
        count = integer;
    }
}
