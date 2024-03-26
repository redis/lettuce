package io.lettuce.core;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;

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

    RedisCommandBuilder<String, String> sut = new RedisCommandBuilder<>(StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructXreadgroup() {

        Command<String, String, ?> command = sut.xreadgroup(Consumer.from("a", "b"), new XReadArgs(),
                XReadArgs.StreamOffset.latest("stream"));

        assertThat(Unpooled.wrappedBuffer(command.getArgs().getFirstEncodedKey()).toString(StandardCharsets.UTF_8))
                .isEqualTo("stream");
    }

}
