/*
 * Copyright 2016-2018 the original author or authors.
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
package io.lettuce.core.sentinel;

import static io.lettuce.test.settings.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import io.lettuce.core.KillArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class SentinelServerCommandTest extends AbstractSentinelTest {

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        sentinelClient = RedisClient.create(TestClientResources.get(), RedisURI.Builder
                .sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        super.openConnection();

        try {
            sentinel.master(MASTER_ID);
        } catch (Exception e) {
            sentinelRule.monitor(MASTER_ID, hostAddr(), TestSettings.port(3), 1, true);
        }
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

        RedisSentinelCommands<String, String> connection2 = sentinelClient.connectSentinel().sync();
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
