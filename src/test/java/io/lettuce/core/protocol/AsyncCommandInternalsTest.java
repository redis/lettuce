/*
 * Copyright 2011-2016 the original author or authors.
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

import static io.lettuce.core.protocol.LettuceCharsets.buffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import io.lettuce.core.*;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;

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
        LettuceFutures.awaitOrCancel(sut, 0, TimeUnit.NANOSECONDS);
    }

    @Test(expected = RedisException.class)
    public void awaitWithExecutionException() throws Exception {
        sut.completeExceptionally(new RedisException("error"));
        LettuceFutures.awaitOrCancel(sut, 1, TimeUnit.SECONDS);
    }

    @Test(expected = CancellationException.class)
    public void awaitWithCancelledCommand() throws Exception {
        sut.cancel();
        LettuceFutures.awaitOrCancel(sut, 5, TimeUnit.SECONDS);
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
        sut = new AsyncCommand<>(
                new Command<String, String, String>(MyKeywords.DUMMY, new StatusOutput<String, String>(codec), null));

        assertThat(sut.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    public void customKeywordWithArgs() throws Exception {
        sut = new AsyncCommand<>(
                new Command<String, String, String>(MyKeywords.DUMMY, null, new CommandArgs<String, String>(codec)));
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
