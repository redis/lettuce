package com.lambdaworks.redis.sentinel;

import static com.lambdaworks.redis.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncCommands;

public class SentinelCommandTest extends AbstractSentinelTest {

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        sentinelClient = new RedisClient(RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();

        try {
            sentinel.master(MASTER_ID).get();
        } catch (Exception e) {
            sentinelRule.monitor(MASTER_ID, hostAddr(), TestSettings.port(3), 1, true);
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

        Future<SocketAddress> result = sentinel.getMasterAddrByName("unknown");
        InetSocketAddress socketAddress = (InetSocketAddress) result.get();
        assertThat(socketAddress).isNull();
    }

    @Test
    public void getMasterAddrByName() throws Exception {

        Future<SocketAddress> result = sentinel.getMasterAddrByName(MASTER_ID);

        InetSocketAddress socketAddress = (InetSocketAddress) result.get();

        assertThat(socketAddress.getPort()).isBetween(6479, 6485);
    }

    @Test
    public void masters() throws Exception {

        Future<List<Map<String, String>>> result = sentinel.masters();
        List<Map<String, String>> list = result.get();

        assertThat(list.size()).isGreaterThan(0);

        Map<String, String> map = list.get(0);
        assertThat(map.get("flags")).isNotNull();
        assertThat(map.get("config-epoch")).isNotNull();
        assertThat(map.get("port")).isNotNull();
    }

    @Test
    public void sentinelConnectWith() throws Exception {

        RedisClient client = new RedisClient(RedisURI.Builder.sentinel(TestSettings.host(), 1234, MASTER_ID)
                .withSentinel(TestSettings.host()).build());

        RedisSentinelAsyncCommands<String, String> sentinelConnection = client.connectSentinelAsync();
        assertThat(sentinelConnection.ping().get()).isEqualTo("PONG");

        sentinelConnection.close();

        RedisConnection<String, String> connection2 = client.connect();
        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.quit();
        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.close();
        client.shutdown(0, 0, TimeUnit.SECONDS);
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

        client.shutdown(0, 0, TimeUnit.SECONDS);
    }

    @Test
    public void sentinelConnect() throws Exception {

        RedisClient client = new RedisClient(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build());

        RedisSentinelAsyncCommands<String, String> connection = client.connectSentinelAsync();
        assertThat(connection.ping().get()).isEqualTo("PONG");

        connection.close();
        client.shutdown(0, 0, TimeUnit.SECONDS);
    }

    @Test
    public void getMaster() throws Exception {

        Future<Map<String, String>> result = sentinel.master(MASTER_ID);
        Map<String, String> map = result.get();
        assertThat(map.get("ip")).isEqualTo(hostAddr()); // !! IPv4/IPv6
        assertThat(map).containsKey("role-reported");
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
            redisClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void getSlaves() throws Exception {

        Future<List<Map<String, String>>> result = sentinel.slaves(MASTER_ID);
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().get(0)).containsKey("port");
    }

    @Test
    public void reset() throws Exception {

        Future<Long> result = sentinel.reset("other");
        Long val = result.get();
        assertThat(val.intValue()).isEqualTo(0);
    }

    @Test
    public void failover() throws Exception {

        RedisFuture<String> mymaster = sentinel.failover("other");
        try {
            mymaster.get();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("ERR No such master with that name");
        }
    }

    @Test
    public void monitor() throws Exception {

        try {
            sentinel.remove("mymaster2").get();
        } catch (Exception e) {
        }
        Future<String> result = sentinel.monitor("mymaster2", hostAddr(), 8989, 2);
        String val = result.get();
        assertThat(val).isEqualTo("OK");
    }

    @Test
    public void ping() throws Exception {

        Future<String> result = sentinel.ping();
        String val = result.get();
        assertThat(val).isEqualTo("PONG");
    }

    @Test
    public void set() throws Exception {

        Future<String> result = sentinel.set(MASTER_ID, "down-after-milliseconds", "1000");
        String val = result.get();
        assertThat(val).isEqualTo("OK");
    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {
        RedisConnection<String, String> connect = sentinelClient.connect();
        connect.ping();
        connect.close();
    }

    @Test
    public void connectToRedisUsingSentinelWithReconnect() throws Exception {
        RedisConnection<String, String> connect = sentinelClient.connect();
        connect.ping();
        connect.quit();
        connect.ping();
        connect.close();
    }
}
