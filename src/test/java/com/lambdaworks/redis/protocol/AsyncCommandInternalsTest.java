// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import static com.lambdaworks.redis.protocol.LettuceCharsets.*;
import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.output.CommandOutput;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

public class AsyncCommandInternalsTest {
    protected RedisCodec<String, String> codec = new Utf8StringCodec();
    protected Command<String, String, String> command;
    protected AsyncCommand<String, String, String> sut;

    @Before
    public final void createCommand() throws Exception {
        CommandOutput<String, String, String> output = new StatusOutput<String, String>(codec);
        command = new Command<String, String, String>(CommandType.INFO, output, null);
        sut = new AsyncCommand<>(command);
    }

    @Test
    public void isCancelled() throws Exception {
        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.cancel(true)).isTrue();
        assertThat(sut.isCancelled()).isTrue();
        assertThat(sut.cancel(true)).isTrue();
    }

    @Test
    public void isDone() throws Exception {
        assertThat(sut.isDone()).isFalse();
        sut.complete();
        assertThat(sut.isDone()).isTrue();
    }

    @Test
    public void get() throws Exception {
        sut.getOutput().set(buffer("one"));
        sut.complete();
        assertThat(sut.get()).isEqualTo("one");
        sut.getOutput().toString();
    }

    @Test
    public void customKeyword() throws Exception {

        command = new Command<String, String, String>(MyKeywords.DUMMY, null, null);
        sut = new AsyncCommand<>(command);
        sut.setOutput(new StatusOutput<String, String>(codec));

        assertThat(sut.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void customKeywordWithArgs() throws Exception {
        command = new Command<String, String, String>(MyKeywords.DUMMY, null, new CommandArgs<String, String>(codec));
        sut = new AsyncCommand<>(command);
        sut.getArgs().add(MyKeywords.DUMMY);
        assertThat(sut.getArgs().toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void getWithTimeout() throws Exception {
        sut.getOutput().set(buffer("one"));
        sut.complete();

        assertThat(sut.get(0, TimeUnit.MILLISECONDS)).isEqualTo("one");
    }

    @Test(expected = TimeoutException.class, timeout = 100)
    public void getTimeout() throws Exception {
        assertThat(sut.get(2, TimeUnit.MICROSECONDS)).isNull();
    }

    @Test(timeout = 100)
    public void awaitTimeout() throws Exception {
        assertThat(sut.await(2, TimeUnit.MICROSECONDS)).isFalse();
    }

    @Test(expected = InterruptedException.class, timeout = 100)
    public void getInterrupted() throws Exception {
        Thread.currentThread().interrupt();
        sut.get();
    }

    @Test(expected = InterruptedException.class, timeout = 100)
    public void getInterrupted2() throws Exception {
        Thread.currentThread().interrupt();
        sut.get(5, TimeUnit.MILLISECONDS);
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 100)
    public void awaitInterrupted2() throws Exception {
        Thread.currentThread().interrupt();
        sut.await(5, TimeUnit.MILLISECONDS);
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
