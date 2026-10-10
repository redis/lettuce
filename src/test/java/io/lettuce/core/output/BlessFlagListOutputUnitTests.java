package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.BlessFlag;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link BlessFlagListOutput}.
 */
@Tag(UNIT_TEST)
class BlessFlagListOutputUnitTests {

    private final BlessFlagListOutput<String, String> sut = new BlessFlagListOutput<>(StringCodec.UTF8);

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void shouldDecodeKnownFlag() {

        sut.multi(1);
        sut.set(bytes("NO-EVICT"));

        assertThat(sut.get()).containsExactly(BlessFlag.NO_EVICT);
    }

    @Test
    void emptyArrayYieldsEmptyList() {

        sut.multi(0);

        assertThat(sut.get()).isEmpty();
    }

    @Test
    void shouldKeepUnknownTokensVerbatim() {

        sut.multi(2);
        sut.set(bytes("NO-EVICT"));
        sut.set(bytes("FUTURE-FLAG"));

        assertThat(sut.get()).containsExactly(BlessFlag.NO_EVICT, BlessFlag.of("FUTURE-FLAG"));
    }

    private static ByteBuffer bytes(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.US_ASCII));
    }

}
