// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
import static com.lambdaworks.redis.TestSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.models.command.CommandDetail;
import com.lambdaworks.redis.models.command.CommandDetailParser;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RoleParser;
import com.lambdaworks.redis.protocol.CommandType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerCommandTest extends AbstractCommandTest {
    @Test
    public void bgrewriteaof() throws Exception {
        String msg = "Background append only file rewriting";
        assertThat(redis.bgrewriteaof(), containsString(msg));
    }

    @Test
    public void bgsave() throws Exception {
        while (redis.info().contains("aof_rewrite_in_progress:1")) {
            Thread.sleep(100);
        }
        String msg = "Background saving started";
        assertThat(redis.bgsave()).isEqualTo(msg);
    }

    @Test
    public void clientGetSetname() throws Exception {
        assertThat(redis.clientGetname()).isNull();
        assertThat(redis.clientSetname("test")).isEqualTo("OK");
        assertThat(redis.clientGetname()).isEqualTo("test");
        assertThat(redis.clientSetname("")).isEqualTo("OK");
        assertThat(redis.clientGetname()).isNull();
    }

    @Test
    public void clientPause() throws Exception {
        assertThat(redis.clientPause(1000)).isEqualTo("OK");
    }

    @Test
    public void clientKill() throws Exception {
        Pattern p = Pattern.compile(".*addr=([^ ]+).*");
        String clients = redis.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.lookingAt()).isTrue();
        assertThat(redis.clientKill(m.group(1))).isEqualTo("OK");
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientKillEmpty() throws Exception {
        redis.clientKill("");
    }

    @Test
    public void clientKillExtended() throws Exception {

        RedisConnection<String, String> connection2 = client.connect();
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
    public void clientList() throws Exception {
        assertThat(redis.clientList().contains("addr=")).isTrue();
    }

    @Test
    public void commandCount() throws Exception {
        assertThat(redis.commandCount()).isGreaterThan(100);
    }

    @Test
    public void command() throws Exception {

        List<Object> result = redis.command();

        assertThat(result.size()).isGreaterThan(100);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);
    }

    @Test
    public void commandInfo() throws Exception {

        List<Object> result = redis.commandInfo(CommandType.GETRANGE, CommandType.SET);

        assertThat(result.size()).isEqualTo(2);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);

        result = redis.commandInfo("a missing command");

        assertThat(result.size()).isEqualTo(0);

    }

    @Test
    public void configGet() throws Exception {
        assertThat(redis.configGet("maxmemory")).isEqualTo(list("maxmemory", "0"));
    }

    @Test
    public void configResetstat() throws Exception {
        redis.get(key);
        redis.get(key);
        assertThat(redis.configResetstat()).isEqualTo("OK");
        assertThat(redis.info().contains("keyspace_misses:0")).isTrue();
    }

    @Test
    public void configSet() throws Exception {
        String maxmemory = redis.configGet("maxmemory").get(1);
        assertThat(redis.configSet("maxmemory", "1024")).isEqualTo("OK");
        assertThat(redis.configGet("maxmemory").get(1)).isEqualTo("1024");
        redis.configSet("maxmemory", maxmemory);
    }

    @Test
    public void configRewrite() throws Exception {

        String result = redis.configRewrite();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    public void dbsize() throws Exception {
        assertThat(redis.dbsize()).isEqualTo(0);
        redis.set(key, value);
        assertThat(redis.dbsize()).isEqualTo(1);
    }

    @Test
    public void debugObject() throws Exception {
        redis.set(key, value);
        redis.debugObject(key);
    }

    /**
     * this test causes a stop of the redis. This means, you cannot repeat the test without restarting your redis.
     *
     * @throws Exception
     */
    @Test
    public void debugSegfault() throws Exception {
        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(3))
                .build());
        connection.debugSegfault();
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return !connection.isOpen();
            }
        }, timeout(seconds(5)));
        assertThat(connection.isOpen()).isFalse();
    }

    @Test
    public void debugHtstats() throws Exception {
        redis.set(key, value);
        String result = redis.debugHtstats(0);
        assertThat(result).contains("table size");
    }

    @Test
    public void flushall() throws Exception {
        redis.set(key, value);
        assertThat(redis.flushall()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    public void flushdb() throws Exception {
        redis.set(key, value);
        redis.select(1);
        redis.set(key, value + "X");
        assertThat(redis.flushdb()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
        redis.select(0);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void info() throws Exception {
        assertThat(redis.info().contains("redis_version")).isTrue();
        assertThat(redis.info("server").contains("redis_version")).isTrue();
    }

    @Test
    public void lastsave() throws Exception {
        Date start = new Date(System.currentTimeMillis() / 1000);
        assertThat(start.compareTo(redis.lastsave()) <= 0).isTrue();
    }

    @Test
    public void save() throws Exception {

        while (redis.info().contains("aof_rewrite_in_progress:1")) {
            Thread.sleep(100);
        }
        assertThat(redis.save()).isEqualTo("OK");
    }

    @Test
    public void slaveof() throws Exception {
        assertThat(redis.slaveof(TestSettings.host(), 0)).isEqualTo("OK");
        redis.slaveofNoOne();
    }

    @Test(expected = IllegalArgumentException.class)
    public void slaveofEmptyHost() throws Exception {
        redis.slaveof("", 0);
    }

    @Test
    public void role() throws Exception {

        RedisClient redisClient = new RedisClient(host(), port(1));
        RedisAsyncConnection<String, String> connection = redisClient.connectAsync();
        try {

            RedisFuture<List<Object>> role = connection.role();
            List<Object> objects = role.get();

            assertThat(objects.get(0)).isEqualTo("master");
            assertThat(objects.get(1).getClass()).isEqualTo(Long.class);

            RedisInstance redisInstance = RoleParser.parse(objects);
            assertThat(redisInstance.getRole()).isEqualTo(RedisInstance.Role.MASTER);
        } finally {
            connection.close();
            FastShutdown.shutdown(redisClient);
        }
    }

    /**
     * this test causes a stop of the redis. This means, you cannot repeat the test without restarting your redis.
     * 
     * @throws Exception
     */
    @Test
    public void shutdown() throws Exception {

        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(4))
                .build());
        try {

            connection.shutdown(true);
            connection.shutdown(false);
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return !connection.isOpen();
                }
            }, timeout(seconds(5)));

            assertThat(connection.isOpen()).isFalse();

        } finally {
            connection.close();
        }

    }

    @Test
    public void slaveofNoOne() throws Exception {
        assertThat(redis.slaveofNoOne()).isEqualTo("OK");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void slowlog() throws Exception {
        long start = System.currentTimeMillis() / 1000;

        assertThat(redis.configSet("slowlog-log-slower-than", "0")).isEqualTo("OK");
        assertThat(redis.slowlogReset()).isEqualTo("OK");
        redis.set(key, value);

        List<Object> log = redis.slowlogGet();
        assertThat(log).hasSize(2);

        List<Object> entry = (List<Object>) log.get(0);
        assertThat(entry).hasSize(4);
        assertThat(entry.get(0) instanceof Long).isTrue();
        assertThat((Long) entry.get(1) >= start).isTrue();
        assertThat(entry.get(2) instanceof Long).isTrue();
        assertThat(entry.get(3)).isEqualTo(list("SET", key, value));

        entry = (List<Object>) log.get(1);
        assertThat(entry).hasSize(4);
        assertThat(entry.get(0) instanceof Long).isTrue();
        assertThat((Long) entry.get(1) >= start).isTrue();
        assertThat(entry.get(2) instanceof Long).isTrue();
        assertThat(entry.get(3)).isEqualTo(list("SLOWLOG", "RESET"));

        assertThat(redis.slowlogGet(1)).hasSize(1);
        assertThat((long) redis.slowlogLen()).isGreaterThanOrEqualTo(4);

        redis.configSet("slowlog-log-slower-than", "10000");
    }

    @Test
    public void sync() throws Exception {
        assertThat(redis.sync().startsWith("REDIS")).isTrue();
    }

    @Test
    public void migrate() throws Exception {
        redis.set(key, value);

        String result = redis.migrate("localhost", port + 1, key, 0, 10);
        assertThat(result).isEqualTo("OK");
    }
}
