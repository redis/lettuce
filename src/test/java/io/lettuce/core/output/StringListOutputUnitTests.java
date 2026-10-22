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
    void nilShouldProduceEmptyList() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        output.multi(1);
        output.set((ByteBuffer) null);

        assertThat(output.get()).isEmpty();
    }

    @Test
    void nilShouldNotNotifySubscriber() {

        StringListOutput<String, String> output = new StringListOutput<>(StringCodec.UTF8);
        AtomicInteger notifications = new AtomicInteger();
        output.setSubscriber(new Subscriber<String>() {

            @Override
            public void onNext(String t) {
                notifications.incrementAndGet();
            }

        });

        output.multi(1);
        output.set((ByteBuffer) null);

        assertThat(notifications.get()).isZero();
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
