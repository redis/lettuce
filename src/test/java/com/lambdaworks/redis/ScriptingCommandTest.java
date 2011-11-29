// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ScriptingCommandTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testName() throws Exception {
        assertEquals(list(1L, "one", list(2L, "two", list(3L), 4L), "five"), redis.eval("return {1, 'one', {2, 'two', {3}, 4}, 'five'}", List.class));
        System.out.printf("foo %s\n", redis.eval("return {{err='foo'}, 1, {2, {err='hi'}}}", List.class));
        System.out.printf("foo %s\n", redis.eval("return {blah, 1}", List.class));
        System.out.printf("foo %s\n", redis.evalsha("asdf", List.class));
        System.out.printf("foo %s\n", redis.eval("return 1 + 1 == 4", Long.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void eval() throws Exception {
        assertEquals(2L, (long) redis.eval("return 1 + 1", Long.class));
        assertEquals("one", redis.eval("return 'one'", String.class));
        assertEquals(list(1L, "one", list(2L)), redis.eval("return {1, 'one', {2}}", List.class));
        assertEquals("status", redis.eval("return {ok='status'}", String.class));
        assertEquals(list(false), redis.eval("return 1 + 1 == 4", List.class));
        exception.expectMessage("Oops!");
        redis.eval("return {err='Oops!'}", String.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void evalsha() throws Exception {
        redis.scriptFlush();
        String script = "return 1 + 1";
        String digest = redis.digest(script);
        assertEquals(2L, (long) redis.eval(script, Long.class));
        assertEquals(2L, (long) redis.evalsha(digest, Long.class));
        exception.expectMessage("NOSCRIPT No matching script. Please use EVAL.");
        redis.evalsha(redis.digest("return 1 + 1 == 4"), Long.class);
    }

    @Test
    public void script() throws Exception {
        assertEquals("OK", redis.scriptFlush());

        String script1 = "return 1 + 1";
        String digest1 = redis.digest(script1);
        String script2 = "return 1 + 1 == 4";
        String digest2 = redis.digest(script2);

        assertEquals(list(false, false), redis.scriptExists(digest1, digest2));
        assertEquals(digest1, redis.scriptLoad(script1));
        assertEquals(2L, (long) redis.evalsha(digest1, Long.class));
        assertEquals(list(true, false), redis.scriptExists(digest1, digest2));

        assertEquals("OK", redis.scriptFlush());
        assertEquals(list(false, false), redis.scriptExists(digest1, digest2));
    }
}
