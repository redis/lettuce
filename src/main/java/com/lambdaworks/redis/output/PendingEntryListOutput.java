/*
 * Copyright 2018 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import com.lambdaworks.redis.PendingEntry;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link List} of {@link PendingEntry} output to be used with XPENDING with key and group.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.5
 */
public class PendingEntryListOutput<K, V> extends CommandOutput<K, V, List<PendingEntry>> implements
        StreamingOutput<PendingEntry> {

    private boolean initialized;
    private Subscriber<PendingEntry> subscriber;

    private String messageId;
    private String consumer;
    private Long millisSinceDelivery;

    public PendingEntryListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (messageId == null) {
            messageId = StringCodec.UTF8.decodeKey(bytes);
            return;
        }

        if (consumer == null) {
            consumer = StringCodec.UTF8.decodeKey(bytes);
            return;
        }

    }

    @Override
    public void set(long integer) {

        if (millisSinceDelivery == null) {
            millisSinceDelivery = integer;
            return;
        }

        subscriber.onNext(output, new PendingEntry(messageId, consumer, millisSinceDelivery, integer));
        messageId = null;
        consumer = null;
        millisSinceDelivery = null;
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<PendingEntry> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<PendingEntry> getSubscriber() {
        return subscriber;
    }
}
