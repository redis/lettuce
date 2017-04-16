/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

import com.lambdaworks.redis.codec.ByteArrayCodec;
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

        assertThat(CommandArgsAccessor.getFirstInteger(args)).isNull();
    }

    @Test
    public void getFirstIntegerShouldReturnFirstInteger() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).add(1L).add(127).add(128).add(129).add(0).add(-1);

        assertThat(CommandArgsAccessor.getFirstInteger(args)).isEqualTo(1L);
    }

    @Test
    public void getFirstStringShouldReturnNull() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec);

        assertThat(CommandArgsAccessor.getFirstString(args)).isNull();
    }

    @Test
    public void getFirstStringShouldReturnFirstString() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).add("one").add("two");

        assertThat(CommandArgsAccessor.getFirstString(args)).isEqualTo("one");
    }

    @Test
    public void getFirstEncodedKeyShouldReturnNull() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec);

        assertThat(CommandArgsAccessor.getFirstString(args)).isNull();
    }

    @Test
    public void getFirstEncodedKeyShouldReturnFirstKey() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).addKey("one").addKey("two");

        assertThat(CommandArgsAccessor.encodeFirstKey(args)).isEqualTo(ByteBuffer.wrap("one".getBytes()));
    }

    @Test
    public void addValues() throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(codec).addValues(Arrays.asList("1", "2"));

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$1\r\n" + "1\r\n" + "$1\r\n" + "2\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
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

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(CommandArgs.ExperimentalByteArrayCodec.INSTANCE)
                .add("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    public void addValueUsingDirectByteCodec() throws Exception {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(CommandArgs.ExperimentalByteArrayCodec.INSTANCE)
                .addValue("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    public void addKeyUsingDirectByteCodec() throws Exception {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(CommandArgs.ExperimentalByteArrayCodec.INSTANCE)
                .addValue("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    public void addByteUsingByteCodec() throws Exception {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                .add("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    public void addValueUsingByteCodec() throws Exception {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                .addValue("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    public void addKeyUsingByteCodec() throws Exception {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                .addValue("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }
}
