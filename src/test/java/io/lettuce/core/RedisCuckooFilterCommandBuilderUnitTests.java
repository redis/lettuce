/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.cf.arguments.CfInsertArgs;
import io.lettuce.core.cf.arguments.CfReserveArgs;
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
 * Unit tests for {@link RedisCuckooFilterCommandBuilder}.
 *
 * @author Gyumin Hwang
 */
@Tag(UNIT_TEST)
class RedisCuckooFilterCommandBuilderUnitTests {

    private static final String MY_KEY = "books:name";

    private static final String MY_VALUE = "Dune";

    private static final String MY_VALUE_2 = "Dune Messiah";

    private final RedisCuckooFilterCommandBuilder<String, String> builder = new RedisCuckooFilterCommandBuilder<>(
            StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructCfReserveCommand() {
        Command<String, String, String> command = builder.cfReserve(MY_KEY, 100);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$10\r\nCF.RESERVE\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$3\r\n100\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfReserveCommandWithArgs() {
        CfReserveArgs reserveArgs = CfReserveArgs.Builder.bucketSize(4).maxIterations(10).expansion(2);
        Command<String, String, String> command = builder.cfReserve(MY_KEY, 100, reserveArgs);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*9\r\n" + "$10\r\nCF.RESERVE\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$3\r\n100\r\n" + "$10\r\nBUCKETSIZE\r\n"
                        + "$1\r\n4\r\n" + "$13\r\nMAXITERATIONS\r\n" + "$2\r\n10\r\n" + "$9\r\nEXPANSION\r\n" + "$1\r\n2\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfAddCommand() {
        Command<String, String, Boolean> command = builder.cfAdd(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$6\r\nCF.ADD\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfAddNxCommand() {
        Command<String, String, Boolean> command = builder.cfAddNx(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$8\r\nCF.ADDNX\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertSingleValueCommand() {
        Command<String, String, List<Boolean>> command = builder.cfInsert(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$9\r\nCF.INSERT\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertCommand() {
        Command<String, String, List<Boolean>> command = builder.cfInsert(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$9\r\nCF.INSERT\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertCommandWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100).noCreate();
        Command<String, String, List<Boolean>> command = builder.cfInsert(MY_KEY, insertArgs, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$9\r\nCF.INSERT\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$8\r\nCAPACITY\r\n"
                        + "$3\r\n100\r\n" + "$8\r\nNOCREATE\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertCommandWithVarargs() {
        Command<String, String, List<Boolean>> command = builder.cfInsert(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$9\r\nCF.INSERT\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertCommandVarargWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);
        Command<String, String, List<Boolean>> command = builder.cfInsert(MY_KEY, insertArgs, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$9\r\nCF.INSERT\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$8\r\nCAPACITY\r\n"
                        + "$3\r\n100\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertNxSingleValueCommand() {
        Command<String, String, List<Long>> command = builder.cfInsertNx(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$11\r\nCF.INSERTNX\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertNxCommand() {
        Command<String, String, List<Long>> command = builder.cfInsertNx(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$11\r\nCF.INSERTNX\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertNxCommandWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(200).noCreate();
        Command<String, String, List<Long>> command = builder.cfInsertNx(MY_KEY, insertArgs, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$11\r\nCF.INSERTNX\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$8\r\nCAPACITY\r\n"
                        + "$3\r\n200\r\n" + "$8\r\nNOCREATE\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInsertNxCommandWithVarargs() {
        Command<String, String, List<Long>> command = builder.cfInsertNx(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$11\r\nCF.INSERTNX\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$5\r\nITEMS\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfExistsCommand() {
        Command<String, String, Boolean> command = builder.cfExists(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$9\r\nCF.EXISTS\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfMExistsCommand() {
        Command<String, String, List<Boolean>> command = builder.cfMExists(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$10\r\nCF.MEXISTS\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfDelCommand() {
        Command<String, String, Boolean> command = builder.cfDel(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$6\r\nCF.DEL\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfCountCommand() {
        Command<String, String, Long> command = builder.cfCount(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$8\r\nCF.COUNT\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfScanDumpCommand() {
        Command<String, String, ?> command = builder.cfScanDump(MY_KEY, 0);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$11\r\nCF.SCANDUMP\r\n" + "$10\r\n" + MY_KEY + "\r\n" + "$1\r\n0\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfLoadChunkCommand() {
        byte[] valueBytes = MY_VALUE.getBytes();
        Command<String, String, String> command = builder.cfLoadChunk(MY_KEY, 0, valueBytes);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$12\r\nCF.LOADCHUNK\r\n" + "$10\r\n" + MY_KEY
                + "\r\n" + "$1\r\n0\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCfInfoCommand() {
        Command<String, String, ?> command = builder.cfInfo(MY_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$7\r\nCF.INFO\r\n" + "$10\r\n" + MY_KEY + "\r\n");
    }

}
