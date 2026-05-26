/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

import io.lettuce.core.bf.BfInfoValue;
import io.lettuce.core.bf.BfScanDumpValue;
import io.lettuce.core.bf.arguments.BfInsertArgs;
import io.lettuce.core.bf.arguments.BfReserveArgs;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisBloomFilterCommandBuilder}.
 *
 * @author Yordan Tsintsov
 */
@Tag(UNIT_TEST)
class RedisBloomFilterCommandBuilderUnitTests {

    private static final String MY_KEY = "books:name";

    private static final String MY_VALUE = "Dune";

    private static final String MY_VALUE_2 = "Dune Messiah";

    private final RedisBloomFilterCommandBuilder<String, String> builder = new RedisBloomFilterCommandBuilder<>(
            StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructBfAddCommand() {
        Command<String, String, Boolean> command = builder.bfAdd(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$6\r\nBF.ADD\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfCardCommand() {
        Command<String, String, Long> command = builder.bfCard(MY_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$7\r\nBF.CARD\r\n" + "$10\r\n" + MY_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfExistsCommand() {
        Command<String, String, Boolean> command = builder.bfExists(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$9\r\nBF.EXISTS\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfInfoCommand() {
        Command<String, String, BfInfoValue> command = builder.bfInfo(MY_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$7\r\nBF.INFO\r\n" + "$10\r\n" + MY_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfInsertCommand() {
        Command<String, String, List<Boolean>> command = builder.bfInsert(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$9\r\nBF.INSERT\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfInsertCommandWithVarargs() {
        Command<String, String, List<Boolean>> command = builder.bfInsert(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$9\r\nBF.INSERT\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfInsertCommandWithArgs() {
        BfInsertArgs insertArgs = BfInsertArgs.Builder.capacity(100).error(0.01);
        Command<String, String, List<Boolean>> command = builder.bfInsert(MY_KEY, insertArgs, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*8\r\n" + "$9\r\nBF.INSERT\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$8\r\nCAPACITY\r\n" + "$3\r\n100\r\n"
                        + "$5\r\nERROR\r\n" + "$4\r\n0.01\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfLoadChunkCommand() {
        byte[] valueBytes = MY_VALUE.getBytes();
        Command<String, String, String> command = builder.bfLoadChunk(MY_KEY, 0, valueBytes);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$12\r\nBF.LOADCHUNK\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$1\r\n0\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfMAddCommand() {
        Command<String, String, List<Boolean>> command = builder.bfMAdd(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$7\r\nBF.MADD\r\n" + "$10\r\n" + MY_KEY + "\r\n"
                + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfMExistsCommand() {
        Command<String, String, List<Boolean>> command = builder.bfMExists(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$10\r\nBF.MEXISTS\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfReserveCommand() {
        Command<String, String, String> command = builder.bfReserve(MY_KEY, 0.01, 100);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$10\r\nBF.RESERVE\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n0.01\r\n" + "$3\r\n100\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfReserveCommandWithArgs() {
        BfReserveArgs reserveArgs = BfReserveArgs.Builder.expansion(2).nonScaling();
        Command<String, String, String> command = builder.bfReserve(MY_KEY, 0.01, 100, reserveArgs);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$10\r\nBF.RESERVE\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n0.01\r\n" + "$3\r\n100\r\n"
                        + "$9\r\nEXPANSION\r\n" + "$1\r\n2\r\n" + "$10\r\nNONSCALING\r\n");
    }

    @Test
    void shouldCorrectlyConstructBfScanDumpCommand() {
        Command<String, String, BfScanDumpValue> command = builder.bfScanDump(MY_KEY, 0);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$11\r\nBF.SCANDUMP\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$1\r\n0\r\n");
    }

}
