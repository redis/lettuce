/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.commands;

import static io.lettuce.core.ScriptOutputType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScriptingCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    protected ScriptingCommandIntegrationTests(RedisClient client, RedisCommands<String, String> redis) {
        this.client = client;
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @AfterEach
    void tearDown() {

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
    void eval() {
        assertThat((Boolean) redis.eval("return 1 + 1 == 4", BOOLEAN)).isEqualTo(false);
        assertThat((Number) redis.eval("return 1 + 1", INTEGER)).isEqualTo(2L);
        assertThat((String) redis.eval("return {ok='status'}", STATUS)).isEqualTo("status");
        assertThat((String) redis.eval("return 'one'", VALUE)).isEqualTo("one");
        assertThat((List<?>) redis.eval("return {1, 'one', {2}}", MULTI)).isEqualTo(list(1L, "one", list(2L)));

        assertThatThrownBy(() -> redis.eval("return {err='Oops!'}", STATUS)).hasMessageContaining("Oops!");
    }

    @Test
    void evalWithSingleKey() {
        assertThat((List<?>) redis.eval("return KEYS[1]", MULTI, "one")).isEqualTo(list("one"));
    }

    @Test
    void evalWithNonAsciiChar() {
        assertThat((Object) redis.eval("return 'füö'", VALUE, "one")).isEqualTo("füö");
    }

    @Test
    void evalReturningNullInMulti() {
        assertThat((List<?>) redis.eval("return nil", MULTI, "one")).isEqualTo(Collections.singletonList(null));
    }

    @Test
    void evalWithKeys() {
        assertThat((List<?>) redis.eval("return {KEYS[1], KEYS[2]}", MULTI, "one", "two")).isEqualTo(list("one", "two"));
    }

    @Test
    void evalWithArgs() {
        String[] keys = new String[0];
        assertThat((List<?>) redis.eval("return {ARGV[1], ARGV[2]}", MULTI, keys, "a", "b")).isEqualTo(list("a", "b"));
    }

    @Test
    void evalsha() {
        redis.scriptFlush();
        String script = "return 1 + 1";
        String digest = redis.digest(script);
        assertThat((Number) redis.eval(script, INTEGER)).isEqualTo(2L);
        assertThat((Number) redis.evalsha(digest, INTEGER)).isEqualTo(2L);

        assertThatThrownBy(() -> redis.evalsha(redis.digest("return 1 + 1 == 4"), INTEGER))
                .isInstanceOf(RedisNoScriptException.class)
                .hasMessageContaining("NOSCRIPT No matching script. Please use EVAL.");
    }

    @Test
    void evalshaWithMulti() {
        redis.scriptFlush();
        String digest = redis.digest("return {1234, 5678}");

        assertThatThrownBy(() -> redis.evalsha(digest, MULTI)).isInstanceOf(RedisNoScriptException.class)
                .hasMessageContaining("NOSCRIPT No matching script. Please use EVAL.");
    }

    @Test
    void evalshaWithKeys() {
        redis.scriptFlush();
        String digest = redis.scriptLoad("return {KEYS[1], KEYS[2]}");
        assertThat((Object) redis.evalsha(digest, MULTI, "one", "two")).isEqualTo(list("one", "two"));
    }

    @Test
    void evalshaWithArgs() {
        redis.scriptFlush();
        String digest = redis.scriptLoad("return {ARGV[1], ARGV[2]}");
        String[] keys = new String[0];
        assertThat((Object) redis.evalsha(digest, MULTI, keys, "a", "b")).isEqualTo(list("a", "b"));
    }

    @Test
    void script() throws InterruptedException {
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
            connection.async().eval("while true do end", STATUS, new String[0]).await(100, TimeUnit.MILLISECONDS);

            assertThat(redis.scriptKill()).isEqualTo("OK");
        } finally {
            connection.close();
        }
    }

}
