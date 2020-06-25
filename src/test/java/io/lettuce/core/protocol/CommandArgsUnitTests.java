/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Mark Paluch
 */
class CommandArgsUnitTests {

    @Test
    void getFirstIntegerShouldReturnNull() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add("foo");

        assertThat(CommandArgsAccessor.getFirstInteger(args)).isNull();
    }

    @Test
    void getFirstIntegerShouldReturnFirstInteger() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(1L).add(127).add(128).add(129).add(0)
                .add(-1);

        assertThat(CommandArgsAccessor.getFirstInteger(args)).isEqualTo(1L);
    }

    @Test
    void getFirstIntegerShouldReturnFirstNegativeInteger() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(-1L).add(-127).add(-128).add(-129);

        assertThat(CommandArgsAccessor.getFirstInteger(args)).isEqualTo(-1L);
    }

    @Test
    void getFirstStringShouldReturnNull() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(1);

        assertThat(CommandArgsAccessor.getFirstString(args)).isNull();
    }

    @Test
    void getFirstStringShouldReturnFirstString() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add("one").add("two");

        assertThat(CommandArgsAccessor.getFirstString(args)).isEqualTo("one");
    }

    @Test
    void getFirstCharArrayShouldReturnCharArray() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(1L).add("two".toCharArray());

        assertThat(CommandArgsAccessor.getFirstCharArray(args)).isEqualTo("two".toCharArray());
    }

    @Test
    void getFirstCharArrayShouldReturnNull() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(1L);

        assertThat(CommandArgsAccessor.getFirstCharArray(args)).isNull();
    }

    @Test
    void getFirstEncodedKeyShouldReturnNull() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(1L);

        assertThat(CommandArgsAccessor.getFirstString(args)).isNull();
    }

    @Test
    void getFirstEncodedKeyShouldReturnFirstKey() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).addKey("one").addKey("two");

        assertThat(CommandArgsAccessor.encodeFirstKey(args)).isEqualTo(ByteBuffer.wrap("one".getBytes()));
    }

    @Test
    void addValues() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).addValues(Arrays.asList("1", "2"));

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$1\r\n" + "1\r\n" + "$1\r\n" + "2\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    void addByte() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    void addByteUsingByteCodec() {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE).add("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    void addValueUsingByteCodec() {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE).addValue("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

    @Test
    void addKeyUsingByteCodec() {

        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE).addValue("one".getBytes());

        ByteBuf buffer = Unpooled.buffer();
        args.encode(buffer);

        ByteBuf expected = Unpooled.buffer();
        expected.writeBytes(("$3\r\n" + "one\r\n").getBytes());

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(expected.toString(LettuceCharsets.ASCII));
    }

}
