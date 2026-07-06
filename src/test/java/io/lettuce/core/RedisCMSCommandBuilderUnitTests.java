/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.output.IntegerListOutput;
import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.probabilistic.MergePair;
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
 * Unit tests for {@link RedisCMSCommandBuilder}.
 *
 * @author Yordan Tsintsov
 */
@Tag(UNIT_TEST)
class RedisCMSCommandBuilderUnitTests {

    private static final String MY_KEY = "cms";

    private static final String MY_ITEM = "item1";

    private static final String MY_ITEM_2 = "item2";

    private static final String MY_DEST = "dest";

    private static final String MY_SRC = "src";

    private static final String MY_SRC_2 = "src2";

    private final RedisCMSCommandBuilder<String, String> builder = new RedisCMSCommandBuilder<>(StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructCmsIncrByCommand() {
        Command<String, String, List<Long>> command = builder.cmsIncrBy(MY_KEY, IncrementPair.of(MY_ITEM, 5));
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$10\r\nCMS.INCRBY\r\n" + "$3\r\n" + MY_KEY + "\r\n" + "$5\r\n" + MY_ITEM + "\r\n" + "$1\r\n5\r\n");
        assertThat(command.getOutput()).isInstanceOf(IntegerListOutput.class);
    }

    @Test
    void shouldCorrectlyConstructCmsIncrByCommandWithVarargs() {
        Command<String, String, List<Long>> command = builder.cmsIncrBy(MY_KEY, IncrementPair.of(MY_ITEM, 5),
                IncrementPair.of(MY_ITEM_2, 3));
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$10\r\nCMS.INCRBY\r\n" + "$3\r\n" + MY_KEY
                + "\r\n" + "$5\r\n" + MY_ITEM + "\r\n" + "$1\r\n5\r\n" + "$5\r\n" + MY_ITEM_2 + "\r\n" + "$1\r\n3\r\n");
        assertThat(command.getOutput()).isInstanceOf(IntegerListOutput.class);
    }

    @Test
    void shouldCorrectlyConstructCmsInfoCommand() {
        Command<String, String, ?> command = builder.cmsInfo(MY_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$8\r\nCMS.INFO\r\n" + "$3\r\n" + MY_KEY + "\r\n");
        assertThat(command.getOutput()).isInstanceOf(EncodedComplexOutput.class);
    }

    @Test
    void shouldCorrectlyConstructCmsInitByDimCommand() {
        Command<String, String, String> command = builder.cmsInitByDim(MY_KEY, 2000, 5);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$13\r\nCMS.INITBYDIM\r\n" + "$3\r\n" + MY_KEY + "\r\n" + "$4\r\n2000\r\n" + "$1\r\n5\r\n");
    }

    @Test
    void shouldCorrectlyConstructCmsInitByProbCommand() {
        Command<String, String, String> command = builder.cmsInitByProb(MY_KEY, 0.001, 0.01);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$14\r\nCMS.INITBYPROB\r\n" + "$3\r\n" + MY_KEY + "\r\n" + "$5\r\n0.001\r\n" + "$4\r\n0.01\r\n");
    }

    @Test
    void shouldCorrectlyConstructCmsMergeCommand() {
        Command<String, String, String> command = builder.cmsMerge(MY_DEST, MY_SRC);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$9\r\nCMS.MERGE\r\n" + "$4\r\n" + MY_DEST + "\r\n" + "$1\r\n1\r\n" + "$3\r\n" + MY_SRC + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCmsMergeCommandWithVarargs() {
        Command<String, String, String> command = builder.cmsMerge(MY_DEST, MY_SRC, MY_SRC_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$9\r\nCMS.MERGE\r\n" + "$4\r\n" + MY_DEST
                + "\r\n" + "$1\r\n2\r\n" + "$3\r\n" + MY_SRC + "\r\n" + "$4\r\n" + MY_SRC_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructCmsMergeCommandWithWeight() {
        Command<String, String, String> command = builder.cmsMerge(MY_DEST, MY_SRC, 3);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$9\r\nCMS.MERGE\r\n" + "$4\r\n" + MY_DEST
                + "\r\n" + "$1\r\n1\r\n" + "$3\r\n" + MY_SRC + "\r\n" + "$7\r\nWEIGHTS\r\n" + "$1\r\n3\r\n");
    }

    @Test
    void shouldCorrectlyConstructCmsMergeCommandWithMergePairs() {
        Command<String, String, String> command = builder.cmsMerge(MY_DEST, MergePair.of(MY_SRC, 3), MergePair.of(MY_SRC_2, 4));
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*8\r\n" + "$9\r\nCMS.MERGE\r\n" + "$4\r\n" + MY_DEST + "\r\n" + "$1\r\n2\r\n" + "$3\r\n" + MY_SRC
                        + "\r\n" + "$4\r\n" + MY_SRC_2 + "\r\n" + "$7\r\nWEIGHTS\r\n" + "$1\r\n3\r\n" + "$1\r\n4\r\n");
    }

    @Test
    void shouldCorrectlyConstructCmsQueryCommand() {
        Command<String, String, List<Long>> command = builder.cmsQuery(MY_KEY, MY_ITEM, MY_ITEM_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$9\r\nCMS.QUERY\r\n" + "$3\r\n" + MY_KEY
                + "\r\n" + "$5\r\n" + MY_ITEM + "\r\n" + "$5\r\n" + MY_ITEM_2 + "\r\n");
        assertThat(command.getOutput()).isInstanceOf(IntegerListOutput.class);
    }

}
