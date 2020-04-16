/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.RedisBug;
import io.lettuce.core.KillArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
public class SentinelServerCommandIntegrationTests extends TestSupport {

    private final RedisClient redisClient;
    private StatefulRedisSentinelConnection<String, String> connection;
    private RedisSentinelCommands<String, String> sentinel;

    @Inject
    public SentinelServerCommandIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @BeforeEach
    void before() {

        this.connection = this.redisClient.connectSentinel(SentinelTestSettings.SENTINEL_URI);
        this.sentinel = getSyncConnection(this.connection);
    }

    protected RedisSentinelCommands<String, String> getSyncConnection(
            StatefulRedisSentinelConnection<String, String> connection) {
        return connection.sync();
    }

    @AfterEach
    void after() {
        this.connection.close();
    }

    @Test
    public void clientGetSetname() {
        assertThat(sentinel.clientGetname()).isNull();
        assertThat(sentinel.clientSetname("test")).isEqualTo("OK");
        assertThat(sentinel.clientGetname()).isEqualTo("test");
        assertThat(sentinel.clientSetname("")).isEqualTo("OK");
        assertThat(sentinel.clientGetname()).isNull();
    }

    @Test
    public void clientPause() {
        assertThat(sentinel.clientPause(10)).isEqualTo("OK");
    }

    @Test
    public void clientKill() {
        Pattern p = Pattern.compile(".*addr=([^ ]+).*");
        String clients = sentinel.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.lookingAt()).isTrue();
        assertThat(sentinel.clientKill(m.group(1))).isEqualTo("OK");
    }

    @Test
    public void clientKillExtended() {

        RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), SentinelTestSettings.MASTER_ID).build();
        RedisSentinelCommands<String, String> connection2 = redisClient.connectSentinel(redisURI).sync();
        connection2.clientSetname("killme");

        Pattern p = Pattern.compile("^.*addr=([^ ]+).*name=killme.*$", Pattern.MULTILINE | Pattern.DOTALL);
        String clients = sentinel.clientList();
        Matcher m = p.matcher(clients);

        assertThat(m.matches()).isTrue();
        String addr = m.group(1);
        assertThat(sentinel.clientKill(KillArgs.Builder.addr(addr).skipme())).isGreaterThan(0);

        assertThat(sentinel.clientKill(KillArgs.Builder.id(4234))).isEqualTo(0);
        assertThat(sentinel.clientKill(KillArgs.Builder.typeSlave().id(4234))).isEqualTo(0);
        assertThat(sentinel.clientKill(KillArgs.Builder.typeNormal().id(4234))).isEqualTo(0);
        assertThat(sentinel.clientKill(KillArgs.Builder.typePubsub().id(4234))).isEqualTo(0);

        connection2.getStatefulConnection().close();
    }

    @Test
    public void clientList() {
        assertThat(sentinel.clientList().contains("addr=")).isTrue();
    }

    @Test
    public void info() {
        assertThat(sentinel.info().contains("redis_version")).isTrue();
        assertThat(sentinel.info("server").contains("redis_version")).isTrue();
    }
}
