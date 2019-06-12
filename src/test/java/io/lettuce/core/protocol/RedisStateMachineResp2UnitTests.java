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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.*;

import io.lettuce.core.RedisException;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link RedisStateMachine} using RESP2.
 *
 * @author Will Glozer
 * @author Mark Paluch
 */
class RedisStateMachineResp2UnitTests {

    private RedisCodec<String, String> codec = StringCodec.UTF8;
    private Charset charset = StandardCharsets.UTF_8;
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
        rsm = new RedisStateMachine(ByteBufAllocator.DEFAULT);
    }

    @AfterEach
    void tearDown() {
        rsm.close();
    }

    @Test
    void helloShouldSwitchToResp3() {
        assertThat(rsm.decode(buffer("@0\r\n"), output)).isTrue();
        assertThat(rsm.isDiscoverProtocol()).isFalse();
        assertThat(rsm.getProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
    }

    @Test
    void single() {
        assertThat(rsm.decode(buffer("+OK\r\n"), output)).isTrue();
        assertThat(output.get()).isEqualTo("OK");
    }

    @Test
    void error() {
        ByteBuf buffer = buffer("-ERR\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.getError()).isEqualTo("ERR");
        assertThat(buffer.readerIndex()).isEqualTo(6);
    }

    @Test
    void errorWithoutLineBreak() {
        assertThat(rsm.decode(buffer("-ERR"), output)).isFalse();
        assertThat(rsm.decode(buffer("\r\n"), output)).isTrue();
        assertThat(output.getError()).isEqualTo("");
    }

    @Test
    void integer() {
        CommandOutput<String, String, Long> output = new IntegerOutput<>(codec);
        ByteBuf buffer = buffer(":1\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat((long) output.get()).isEqualTo(1);
        assertThat(buffer.readerIndex()).isEqualTo(4);
    }

    @Test
    void bulk() {
        CommandOutput<String, String, String> output = new ValueOutput<>(codec);
        ByteBuf buffer = buffer("$-1\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(buffer.readerIndex()).isEqualTo(5);
        assertThat(output.get()).isNull();
        buffer = buffer("$3\r\nfoo\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get()).isEqualTo("foo");
        assertThat(buffer.readerIndex()).isEqualTo(9);
    }

    @Test
    void multi() {
        CommandOutput<String, String, List<String>> output = new ValueListOutput<>(codec);
        ByteBuf buffer = buffer("*2\r\n$-1\r\n$2\r\nok\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get()).isEqualTo(Arrays.asList(null, "ok"));
    }

    @Test
    void multiEmptyArray1() {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<>(codec);
        ByteBuf buffer = buffer("*2\r\n$3\r\nABC\r\n*0\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo("ABC");
        assertThat(output.get().get(1)).isEqualTo(Arrays.asList());
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    void multiEmptyArray2() {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<>(codec);
        ByteBuf buffer = buffer("*2\r\n*0\r\n$3\r\nABC\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo(Arrays.asList());
        assertThat(output.get().get(1)).isEqualTo("ABC");
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    void multiEmptyArray3() {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<>(codec);
        ByteBuf buffer = buffer("*2\r\n*2\r\n$2\r\nAB\r\n$2\r\nXY\r\n*0\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo(Arrays.asList("AB", "XY"));
        assertThat(output.get().get(1)).isEqualTo(Collections.emptyList());
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
