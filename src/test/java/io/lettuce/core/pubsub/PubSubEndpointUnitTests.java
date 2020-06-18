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
package io.lettuce.core.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.lettuce.core.ByteBufferCodec;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.test.resource.TestClientResources;

/**
 * Unit tests for {@link PubSubEndpoint}.
 *
 * @author Mark Paluch
 */
class PubSubEndpointUnitTests {

    @Test
    void shouldRetainUniqueChannelNames() {

        PubSubEndpoint<String, String> sut = new PubSubEndpoint<>(ClientOptions.create(), TestClientResources.get());

        sut.notifyMessage(createMessage("subscribe", "channel1", StringCodec.UTF8));
        sut.notifyMessage(createMessage("subscribe", "channel1", StringCodec.UTF8));
        sut.notifyMessage(createMessage("subscribe", "channel1", StringCodec.UTF8));
        sut.notifyMessage(createMessage("subscribe", "channel2", StringCodec.UTF8));

        assertThat(sut.getChannels()).hasSize(2).containsOnly("channel1", "channel2");
    }

    @Test
    void shouldRetainUniqueBinaryChannelNames() {

        PubSubEndpoint<byte[], byte[]> sut = new PubSubEndpoint<>(ClientOptions.create(), TestClientResources.get());

        sut.notifyMessage(createMessage("subscribe", "channel1", ByteArrayCodec.INSTANCE));
        sut.notifyMessage(createMessage("subscribe", "channel1", ByteArrayCodec.INSTANCE));
        sut.notifyMessage(createMessage("subscribe", "channel1", ByteArrayCodec.INSTANCE));
        sut.notifyMessage(createMessage("subscribe", "channel2", ByteArrayCodec.INSTANCE));

        assertThat(sut.getChannels()).hasSize(2);
    }

    @Test
    void shouldRetainUniqueByteBufferChannelNames() {

        PubSubEndpoint<ByteBuffer, ByteBuffer> sut = new PubSubEndpoint<>(ClientOptions.create(), TestClientResources.get());

        sut.notifyMessage(createMessage("subscribe", "channel1", new ByteBufferCodec()));
        sut.notifyMessage(createMessage("subscribe", "channel1", new ByteBufferCodec()));
        sut.notifyMessage(createMessage("subscribe", "channel1", new ByteBufferCodec()));
        sut.notifyMessage(createMessage("subscribe", "channel2", new ByteBufferCodec()));

        assertThat(sut.getChannels()).hasSize(2).containsOnly(ByteBuffer.wrap("channel1".getBytes()),
                ByteBuffer.wrap("channel2".getBytes()));
    }

    @Test
    void addsAndRemovesChannels() {

        PubSubEndpoint<byte[], byte[]> sut = new PubSubEndpoint<>(ClientOptions.create(), TestClientResources.get());

        sut.notifyMessage(createMessage("subscribe", "channel1", ByteArrayCodec.INSTANCE));
        sut.notifyMessage(createMessage("unsubscribe", "channel1", ByteArrayCodec.INSTANCE));

        assertThat(sut.getChannels()).isEmpty();
    }

    @Test
    void listenerNotificationShouldFailGracefully() {

        PubSubEndpoint<byte[], byte[]> sut = new PubSubEndpoint<>(ClientOptions.create(), TestClientResources.get());

        AtomicInteger notified = new AtomicInteger();

        sut.addListener(new RedisPubSubAdapter<byte[], byte[]>() {
            @Override
            public void message(byte[] channel, byte[] message) {

                notified.incrementAndGet();
                throw new UnsupportedOperationException();
            }
        });

        sut.addListener(new RedisPubSubAdapter<byte[], byte[]>() {
            @Override
            public void message(byte[] channel, byte[] message) {
                notified.incrementAndGet();
            }
        });

        sut.notifyMessage(createMessage("message", "channel1", ByteArrayCodec.INSTANCE));

        assertThat(notified).hasValue(1);
    }

    private static <K, V> PubSubOutput<K, V> createMessage(String action, String channel, RedisCodec<K, V> codec) {

        PubSubOutput<K, V> output = new PubSubOutput<>(codec);

        output.set(ByteBuffer.wrap(action.getBytes()));
        output.set(ByteBuffer.wrap(channel.getBytes()));

        return output;
    }
}
