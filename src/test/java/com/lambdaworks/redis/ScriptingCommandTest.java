// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.lambdaworks.redis.ScriptOutputType.*;
import static org.junit.Assert.assertEquals;

public class ScriptingCommandTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    @SuppressWarnings("unchecked")
    public void eval() throws Exception {
        assertEquals(false, redis.eval("return 1 + 1 == 4", BOOLEAN));
        assertEquals(2L, redis.eval("return 1 + 1", INTEGER));
        assertEquals("status", redis.eval("return {ok='status'}", STATUS));
        assertEquals("one", redis.eval("return 'one'", VALUE));
        assertEquals(list(1L, "one", list(2L)), redis.eval("return {1, 'one', {2}}", MULTI));
        exception.expectMessage("Oops!");
        redis.eval("return {err='Oops!'}", STATUS);
    }

    @Test
    public void evalWithKeys() throws Exception {
        assertEquals(list("one", "two"), redis.eval("return {KEYS[1], KEYS[2]}", MULTI, "one", "two"));
    }

    @Test
    public void evalWithArgs() throws Exception {
        String[] keys = new String[0];
        assertEquals(list("a", "b"), redis.eval("return {ARGV[1], ARGV[2]}", MULTI, keys, "a", "b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void evalsha() throws Exception {
        redis.scriptFlush();
        String script = "return 1 + 1";
        String digest = redis.digest(script);
        assertEquals(2L, redis.eval(script, INTEGER));
        assertEquals(2L, redis.evalsha(digest, INTEGER));
        exception.expectMessage("NOSCRIPT No matching script. Please use EVAL.");
        redis.evalsha(redis.digest("return 1 + 1 == 4"), INTEGER);
    }

    @Test
    public void evalshaWithKeys() throws Exception {
        redis.scriptFlush();
        String digest = redis.scriptLoad("return {KEYS[1], KEYS[2]}");
        assertEquals(list("one", "two"), redis.evalsha(digest, MULTI, "one", "two"));
    }

    @Test
    public void evalshaWithArgs() throws Exception {
        redis.scriptFlush();
        String digest = redis.scriptLoad("return {ARGV[1], ARGV[2]}");
        String[] keys = new String[0];
        assertEquals(list("a", "b"), redis.evalsha(digest, MULTI, keys, "a", "b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void script() throws Exception {
        assertEquals("OK", redis.scriptFlush());

        String script1 = "return 1 + 1";
        String digest1 = redis.digest(script1);
        String script2 = "return 1 + 1 == 4";
        String digest2 = redis.digest(script2);

        assertEquals(list(false, false), redis.scriptExists(digest1, digest2));
        assertEquals(digest1, redis.scriptLoad(script1));
        assertEquals(2L, redis.evalsha(digest1, INTEGER));
        assertEquals(list(true, false), redis.scriptExists(digest1, digest2));

        assertEquals("OK", redis.scriptFlush());
        assertEquals(list(false, false), redis.scriptExists(digest1, digest2));

        redis.configSet("lua-time-limit", "10");
        RedisAsyncConnection<String, String> async = client.connectAsync();
        try {
            async.eval("while true do end", STATUS, new String[0]);
            assertEquals("OK", redis.scriptKill());
        } finally {
            async.close();
        }
    }
}
