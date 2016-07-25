// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.ScriptOutputType.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;

import com.lambdaworks.Wait;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.StatefulRedisConnection;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScriptingCommandTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void tearDown() throws Exception {

        Wait.untilNoException(() -> {
            try {
                redis.scriptKill();
            } catch (RedisException e) {
                // ignore
            }
            redis.ping();
        }).waitOrTimeout();

    }

    @Test
    public void eval() throws Exception {
        assertThat((Boolean) redis.eval("return 1 + 1 == 4", BOOLEAN)).isEqualTo(false);
        assertThat((Number) redis.eval("return 1 + 1", INTEGER)).isEqualTo(2L);
        assertThat((String) redis.eval("return {ok='status'}", STATUS)).isEqualTo("status");
        assertThat((String) redis.eval("return 'one'", VALUE)).isEqualTo("one");
        assertThat((List<?>) redis.eval("return {1, 'one', {2}}", MULTI)).isEqualTo(list(1L, "one", list(2L)));
        exception.expectMessage("Oops!");
        redis.eval("return {err='Oops!'}", STATUS);
    }

    @Test
    public void evalWithKeys() throws Exception {
        assertThat((List<?>) redis.eval("return {KEYS[1], KEYS[2]}", MULTI, "one", "two")).isEqualTo(list("one", "two"));
    }

    @Test
    public void evalWithArgs() throws Exception {
        String[] keys = new String[0];
        assertThat((List<?>) redis.eval("return {ARGV[1], ARGV[2]}", MULTI, keys, "a", "b")).isEqualTo(list("a", "b"));
    }

    @Test
    public void evalsha() throws Exception {
        redis.scriptFlush();
        String script = "return 1 + 1";
        String digest = redis.digest(script);
        assertThat((Number) redis.eval(script, INTEGER)).isEqualTo(2L);
        assertThat((Number) redis.evalsha(digest, INTEGER)).isEqualTo(2L);
        exception.expectMessage("NOSCRIPT No matching script. Please use EVAL.");
        redis.evalsha(redis.digest("return 1 + 1 == 4"), INTEGER);
    }

    @Test
    public void evalshaWithMulti() throws Exception {
        redis.scriptFlush();
        String digest = redis.digest("return {1234, 5678}");
        exception.expectMessage("NOSCRIPT No matching script. Please use EVAL.");
        redis.evalsha(digest, MULTI);
    }

    @Test
    public void evalshaWithKeys() throws Exception {
        redis.scriptFlush();
        String digest = redis.scriptLoad("return {KEYS[1], KEYS[2]}");
        assertThat((Object) redis.evalsha(digest, MULTI, "one", "two")).isEqualTo(list("one", "two"));
    }

    @Test
    public void evalshaWithArgs() throws Exception {
        redis.scriptFlush();
        String digest = redis.scriptLoad("return {ARGV[1], ARGV[2]}");
        String[] keys = new String[0];
        assertThat((Object) redis.evalsha(digest, MULTI, keys, "a", "b")).isEqualTo(list("a", "b"));
    }

    @Test
    public void script() throws Exception {
        assertThat(redis.scriptFlush()).isEqualTo("OK");

        String script1 = "return 1 + 1";
        String digest1 = redis.digest(script1);
        String script2 = "return 1 + 1 == 4";
        String digest2 = redis.digest(script2);

        assertThat(redis.scriptExists(digest1, digest2)).isEqualTo(list(false, false));
        assertThat(redis.scriptLoad(script1)).isEqualTo(digest1);
        assertThat((Object) redis.evalsha(digest1, INTEGER)).isEqualTo(2L);
        assertThat(redis.scriptExists(digest1, digest2)).isEqualTo(list(true, false));

        assertThat(redis.scriptFlush()).isEqualTo("OK");
        assertThat(redis.scriptExists(digest1, digest2)).isEqualTo(list(false, false));

        redis.configSet("lua-time-limit", "10");
        StatefulRedisConnection<String, String> connection = client.connect();
        try {
            connection.async().eval("while true do end", STATUS, new String[0]);
            Thread.sleep(100);
            assertThat(redis.scriptKill()).isEqualTo("OK");
        } finally {
            connection.close();
        }
    }
}
