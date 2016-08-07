// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import static com.lambdaworks.redis.protocol.LettuceCharsets.buffer;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.output.StatusOutput;

public class CommandInternalsTest {
    protected RedisCodec<String, String> codec = new Utf8StringCodec();
    protected Command<String, String, String> sut;

    @Before
    public final void createCommand() throws Exception {
        CommandOutput<String, String, String> output = new StatusOutput<String, String>(codec);
        sut = new Command<>(CommandType.INFO, output, null);
    }

    @Test
    public void isCancelled() throws Exception {
        assertThat(sut.isCancelled()).isFalse();
        sut.cancel();

        assertThat(sut.isCancelled()).isTrue();
        sut.cancel();
    }

    @Test
    public void isDone() throws Exception {
        assertThat(sut.isDone()).isFalse();
        sut.complete();
        assertThat(sut.isDone()).isTrue();
    }

    @Test
    public void get() throws Exception {
        assertThat(sut.get()).isNull();
        sut.getOutput().set(buffer("one"));
        assertThat(sut.get()).isEqualTo("one");
    }

    @Test
    public void getError() throws Exception {
        sut.getOutput().setError("error");
        assertThat(sut.getError()).isEqualTo("error");
    }

    @Test(expected = IllegalStateException.class)
    public void setOutputAfterCompleted() throws Exception {
        sut.complete();
        sut.setOutput(new StatusOutput<>(codec));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(sut.toString()).contains("Command");
    }

    @Test
    public void customKeyword() throws Exception {

        sut = new Command<String, String, String>(MyKeywords.DUMMY, null, null);
        sut.setOutput(new StatusOutput<String, String>(codec));

        assertThat(sut.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void customKeywordWithArgs() throws Exception {
        sut = new Command<String, String, String>(MyKeywords.DUMMY, null, new CommandArgs<String, String>(codec));
        sut.getArgs().add(MyKeywords.DUMMY);
        assertThat(sut.getArgs().toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void getWithTimeout() throws Exception {
        sut.getOutput().set(buffer("one"));
        sut.complete();

        assertThat(sut.get()).isEqualTo("one");
    }

    @Test(expected = IllegalStateException.class)
    public void outputSubclassOverride1() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(codec, null) {
            @Override
            public String get() throws RedisException {
                return null;
            }
        };
        output.set(null);
    }

    @Test(expected = IllegalStateException.class)
    public void outputSubclassOverride2() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(codec, null) {
            @Override
            public String get() throws RedisException {
                return null;
            }
        };
        output.set(0);
    }

    @Test
    public void sillyTestsForEmmaCoverage() throws Exception {
        assertThat(CommandType.valueOf("APPEND")).isEqualTo(CommandType.APPEND);
        assertThat(CommandKeyword.valueOf("AFTER")).isEqualTo(CommandKeyword.AFTER);
    }

    private enum MyKeywords implements ProtocolKeyword {
        DUMMY;

        @Override
        public byte[] getBytes() {
            return name().getBytes();
        }
    }
}
