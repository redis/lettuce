/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.probabilistic.TopKInfoValue;
import io.lettuce.core.probabilistic.TopKListValue;
import io.lettuce.core.probabilistic.arguments.TopKReserveArgs;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RedisTopKCommandBuilder}.
 *
 * @author Yordan Tsintsov
 */
@Tag(UNIT_TEST)
class RedisTopKCommandBuilderUnitTests {

    private static final String MY_KEY = "topk:name";

    private static final String MY_VALUE = "Dune";

    private static final String MY_VALUE_2 = "Dune Messiah";

    private final RedisTopKCommandBuilder<String, String> builder = new RedisTopKCommandBuilder<>(StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructTopKAddCommand() {
        Command<String, String, List<String>> command = builder.topKAdd(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$8\r\nTOPK.ADD\r\n" + "$9\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKAddCommandWithVarargs() {
        Command<String, String, List<String>> command = builder.topKAdd(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$8\r\nTOPK.ADD\r\n" + "$9\r\n" + MY_KEY + "\r\n"
                + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKAddValuesCommand() {
        Command<String, String, List<Value<String>>> command = builder.topKAddValues(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$8\r\nTOPK.ADD\r\n" + "$9\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKAddValuesCommandWithVarargs() {
        Command<String, String, List<Value<String>>> command = builder.topKAddValues(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$8\r\nTOPK.ADD\r\n" + "$9\r\n" + MY_KEY + "\r\n"
                + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKIncrByCommand() {
        Command<String, String, List<String>> command = builder.topKIncrBy(MY_KEY, MY_VALUE, 3L);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$11\r\nTOPK.INCRBY\r\n" + "$9\r\n" + MY_KEY
                + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$1\r\n3\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKIncrByCommandWithVarargs() {
        Command<String, String, List<String>> command = builder.topKIncrBy(MY_KEY, IncrementPair.of(MY_VALUE, 3L),
                IncrementPair.of(MY_VALUE_2, 5L));
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$11\r\nTOPK.INCRBY\r\n" + "$9\r\n" + MY_KEY
                + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$1\r\n3\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n" + "$1\r\n5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKIncrByValuesCommand() {
        Command<String, String, List<Value<String>>> command = builder.topKIncrByValues(MY_KEY, MY_VALUE, 3L);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$11\r\nTOPK.INCRBY\r\n" + "$9\r\n" + MY_KEY
                + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$1\r\n3\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKIncrByValuesCommandWithVarargs() {
        Command<String, String, List<Value<String>>> command = builder.topKIncrByValues(MY_KEY, IncrementPair.of(MY_VALUE, 3L),
                IncrementPair.of(MY_VALUE_2, 5L));
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$11\r\nTOPK.INCRBY\r\n" + "$9\r\n" + MY_KEY
                + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$1\r\n3\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n" + "$1\r\n5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKInfoCommand() {
        Command<String, String, TopKInfoValue> command = builder.topKInfo(MY_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$9\r\nTOPK.INFO\r\n" + "$9\r\n" + MY_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKListCommand() {
        Command<String, String, List<String>> command = builder.topKList(MY_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$9\r\nTOPK.LIST\r\n" + "$9\r\n" + MY_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKListCommandWithoutCount() {
        Command<String, String, List<TopKListValue>> command = builder.topKList(MY_KEY, false);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$9\r\nTOPK.LIST\r\n" + "$9\r\n" + MY_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKListCommandWithCount() {
        Command<String, String, List<TopKListValue>> command = builder.topKList(MY_KEY, true);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$9\r\nTOPK.LIST\r\n" + "$9\r\n" + MY_KEY + "\r\n" + "$9\r\nWITHCOUNT\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKQueryCommand() {
        Command<String, String, List<Boolean>> command = builder.topKQuery(MY_KEY, MY_VALUE);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$10\r\nTOPK.QUERY\r\n" + "$9\r\n" + MY_KEY + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKQueryCommandWithVarargs() {
        Command<String, String, List<Boolean>> command = builder.topKQuery(MY_KEY, MY_VALUE, MY_VALUE_2);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$10\r\nTOPK.QUERY\r\n" + "$9\r\n" + MY_KEY
                + "\r\n" + "$4\r\n" + MY_VALUE + "\r\n" + "$12\r\n" + MY_VALUE_2 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKReserveCommand() {
        Command<String, String, String> command = builder.topKReserve(MY_KEY, 50);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$12\r\nTOPK.RESERVE\r\n" + "$9\r\n" + MY_KEY + "\r\n" + "$2\r\n50\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKReserveCommandWithArgs() {
        TopKReserveArgs reserveArgs = TopKReserveArgs.Builder.width(8).depth(7).decay(0.9);
        Command<String, String, String> command = builder.topKReserve(MY_KEY, 50, reserveArgs);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$12\r\nTOPK.RESERVE\r\n" + "$9\r\n" + MY_KEY
                + "\r\n" + "$2\r\n50\r\n" + "$1\r\n8\r\n" + "$1\r\n7\r\n" + "$3\r\n0.9\r\n");
    }

    @Test
    void shouldCorrectlyConstructTopKReserveCommandWithEmptyArgs() {
        Command<String, String, String> command = builder.topKReserve(MY_KEY, 50, new TopKReserveArgs());
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$12\r\nTOPK.RESERVE\r\n" + "$9\r\n" + MY_KEY + "\r\n" + "$2\r\n50\r\n");
    }

    @Test
    void shouldRejectPartialTopKReserveArgs() {
        TopKReserveArgs reserveArgs = TopKReserveArgs.Builder.width(8).depth(7);

        assertThatThrownBy(() -> builder.topKReserve(MY_KEY, 50, reserveArgs)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("width, depth and decay must be provided together");
    }

}
