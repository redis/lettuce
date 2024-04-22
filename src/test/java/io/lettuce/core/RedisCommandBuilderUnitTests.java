package io.lettuce.core;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link RedisCommandBuilder}.
 *
 * @author Mark Paluch
 */
class RedisCommandBuilderUnitTests {
    public static final String MY_KEY = "hKey";
    public static final String MY_FIELD = "hField";

    RedisCommandBuilder<String, String> sut = new RedisCommandBuilder<>(StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructXreadgroup() {

        Command<String, String, ?> command = sut.xreadgroup(Consumer.from("a", "b"), new XReadArgs(),
                XReadArgs.StreamOffset.latest("stream"));

        assertThat(Unpooled.wrappedBuffer(command.getArgs().getFirstEncodedKey()).toString(StandardCharsets.UTF_8))
                .isEqualTo("stream");
    }

    @Test
    void shouldCorrectlyConstructHexpire() {

        Command<String, String, ?> command = sut.hexpire(MY_KEY, 1, ExpireArgs.Builder.nx(), Collections.singletonList(MY_FIELD));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$7\r\n" + "HEXPIRE\r\n" + "$4\r\n" + "hKey\r\n"
                + "$1\r\n" + "1\r\n" + "$2\r\n" + "NX\r\n" + "$1\r\n" + "1\r\n" + "$6\r\n" + "hField\r\n");
    }
}
