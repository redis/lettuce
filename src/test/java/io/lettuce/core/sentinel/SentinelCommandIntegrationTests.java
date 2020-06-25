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
package io.lettuce.core.sentinel;

import static io.lettuce.test.settings.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
public class SentinelCommandIntegrationTests extends TestSupport {

    private final RedisClient redisClient;

    private StatefulRedisSentinelConnection<String, String> connection;

    private RedisSentinelCommands<String, String> sentinel;

    @Inject
    public SentinelCommandIntegrationTests(RedisClient redisClient) {
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
    void getMasterAddr() {
        SocketAddress result = sentinel.getMasterAddrByName(SentinelTestSettings.MASTER_ID);
        InetSocketAddress socketAddress = (InetSocketAddress) result;
        assertThat(socketAddress.getHostName()).contains(TestSettings.hostAddr());
    }

    @Test
    void getMasterAddrButNoMasterPresent() {
        InetSocketAddress socketAddress = (InetSocketAddress) sentinel.getMasterAddrByName("unknown");
        assertThat(socketAddress).isNull();
    }

    @Test
    void getMasterAddrByName() {
        InetSocketAddress socketAddress = (InetSocketAddress) sentinel.getMasterAddrByName(SentinelTestSettings.MASTER_ID);
        assertThat(socketAddress.getPort()).isBetween(6479, 6485);
    }

    @Test
    void masters() {

        List<Map<String, String>> result = sentinel.masters();

        assertThat(result.size()).isGreaterThan(0);

        Map<String, String> map = result.get(0);
        assertThat(map.get("flags")).isNotNull();
        assertThat(map.get("config-epoch")).isNotNull();
        assertThat(map.get("port")).isNotNull();
    }

    @Test
    void sentinelConnectWith() {

        RedisURI uri = RedisURI.Builder.sentinel(TestSettings.host(), 1234, SentinelTestSettings.MASTER_ID)
                .withSentinel(TestSettings.host()).build();

        RedisSentinelCommands<String, String> sentinelConnection = this.redisClient.connectSentinel(uri).sync();
        assertThat(sentinelConnection.ping()).isEqualTo("PONG");

        sentinelConnection.getStatefulConnection().close();

        RedisCommands<String, String> connection2 = this.redisClient.connect(uri).sync();
        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.quit();

        Wait.untilTrue(() -> connection2.getStatefulConnection().isOpen()).waitOrTimeout();

        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.getStatefulConnection().close();
    }

    @Test
    void sentinelConnectWrongMaster() {

        RedisURI nonexistent = RedisURI.Builder.sentinel(TestSettings.host(), 1234, "nonexistent")
                .withSentinel(TestSettings.host()).build();

        assertThatThrownBy(() -> redisClient.connect(nonexistent)).isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void getMaster() {

        Map<String, String> result = sentinel.master(SentinelTestSettings.MASTER_ID);
        assertThat(result.get("ip")).isEqualTo(hostAddr()); // !! IPv4/IPv6
        assertThat(result).containsKey("role-reported");
    }

    @Test
    void role() {

        RedisCommands<String, String> connection = redisClient.connect(RedisURI.Builder.redis(host, 26380).build()).sync();
        try {

            List<Object> objects = connection.role();

            assertThat(objects).hasSize(2);

            assertThat(objects.get(0)).isEqualTo("sentinel");
            assertThat(objects.get(1).toString()).isEqualTo("[" + SentinelTestSettings.MASTER_ID + "]");
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test
    void getSlaves() {

        List<Map<String, String>> result = sentinel.slaves(SentinelTestSettings.MASTER_ID);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKey("port");
    }

    @Test
    void reset() {

        Long result = sentinel.reset("other");
        assertThat(result.intValue()).isEqualTo(0);
    }

    @Test
    void failover() {

        try {
            sentinel.failover("other");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("ERR No such master with that name");
        }
    }

    @Test
    void monitor() {

        try {
            sentinel.remove("mymaster2");
        } catch (Exception e) {
        }

        String result = sentinel.monitor("mymaster2", hostAddr(), 8989, 2);
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void ping() {

        String result = sentinel.ping();
        assertThat(result).isEqualTo("PONG");
    }

    @Test
    void set() {

        String result = sentinel.set(SentinelTestSettings.MASTER_ID, "down-after-milliseconds", "1000");
        assertThat(result).isEqualTo("OK");
    }

}
