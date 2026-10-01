package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link BlessFlag}.
 */
@Tag(UNIT_TEST)
class BlessFlagUnitTests {

    @Test
    void noEvictUsesServerToken() {

        assertThat(BlessFlag.NO_EVICT.getToken()).isEqualTo("NO-EVICT");
        assertThat(BlessFlag.NO_EVICT.toString()).isEqualTo("NO-EVICT");
        assertThat(new String(BlessFlag.NO_EVICT.getBytes(), StandardCharsets.US_ASCII)).isEqualTo("NO-EVICT");
    }

    @Test
    void ofReturnsConstantForKnownToken() {
        assertThat(BlessFlag.of("NO-EVICT")).isSameAs(BlessFlag.NO_EVICT);
    }

    @Test
    void ofPassesUnknownTokenVerbatim() {

        BlessFlag flag = BlessFlag.of("FUTURE-FLAG");

        assertThat(flag.getToken()).isEqualTo("FUTURE-FLAG");
        assertThat(flag).isEqualTo(BlessFlag.of("FUTURE-FLAG")).isNotEqualTo(BlessFlag.NO_EVICT);
        assertThat(flag.hashCode()).isEqualTo(BlessFlag.of("FUTURE-FLAG").hashCode());
    }

    @Test
    void ofRejectsNullAndEmptyToken() {

        assertThatThrownBy(() -> BlessFlag.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BlessFlag.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldEncodeAsCommandArgument() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        args.add(BlessFlag.NO_EVICT);

        ByteBuf buf = Unpooled.directBuffer();
        args.encode(buf);

        assertThat(buf.toString(StandardCharsets.US_ASCII)).isEqualTo("$8\r\nNO-EVICT\r\n");
    }

}
