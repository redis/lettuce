// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.LettuceCharsets.*;
import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

public class CommandInternalsTest {
    protected RedisCodec<String, String> codec = new Utf8StringCodec();
    protected Command<String, String, String> command;

    @Before
    public final void createCommand() throws Exception {
        CommandOutput<String, String, String> output = new StatusOutput<String, String>(codec);
        command = new Command<String, String, String>(CommandType.INFO, output, null, false);
    }

    @Test
    public void isCancelled() throws Exception {
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.cancel(true)).isTrue();
        assertThat(command.isCancelled()).isTrue();
        assertThat(command.cancel(true)).isFalse();
    }

    @Test
    public void isDone() throws Exception {
        assertThat(command.isDone()).isFalse();
        command.complete();
        assertThat(command.isDone()).isTrue();
    }

    @Test
    public void get() throws Exception {
        command.getOutput().set(buffer("one"));
        command.complete();
        assertThat(command.get()).isEqualTo("one");
        command.getOutput().toString();
    }

    @Test
    public void customKeyword() throws Exception {

        command = new Command<String, String, String>(MyKeywords.DUMMY, null, null, false);
        command.setOutput(new StatusOutput<String, String>(codec));

        assertThat(command.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void customKeywordWithArgs() throws Exception {
        command = new Command<String, String, String>(MyKeywords.DUMMY, null, new CommandArgs<String, String>(codec), false);
        command.getArgs().add(MyKeywords.DUMMY);
        assertThat(command.getArgs().toString()).contains(MyKeywords.DUMMY.name());
        assertThat(command.getArgs().getKeywords()).contains(MyKeywords.DUMMY);
    }

    @Test
    public void getWithTimeout() throws Exception {
        command.getOutput().set(buffer("one"));
        command.complete();

        assertThat(command.get(0, TimeUnit.MILLISECONDS)).isEqualTo("one");
    }

    @Test(expected = TimeoutException.class, timeout = 100)
    public void getTimeout() throws Exception {
        assertThat(command.get(2, TimeUnit.MICROSECONDS)).isNull();
    }

    @Test(timeout = 100)
    public void awaitTimeout() throws Exception {
        assertThat(command.await(2, TimeUnit.MICROSECONDS)).isFalse();
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 100)
    public void getInterrupted() throws Exception {
        Thread.currentThread().interrupt();
        command.get();
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 100)
    public void getInterrupted2() throws Exception {
        Thread.currentThread().interrupt();
        command.get(5, TimeUnit.MILLISECONDS);
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 100)
    public void awaitInterrupted2() throws Exception {
        Thread.currentThread().interrupt();
        command.await(5, TimeUnit.MILLISECONDS);
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
    public void nestedMultiError() throws Exception {
        NestedMultiOutput<String, String> output = new NestedMultiOutput<String, String>(codec);
        output.setError(buffer("Oops!"));
        assertThat(output.get().get(0) instanceof RedisException).isTrue();
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
