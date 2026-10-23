package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
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

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.set((ByteBuffer) null);

        assertThat(output.get()).containsExactly((String) null);
    }

    @Test
    void nilElementInListKeepsNullSlotWithDefaultSubscriber() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.multi(3);
        output.set(bytes("one"));
        output.set((ByteBuffer) null);
        output.set(bytes("three"));

        assertThat(output.get()).containsExactly("one", null, "three");
    }

    @Test
    void wholeNilReplyIsKeptInOutputWithoutNotifyingReactiveSubscriber() {

        // A reactive subscriber rejects null with "Data must not be null"; the
        // slot must still be reserved in the output so positional results stay
        // intact, but the subscriber must not be notified.
        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        AtomicInteger notifications = new AtomicInteger();
        output.setSubscriber(collectingRejectingSubscriber(notifications));

        output.set((ByteBuffer) null);

        assertThat(notifications.get()).isZero();
        assertThat(output.get()).containsExactly((String) null);
    }

    @Test
    void nilElementInListIsKeptInOutputWithoutNotifyingReactiveSubscriber() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        AtomicInteger notifications = new AtomicInteger();
        output.setSubscriber(collectingRejectingSubscriber(notifications));

        output.multi(3);
        output.set(bytes("one"));
        output.set((ByteBuffer) null);
        output.set(bytes("three"));

        assertThat(notifications.get()).isEqualTo(2);
        assertThat(output.get()).containsExactly("one", null, "three");
    }

    @Test
    void shouldDecodeAndCollectValues() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.multi(2);
        output.set(bytes("one"));
        output.set(bytes("two"));

        assertThat(output.get()).containsExactly("one", "two");
    }

    private static ByteBuffer bytes(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Mimics a composite/reactive subscriber: it collects non-null values into the output (as the composite
     * {@link ListSubscriber} delegate would) but rejects null the same way {@code RedisPublisher.RedisSubscription} does.
     */
    private static Subscriber<String> collectingRejectingSubscriber(AtomicInteger notifications) {

        return new Subscriber<String>() {

            @Override
            public void onNext(String t) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void onNext(Collection<String> outputTarget, String t) {

                if (t == null) {
                    throw new IllegalArgumentException("Data must not be null");
                }

                outputTarget.add(t);
                notifications.incrementAndGet();
            }

        };
    }

}
