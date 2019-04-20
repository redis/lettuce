/*
 * Copyright 2019 the original author or authors.
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
package io.lettuce.core.protocol;

import static io.lettuce.core.protocol.RedisStateMachine.State;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisException;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link RedisStateMachine} using RESP3.
 *
 * @author Mark Paluch
 */
class RedisStateMachineResp3UnitTests {

    private RedisCodec<String, String> codec = new Utf8StringCodec();
    private Charset charset = Charset.forName("UTF-8");
    private CommandOutput<String, String, String> output;
    private RedisStateMachine rsm;

    @BeforeAll
    static void beforeClass() {

        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(RedisStateMachine.class.getName());
        loggerConfig.setLevel(Level.ALL);
    }

    @AfterAll
    static void afterClass() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(RedisStateMachine.class.getName());
        loggerConfig.setLevel(null);
    }

    @BeforeEach
    final void createStateMachine() {
        output = new StatusOutput<>(codec);
        rsm = new RedisStateMachine();
        rsm.setProtocolVersion(ProtocolVersion.RESP3);
    }

    @Test
    void single() {
        assertThat(rsm.decode(buffer("+OK\n"), output)).isTrue();
        assertThat(output.get()).isEqualTo("OK");
    }

    @Test
    void error() {
        assertThat(rsm.decode(buffer("-ERR\n"), output)).isTrue();
        assertThat(output.getError()).isEqualTo("ERR");
    }

    @Test
    void errorWithoutLineBreak() {
        assertThat(rsm.decode(buffer("-ERR"), output)).isFalse();
        assertThat(rsm.decode(buffer("\n"), output)).isTrue();
        assertThat(output.getError()).isEqualTo("");
    }

    @Test
    void integer() {
        CommandOutput<String, String, Long> output = new IntegerOutput<>(codec);
        assertThat(rsm.decode(buffer(":1\n"), output)).isTrue();
        assertThat((long) output.get()).isEqualTo(1);
    }

    @Test
    void floatNumber() {
        CommandOutput<String, String, Double> output = new DoubleOutput<>(codec);
        assertThat(rsm.decode(buffer(",12.345\n"), output)).isTrue();
        assertThat(output.get()).isEqualTo(12.345);
    }

    @Test
    void bigNumber() {
        CommandOutput<String, String, String> output = new StatusOutput<>(codec);
        assertThat(rsm.decode(buffer("(3492890328409238509324850943850943825024385\n"), output)).isTrue();
        assertThat(output.get()).isEqualTo("3492890328409238509324850943850943825024385");
    }

    @Test
    void booleanValue() {
        CommandOutput<String, String, Boolean> output = new BooleanOutput<>(codec);
        assertThat(rsm.decode(buffer("#t\n"), output)).isTrue();
        assertThat(output.get()).isTrue();

        output = new BooleanOutput<>(codec);
        assertThat(rsm.decode(buffer("#f\n"), output)).isTrue();
        assertThat(output.get()).isFalse();
    }

    @Test
    void hello() {
        CommandOutput<String, String, Map<String, Object>> output = new GenericMapOutput<>(codec);
        assertThat(
                rsm.decode(buffer("%7\n" + "$6\nserver\n$5\nredis\n" + "$7\nversion\n$11\n999.999.999\n" + "$5\nproto\n:3\n"
                        + "$2\nid\n:184\n" + "$4\nmode\n$10\nstandalone\n" + "$4\nrole\n$6\nmaster\n" + "$7\nmodules\n*0\n"),
                        output)).isTrue();
        assertThat(output.get()).containsEntry("mode", "standalone");
    }

    @Test
    void bulk() {
        CommandOutput<String, String, String> output = new ValueOutput<>(codec);
        assertThat(rsm.decode(buffer("$-1\n"), output)).isTrue();
        assertThat(output.get()).isNull();
        assertThat(rsm.decode(buffer("$3\nfoo\n"), output)).isTrue();
        assertThat(output.get()).isEqualTo("foo");
    }

    @Test
    void multi() {
        CommandOutput<String, String, List<String>> output = new ValueListOutput<>(codec);
        ByteBuf buffer = buffer("*2\n$-1\n$2\nok\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get()).isEqualTo(Arrays.asList(null, "ok"));
    }

    @Test
    void multiSet() {
        CommandOutput<String, String, List<String>> output = new ValueListOutput<>(codec);
        ByteBuf buffer = buffer("~2\n$-1\n$2\nok\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get()).isEqualTo(Arrays.asList(null, "ok"));
    }

    @Test
    void multiMap() {
        CommandOutput<String, String, Map<String, Object>> output = new GenericMapOutput<>(codec);
        ByteBuf buffer = buffer("%1\n$3\nfoo\n$2\nok\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get()).containsEntry("foo", "ok");
    }

    @Test
    void multiEmptyArray1() {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<>(codec);
        ByteBuf buffer = buffer("*2\n$3\nABC\n*0\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo("ABC");
        assertThat(output.get().get(1)).isEqualTo(Arrays.asList());
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    void multiEmptyArray2() {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<>(codec);
        ByteBuf buffer = buffer("*2\n*0\n$3\nABC\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo(Arrays.asList());
        assertThat(output.get().get(1)).isEqualTo("ABC");
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    void multiEmptyArray3() {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<>(codec);
        ByteBuf buffer = buffer("*2\n*2\n$2\nAB\n$2\nXY\n*0\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo(Arrays.asList("AB", "XY"));
        assertThat(output.get().get(1)).isEqualTo(Arrays.asList());
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    void partialFirstLine() {
        assertThat(rsm.decode(buffer("+"), output)).isFalse();
        assertThat(rsm.decode(buffer("-"), output)).isFalse();
        assertThat(rsm.decode(buffer(":"), output)).isFalse();
        assertThat(rsm.decode(buffer("$"), output)).isFalse();
        assertThat(rsm.decode(buffer("*"), output)).isFalse();
    }

    @Test
    void invalidReplyType() {
        assertThatThrownBy(() -> rsm.decode(buffer("?"), output)).isInstanceOf(RedisException.class);
    }

    @Test
    void sillyTestsForEmmaCoverage() {
        assertThat(State.Type.valueOf("SINGLE")).isEqualTo(State.Type.SINGLE);
    }

    ByteBuf buffer(String content) {
        return Unpooled.copiedBuffer(content, charset);
    }
}
