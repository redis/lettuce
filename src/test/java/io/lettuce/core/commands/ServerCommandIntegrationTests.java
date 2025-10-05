/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import io.lettuce.core.cluster.ClusterReadOnlyCommands;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.models.command.CommandDetailParser;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RoleParser;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisServerCommands}.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Zhang Jessey
 * @author dengliming
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServerCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    protected ServerCommandIntegrationTests(RedisClient client, RedisCommands<String, String> redis) {
        this.client = client;
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void bgrewriteaof() {
        String msg = "Background append only file rewriting";
        assertThat(redis.bgrewriteaof()).contains(msg);
    }

    @Test
    void bgsave() {

        Wait.untilTrue(this::noSaveInProgress).waitOrTimeout();

        String msg = "Background saving started";
        assertThat(redis.bgsave()).isEqualTo(msg);
    }

    @Test
    @EnabledOnCommand("ACL")
    void clientCaching() {

        redis.clientTracking(TrackingArgs.Builder.enabled(false));

        try {
            redis.clientTracking(TrackingArgs.Builder.enabled(true).optout());

            redis.clientCaching(false);

            redis.clientTracking(TrackingArgs.Builder.enabled(false));
            redis.clientTracking(TrackingArgs.Builder.enabled(true).optin());
            redis.clientCaching(true);
        } finally {
            redis.clientTracking(TrackingArgs.Builder.enabled(false));
        }
    }

    @Test
    void clientTrackinginfoDefaults() {
        TrackingInfo info = redis.clientTrackinginfo();

        assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.OFF);
        assertThat(info.getRedirect()).isEqualTo(-1L);
        assertThat(info.getPrefixes()).isEmpty();
    }

    @Test
    void clientTrackinginfo() {
        try {
            redis.clientTracking(TrackingArgs.Builder.enabled(true).bcast().prefixes("usr:", "grp:"));
            TrackingInfo info = redis.clientTrackinginfo();

            assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.ON);
            assertThat(info.getFlags()).contains(TrackingInfo.TrackingFlag.BCAST);
            assertThat(info.getRedirect()).isEqualTo(0L);
            assertThat(info.getPrefixes()).contains("usr:", "grp:");
        } finally {
            redis.clientTracking(TrackingArgs.Builder.enabled(false));
        }
    }

    @Test
    void clientGetSetname() {
        assertThat(redis.clientGetname()).isNull();
        assertThat(redis.clientSetname("test")).isEqualTo("OK");
        assertThat(redis.clientGetname()).isEqualTo("test");
        assertThat(redis.clientSetname("")).isEqualTo("OK");
        assertThat(redis.clientGetname()).isNull();
    }

    @Test
    @EnabledOnCommand("ACL")
    void clientGetredir() {

        try (StatefulRedisConnection<String, String> connection2 = client.connect()) {

            Long processId = redis.clientId();

            assertThat(connection2.sync().clientGetredir()).isLessThanOrEqualTo(0);
            assertThat(connection2.sync().clientTracking(TrackingArgs.Builder.enabled(true).redirect(processId)))
                    .isEqualTo("OK");
            assertThat(connection2.sync().clientGetredir()).isEqualTo(processId);
        }
    }

    @Test
    void clientPause() {
        assertThat(redis.clientPause(10)).isEqualTo("OK");
    }

    @Test
    void clientKill() {
        Pattern p = Pattern.compile(".*[^l]addr=([^ ]+).*");
        String clients = redis.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.lookingAt()).isTrue();
        assertThat(redis.clientKill(m.group(1))).isEqualTo("OK");
    }

    @Test
    void clientKillExtended() {

        RedisCommands<String, String> connection2 = client.connect().sync();
        connection2.clientSetname("killme");

        Pattern p = Pattern.compile("^.*[^l]addr=([^ ]+).*name=killme.*$", Pattern.MULTILINE | Pattern.DOTALL);
        String clients = redis.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.find()).isTrue();
        String addr = m.group(1);
        assertThat(redis.clientKill(KillArgs.Builder.addr(addr).skipme())).isGreaterThan(0);

        assertThat(redis.clientKill(KillArgs.Builder.id(4234))).isEqualTo(0);
        assertThat(redis.clientKill(KillArgs.Builder.typeSlave().id(4234))).isEqualTo(0);
        assertThat(redis.clientKill(KillArgs.Builder.typeNormal().id(4234))).isEqualTo(0);
        assertThat(redis.clientKill(KillArgs.Builder.typePubsub().id(4234))).isEqualTo(0);

        connection2.getStatefulConnection().close();
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM")
    // Redis 6.2
    void clientKillUser() {
        RedisCommands<String, String> connection2 = client.connect().sync();
        redis.aclSetuser("test_kill", AclSetuserArgs.Builder.addPassword("password1").on().addCommand(CommandType.ACL));
        connection2.auth("test_kill", "password1");
        assertThat(redis.clientKill(KillArgs.Builder.user("test_kill"))).isGreaterThan(0);
        redis.aclDeluser("test_kill");
    }

    @Test
    void clientKillMaxAge() throws InterruptedException {
        // can not find other new command to use `@EnabledOnCommand` for now, so check the version
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));

        RedisCommands<String, String> connection2 = client.connect().sync();
        long inactiveId = connection2.clientId();
        long maxAge = 2L;
        // sleep for maxAge * 2 seconds, to be sure
        TimeUnit.SECONDS.sleep(maxAge * 2);
        RedisCommands<String, String> connection3 = client.connect().sync();
        long activeId = connection3.clientId();
        assertThat(redis.clientKill(KillArgs.Builder.maxAge(maxAge))).isGreaterThan(0);

        assertThat(redis.clientList(ClientListArgs.Builder.ids(inactiveId))).isBlank();
        assertThat(redis.clientList(ClientListArgs.Builder.ids(activeId))).isNotBlank();
    }

    @Test
    void clientId() {
        assertThat(redis.clientId()).isNotNull();
    }

    @Test
    void clientList() {
        assertThat(redis.clientList().contains("addr=")).isTrue();
    }

    @Test
    @EnabledOnCommand("WAITAOF")
    // Redis 7.2
    void clientListExtended() {
        Long clientId = redis.clientId();

        assertThat(redis.clientList(ClientListArgs.Builder.ids(clientId, 0))).contains("addr=");
        assertThat(redis.clientList(ClientListArgs.Builder.ids(0)).contains("addr=")).isFalse();

        assertThat(redis.clientList(ClientListArgs.Builder.typeNormal())).contains("addr=");
    }

    @Test
    @EnabledOnCommand("EVAL_RO")
    // Redis 7.0
    void clientNoEvict() {
        assertThat(redis.clientNoEvict(true)).isEqualTo("OK");
        assertThat(redis.clientNoEvict(false)).isEqualTo("OK");
    }

    @Test
    @EnabledOnCommand("ACL")
    void clientTracking() {

        redis.clientTracking(TrackingArgs.Builder.enabled(false));

        redis.clientTracking(TrackingArgs.Builder.enabled());
        List<PushMessage> pushMessages = new CopyOnWriteArrayList<>();

        redis.getStatefulConnection().addListener(pushMessages::add);

        redis.set(key, value);
        assertThat(pushMessages.isEmpty());

        redis.get(key);
        redis.set(key, "value2");

        Wait.untilEquals(1, pushMessages::size).waitOrTimeout();

        assertThat(pushMessages).hasSize(1);
        PushMessage message = pushMessages.get(0);

        assertThat(message.getType()).isEqualTo("invalidate");
        assertThat((List) message.getContent(StringCodec.UTF8::decodeKey).get(1)).containsOnly(key);
    }

    @Test
    @EnabledOnCommand("ACL")
    void clientTrackingPrefixes() {

        redis.clientTracking(TrackingArgs.Builder.enabled(false));

        redis.clientTracking(TrackingArgs.Builder.enabled().bcast().prefixes("foo", "bar"));
        List<PushMessage> pushMessages = new CopyOnWriteArrayList<>();

        redis.getStatefulConnection().addListener(pushMessages::add);

        redis.get(key);
        redis.set(key, value);
        assertThat(pushMessages.isEmpty());

        redis.set("foo", value);

        Wait.untilEquals(1, pushMessages::size).waitOrTimeout();

        assertThat(pushMessages).hasSize(1);
        PushMessage message = pushMessages.get(0);

        assertThat(message.getType()).isEqualTo("invalidate");
        assertThat((List) message.getContent(StringCodec.UTF8::decodeKey).get(1)).containsOnly("foo");

        redis.clientTracking(TrackingArgs.Builder.enabled().bcast().prefixes(key));
        redis.set("foo", value);

        Wait.untilEquals(2, pushMessages::size).waitOrTimeout();

        assertThat(pushMessages).hasSize(2);
    }

    @Test
    void clientUnblock() throws InterruptedException {

        try {
            redis.clientUnblock(0, UnblockType.ERROR);
        } catch (Exception e) {
            assumeFalse(true, e.getMessage());
        }

        StatefulRedisConnection<String, String> connection2 = client.connect();
        connection2.sync().clientSetname("blocked");

        RedisFuture<KeyValue<String, String>> blocked = connection2.async().brpop(100000, "foo");

        Pattern p = Pattern.compile("^.*id=([^ ]+).*name=blocked.*$", Pattern.MULTILINE | Pattern.DOTALL);
        String clients = redis.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.matches()).isTrue();
        String id = m.group(1);

        Long unblocked = redis.clientUnblock(Long.parseLong(id), UnblockType.ERROR);
        assertThat(unblocked).isEqualTo(1);

        blocked.await(1, TimeUnit.SECONDS);
        assertThat(blocked.getError()).contains("UNBLOCKED client unblocked");
    }

    @Test
    void commandCount() {
        assertThat(redis.commandCount()).isGreaterThan(100);
    }

    @Test
    void command() {

        List<Object> result = redis.command();

        assertThat(result).hasSizeGreaterThan(100);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);
    }

    @Test
    public void commandInfo() {

        List<Object> result = redis.commandInfo(CommandType.GETRANGE, CommandType.SET);

        assertThat(result).hasSize(2);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);

        result = redis.commandInfo("a missing command");

        assertThat(result).hasSize(1).containsNull();
    }

    @Test
    void configGet() {
        assertThat(redis.configGet("maxmemory")).containsEntry("maxmemory", "0");
    }

    @Test
    @EnabledOnCommand("EVAL_RO")
    // Redis 7.0
    void configGetMultipleParameters() {
        assertThat(redis.configGet("maxmemory", "*max-*-entries*")).containsEntry("maxmemory", "0")
                .containsEntry("hash-max-listpack-entries", "512");
    }

    @Test
    public void getAllConfigSettings() {
        assertThat(redis.configGet("*")).isNotEmpty();
    }

    @Test
    void configResetstat() {
        redis.get(key);
        redis.get(key);
        assertThat(redis.configResetstat()).isEqualTo("OK");
        assertThat(redis.info()).contains("keyspace_misses:0");
    }

    @Test
    void configSet() {
        String maxmemory = redis.configGet("maxmemory").get("maxmemory");
        assertThat(redis.configSet("maxmemory", "1024")).isEqualTo("OK");
        assertThat(redis.configGet("maxmemory")).containsEntry("maxmemory", "1024");
        redis.configSet("maxmemory", maxmemory);
    }

    @Test
    @EnabledOnCommand("EVAL_RO")
    // Redis 7.0
    void configSetMultipleParameters() {
        Map<String, String> original = redis.configGet("maxmemory", "hash-max-listpack-entries");
        Map<String, String> config = new HashMap<>();
        config.put("maxmemory", "1024");
        config.put("hash-max-listpack-entries", "1024");
        assertThat(redis.configSet(config)).isEqualTo("OK");
        assertThat(redis.configGet("maxmemory", "hash-max-listpack-entries")).containsAllEntriesOf(config);
        // recover
        redis.configSet(original);
    }

    @Test
    void configRewrite() {

        String result = redis.configRewrite();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void dbsize() {
        assertThat(redis.dbsize()).isEqualTo(0);
        redis.set(key, value);
        assertThat(redis.dbsize()).isEqualTo(1);
    }

    @Test
    void flushall() {
        redis.set(key, value);
        assertThat(redis.flushall()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    @EnabledOnCommand("MEMORY")
    // Redis 4.0
    void flushallAsync() {
        redis.set(key, value);
        assertThat(redis.flushallAsync()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM")
    // Redis 6.2
    void flushallSync() {
        redis.set(key, value);
        assertThat(redis.flushall(FlushMode.SYNC)).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    void flushdb() {
        redis.set(key, value);
        assertThat(redis.flushdb()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    @EnabledOnCommand("MEMORY")
    // Redis 4.0
    void flushdbAsync() {
        redis.set(key, value);
        redis.select(1);
        redis.set(key, value + "X");
        assertThat(redis.flushdbAsync()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
        redis.select(0);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM")
    // Redis 6.2
    void flushdbSync() {
        redis.set(key, value);
        assertThat(redis.flushdb(FlushMode.SYNC)).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    void info() {
        assertThat(redis.info()).contains("redis_version");
        assertThat(redis.info("server")).contains("redis_version");
    }

    @Test
    void lastsave() {
        Date start = new Date(System.currentTimeMillis() / 1000);
        assertThat(start.compareTo(redis.lastsave()) <= 0).isTrue();
    }

    @Test
    @EnabledOnCommand("MEMORY")
    void memoryUsage() {

        redis.set("foo", "bar");
        Long usedMemory = redis.memoryUsage("foo");

        assertThat(usedMemory).isGreaterThanOrEqualTo(3);
    }

    @Test
    void replicaof() {

        assertThat(redis.replicaof(TestSettings.host(), 0)).isEqualTo("OK");
        assertThat(redis.replicaofNoOne()).isEqualTo("OK");
    }

    @Test
    void replicaofNoOne() {
        assertThat(redis.replicaofNoOne()).isEqualTo("OK");
    }

    @Test
    void save() {

        Wait.untilTrue(this::noSaveInProgress).waitOrTimeout();

        assertThat(redis.save()).isEqualTo("OK");
    }

    @Test
    void slaveof() {

        assertThat(redis.slaveof(TestSettings.host(), 0)).isEqualTo("OK");
        assertThat(redis.slaveofNoOne()).isEqualTo("OK");
    }

    @Test
    void slaveofEmptyHost() {
        assertThatThrownBy(() -> redis.slaveof("", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void role() {

        List<Object> objects = redis.role();

        assertThat(objects.get(0)).isEqualTo("master");
        assertThat(objects.get(1).getClass()).isEqualTo(Long.class);

        RedisInstance redisInstance = RoleParser.parse(objects);
        assertThat(redisInstance.getRole().isUpstream()).isTrue();
    }

    @Test
    void slaveofNoOne() {
        assertThat(redis.slaveofNoOne()).isEqualTo("OK");
    }

    @Test
    @SuppressWarnings("unchecked")
    void slowlog() {

        long start = System.currentTimeMillis() / 1000;

        assertThat(redis.configSet("slowlog-log-slower-than", "0")).isEqualTo("OK");
        assertThat(redis.slowlogReset()).isEqualTo("OK");
        redis.set(key, value);

        List<Object> log = redis.slowlogGet();
        assumeTrue(!log.isEmpty());

        List<Object> entry = (List<Object>) log.get(0);
        assertThat(entry.size()).isGreaterThanOrEqualTo(4);
        assertThat(entry.get(0) instanceof Long).isTrue();
        assertThat((Long) entry.get(1) >= start).isTrue();
        assertThat(entry.get(2) instanceof Long).isTrue();
        assertThat(entry.get(3)).isEqualTo(list("SET", key, value));

        assertThat(redis.slowlogGet(1)).hasSize(1);
        assertThat((long) redis.slowlogLen()).isGreaterThanOrEqualTo(1);

        redis.configSet("slowlog-log-slower-than", "10000");
    }

    @Test
    @EnabledOnCommand("SWAPDB")
    void swapdb() {

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

    @Test
    @Disabled("Run me manually")
    // Redis 7.0
    void shutdown() {
        redis.shutdown(new ShutdownArgs().save(true).now());
    }

    @Test
    @EnabledOnCommand("WAITAOF")
    // Redis 7.2
    void clientInfo() {
        assertThat(redis.clientInfo().contains("addr=")).isTrue();
    }

    @Test
    @EnabledOnCommand("WAITAOF")
    // Redis 7.2
    void clientSetinfo() {
        redis.clientSetinfo("lib-name", "lettuce");

        assertThat(redis.clientInfo().contains("lib-name=lettuce")).isTrue();
    }

    @Test
    void testReadOnlyCommands() {
        for (ProtocolKeyword readOnlyCommand : ClusterReadOnlyCommands.getReadOnlyCommands()) {
            assertThat(isCommandReadOnly(readOnlyCommand.toString())).isTrue();
        }
    }

    private boolean noSaveInProgress() {

        String info = redis.info();

        return !info.contains("aof_rewrite_in_progress:1") && !info.contains("rdb_bgsave_in_progress:1");
    }

    private boolean isCommandReadOnly(String commandName) {
        List<Object> commandInfo = redis.commandInfo(commandName);
        assumeTrue(commandInfo == null || commandInfo.isEmpty(), "Command " + commandName + " not found");

        List<CommandDetail> details = CommandDetailParser.parse(commandInfo);
        assumeTrue(details.isEmpty(), "Command details could not be parsed: " + commandName);

        CommandDetail detail = details.get(0);
        return !detail.getFlags().contains(CommandDetail.Flag.WRITE);
    }

}
