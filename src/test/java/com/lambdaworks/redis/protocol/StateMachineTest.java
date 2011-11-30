// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static com.lambdaworks.redis.protocol.RedisStateMachine.State;
import static org.junit.Assert.*;

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
        assertTrue(rsm.decode(buffer("+OK\r\n"), output));
        assertEquals("OK", output.get());
    }

    @Test
    public void error() throws Exception {
        assertTrue(rsm.decode(buffer("-ERR\r\n"), output));
        assertEquals("ERR", output.getError());
    }

    @Test
    public void integer() throws Exception {
        CommandOutput<String, String, Long> output = new IntegerOutput<String, String>(codec);
        assertTrue(rsm.decode(buffer(":1\r\n"), output));
        assertEquals(1, (long) output.get());
    }

    @Test
    public void bulk() throws Exception {
        CommandOutput<String, String, String> output = new ValueOutput<String, String>(codec);
        assertTrue(rsm.decode(buffer("$-1\r\n"), output));
        assertNull(output.get());
        assertTrue(rsm.decode(buffer("$3\r\nfoo\r\n"), output));
        assertEquals("foo", output.get());
    }

    @Test
    public void multi() throws Exception {
        CommandOutput<String, String, List<String>> output = new ValueListOutput<String, String>(codec);
        ChannelBuffer buffer = buffer("*2\r\n$-1\r\n$2\r\nok\r\n");
        assertTrue(rsm.decode(buffer, output));
        assertEquals(Arrays.asList(null, "ok"), output.get());
    }

    @Test
    public void partialFirstLine() throws Exception {
        assertFalse(rsm.decode(buffer("+"), output));
        assertFalse(rsm.decode(buffer("-"), output));
        assertFalse(rsm.decode(buffer(":"), output));
        assertFalse(rsm.decode(buffer("$"), output));
        assertFalse(rsm.decode(buffer("*"), output));
    }

    @Test(expected = RedisException.class)
    public void invalidReplyType() throws Exception {
        rsm.decode(buffer("="), output);
    }

    @Test
    public void sillyTestsForEmmaCoverage() throws Exception {
        assertEquals(State.Type.SINGLE, State.Type.valueOf("SINGLE"));
    }

    protected ChannelBuffer buffer(String content) {
        return ChannelBuffers.copiedBuffer(content, charset);
    }
}
