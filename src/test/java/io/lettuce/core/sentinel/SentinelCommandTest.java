/*
 * Copyright 2011-2018 the original author or authors.
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
import static org.assertj.core.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.Delay;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.DefaultRedisClient;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class SentinelCommandTest extends AbstractSentinelTest {

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
    public void getMasterAddr() {
        SocketAddress result = sentinel.getMasterAddrByName(MASTER_ID);
        InetSocketAddress socketAddress = (InetSocketAddress) result;
        assertThat(socketAddress.getHostName()).contains(TestSettings.hostAddr());
    }

    @Test
    public void getMasterAddrButNoMasterPresent() {
        InetSocketAddress socketAddress = (InetSocketAddress) sentinel.getMasterAddrByName("unknown");
        assertThat(socketAddress).isNull();
    }

    @Test
    public void getMasterAddrByName() {
        InetSocketAddress socketAddress = (InetSocketAddress) sentinel.getMasterAddrByName(MASTER_ID);
        assertThat(socketAddress.getPort()).isBetween(6479, 6485);
    }

    @Test
    public void masters() {

        List<Map<String, String>> result = sentinel.masters();

        assertThat(result.size()).isGreaterThan(0);

        Map<String, String> map = result.get(0);
        assertThat(map.get("flags")).isNotNull();
        assertThat(map.get("config-epoch")).isNotNull();
        assertThat(map.get("port")).isNotNull();
    }

    @Test
    public void sentinelConnectWith() {

        RedisClient client = RedisClient.create(TestClientResources.get(),
                RedisURI.Builder.sentinel(TestSettings.host(), 1234, MASTER_ID).withSentinel(TestSettings.host()).build());

        RedisSentinelCommands<String, String> sentinelConnection = client.connectSentinel().sync();
        assertThat(sentinelConnection.ping()).isEqualTo("PONG");

        sentinelConnection.getStatefulConnection().close();

        RedisCommands<String, String> connection2 = client.connect().sync();
        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.quit();

        Wait.untilTrue(() -> connection2.getStatefulConnection().isOpen()).waitOrTimeout();

        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.getStatefulConnection().close();
        FastShutdown.shutdown(client);
    }

    @Test
    public void sentinelConnectWrongMaster() {

        RedisClient client = RedisClient.create(TestClientResources.get(),
                RedisURI.Builder.sentinel(TestSettings.host(), 1234, "nonexistent").withSentinel(TestSettings.host()).build());
        try {
            client.connect();
            fail("missing RedisConnectionException");
        } catch (RedisConnectionException e) {
        }

        FastShutdown.shutdown(client);
    }

    @Test
    public void sentinelConnect() {

        RedisClient client = DefaultRedisClient.get();

        RedisURI redisURI = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();
        RedisSentinelCommands<String, String> connection = client.connectSentinel(redisURI).sync();
        assertThat(connection.ping()).isEqualTo("PONG");

        connection.getStatefulConnection().close();
    }

    @Test
    public void getMaster() {

        Map<String, String> result = sentinel.master(MASTER_ID);
        assertThat(result.get("ip")).isEqualTo(hostAddr()); // !! IPv4/IPv6
        assertThat(result).containsKey("role-reported");
    }

    @Test
    public void role() {

        RedisCommands<String, String> connection = sentinelClient.connect(RedisURI.Builder.redis(host, 26380).build()).sync();
        try {

            List<Object> objects = connection.role();

            assertThat(objects).hasSize(2);

            assertThat(objects.get(0)).isEqualTo("sentinel");
            assertThat(objects.get(1).toString()).isEqualTo("[" + MASTER_ID + "]");
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test
    public void getSlaves() {

        List<Map<String, String>> result = sentinel.slaves(MASTER_ID);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKey("port");
    }

    @Test
    public void reset() {

        Long result = sentinel.reset("other");
        assertThat(result.intValue()).isEqualTo(0);
    }

    @Test
    public void failover() {

        try {
            sentinel.failover("other");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("ERR No such master with that name");
        }
    }

    @Test
    public void monitor() {

        try {
            sentinel.remove("mymaster2");
        } catch (Exception e) {
        }

        String result = sentinel.monitor("mymaster2", hostAddr(), 8989, 2);
        assertThat(result).isEqualTo("OK");
    }

    @Test
    public void ping() {

        String result = sentinel.ping();
        assertThat(result).isEqualTo("PONG");
    }

    @Test
    public void set() {

        String result = sentinel.set(MASTER_ID, "down-after-milliseconds", "1000");
        assertThat(result).isEqualTo("OK");
    }

    @Test
    public void connectToRedisUsingSentinel() {
        RedisCommands<String, String> connect = sentinelClient.connect().sync();
        connect.ping();
        connect.getStatefulConnection().close();
    }

    @Test
    public void connectToRedisUsingSentinelWithReconnect() {

        RedisCommands<String, String> connect = sentinelClient.connect().sync();

        connect.ping();
        connect.quit();

        Delay.delay(Duration.ofMillis(50));
        Wait.untilTrue(connect::isOpen).waitOrTimeout();
        connect.ping();
        connect.getStatefulConnection().close();
    }
}
