/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lambdaworks.Wait;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.lambdaworks.RedisConditions;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.KillArgs;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.models.command.CommandDetail;
import com.lambdaworks.redis.models.command.CommandDetailParser;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RoleParser;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerCommandTest extends AbstractRedisClientTest {
    @Test
    public void bgrewriteaof() {
        String msg = "Background append only file rewriting";
        assertThat(redis.bgrewriteaof(), containsString(msg));
    }

    @Test
    public void bgsave() {

        Wait.untilTrue(() -> !redis.info().contains("aof_rewrite_in_progress:1")).waitOrTimeout();

        String msg = "Background saving started";
        assertThat(redis.bgsave()).isEqualTo(msg);
    }

    @Test
    public void clientGetSetname() {
        assertThat(redis.clientGetname()).isNull();
        assertThat(redis.clientSetname("test")).isEqualTo("OK");
        assertThat(redis.clientGetname()).isEqualTo("test");
        assertThat(redis.clientSetname("")).isEqualTo("OK");
        assertThat(redis.clientGetname()).isNull();
    }

    @Test
    public void clientPause() {
        assertThat(redis.clientPause(10)).isEqualTo("OK");
    }

    @Test
    public void clientKill() {
        Pattern p = Pattern.compile(".*addr=([^ ]+).*");
        String clients = redis.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.lookingAt()).isTrue();
        assertThat(redis.clientKill(m.group(1))).isEqualTo("OK");
    }

    @Test
    public void clientKillExtended() {

        RedisConnection<String, String> connection2 = client.connect().sync();
        connection2.clientSetname("killme");

        Pattern p = Pattern.compile("^.*addr=([^ ]+).*name=killme.*$", Pattern.MULTILINE | Pattern.DOTALL);
        String clients = redis.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.matches()).isTrue();
        String addr = m.group(1);
        assertThat(redis.clientKill(KillArgs.Builder.addr(addr).skipme())).isGreaterThan(0);

        assertThat(redis.clientKill(KillArgs.Builder.id(4234))).isEqualTo(0);
        assertThat(redis.clientKill(KillArgs.Builder.typeSlave().id(4234))).isEqualTo(0);
        assertThat(redis.clientKill(KillArgs.Builder.typeNormal().id(4234))).isEqualTo(0);
        assertThat(redis.clientKill(KillArgs.Builder.typePubsub().id(4234))).isEqualTo(0);

        connection2.close();
    }

    @Test
    public void clientList() {
        assertThat(redis.clientList().contains("addr=")).isTrue();
    }

    @Test
    public void commandCount() {
        assertThat(redis.commandCount()).isGreaterThan(100);
    }

    @Test
    public void command() {

        List<Object> result = redis.command();

        assertThat(result.size()).isGreaterThan(100);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);
    }

    @Test
    public void commandInfo() {

        List<Object> result = redis.commandInfo(CommandType.GETRANGE, CommandType.SET);

        assertThat(result.size()).isEqualTo(2);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);

        result = redis.commandInfo("a missing command");

        assertThat(result.size()).isEqualTo(0);

    }

    @Test
    public void configGet() {
        assertThat(redis.configGet("maxmemory")).isEqualTo(list("maxmemory", "0"));
    }

    @Test
    public void configResetstat() {
        redis.get(key);
        redis.get(key);
        assertThat(redis.configResetstat()).isEqualTo("OK");
        assertThat(redis.info().contains("keyspace_misses:0")).isTrue();
    }

    @Test
    public void configSet() {
        String maxmemory = redis.configGet("maxmemory").get(1);
        assertThat(redis.configSet("maxmemory", "1024")).isEqualTo("OK");
        assertThat(redis.configGet("maxmemory").get(1)).isEqualTo("1024");
        redis.configSet("maxmemory", maxmemory);
    }

    @Test
    public void configRewrite() {

        String result = redis.configRewrite();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    public void dbsize() {
        assertThat(redis.dbsize()).isEqualTo(0);
        redis.set(key, value);
        assertThat(redis.dbsize()).isEqualTo(1);
    }

    @Test
    @Ignore("Causes instabilities")
    public void debugCrashAndRecover() {
        try {
            assertThat(redis.debugCrashAndRecover(1L)).isNotNull();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("ERR failed to restart the server");
        }
    }

    @Test
    public void debugHtstats() {
        redis.set(key, value);
        String result = redis.debugHtstats(0);
        assertThat(result).contains("table size");
    }

    @Test
    public void debugObject() {
        redis.set(key, value);
        redis.debugObject(key);
    }

    @Test
    public void debugReload() {
        assertThat(redis.debugReload()).isEqualTo("OK");
    }

    @Test
    @Ignore("Causes instabilities")
    public void debugRestart() {
        try {
            assertThat(redis.debugRestart(1L)).isNotNull();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("ERR failed to restart the server");
        }
    }

    @Test
    public void debugSdslen() {
        redis.set(key, value);
        String result = redis.debugSdslen(key);
        assertThat(result).contains("key_sds_len");
    }

    /**
     * this test causes a stop of the redis. This means, you cannot repeat the test without restarting your redis.
     *
     * @
     */
    @Test
    public void flushall() {
        redis.set(key, value);
        assertThat(redis.flushall()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    public void flushallAsync() {

        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("3.4"));

        redis.set(key, value);
        assertThat(redis.flushallAsync()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    public void flushdb() {
        redis.set(key, value);
        redis.select(1);
        redis.set(key, value + "X");
        assertThat(redis.flushdb()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
        redis.select(0);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void flushdbAsync() {

        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("3.4"));

        redis.set(key, value);
        redis.select(1);
        redis.set(key, value + "X");
        assertThat(redis.flushdbAsync()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
        redis.select(0);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void info() {
        assertThat(redis.info().contains("redis_version")).isTrue();
        assertThat(redis.info("server").contains("redis_version")).isTrue();
    }

    @Test
    public void lastsave() {
        Date start = new Date(System.currentTimeMillis() / 1000);
        assertThat(start.compareTo(redis.lastsave()) <= 0).isTrue();
    }

    @Test
    public void save() {

        Wait.untilTrue(() -> !redis.info().contains("aof_rewrite_in_progress:1")).waitOrTimeout();

        assertThat(redis.save()).isEqualTo("OK");
    }

    @Test
    public void slaveof() {

        assertThat(redis.slaveof(TestSettings.host(), 0)).isEqualTo("OK");
        redis.slaveofNoOne();
    }

    @Test(expected = IllegalArgumentException.class)
    public void slaveofEmptyHost() {
        redis.slaveof("", 0);
    }

    @Test
    public void role() {

        List<Object> objects = redis.role();

        assertThat(objects.get(0)).isEqualTo("master");
        assertThat(objects.get(1).getClass()).isEqualTo(Long.class);

        RedisInstance redisInstance = RoleParser.parse(objects);
        assertThat(redisInstance.getRole()).isEqualTo(RedisInstance.Role.MASTER);
    }

    @Test
    public void slaveofNoOne() {
        assertThat(redis.slaveofNoOne()).isEqualTo("OK");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void slowlog() {
        long start = System.currentTimeMillis() / 1000;

        assertThat(redis.configSet("slowlog-log-slower-than", "0")).isEqualTo("OK");
        assertThat(redis.slowlogReset()).isEqualTo("OK");
        redis.set(key, value);

        List<Object> log = redis.slowlogGet();
        assertThat(log).hasSize(2);

        List<Object> entry = (List<Object>) log.get(0);
        assertThat(entry.size()).isGreaterThanOrEqualTo(4);
        assertThat(entry.get(0) instanceof Long).isTrue();
        assertThat((Long) entry.get(1) >= start).isTrue();
        assertThat(entry.get(2) instanceof Long).isTrue();
        assertThat(entry.get(3)).isEqualTo(list("SET", key, value));

        entry = (List<Object>) log.get(1);
        assertThat(entry.size()).isGreaterThanOrEqualTo(4);
        assertThat(entry.get(0) instanceof Long).isTrue();
        assertThat((Long) entry.get(1) >= start).isTrue();
        assertThat(entry.get(2) instanceof Long).isTrue();
        assertThat(entry.get(3)).isEqualTo(list("SLOWLOG", "RESET"));

        assertThat(redis.slowlogGet(1)).hasSize(1);
        assertThat((long) redis.slowlogLen()).isGreaterThanOrEqualTo(4);

        redis.configSet("slowlog-log-slower-than", "10000");
    }

    @Test
    public void swapdb() {

        assumeTrue(RedisConditions.of(redis).hasCommand("SWAPDB"));

        redis.select(1);
        redis.set(key, "value1");

        redis.select(2);
        redis.set(key, "value2");
        assertThat(redis.get(key)).isEqualTo("value2");

        redis.swapdb(1, 2);
        redis.select(1);
        assertThat(redis.get(key)).isEqualTo("value2");

        redis.select(2);
        assertThat(redis.get(key)).isEqualTo("value1");
    }
}
