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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link StreamReadOutput}.
 *
 * @author Mark Paluch
 */
class StreamReadOutputUnitTests {

    private StreamReadOutput<String, String> sut = new StreamReadOutput<>(StringCodec.UTF8);

    @Test
    void shouldDecodeSingleEntryMessage() {

        sut.multi(2);
        sut.set(ByteBuffer.wrap("stream-key".getBytes()));
        sut.complete(1);
        sut.multi(1);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("1234-12".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value".getBytes()));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        assertThat(sut.get()).hasSize(1);
        StreamMessage<String, String> streamMessage = sut.get().get(0);

        assertThat(streamMessage.getId()).isEqualTo("1234-12");
        assertThat(streamMessage.getStream()).isEqualTo("stream-key");
        assertThat(streamMessage.getBody()).hasSize(1).containsEntry("key", "value");
    }

    @Test
    void shouldDecodeMultiEntryMessage() {

        sut.multi(2);
        sut.set(ByteBuffer.wrap("stream-key".getBytes()));
        sut.complete(1);
        sut.multi(1);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("1234-12".getBytes()));
        sut.complete(3);
        sut.multi(4);
        sut.set(ByteBuffer.wrap("key1".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value1".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("key2".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value2".getBytes()));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        assertThat(sut.get()).hasSize(1);
        StreamMessage<String, String> streamMessage = sut.get().get(0);

        assertThat(streamMessage.getId()).isEqualTo("1234-12");
        assertThat(streamMessage.getStream()).isEqualTo("stream-key");
        assertThat(streamMessage.getBody()).hasSize(2).containsEntry("key1", "value1").containsEntry("key2", "value2");
    }

    @Test
    void shouldDecodeTwoSingleEntryMessage() {

        sut.multi(2);
        sut.set(ByteBuffer.wrap("stream-key".getBytes()));
        sut.complete(1);
        sut.multi(2);

        sut.multi(2);
        sut.set(ByteBuffer.wrap("1234-11".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("key1".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value1".getBytes()));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);

        sut.multi(2);
        sut.set(ByteBuffer.wrap("1234-22".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("key2".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value2".getBytes()));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);

        sut.complete(1);
        sut.complete(0);

        assertThat(sut.get()).hasSize(2);
        StreamMessage<String, String> streamMessage1 = sut.get().get(0);

        assertThat(streamMessage1.getId()).isEqualTo("1234-11");
        assertThat(streamMessage1.getStream()).isEqualTo("stream-key");
        assertThat(streamMessage1.getBody()).hasSize(1).containsEntry("key1", "value1");

        StreamMessage<String, String> streamMessage2 = sut.get().get(1);

        assertThat(streamMessage2.getId()).isEqualTo("1234-22");
        assertThat(streamMessage2.getStream()).isEqualTo("stream-key");
        assertThat(streamMessage2.getBody()).hasSize(1).containsEntry("key2", "value2");
    }

    @Test
    void shouldDecodeFromTwoStreams() {

        sut.multi(4);

        sut.set(ByteBuffer.wrap("stream1".getBytes()));
        sut.complete(1);
        sut.multi(1);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("1234-11".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("key1".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value1".getBytes()));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);

        sut.set(ByteBuffer.wrap("stream2".getBytes()));
        sut.complete(1);
        sut.multi(1);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("1234-22".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("key2".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value2".getBytes()));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);

        sut.complete(0);

        assertThat(sut.get()).hasSize(2);
        StreamMessage<String, String> streamMessage1 = sut.get().get(0);

        assertThat(streamMessage1.getId()).isEqualTo("1234-11");
        assertThat(streamMessage1.getStream()).isEqualTo("stream1");
        assertThat(streamMessage1.getBody()).hasSize(1).containsEntry("key1", "value1");

        StreamMessage<String, String> streamMessage2 = sut.get().get(1);

        assertThat(streamMessage2.getId()).isEqualTo("1234-22");
        assertThat(streamMessage2.getStream()).isEqualTo("stream2");
        assertThat(streamMessage2.getBody()).hasSize(1).containsEntry("key2", "value2");
    }
}
