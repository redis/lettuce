package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link StreamReadOutput}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
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

    @Test
    void shouldDecodeClaimedEntryWithMetadata() {

        // Stream and single claimed entry
        sut.multi(2);
        sut.set(ByteBuffer.wrap("stream-key".getBytes()));
        sut.complete(1);
        sut.multi(1);
        sut.multi(4);
        sut.set(ByteBuffer.wrap("1234-12".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value".getBytes()));
        sut.complete(4);
        // extras for claimed pending entry
        sut.set(5000);
        sut.set(2);
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        assertThat(sut.get()).hasSize(1);
        StreamMessage<String, String> streamMessage = sut.get().get(0);
        assertThat(streamMessage.getMillisElapsedFromDelivery()).isEqualTo(5000);
        assertThat(streamMessage.getDeliveredCount()).isEqualTo(2);
        assertThat(streamMessage.getBody()).hasSize(1).containsEntry("key", "value");
    }

    @Test
    void shouldDecodeFreshEntryWithZeroRedeliveriesAsNotClaimed() {

        // Stream and single entry that carries extras with redeliveryCount=0
        sut.multi(2);
        sut.set(ByteBuffer.wrap("stream-key".getBytes()));
        sut.complete(1);
        sut.multi(1);
        sut.multi(4);
        sut.set(ByteBuffer.wrap("1234-12".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("value".getBytes()));
        sut.complete(4);
        // extras indicate not previously delivered (redeliveryCount=0)
        sut.set(1000); // ms since last delivery
        sut.set(0); // redeliveryCount
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        assertThat(sut.get()).hasSize(1);
        StreamMessage<String, String> streamMessage = sut.get().get(0);
        assertThat(streamMessage.isClaimed()).isFalse();
        assertThat(streamMessage.getMillisElapsedFromDelivery()).isEqualTo(1000);
        assertThat(streamMessage.getDeliveredCount()).isEqualTo(0);
    }

    @Test
    void shouldDecodeMixedBatchClaimedFirstThenFresh() {

        // One stream with three entries: two claimed (redelivery >= 1) then one fresh (redelivery == 0)
        sut.multi(2);
        sut.set(ByteBuffer.wrap("stream-key".getBytes()));
        sut.complete(1);
        sut.multi(3);

        // Entry #1 (claimed)
        sut.multi(4);
        sut.set(ByteBuffer.wrap("1-0".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("f1".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("v1".getBytes()));
        sut.complete(4);
        sut.set(1500); // msSinceLastDelivery
        sut.set(2); // redeliveryCount
        sut.complete(3);
        sut.complete(2);

        // Entry #2 (claimed)
        sut.multi(4);
        sut.set(ByteBuffer.wrap("2-0".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("f2".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("v2".getBytes()));
        sut.complete(4);
        sut.set(1200);
        sut.set(1);
        sut.complete(3);
        sut.complete(2);

        // Entry #3 (fresh, still carries metadata with redeliveryCount=0)
        sut.multi(4);
        sut.set(ByteBuffer.wrap("3-0".getBytes()));
        sut.complete(3);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("f3".getBytes()));
        sut.complete(4);
        sut.set(ByteBuffer.wrap("v3".getBytes()));
        sut.complete(4);
        sut.set(10);
        sut.set(0);
        sut.complete(3);

        sut.complete(2);

        sut.complete(1);
        sut.complete(0);

        assertThat(sut.get()).hasSize(3);
        StreamMessage<String, String> m1 = sut.get().get(0);
        StreamMessage<String, String> m2 = sut.get().get(1);
        StreamMessage<String, String> m3 = sut.get().get(2);

        assertThat(m1.isClaimed()).isTrue();
        assertThat(m2.isClaimed()).isTrue();
        assertThat(m3.isClaimed()).isFalse();

        assertThat(m1.getDeliveredCount()).isEqualTo(2);
        assertThat(m2.getDeliveredCount()).isEqualTo(1);
        assertThat(m3.getDeliveredCount()).isEqualTo(0);
    }

}
