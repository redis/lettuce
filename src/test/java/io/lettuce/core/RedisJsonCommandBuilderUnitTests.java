package io.lettuce.core;

/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.DefaultJsonParser;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisJsonCommandBuilder}.
 *
 * @author Tihomir Mateev
 */
class RedisJsonCommandBuilderUnitTests {

    public static final String MY_KEY = "bikes:inventory";

    public static final String MY_KEY2 = "bikes:repairLog";

    public static final String ID_BIKE_6 = "{\"id\":\"bike6\"}";

    public static final JsonParser PARSER = DefaultJsonParser.INSTANCE;

    public static final JsonValue ELEMENT = PARSER.createJsonValue(ID_BIKE_6);

    public static final JsonPath MY_PATH = JsonPath.of("$..commuter_bikes");

    RedisJsonCommandBuilder<String, String> builder = new RedisJsonCommandBuilder<>(StringCodec.UTF8, PARSER);

    @Test
    void shouldCorrectlyConstructJsonArrappend() {
        Command<String, String, List<Long>> command = builder.jsonArrappend(MY_KEY, MY_PATH, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$14\r\n" + "JSON.ARRAPPEND\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$14\r\n" + ID_BIKE_6 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonArrindex() {
        JsonRangeArgs range = JsonRangeArgs.Builder.start(0).stop(1);
        Command<String, String, List<Long>> command = builder.jsonArrindex(MY_KEY, MY_PATH, ELEMENT, range);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$13\r\n" + "JSON.ARRINDEX\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$14\r\n" + ID_BIKE_6 + "\r\n" + "$1" + "\r\n"
                + "0" + "\r\n" + "$1" + "\r\n" + "1" + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonArrinsert() {
        Command<String, String, List<Long>> command = builder.jsonArrinsert(MY_KEY, MY_PATH, 1, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*5\r\n" + "$14\r\n" + "JSON.ARRINSERT\r\n" + "$15\r\n" + "bikes:inventory\r\n" + "$17\r\n"
                        + "$..commuter_bikes\r\n" + "$1" + "\r\n" + "1" + "\r\n" + "$14\r\n" + ID_BIKE_6 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonArrlen() {
        Command<String, String, List<Long>> command = builder.jsonArrlen(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$11\r\n" + "JSON.ARRLEN\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonArrpop() {
        Command<String, String, List<JsonValue>> command = builder.jsonArrpop(MY_KEY, MY_PATH, 3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$11\r\n" + "JSON.ARRPOP\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$1" + "\r\n" + "3" + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonArrtrim() {
        JsonRangeArgs range = JsonRangeArgs.Builder.start(0).stop(1);
        Command<String, String, List<Long>> command = builder.jsonArrtrim(MY_KEY, MY_PATH, range);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*5\r\n" + "$12\r\n" + "JSON.ARRTRIM\r\n" + "$15\r\n" + "bikes:inventory\r\n" + "$17\r\n"
                        + "$..commuter_bikes\r\n" + "$1" + "\r\n" + "0" + "\r\n" + "$1" + "\r\n" + "1" + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonClear() {
        Command<String, String, Long> command = builder.jsonClear(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$10\r\n" + "JSON.CLEAR\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonGet() {
        JsonGetArgs args = JsonGetArgs.Builder.indent("   ").newline("\n").space("/");
        Command<String, String, List<JsonValue>> command = builder.jsonGet(MY_KEY, args, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*9\r\n" + "$8\r\n" + "JSON.GET\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$6\r\n" + "INDENT\r\n" + "$3\r\n" + "   \r\n" + "$7\r\n" + "NEWLINE\r\n" + "$1\r\n"
                + "\n\r\n" + "$5\r\n" + "SPACE\r\n" + "$1\r\n" + "/\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonMerge() {
        Command<String, String, String> command = builder.jsonMerge(MY_KEY, MY_PATH, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$10\r\n" + "JSON.MERGE\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$14\r\n" + ID_BIKE_6 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonMget() {
        Command<String, String, List<JsonValue>> command = builder.jsonMGet(MY_PATH, MY_KEY, MY_KEY2);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$9\r\n" + "JSON.MGET\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$15\r\n" + "bikes:repairLog\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonMset() {
        JsonMsetArgs<String, String> args1 = new JsonMsetArgs<>(MY_KEY, MY_PATH, ELEMENT);
        Command<String, String, String> command = builder.jsonMSet(Collections.singletonList(args1));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$9\r\n" + "JSON.MSET\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$14\r\n" + ID_BIKE_6 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonNumincrby() {
        Command<String, String, List<Number>> command = builder.jsonNumincrby(MY_KEY, MY_PATH, 3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$14\r\n" + "JSON.NUMINCRBY\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$1" + "\r\n" + "3" + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonObjkeys() {
        Command<String, String, List<String>> command = builder.jsonObjkeys(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$12\r\n" + "JSON.OBJKEYS\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonObjlen() {
        Command<String, String, List<Long>> command = builder.jsonObjlen(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$11\r\n" + "JSON.OBJLEN\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonSet() {
        JsonSetArgs args = JsonSetArgs.Builder.nx();
        Command<String, String, String> command = builder.jsonSet(MY_KEY, MY_PATH, ELEMENT, args);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*5\r\n" + "$8\r\n" + "JSON.SET\r\n" + "$15\r\n" + "bikes:inventory\r\n" + "$17\r\n"
                        + "$..commuter_bikes\r\n" + "$14\r\n" + ID_BIKE_6 + "\r\n" + "$2\r\n" + "NX\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonStrappend() {
        Command<String, String, List<Long>> command = builder.jsonStrappend(MY_KEY, MY_PATH, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$14\r\n" + "JSON.STRAPPEND\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$14\r\n" + ID_BIKE_6 + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonStrlen() {
        Command<String, String, List<Long>> command = builder.jsonStrlen(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$11\r\n" + "JSON.STRLEN\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonToggle() {
        Command<String, String, List<Long>> command = builder.jsonToggle(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$11\r\n" + "JSON.TOGGLE\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonDel() {
        Command<String, String, Long> command = builder.jsonDel(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*3\r\n" + "$8\r\n" + "JSON.DEL\r\n" + "$15\r\n" + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonType() {
        Command<String, String, ?> command = builder.jsonType(MY_KEY, MY_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$9\r\n" + "JSON.TYPE\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonTypeRootPath() {
        Command<String, String, ?> command = builder.jsonType(MY_KEY, JsonPath.ROOT_PATH);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$9\r\n" + "JSON.TYPE\r\n" + "$15\r\n" + "bikes:inventory\r\n");
    }

}
