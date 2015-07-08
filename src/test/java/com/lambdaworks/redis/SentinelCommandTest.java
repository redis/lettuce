package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
import static com.lambdaworks.redis.TestSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;

public class SentinelCommandTest extends AbstractCommandTest {

    public static final String MASTER_ID = "mymaster";

    private RedisSentinelAsyncConnection<String, String> sentinel;

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(client, false, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        client = new RedisClient(RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = client.connectSentinelAsync();

        try {
            sentinel.master(MASTER_ID);
        } catch (Exception e) {
            sentinelRule.monitor(MASTER_ID, hostAddr(), TestSettings.port(3), 1, true);
        }
    }

    @After
    public void closeConnection() throws Exception {
        if (sentinel != null) {
            sentinel.close();
        }
    }

    @Test
    public void getMasterAddr() throws Exception {
        Future<SocketAddress> result = sentinel.getMasterAddrByName(MASTER_ID);
        InetSocketAddress socketAddress = (InetSocketAddress) result.get();
        assertThat(socketAddress.getHostName()).contains(TestSettings.host());
    }

    @Test
    public void getMasterAddrButNoMasterPresent() throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) sentinel.getMasterAddrByName("unknown").get();
        assertThat(socketAddress).isNull();
    }

    @Test
    public void getMasterAddrByName() throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) sentinel.getMasterAddrByName(MASTER_ID).get();
        assertThat(socketAddress.getPort()).isBetween(6479, 6485);
    }

    @Test
    public void masters() throws Exception {

        List<Map<String, String>> result = sentinel.masters().get();

        assertThat(result.size()).isGreaterThan(0);

        Map<String, String> map = result.get(0);
        assertThat(map.get("flags")).isNotNull();
        assertThat(map.get("config-epoch")).isNotNull();
        assertThat(map.get("port")).isNotNull();
    }

    @Test
    public void sentinelConnectWith() throws Exception {

        RedisClient client = new RedisClient(RedisURI.Builder.sentinel(TestSettings.host(), 1234, MASTER_ID)
                .withSentinel(TestSettings.host()).build());

        RedisSentinelAsyncConnection<String, String> sentinelConnection = client.connectSentinelAsync();
        assertThat(sentinelConnection.ping().get()).isEqualTo("PONG");

        sentinelConnection.close();

        final RedisConnection<String, String> connection2 = client.connect();
        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.quit();
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return connection2.isOpen();
            }
        }, timeout(seconds(10)));

        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.close();
        FastShutdown.shutdown(client);
    }

    @Test
    public void sentinelConnectWrongMaster() throws Exception {

        RedisClient client = new RedisClient(RedisURI.Builder.sentinel(TestSettings.host(), 1234, "nonexistent")
                .withSentinel(TestSettings.host()).build());
        try {
            client.connect();
            fail("missing RedisConnectionException");
        } catch (RedisConnectionException e) {
        }

        FastShutdown.shutdown(client);
    }

    @Test
    public void sentinelConnect() throws Exception {

        RedisClient client = new RedisClient(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build());

        RedisSentinelAsyncConnection<String, String> connection = client.connectSentinelAsync();
        assertThat(connection.ping().get()).isEqualTo("PONG");

        connection.close();
        FastShutdown.shutdown(client);
    }

    @Test
    public void getMaster() throws Exception {

        Map<String, String> result = sentinel.master(MASTER_ID).get();
        assertThat(result.get("ip")).isEqualTo(hostAddr()); // !! IPv4/IPv6
        assertThat(result).containsKey("role-reported");
    }

    @Test
    public void role() throws Exception {

        RedisClient redisClient = new RedisClient("localhost", 26380);
        RedisAsyncConnection<String, String> connection = redisClient.connectAsync();
        try {

            RedisFuture<List<Object>> role = connection.role();
            List<Object> objects = role.get();

            assertThat(objects).hasSize(2);

            assertThat(objects.get(0)).isEqualTo("sentinel");
            assertThat(objects.get(1).toString()).isEqualTo("[" + MASTER_ID + "]");
        } finally {
            connection.close();
            FastShutdown.shutdown(redisClient);
        }
    }

    @Test
    public void getSlaves() throws Exception {

        List<Map<String, String>> result = sentinel.slaves(MASTER_ID).get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKey("port");
    }

    @Test
    public void reset() throws Exception {

        Long result = sentinel.reset("other").get();
        assertThat(result.intValue()).isEqualTo(0);
    }

    @Test
    public void failover() throws Exception {

        try {
            sentinel.failover("other");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("ERR No such master with that name");
        }
    }

    @Test
    public void monitor() throws Exception {

        try {
            sentinel.remove("mymaster2");
        } catch (Exception e) {
        }

        String result = sentinel.monitor("mymaster2", hostAddr(), 8989, 2).get();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    public void ping() throws Exception {

        String result = sentinel.ping().get();
        assertThat(result).isEqualTo("PONG");
    }

    @Test
    public void set() throws Exception {

        String result = sentinel.set(MASTER_ID, "down-after-milliseconds", "1000").get();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {
        RedisConnection<String, String> connect = client.connect();
        connect.ping();
        connect.close();
    }

    @Test
    public void connectToRedisUsingSentinelWithReconnect() throws Exception {
        RedisConnection<String, String> connect = client.connect();
        connect.ping();
        connect.quit();
        connect.ping();
        connect.close();
    }
}
