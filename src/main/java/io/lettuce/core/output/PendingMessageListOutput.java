/*
 * Copyright 2020 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.stream.PendingMessage;

/**
 * Decodes a list of {@link PendingMessage}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
public class PendingMessageListOutput<K, V> extends CommandOutput<K, V, List<PendingMessage>>
        implements StreamingOutput<PendingMessage> {

    private boolean initialized;

    private Subscriber<PendingMessage> subscriber;

    private String messageId;

    private String consumer;

    private Long msSinceLastDelivery;

    public PendingMessageListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (messageId == null) {
            messageId = decodeAscii(bytes);
            return;
        }

        if (consumer == null) {
            consumer = StringCodec.UTF8.decodeKey(bytes);
            return;
        }

        set(Long.parseLong(decodeAscii(bytes)));
    }

    @Override
    public void set(long integer) {

        if (msSinceLastDelivery == null) {
            msSinceLastDelivery = integer;
            return;
        }

        PendingMessage message = new PendingMessage(messageId, consumer, msSinceLastDelivery, integer);
        messageId = null;
        consumer = null;
        msSinceLastDelivery = null;
        subscriber.onNext(output, message);
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<PendingMessage> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<PendingMessage> getSubscriber() {
        return subscriber;
    }

}
