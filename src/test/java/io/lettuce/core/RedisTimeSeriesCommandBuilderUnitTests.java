/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.timeseries.TsAggregationType;
import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsInfoValue;
import io.lettuce.core.timeseries.TsMGetValue;
import io.lettuce.core.timeseries.TsSample;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsGetArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.lettuce.core.timeseries.arguments.TsMGetArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RedisTimeSeriesCommandBuilder}.
 *
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given a key with no options, when {@code tsCreate(key)} is built, then the wire is {@code TS.CREATE key} with no
 * options.</li>
 * <li>Given a key with {@code TsCreateArgs} (retention + a label), when {@code tsCreate(key, args)} is built, then the wire is
 * {@code TS.CREATE key RETENTION 1000 LABELS a b} with {@code LABELS} emitted last (server consumes argv to the end for
 * LABELS).</li>
 * <li>Given a key with {@code TsAlterArgs}, when {@code tsAlter(key, args)} is built, then the wire is
 * {@code TS.ALTER key CHUNK_SIZE 4096}.</li>
 * <li>Given a source/dest key pair and an aggregation type/bucket duration, when {@code tsCreateRule(...)} is built without
 * {@code alignTimestamp}, then the wire is {@code TS.CREATERULE src dst AGGREGATION AVG 60000}.</li>
 * <li>Given the same inputs plus an {@code alignTimestamp}, when the overload is built, then the wire is
 * {@code TS.CREATERULE src dst AGGREGATION AVG 60000 0}.</li>
 * <li>Given a source/dest key pair, when {@code tsDeleteRule(...)} is built, then the wire is
 * {@code TS.DELETERULE src dst}.</li>
 * <li>Given a key and a [from, to] timestamp range, when {@code tsDel(...)} is built, then the wire is
 * {@code TS.DEL key 100 200}.</li>
 * <li>Given a key, timestamp and value, when {@code tsAdd(key, ts, value)} is built, then the wire is
 * {@code TS.ADD key 1000 23.5}.</li>
 * <li>Given a key, timestamp, value and {@code TsAddArgs}, when {@code tsAdd(key, ts, value, args)} is built, then the wire is
 * {@code TS.ADD key 1000 23.5 ON_DUPLICATE LAST LABELS a b} with {@code LABELS} emitted last.</li>
 * <li>Given a key and value with no timestamp, when {@code tsAdd(key, value)} is built, then the wire is
 * {@code TS.ADD key * 23.5} (server auto-assigns the timestamp).</li>
 * <li>Given two (key, {@link TsSample}) entries, when {@code tsMAdd(...)} is built, then the wire is
 * {@code TS.MADD src 1000 23.5 dst 2000 24.5}.</li>
 * <li>Given a single (key, {@link TsSample}) entry, when the non-varargs {@code tsMAdd(entry)} overload is built, then the wire
 * is {@code TS.MADD src 1000 23.5}.</li>
 * <li>Given an entry whose {@link TsSample} carries more than one value, when {@code tsMAdd(...)} (either overload) is built,
 * then an {@link IllegalArgumentException} is thrown instead of silently dropping the extra values.</li>
 * <li>Given a key and an addend, when {@code tsIncrBy(key, value)} is built, then the wire is {@code TS.INCRBY key 1.5}.</li>
 * <li>Given a key, addend and {@code TsIncrByArgs}, when {@code tsIncrBy(key, value, args)} is built, then the wire is
 * {@code TS.INCRBY key 1.5 RETENTION 1000}.</li>
 * <li>Given a key and a subtrahend, when {@code tsDecrBy(key, value)} is built, then the wire is
 * {@code TS.DECRBY key 1.5}.</li>
 * <li>Given a key, subtrahend and {@code TsIncrByArgs}, when {@code tsDecrBy(key, value, args)} is built, then the wire is
 * {@code TS.DECRBY key 1.5 CHUNK_SIZE 4096}.</li>
 * <li>Given a key, when {@code tsGet(key)} is built, then the wire is {@code TS.GET key}.</li>
 * <li>Given a key and {@code TsGetArgs.latest()}, when {@code tsGet(key, args)} is built, then the wire is
 * {@code TS.GET key LATEST}.</li>
 * <li>Given a key, when {@code tsInfo(key)} is built, then the wire is {@code TS.INFO key}.</li>
 * <li>Given a key, when {@code tsInfoDebug(key)} is built, then the wire is {@code TS.INFO key DEBUG}.</li>
 * <li>Given a single filter, when the non-varargs {@code tsMGet(filter)} overload is built, then the wire is
 * {@code TS.MGET FILTER a=1}.</li>
 * <li>Given two or more filters, when the varargs {@code tsMGet(filters)} overload is built, then the wire is
 * {@code TS.MGET FILTER a=1 b=2}.</li>
 * <li>Given {@code TsMGetArgs.withLabels()} and a single filter, when the non-varargs {@code tsMGet(args, filter)} overload is
 * built, then the wire is {@code TS.MGET WITHLABELS FILTER a=1} with {@code WITHLABELS} preceding {@code FILTER} (the server's
 * greedy {@code SELECTED_LABELS}/{@code WITHLABELS} scan must stop at {@code FILTER}).</li>
 * <li>Given {@code TsMGetArgs.withLabels()} and two or more filters, when the varargs {@code tsMGet(args, filters)} overload is
 * built, then the wire is {@code TS.MGET WITHLABELS FILTER a=1 b=2}.</li>
 * <li>Given two or more filters, when the varargs {@code tsQueryIndex(filters)} overload is built, then the wire is
 * {@code TS.QUERYINDEX a=1 b=2} (keyless).</li>
 * <li>Given a single filter, when the non-varargs {@code tsQueryIndex(filter)} overload is built, then the wire is
 * {@code TS.QUERYINDEX a=1} (keyless).</li>
 * </ul>
 * Each assertion also verifies the exact {@link io.lettuce.core.protocol.CommandType} used for encoding, via the RESP
 * command-name bulk string in the wire output (custom {@code CommandType}/{@code ProtocolKeyword} render correctly through
 * {@code encode(ByteBuf)}; {@code toCommandString()} is avoided per the known Base64 debug-rendering issue for
 * non-{@code CommandKeyword}/{@code CommandType} {@code ProtocolKeyword}s).
 */
@Tag(UNIT_TEST)
class RedisTimeSeriesCommandBuilderUnitTests {

    private static final String SOURCE_KEY = "temperature:raw";

    private static final String DEST_KEY = "temperature:hourly";

    private final RedisTimeSeriesCommandBuilder<String, String> builder = new RedisTimeSeriesCommandBuilder<>(StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructTsCreateCommand() {
        Command<String, String, String> command = builder.tsCreate(SOURCE_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$9\r\nTS.CREATE\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsCreateCommandWithArgs() {
        TsCreateArgs args = TsCreateArgs.Builder.retention(1000).label("a", "b");
        Command<String, String, String> command = builder.tsCreate(SOURCE_KEY, args);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*7\r\n" + "$9\r\nTS.CREATE\r\n" + "$15\r\n" + SOURCE_KEY
                + "\r\n" + "$9\r\nRETENTION\r\n" + "$4\r\n1000\r\n" + "$6\r\nLABELS\r\n" + "$1\r\na\r\n" + "$1\r\nb\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsAlterCommand() {
        TsAlterArgs args = TsAlterArgs.Builder.chunkSize(4096);
        Command<String, String, String> command = builder.tsAlter(SOURCE_KEY, args);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$8\r\nTS.ALTER\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$10\r\nCHUNK_SIZE\r\n" + "$4\r\n4096\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsCreateRuleCommand() {
        Command<String, String, String> command = builder.tsCreateRule(SOURCE_KEY, DEST_KEY, TsAggregationType.AVG, 60000);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$13\r\nTS.CREATERULE\r\n" + "$"
                + SOURCE_KEY.length() + "\r\n" + SOURCE_KEY + "\r\n" + "$" + DEST_KEY.length() + "\r\n" + DEST_KEY + "\r\n"
                + "$11\r\nAGGREGATION\r\n" + "$3\r\nAVG\r\n" + "$5\r\n60000\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsCreateRuleCommandWithAlignTimestamp() {
        Command<String, String, String> command = builder.tsCreateRule(SOURCE_KEY, DEST_KEY, TsAggregationType.AVG, 60000, 0);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*7\r\n" + "$13\r\nTS.CREATERULE\r\n" + "$"
                + SOURCE_KEY.length() + "\r\n" + SOURCE_KEY + "\r\n" + "$" + DEST_KEY.length() + "\r\n" + DEST_KEY + "\r\n"
                + "$11\r\nAGGREGATION\r\n" + "$3\r\nAVG\r\n" + "$5\r\n60000\r\n" + "$1\r\n0\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsDeleteRuleCommand() {
        Command<String, String, String> command = builder.tsDeleteRule(SOURCE_KEY, DEST_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$13\r\nTS.DELETERULE\r\n" + "$"
                + SOURCE_KEY.length() + "\r\n" + SOURCE_KEY + "\r\n" + "$" + DEST_KEY.length() + "\r\n" + DEST_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsDelCommand() {
        Command<String, String, Long> command = builder.tsDel(SOURCE_KEY, 100, 200);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*4\r\n" + "$6\r\nTS.DEL\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$3\r\n100\r\n" + "$3\r\n200\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsAddCommand() {
        Command<String, String, Long> command = builder.tsAdd(SOURCE_KEY, 1000, 23.5);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$6\r\nTS.ADD\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$4\r\n1000\r\n" + "$4\r\n23.5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsAddCommandWithArgs() {
        TsAddArgs args = TsAddArgs.Builder.onDuplicate(TsDuplicatePolicy.LAST).label("a", "b");
        Command<String, String, Long> command = builder.tsAdd(SOURCE_KEY, 1000, 23.5, args);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*9\r\n" + "$6\r\nTS.ADD\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$4\r\n1000\r\n" + "$4\r\n23.5\r\n"
                        + "$12\r\nON_DUPLICATE\r\n" + "$4\r\nLAST\r\n" + "$6\r\nLABELS\r\n" + "$1\r\na\r\n" + "$1\r\nb\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsAddCommandWithAutoTimestamp() {
        Command<String, String, Long> command = builder.tsAdd(SOURCE_KEY, 23.5);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*4\r\n" + "$6\r\nTS.ADD\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$1\r\n*\r\n" + "$4\r\n23.5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsMAddCommand() {
        Map.Entry<String, TsSample> first = new AbstractMap.SimpleEntry<>(SOURCE_KEY,
                new TsSample(1000, Collections.singletonList(23.5)));
        Map.Entry<String, TsSample> second = new AbstractMap.SimpleEntry<>(DEST_KEY,
                new TsSample(2000, Collections.singletonList(24.5)));

        Command<String, String, List<Long>> command = builder.tsMAdd(first, second);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*7\r\n" + "$7\r\nTS.MADD\r\n" + "$" + SOURCE_KEY.length()
                + "\r\n" + SOURCE_KEY + "\r\n" + "$4\r\n1000\r\n" + "$4\r\n23.5\r\n" + "$" + DEST_KEY.length() + "\r\n"
                + DEST_KEY + "\r\n" + "$4\r\n2000\r\n" + "$4\r\n24.5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsMAddCommandWithSingleEntry() {
        Map.Entry<String, TsSample> entry = new AbstractMap.SimpleEntry<>(SOURCE_KEY,
                new TsSample(1000, Collections.singletonList(23.5)));

        Command<String, String, List<Long>> command = builder.tsMAdd(entry);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$7\r\nTS.MADD\r\n" + "$" + SOURCE_KEY.length()
                + "\r\n" + SOURCE_KEY + "\r\n" + "$4\r\n1000\r\n" + "$4\r\n23.5\r\n");
    }

    @Test
    void shouldRejectTsMAddSingleEntryWithMultiValueSample() {
        Map.Entry<String, TsSample> entry = new AbstractMap.SimpleEntry<>(SOURCE_KEY,
                new TsSample(1000, Arrays.asList(23.5, 24.5)));

        assertThatThrownBy(() -> builder.tsMAdd(entry)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TS.MADD");
    }

    @Test
    void shouldRejectTsMAddVarargsWithMultiValueSample() {
        Map.Entry<String, TsSample> first = new AbstractMap.SimpleEntry<>(SOURCE_KEY,
                new TsSample(1000, Collections.singletonList(23.5)));
        Map.Entry<String, TsSample> second = new AbstractMap.SimpleEntry<>(DEST_KEY,
                new TsSample(2000, Arrays.asList(24.5, 25.5)));

        assertThatThrownBy(() -> builder.tsMAdd(first, second)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TS.MADD");
    }

    @Test
    void shouldCorrectlyConstructTsIncrByCommand() {
        Command<String, String, Long> command = builder.tsIncrBy(SOURCE_KEY, 1.5);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$9\r\nTS.INCRBY\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$3\r\n1.5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsIncrByCommandWithArgs() {
        TsIncrByArgs args = TsIncrByArgs.Builder.retention(1000);
        Command<String, String, Long> command = builder.tsIncrBy(SOURCE_KEY, 1.5, args);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$9\r\nTS.INCRBY\r\n" + "$15\r\n" + SOURCE_KEY
                + "\r\n" + "$3\r\n1.5\r\n" + "$9\r\nRETENTION\r\n" + "$4\r\n1000\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsDecrByCommand() {
        Command<String, String, Long> command = builder.tsDecrBy(SOURCE_KEY, 1.5);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$9\r\nTS.DECRBY\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$3\r\n1.5\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsDecrByCommandWithArgs() {
        TsIncrByArgs args = TsIncrByArgs.Builder.chunkSize(4096);
        Command<String, String, Long> command = builder.tsDecrBy(SOURCE_KEY, 1.5, args);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$9\r\nTS.DECRBY\r\n" + "$15\r\n" + SOURCE_KEY
                + "\r\n" + "$3\r\n1.5\r\n" + "$10\r\nCHUNK_SIZE\r\n" + "$4\r\n4096\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsGetCommand() {
        Command<String, String, TsSample> command = builder.tsGet(SOURCE_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$6\r\nTS.GET\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsGetCommandWithArgs() {
        TsGetArgs args = TsGetArgs.Builder.latest();
        Command<String, String, TsSample> command = builder.tsGet(SOURCE_KEY, args);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$6\r\nTS.GET\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$6\r\nLATEST\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsInfoCommand() {
        Command<String, String, TsInfoValue<String>> command = builder.tsInfo(SOURCE_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$7\r\nTS.INFO\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsInfoDebugCommand() {
        Command<String, String, TsInfoValue<String>> command = builder.tsInfoDebug(SOURCE_KEY);
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$7\r\nTS.INFO\r\n" + "$15\r\n" + SOURCE_KEY + "\r\n" + "$5\r\nDEBUG\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsMGetCommand() {
        Command<String, String, List<TsMGetValue<String>>> command = builder.tsMGet("region=us");
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$7\r\nTS.MGET\r\n" + "$6\r\nFILTER\r\n" + "$9\r\nregion=us\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsMGetCommandWithMultipleFilters() {
        Command<String, String, List<TsMGetValue<String>>> command = builder.tsMGet("region=us", "env=prod");
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*4\r\n" + "$7\r\nTS.MGET\r\n" + "$6\r\nFILTER\r\n" + "$9\r\nregion=us\r\n" + "$8\r\nenv=prod\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsMGetCommandWithArgs() {
        TsMGetArgs args = TsMGetArgs.Builder.withLabels();
        Command<String, String, List<TsMGetValue<String>>> command = builder.tsMGet(args, "region=us");
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*4\r\n" + "$7\r\nTS.MGET\r\n" + "$10\r\nWITHLABELS\r\n" + "$6\r\nFILTER\r\n" + "$9\r\nregion=us\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsMGetCommandWithArgsAndMultipleFilters() {
        TsMGetArgs args = TsMGetArgs.Builder.withLabels();
        Command<String, String, List<TsMGetValue<String>>> command = builder.tsMGet(args, "region=us", "env=prod");
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$7\r\nTS.MGET\r\n" + "$10\r\nWITHLABELS\r\n"
                + "$6\r\nFILTER\r\n" + "$9\r\nregion=us\r\n" + "$8\r\nenv=prod\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsQueryIndexCommand() {
        Command<String, String, List<String>> command = builder.tsQueryIndex("region=us", "env=prod");
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*3\r\n" + "$13\r\nTS.QUERYINDEX\r\n" + "$9\r\nregion=us\r\n" + "$8\r\nenv=prod\r\n");
    }

    @Test
    void shouldCorrectlyConstructTsQueryIndexCommandWithSingleFilter() {
        Command<String, String, List<String>> command = builder.tsQueryIndex("region=us");
        ByteBuf buff = Unpooled.buffer();
        command.encode(buff);

        assertThat(buff.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$13\r\nTS.QUERYINDEX\r\n" + "$9\r\nregion=us\r\n");
    }

}
