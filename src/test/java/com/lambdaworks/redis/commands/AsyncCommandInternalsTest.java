package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.protocol.LettuceCharsets.buffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

public class AsyncCommandInternalsTest {
    protected RedisCodec<String, String> codec = new Utf8StringCodec();
    protected Command<String, String, String> internal;
    protected AsyncCommand<String, String, String> sut;

    @Before
    public final void createCommand() throws Exception {
        CommandOutput<String, String, String> output = new StatusOutput<String, String>(codec);
        internal = new Command<String, String, String>(CommandType.INFO, output, null);
        sut = new AsyncCommand<>(internal);
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
    public void awaitAllCompleted() throws Exception {
        sut.complete();
        assertThat(LettuceFutures.awaitAll(5, TimeUnit.MILLISECONDS, sut)).isTrue();
    }

    @Test
    public void awaitAll() throws Exception {
        assertThat(LettuceFutures.awaitAll(-1, TimeUnit.NANOSECONDS, sut)).isFalse();
    }

    @Test(expected = RedisCommandTimeoutException.class)
    public void awaitNotCompleted() throws Exception {
        LettuceFutures.await(sut, 0, TimeUnit.NANOSECONDS);
    }

    @Test(expected = RedisException.class)
    public void awaitWithExecutionException() throws Exception {
        sut.completeExceptionally(new RedisException("error"));
        LettuceFutures.await(sut, 1, TimeUnit.SECONDS);
    }

    @Test(expected = RedisException.class)
    public void awaitAllWithExecutionException() throws Exception {
        sut.completeExceptionally(new RedisCommandExecutionException("error"));

        assertThat(LettuceFutures.awaitAll(0, TimeUnit.SECONDS, sut));
    }

    @Test
    public void getError() throws Exception {
        sut.getOutput().setError("error");
        assertThat(internal.getError()).isEqualTo("error");
    }

    @Test(expected = ExecutionException.class)
    public void getErrorAsync() throws Exception {
        sut.getOutput().setError("error");
        sut.complete();
        sut.get();
    }

    @Test(expected = ExecutionException.class)
    public void completeExceptionally() throws Exception {
        sut.completeExceptionally(new RuntimeException("test"));
        assertThat(internal.getError()).isEqualTo("test");

        sut.get();
    }

    @Test
    public void asyncGet() throws Exception {
        sut.getOutput().set(buffer("one"));
        sut.complete();
        assertThat(sut.get()).isEqualTo("one");
        sut.getOutput().toString();
    }

    @Test
    public void customKeyword() throws Exception {
        sut = new AsyncCommand<>(new Command<String, String, String>(MyKeywords.DUMMY, new StatusOutput<String, String>(codec),
                null));

        assertThat(sut.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void customKeywordWithArgs() throws Exception {
        sut = new AsyncCommand<>(new Command<String, String, String>(MyKeywords.DUMMY, null, new CommandArgs<String, String>(
                codec)));
        sut.getArgs().add(MyKeywords.DUMMY);
        assertThat(sut.getArgs().toString()).contains(MyKeywords.DUMMY.name());
        assertThat(sut.getArgs().getKeywords()).contains(MyKeywords.DUMMY);
    }

    @Test
    public void getWithTimeout() throws Exception {
        sut.getOutput().set(buffer("one"));
        sut.complete();

        assertThat(sut.get(0, TimeUnit.MILLISECONDS)).isEqualTo("one");
    }

    @Test(expected = TimeoutException.class, timeout = 100)
    public void getTimeout() throws Exception {
        assertThat(sut.get(2, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test(timeout = 100)
    public void awaitTimeout() throws Exception {
        assertThat(sut.await(2, TimeUnit.MILLISECONDS)).isFalse();
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

    private enum MyKeywords implements ProtocolKeyword {
        DUMMY;

        @Override
        public byte[] getBytes() {
            return name().getBytes();
        }
    }
}
