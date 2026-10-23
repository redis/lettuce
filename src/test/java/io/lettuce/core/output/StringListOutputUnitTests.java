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
    void wholeNilReplyKeepsNullSlotWithDefaultSubscriber() {

        // With the default ListSubscriber a missing key yields a single null
        // slot (collection behavior is unchanged from before the reactive fix).
        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.set((ByteBuffer) null);

        assertThat(output.get()).hasSize(1);
        assertThat(output.get().get(0)).isNull();
    }

    @Test
    void wholeNilReplyIsSkippedForReactiveSubscriber() {

        // A reactive (non ListSubscriber) subscriber must not receive null,
        // otherwise decoding aborts with "Data must not be null".
        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        AtomicInteger notifications = new AtomicInteger();
        output.setSubscriber(new Subscriber<String>() {

            @Override
            public void onNext(String t) {
                notifications.incrementAndGet();
            }

        });

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
    void nilElementInListIsSkippedForReactiveSubscriber() {

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

        assertThat(notifications.get()).isEqualTo(1);
        assertThat(output.get()).isEmpty();
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
