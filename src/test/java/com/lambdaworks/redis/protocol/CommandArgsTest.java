package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.lambdaworks.redis.codec.Utf8StringCodec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Mark Paluch
 */
public class CommandArgsTest {

    private Utf8StringCodec codec = new Utf8StringCodec();

    @Test
    public void getFirstIntegerShouldReturnNull() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec);

        assertThat(args.getFirstInteger()).isNull();
    }

    @Test
    public void getFirstIntegerShouldReturnFirstInteger() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).add(1L).add(127).add(128).add(129).add(0).add(-1);

        assertThat(args.getFirstInteger()).isEqualTo(1L);
    }

    @Test
    public void getFirstStringShouldReturnNull() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec);

        assertThat(args.getFirstString()).isNull();
    }

    @Test
    public void getFirstStringShouldReturnFirstString() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).add("one").add("two");

        assertThat(args.getFirstString()).isEqualTo("one");
    }

    @Test
    public void getFirstEncodedKeyShouldReturnNull() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec);

        assertThat(args.getFirstString()).isNull();
    }

    @Test
    public void getFirstEncodedKeyShouldReturnFirstKey() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).addKey("one").addKey("two");

        assertThat(args.getFirstEncodedKey()).isEqualTo(ByteBuffer.wrap("one".getBytes()));
    }

    @Test
    public void addByte() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).add("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    public void addByteUsingDirectByteCodec() throws Exception {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(CommandArgs.ExperimentalByteArrayCodec.INSTANCE).add("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }
}