package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
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
    public static final String SLAVE_ID = "myslave";

    public static final String MASTER_WITH_SLAVE_ID = "master_with_slave";

    private static RedisClient sentinelClient;
    private RedisSentinelAsyncConnection<String, String> sentinel;

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        sentinelClient = getRedisSentinelClient();
    }

    @AfterClass
    public static void shutdownClient() {
        sentinelClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();

        sentinelRule.monitor(MASTER_ID, "127.0.0.1", 6479, 1);
        sentinelRule.monitor(SLAVE_ID, "127.0.0.1", 16379, 1);
    }

    @After
    public void closeConnection() throws Exception {
        sentinel.close();
    }

    @Test
    public void getMasterAddr() throws Exception {

        Future<SocketAddress> result = sentinel.getMasterAddrByName(MASTER_ID);
        InetSocketAddress socketAddress = (InetSocketAddress) result.get();
        assertThat(socketAddress.getHostName()).contains("localhost");
    }

    @Test
    public void getMasterAddrButNoMasterPresent() throws Exception {

        sentinelRule.flush();

        Future<SocketAddress> result = sentinel.getMasterAddrByName(MASTER_ID);
        InetSocketAddress socketAddress = (InetSocketAddress) result.get();
        assertThat(socketAddress).isNull();
    }

    @Test
    public void getSlaveAddr() throws Exception {

        Future<SocketAddress> result = sentinel.getMasterAddrByName(SLAVE_ID);

        InetSocketAddress socketAddress = (InetSocketAddress) result.get();

        assertThat(socketAddress.getPort()).isEqualTo(16379);

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
    public void sentinelConnectWithFailover() throws Exception {

        RedisClient client = new RedisClient(RedisURI.Builder.sentinel("localhost", 1234, MASTER_ID).withSentinel("localhost")
                .build());

        RedisSentinelAsyncConnection<String, String> sentinelConnection = client.connectSentinelAsync();
        assertThat(sentinelConnection.ping().get()).isEqualTo("PONG");

        sentinelConnection.close();

        RedisConnection<String, String> connection2 = client.connect();
        assertThat(connection2.ping()).isEqualTo("PONG");
        connection2.close();
        client.shutdown();
    }

    @Test
    public void sentinelConnect() throws Exception {

        RedisClient client = new RedisClient(RedisURI.Builder.redis("localhost", port).build());

        RedisSentinelAsyncConnection<String, String> connection = client.connectSentinelAsync();
        assertThat(connection.ping().get()).isEqualTo("PONG");

        connection.close();
        client.shutdown();
    }

    @Test
    public void getSlaveDownstate() throws Exception {

        Future<Map<String, String>> result = sentinel.master(SLAVE_ID);
        Map<String, String> map = result.get();
        assertThat(map.get("flags")).contains("disconnected");

    }

    @Test
    public void getMaster() throws Exception {

        Future<Map<String, String>> result = sentinel.master(MASTER_ID);
        Map<String, String> map = result.get();
        assertThat(map.get("ip")).isEqualTo("127.0.0.1"); // !! IPv4/IPv6
        assertThat(map.get("role-reported")).isEqualTo("master");

    }

    @Test
    public void role() throws Exception {

        RedisClient redisClient = new RedisClient("localhost", 26381);
        RedisAsyncConnection<String, String> connection = redisClient.connectAsync();
        try {

            RedisFuture<List<Object>> role = connection.role();
            List<Object> objects = role.get();

            assertThat(objects).hasSize(2);

            assertThat(objects.get(0)).isEqualTo("sentinel");
            assertThat(objects.get(1).toString()).isEqualTo("[mymasterfailover]");

        } finally {
            connection.close();
            redisClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void getSlaves() throws Exception {

        Future<List<Map<String, String>>> result = sentinel.slaves(MASTER_ID);
        assertThat(result.get()).hasSize(0);

        sentinelRule.monitor(MASTER_WITH_SLAVE_ID, "127.0.0.1", sentinelRule.findMaster(6484, 6485), 1);

        try {
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return sentinelRule.hasSlaves(MASTER_WITH_SLAVE_ID);
                }
            }, timeout(seconds(15)));
        } catch (Exception e) {
            RedisConnection<String, String> master = sentinelClient.connect(RedisURI.Builder.redis("127.0.0.1", 6484).build());
            RedisConnection<String, String> slave = sentinelClient.connect(RedisURI.Builder.redis("127.0.0.1", 6485).build());

            fail("Timeout when waiting for slaves: Master role " + master.role() + ", Slave role " + slave.role() + ", "
                    + e.getMessage());
        }

        Future<List<Map<String, String>>> slaves = sentinel.slaves(MASTER_WITH_SLAVE_ID);

        assertThat(slaves.get()).hasSize(1);
        assertThat(slaves.get().get(0)).containsKey("port");
    }

    @Test
    public void reset() throws Exception {

        Future<Long> result = sentinel.reset(SLAVE_ID);
        Long val = result.get();
        assertThat(val.intValue()).isEqualTo(1);

    }

    @Test
    public void failover() throws Exception {

        RedisFuture<String> mymaster = sentinel.failover(MASTER_ID);
        String s = mymaster.get();
        assertThat(s).isNull();
    }

    @Test
    public void monitor() throws Exception {

        sentinelRule.flush();

        Future<String> result = sentinel.monitor("mymaster2", "127.0.0.1", 8989, 2);
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

    protected static RedisClient getRedisSentinelClient() {
        return new RedisClient(RedisURI.Builder.sentinel("localhost", MASTER_ID).build());
    }

}
