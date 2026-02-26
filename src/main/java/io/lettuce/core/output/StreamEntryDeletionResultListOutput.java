/*
 * Copyright 2025-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import java.util.Collections;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.stream.StreamEntryDeletionResult;

/**
 * {@link List} of {@link StreamEntryDeletionResult} output for XDELEX and XACKDEL commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class StreamEntryDeletionResultListOutput<K, V> extends CommandOutput<K, V, List<StreamEntryDeletionResult>>
        implements StreamingOutput<StreamEntryDeletionResult> {

    private boolean initialized;

    private Subscriber<StreamEntryDeletionResult> subscriber;

    public StreamEntryDeletionResultListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        if (!initialized) {
            multi(1);
        }

        StreamEntryDeletionResult result = StreamEntryDeletionResult.fromCode((int) integer);
        subscriber.onNext(output, result);
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<StreamEntryDeletionResult> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<StreamEntryDeletionResult> getSubscriber() {
        return subscriber;
    }

}
