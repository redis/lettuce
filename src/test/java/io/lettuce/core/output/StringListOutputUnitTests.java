package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StreamingOutput.Subscriber;

/**
 * Unit tests for {@link StringListOutput}.
 */
@Tag(UNIT_TEST)
class StringListOutputUnitTests {

    @Test
    void wholeNilReplyProducesEmptyList() {

        // A missing key yields a nil reply before the list is initialized.
        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.set((ByteBuffer) null);

        assertThat(output.get()).isEmpty();
    }

    @Test
    void wholeNilReplyDoesNotNotifySubscriber() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        AtomicInteger notifications = new AtomicInteger();
        output.setSubscriber(new Subscriber<String>() {

            @Override
            public void onNext(String t) {
                notifications.incrementAndGet();
            }

        });

        // Not yet initialized -> nil reply for the whole list.
        output.set((ByteBuffer) null);

        assertThat(notifications.get()).isZero();
        assertThat(output.get()).isEmpty();
    }

    @Test
    void nilElementInListKeepsNullSlot() {

        // A nil element inside an already-initialized list must keep its null
        // slot so that length and element positions are preserved.
        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.multi(3);
        output.set(ByteBuffer.wrap("one".getBytes(StandardCharsets.UTF_8)));
        output.set((ByteBuffer) null);
        output.set(ByteBuffer.wrap("three".getBytes(StandardCharsets.UTF_8)));

        assertThat(output.get()).containsExactly("one", null, "three");
    }

    @Test
    void nilElementInListNotifiesSubscriber() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        AtomicInteger notifications = new AtomicInteger();
        output.setSubscriber(new Subscriber<String>() {

            @Override
            public void onNext(String t) {
                notifications.incrementAndGet();
            }

        });

        output.multi(2);
        output.set(ByteBuffer.wrap("one".getBytes(StandardCharsets.UTF_8)));
        output.set((ByteBuffer) null);

        assertThat(notifications.get()).isEqualTo(2);
    }

    @Test
    void shouldDecodeAndCollectValues() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.multi(2);
        output.set(ByteBuffer.wrap("one".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("two".getBytes(StandardCharsets.UTF_8)));

        assertThat(output.get()).containsExactly("one", "two");
    }

}
