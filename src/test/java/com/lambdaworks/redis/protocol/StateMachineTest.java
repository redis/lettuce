// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import static com.lambdaworks.redis.protocol.RedisStateMachine.*;
import static org.assertj.core.api.Assertions.*;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.lambdaworks.redis.output.CommandOutput;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.IntegerOutput;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.output.ValueListOutput;
import com.lambdaworks.redis.output.ValueOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class StateMachineTest {
    protected RedisCodec<String, String> codec = new Utf8StringCodec();
    protected Charset charset = Charset.forName("UTF-8");
    protected CommandOutput<String, String, String> output;
    protected RedisStateMachine<String, String> rsm;

    @Before
    public final void createStateMachine() throws Exception {
        output = new StatusOutput<String, String>(codec);
        rsm = new RedisStateMachine<String, String>();
    }

    @Test
    public void single() throws Exception {
        assertThat(rsm.decode(buffer("+OK\r\n"), output)).isTrue();
        assertThat(output.get()).isEqualTo("OK");
    }

    @Test
    public void error() throws Exception {
        assertThat(rsm.decode(buffer("-ERR\r\n"), output)).isTrue();
        assertThat(output.getError()).isEqualTo("ERR");
    }

    @Test
    public void integer() throws Exception {
        CommandOutput<String, String, Long> output = new IntegerOutput<String, String>(codec);
        assertThat(rsm.decode(buffer(":1\r\n"), output)).isTrue();
        assertThat((long) output.get()).isEqualTo(1);
    }

    @Test
    public void bulk() throws Exception {
        CommandOutput<String, String, String> output = new ValueOutput<String, String>(codec);
        assertThat(rsm.decode(buffer("$-1\r\n"), output)).isTrue();
        assertThat(output.get()).isNull();
        assertThat(rsm.decode(buffer("$3\r\nfoo\r\n"), output)).isTrue();
        assertThat(output.get()).isEqualTo("foo");
    }

    @Test
    public void multi() throws Exception {
        CommandOutput<String, String, List<String>> output = new ValueListOutput<String, String>(codec);
        ByteBuf buffer = buffer("*2\r\n$-1\r\n$2\r\nok\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get()).isEqualTo(Arrays.asList(null, "ok"));
    }

    @Test
    public void multiEmptyArray1() throws Exception {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<String, String>(codec);
        ByteBuf buffer = buffer("*2\r\n$3\r\nABC\r\n*0\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo("ABC");
        assertThat(output.get().get(1)).isEqualTo(Arrays.asList());
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    public void multiEmptyArray2() throws Exception {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<String, String>(codec);
        ByteBuf buffer = buffer("*2\r\n*0\r\n$3\r\nABC\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo(Arrays.asList());
        assertThat(output.get().get(1)).isEqualTo("ABC");
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    public void multiEmptyArray3() throws Exception {
        CommandOutput<String, String, List<Object>> output = new NestedMultiOutput<String, String>(codec);
        ByteBuf buffer = buffer("*2\r\n*2\r\n$2\r\nAB\r\n$2\r\nXY\r\n*0\r\n");
        assertThat(rsm.decode(buffer, output)).isTrue();
        assertThat(output.get().get(0)).isEqualTo(Arrays.asList("AB", "XY"));
        assertThat(output.get().get(1)).isEqualTo(Arrays.asList());
        assertThat(output.get().size()).isEqualTo(2);
    }

    @Test
    public void partialFirstLine() throws Exception {
        assertThat(rsm.decode(buffer("+"), output)).isFalse();
        assertThat(rsm.decode(buffer("-"), output)).isFalse();
        assertThat(rsm.decode(buffer(":"), output)).isFalse();
        assertThat(rsm.decode(buffer("$"), output)).isFalse();
        assertThat(rsm.decode(buffer("*"), output)).isFalse();
    }

    @Test(expected = RedisException.class)
    public void invalidReplyType() throws Exception {
        rsm.decode(buffer("="), output);
    }

    @Test
    public void sillyTestsForEmmaCoverage() throws Exception {
        assertThat(State.Type.valueOf("SINGLE")).isEqualTo(State.Type.SINGLE);
    }

    protected ByteBuf buffer(String content) {
        return Unpooled.copiedBuffer(content, charset);
    }
}
