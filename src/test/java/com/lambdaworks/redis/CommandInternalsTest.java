// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.lambdaworks.redis.protocol.Charsets.buffer;
import static junit.framework.Assert.*;

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
        assertFalse(command.isCancelled());
        assertTrue(command.cancel(true));
        assertTrue(command.isCancelled());
        assertFalse(command.cancel(true));
    }

    @Test
    public void isDone() throws Exception {
        assertFalse(command.isDone());
        command.complete();
        assertTrue(command.isDone());
    }

    @Test
    public void get() throws Exception {
        command.getOutput().set(buffer("one"));
        command.complete();
        assertEquals("one", command.get());
    }

    @Test
    public void getWithTimeout() throws Exception {
        command.getOutput().set(buffer("one"));
        command.complete();
        assertEquals("one", command.get(0, TimeUnit.MILLISECONDS));
    }

    @Test(expected = TimeoutException.class, timeout = 10)
    public void getTimeout() throws Exception {
        assertNull(command.get(2, TimeUnit.MICROSECONDS));
    }

    @Test(timeout = 10)
    public void awaitTimeout() throws Exception {
        assertFalse(command.await(2, TimeUnit.MICROSECONDS));
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 10)
    public void getInterrupted() throws Exception {
        Thread.currentThread().interrupt();
        command.get();
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 10)
    public void getInterrupted2() throws Exception {
        Thread.currentThread().interrupt();
        command.get(5, TimeUnit.MILLISECONDS);
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 10)
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
        assertTrue(output.get().get(0) instanceof RedisException);
    }

    @Test
    public void sillyTestsForEmmaCoverage() throws Exception {
        assertEquals(CommandType.APPEND, CommandType.valueOf("APPEND"));
        assertEquals(CommandKeyword.AFTER, CommandKeyword.valueOf("AFTER"));
        assertNotNull(new ZStoreArgs.Builder());
        assertNotNull(new SortArgs.Builder());
        assertNotNull(new Charsets());
    }
}
