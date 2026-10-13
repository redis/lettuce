/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.BlessFlag;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of {@link BlessFlag} output, decoding the array of flag tokens returned by {@literal BLESS GET}. An empty array
 * yields an empty list.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.9
 */
public class BlessFlagListOutput<K, V> extends CommandOutput<K, V, List<BlessFlag>> implements StreamingOutput<BlessFlag> {

    private boolean initialized;

    private Subscriber<BlessFlag> subscriber;

    /**
     * Create a new {@link BlessFlagListOutput}.
     *
     * @param codec the codec used to decode the reply, must not be {@code null}.
     * @since 7.9
     */
    public BlessFlagListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            return;
        }

        if (!initialized) {
            multi(1);
        }

        subscriber.onNext(output, BlessFlag.of(decodeString(bytes)));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<BlessFlag> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<BlessFlag> getSubscriber() {
        return subscriber;
    }

}
