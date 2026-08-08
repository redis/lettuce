/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link RedisTDigestCommandBuilder}.
 *
 * @author Yordan Tsintsov
 */
@Tag(UNIT_TEST)
class RedisTDigestCommandBuilderUnitTests {

    private static final String MY_KEY = "sketch";

    private final RedisTDigestCommandBuilder<String, String> builder = new RedisTDigestCommandBuilder<>(StringCodec.UTF8);

    private static String encode(Command<?, ?, ?> command) {
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);
        return buff.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldCorrectlyConstructTdigestAddCommand() {
        Command<String, String, String> command = builder.tdigestAdd(MY_KEY, "1.0");

        assertThat(encode(command)).isEqualTo("*3\r\n" + "$11\r\nTDIGEST.ADD\r\n" + "$6\r\nsketch\r\n" + "$3\r\n1.0\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestAddMultipleCommand() {
        Command<String, String, String> command = builder.tdigestAdd(MY_KEY, "1.0", "2.0");

        assertThat(encode(command))
                .isEqualTo("*4\r\n" + "$11\r\nTDIGEST.ADD\r\n" + "$6\r\nsketch\r\n" + "$3\r\n1.0\r\n" + "$3\r\n2.0\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestCreateCommand() {
        Command<String, String, String> command = builder.tdigestCreate(MY_KEY);

        assertThat(encode(command)).isEqualTo("*2\r\n" + "$14\r\nTDIGEST.CREATE\r\n" + "$6\r\nsketch\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestCreateWithCompressionCommand() {
        Command<String, String, String> command = builder.tdigestCreate(MY_KEY, 100);

        assertThat(encode(command)).isEqualTo(
                "*4\r\n" + "$14\r\nTDIGEST.CREATE\r\n" + "$6\r\nsketch\r\n" + "$11\r\nCOMPRESSION\r\n" + "$3\r\n100\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestByRankCommand() {
        Command<String, String, ?> command = builder.tdigestByRank(MY_KEY, 5);

        assertThat(encode(command)).isEqualTo("*3\r\n" + "$14\r\nTDIGEST.BYRANK\r\n" + "$6\r\nsketch\r\n" + "$1\r\n5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestByRevRankMultipleCommand() {
        Command<String, String, ?> command = builder.tdigestByRevRank(MY_KEY, 0, 1);

        assertThat(encode(command))
                .isEqualTo("*4\r\n" + "$17\r\nTDIGEST.BYREVRANK\r\n" + "$6\r\nsketch\r\n" + "$1\r\n0\r\n" + "$1\r\n1\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestCdfCommand() {
        Command<String, String, ?> command = builder.tdigestCDF(MY_KEY, "10");

        assertThat(encode(command)).isEqualTo("*3\r\n" + "$11\r\nTDIGEST.CDF\r\n" + "$6\r\nsketch\r\n" + "$2\r\n10\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestInfoCommand() {
        Command<String, String, ?> command = builder.tdigestInfo(MY_KEY);

        assertThat(encode(command)).isEqualTo("*2\r\n" + "$12\r\nTDIGEST.INFO\r\n" + "$6\r\nsketch\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestMaxCommand() {
        Command<String, String, Double> command = builder.tdigestMax(MY_KEY);

        assertThat(encode(command)).isEqualTo("*2\r\n" + "$11\r\nTDIGEST.MAX\r\n" + "$6\r\nsketch\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestMinCommand() {
        Command<String, String, Double> command = builder.tdigestMin(MY_KEY);

        assertThat(encode(command)).isEqualTo("*2\r\n" + "$11\r\nTDIGEST.MIN\r\n" + "$6\r\nsketch\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestMergeSingleSourceCommand() {
        Command<String, String, String> command = builder.tdigestMerge("dest", "src");

        assertThat(encode(command))
                .isEqualTo("*4\r\n" + "$13\r\nTDIGEST.MERGE\r\n" + "$4\r\ndest\r\n" + "$1\r\n1\r\n" + "$3\r\nsrc\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestMergeWithCompressionCommand() {
        Command<String, String, String> command = builder.tdigestMerge("dest", "src", 100);

        assertThat(encode(command)).isEqualTo("*6\r\n" + "$13\r\nTDIGEST.MERGE\r\n" + "$4\r\ndest\r\n" + "$1\r\n1\r\n"
                + "$3\r\nsrc\r\n" + "$11\r\nCOMPRESSION\r\n" + "$3\r\n100\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestMergeWithCompressionAndOverrideCommand() {
        Command<String, String, String> command = builder.tdigestMerge("dest", 100, true, "s1", "s2");

        assertThat(encode(command)).isEqualTo("*8\r\n" + "$13\r\nTDIGEST.MERGE\r\n" + "$4\r\ndest\r\n" + "$1\r\n2\r\n"
                + "$2\r\ns1\r\n" + "$2\r\ns2\r\n" + "$11\r\nCOMPRESSION\r\n" + "$3\r\n100\r\n" + "$8\r\nOVERRIDE\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestQuantileCommand() {
        Command<String, String, ?> command = builder.tdigestQuantile(MY_KEY, 0.5);

        assertThat(encode(command)).isEqualTo("*3\r\n" + "$16\r\nTDIGEST.QUANTILE\r\n" + "$6\r\nsketch\r\n" + "$3\r\n0.5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestRankCommand() {
        Command<String, String, ?> command = builder.tdigestRank(MY_KEY, "10");

        assertThat(encode(command)).isEqualTo("*3\r\n" + "$12\r\nTDIGEST.RANK\r\n" + "$6\r\nsketch\r\n" + "$2\r\n10\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestResetCommand() {
        Command<String, String, String> command = builder.tdigestReset(MY_KEY);

        assertThat(encode(command)).isEqualTo("*2\r\n" + "$13\r\nTDIGEST.RESET\r\n" + "$6\r\nsketch\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestRevRankCommand() {
        Command<String, String, ?> command = builder.tdigestRevRank(MY_KEY, "10");

        assertThat(encode(command)).isEqualTo("*3\r\n" + "$15\r\nTDIGEST.REVRANK\r\n" + "$6\r\nsketch\r\n" + "$2\r\n10\r\n");
    }

    @Test
    void shouldCorrectlyConstructTdigestTrimmedMeanCommand() {
        Command<String, String, Double> command = builder.tdigestTrimmedMean(MY_KEY, 0.1, 0.9);

        assertThat(encode(command)).isEqualTo(
                "*4\r\n" + "$20\r\nTDIGEST.TRIMMED_MEAN\r\n" + "$6\r\nsketch\r\n" + "$3\r\n0.1\r\n" + "$3\r\n0.9\r\n");
    }

}
