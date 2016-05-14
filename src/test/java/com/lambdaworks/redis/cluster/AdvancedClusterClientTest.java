package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.RandomKeys;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("rawtypes")
public class AdvancedClusterClientTest extends AbstractClusterTest {

    public static final String KEY_ON_NODE_1 = "a";
    public static final String KEY_ON_NODE_2 = "b";

    private RedisAdvancedClusterAsyncCommands<String, String> commands;
    private RedisAdvancedClusterCommands<String, String> syncCommands;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @Before
    public void before() throws Exception {
        clusterClient.reloadPartitions();
        clusterConnection = clusterClient.connect();
        commands = clusterConnection.async();
        syncCommands = clusterConnection.sync();
    }

    @After
    public void after() throws Exception {
        commands.close();
    }

    @Test
    public void nodeConnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = commands.getConnection(redisClusterNode.getNodeId());

            String myid = nodeConnection.clusterMyId().get();
            assertThat(myid).isEqualTo(redisClusterNode.getNodeId());
        }
    }

    @Test(expected = RedisException.class)
    public void unknownNodeId() throws Exception {

        commands.getConnection("unknown");
    }

    @Test(expected = RedisException.class)
    public void invalidHost() throws Exception {
        commands.getConnection("invalid-host", -1);
    }

    @Test
    public void partitions() throws Exception {

        Partitions partitions = commands.getStatefulConnection().getPartitions();
        assertThat(partitions).hasSize(4);
    }

    @Test
    public void doWeirdThingsWithClusterconnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = commands.getConnection(redisClusterNode.getNodeId());

            nodeConnection.close();

            RedisClusterAsyncConnection<String, String> nextConnection = commands.getConnection(redisClusterNode.getNodeId());
            assertThat(commands).isNotSameAs(nextConnection);
        }
    }

    @Test
    public void differentConnections() throws Exception {

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeId = commands.getConnection(redisClusterNode.getNodeId());
            RedisClusterAsyncConnection<String, String> hostAndPort = commands
                    .getConnection(redisClusterNode.getUri().getHost(), redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }

        StatefulRedisClusterConnection<String, String> statefulConnection = commands.getStatefulConnection();
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {

            StatefulRedisConnection<String, String> nodeId = statefulConnection.getConnection(redisClusterNode.getNodeId());
            StatefulRedisConnection<String, String> hostAndPort = statefulConnection
                    .getConnection(redisClusterNode.getUri().getHost(), redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }

        RedisAdvancedClusterCommands<String, String> sync = statefulConnection.sync();
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {

            RedisClusterCommands<String, String> nodeId = sync.getConnection(redisClusterNode.getNodeId());
            RedisClusterCommands<String, String> hostAndPort = sync.getConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }

        RedisAdvancedClusterReactiveCommands<String, String> rx = statefulConnection.reactive();
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {

            RedisClusterReactiveCommands<String, String> nodeId = rx.getConnection(redisClusterNode.getNodeId());
            RedisClusterReactiveCommands<String, String> hostAndPort = rx.getConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }
    }

    @Test
    public void msetRegular() throws Exception {

        Map<String, String> mset = Collections.singletonMap(key, value);

        String result = syncCommands.mset(mset);

        assertThat(result).isEqualTo("OK");
        assertThat(syncCommands.get(key)).isEqualTo(value);
    }

    @Test
    public void msetCrossSlot() throws Exception {

        Map<String, String> mset = prepareMset();

        String result = syncCommands.mset(mset);

        assertThat(result).isEqualTo("OK");

        for (String mykey : mset.keySet()) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isEqualTo("value-" + mykey);
        }
    }

    protected Map<String, String> prepareMset() {
        Map<String, String> mset = new HashMap<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            mset.put(key, "value-" + key);
        }
        return mset;
    }

    @Test
    public void msetnxCrossSlot() throws Exception {

        Map<String, String> mset = prepareMset();

        RedisFuture<Boolean> result = commands.msetnx(mset);

        assertThat(result.get()).isTrue();

        for (String mykey : mset.keySet()) {
            String s1 = commands.get(mykey).get();
            assertThat(s1).isEqualTo("value-" + mykey);
        }
    }

    @Test
    public void mgetRegular() throws Exception {

        msetRegular();
        List<String> result = syncCommands.mget(key);

        assertThat(result).hasSize(1);
    }

    @Test
    public void mgetCrossSlot() throws Exception {

        msetCrossSlot();
        List<String> keys = new ArrayList<>();
        List<String> expectation = new ArrayList<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            keys.add(key);
            expectation.add("value-" + key);
        }

        List<String> result = syncCommands.mget(keys.toArray(new String[keys.size()]));

        assertThat(result).hasSize(keys.size());
        assertThat(result).isEqualTo(expectation);
    }

    @Test
    public void delRegular() throws Exception {

        msetRegular();
        Long result = syncCommands.unlink(key);

        assertThat(result).isEqualTo(1);
        assertThat(commands.get(key).get()).isNull();
    }

    @Test
    public void delCrossSlot() throws Exception {

        List<String> keys = prepareKeys();

        Long result = syncCommands.del(keys.toArray(new String[keys.size()]));

        assertThat(result).isEqualTo(25);

        for (String mykey : keys) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    public void unlinkRegular() throws Exception {

        msetRegular();
        Long result = syncCommands.unlink(key);

        assertThat(result).isEqualTo(1);
        assertThat(syncCommands.get(key)).isNull();
    }

    @Test
    public void unlinkCrossSlot() throws Exception {

        List<String> keys = prepareKeys();

        Long result = syncCommands.unlink(keys.toArray(new String[keys.size()]));

        assertThat(result).isEqualTo(25);

        for (String mykey : keys) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    protected List<String> prepareKeys() throws Exception {
        msetCrossSlot();
        List<String> keys = new ArrayList<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            keys.add(key);
        }
        return keys;
    }

    @Test
    public void clientSetname() throws Exception {

        String name = "test-cluster-client";

        assertThat(clusterClient.getPartitions().size()).isGreaterThan(0);

        syncCommands.clientSetname(name);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterCommands<String, String> nodeConnection = commands.getStatefulConnection().sync()
                    .getConnection(redisClusterNode.getNodeId());
            assertThat(nodeConnection.clientList()).contains(name);
        }
    }

    @Test(expected = RedisCommandExecutionException.class)
    public void clientSetnameRunOnError() throws Exception {
        syncCommands.clientSetname("not allowed");
    }

    @Test
    public void dbSize() throws Exception {

        writeKeysToTwoNodes();

        RedisClusterCommands<String, String> nodeConnection1 = clusterConnection.getConnection(host, port1).sync();
        RedisClusterCommands<String, String> nodeConnection2 = clusterConnection.getConnection(host, port2).sync();

        assertThat(nodeConnection1.dbsize()).isEqualTo(1);
        assertThat(nodeConnection2.dbsize()).isEqualTo(1);

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(2);
    }

    @Test
    public void flushall() throws Exception {

        writeKeysToTwoNodes();

        assertThat(syncCommands.flushall()).isEqualTo("OK");

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    public void flushdb() throws Exception {

        writeKeysToTwoNodes();

        assertThat(syncCommands.flushdb()).isEqualTo("OK");

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    public void keys() throws Exception {

        writeKeysToTwoNodes();

        assertThat(syncCommands.keys("*")).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    public void keysStreaming() throws Exception {

        writeKeysToTwoNodes();
        ListStreamingAdapter<String> result = new ListStreamingAdapter<>();

        assertThat(syncCommands.keys(result, "*")).isEqualTo(2);
        assertThat(result.getList()).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    public void randomKey() throws Exception {

        writeKeysToTwoNodes();

        assertThat(syncCommands.randomkey()).isIn(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    public void scriptFlush() throws Exception {
        assertThat(syncCommands.scriptFlush()).isEqualTo("OK");
    }

    @Test
    public void scriptKill() throws Exception {
        assertThat(syncCommands.scriptKill()).isEqualTo("OK");
    }

    @Test
    @Ignore("Run me manually, I will shutdown all your cluster nodes so you need to restart the Redis Cluster after this test")
    public void shutdown() throws Exception {
        syncCommands.shutdown(true);
    }

    @Test
    public void testSync() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.set(key, value);
        assertThat(sync.get(key)).isEqualTo(value);

        RedisClusterCommands<String, String> node2Connection = sync.getConnection(host, port2);
        assertThat(node2Connection.get(key)).isEqualTo(value);

        assertThat(sync.getStatefulConnection()).isSameAs(commands.getStatefulConnection());
    }

    @Test
    public void routeCommandTonoAddrPartition() throws Exception {

        RedisClusterCommands<String, String> sync = clusterClient.connect().sync();
        try {

            Partitions partitions = clusterClient.getPartitions();
            for (RedisClusterNode partition : partitions) {
                partition.setUri(RedisURI.create("redis://non.existent.host:1234"));
            }

            sync.set("A", "value");// 6373
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("Unable to connect to");
        } finally {
            clusterClient.getPartitions().clear();
            clusterClient.reloadPartitions();
        }
        sync.close();
    }

    @Test
    public void routeCommandToForbiddenHostOnRedirect() throws Exception {

        RedisClusterCommands<String, String> sync = clusterClient.connect().sync();
        try {

            Partitions partitions = clusterClient.getPartitions();
            for (RedisClusterNode partition : partitions) {
                partition.setSlots(Collections.singletonList(0));
                if (partition.getUri().getPort() == 7380) {
                    partition.setSlots(Collections.singletonList(6373));
                } else {
                    partition.setUri(RedisURI.create("redis://non.existent.host:1234"));
                }
            }

            partitions.updateCache();

            sync.set("A", "value");// 6373
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not allowed");
        } finally {
            clusterClient.getPartitions().clear();
            clusterClient.reloadPartitions();
        }
        sync.close();
    }

    @Test
    public void getConnectionToNotAClusterMemberForbidden() throws Exception {

        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        try {
            sync.getConnection(TestSettings.host(), TestSettings.port());
        } catch (RedisException e) {
            assertThat(e).hasRootCauseExactlyInstanceOf(IllegalArgumentException.class);
        }
        sync.close();
    }

    @Test
    public void getConnectionToNotAClusterMemberAllowed() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().validateClusterNodeMembership(false).build());
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        connection.getConnection(TestSettings.host(), TestSettings.port());
        connection.close();
    }

    @Test
    public void pipelining() throws Exception {

        RedisClusterCommands<String, String> verificationConnection = clusterClient.connect().sync();

        // preheat the first connection
        commands.get(key(0)).get();

        int iterations = 1000;
        commands.setAutoFlushCommands(false);
        List<RedisFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            futures.add(commands.set(key(i), value(i)));
        }

        for (int i = 0; i < iterations; i++) {
            assertThat(verificationConnection.get(key(i))).as("Key " + key(i) + " must be null").isNull();
        }

        commands.flushCommands();
        boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));
        assertThat(result).isTrue();

        for (int i = 0; i < iterations; i++) {
            assertThat(verificationConnection.get(key(i))).as("Key " + key(i) + " must be " + value(i)).isEqualTo(value(i));
        }

        verificationConnection.close();

    }

    @Test
    public void transactions() throws Exception {

        commands.multi();
        commands.set(key, value);
        commands.discard();

        commands.multi();
        commands.set(key, value);
        commands.exec();
    }

    @Test
    public void clusterScan() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(RandomKeys.MAP);

        Set<String> allKeys = new HashSet<>();

        KeyScanCursor<String> scanCursor = null;

        do {
            if (scanCursor == null) {
                scanCursor = sync.scan();
            } else {
                scanCursor = sync.scan(scanCursor);
            }
            allKeys.addAll(scanCursor.getKeys());
        } while (!scanCursor.isFinished());

        assertThat(allKeys).containsAll(RandomKeys.KEYS);

    }

    @Test
    public void clusterScanWithArgs() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(RandomKeys.MAP);

        Set<String> allKeys = new HashSet<>();

        KeyScanCursor<String> scanCursor = null;

        do {
            if (scanCursor == null) {
                scanCursor = sync.scan(ScanArgs.Builder.matches("a*"));
            } else {
                scanCursor = sync.scan(scanCursor, ScanArgs.Builder.matches("a*"));
            }
            allKeys.addAll(scanCursor.getKeys());
        } while (!scanCursor.isFinished());

        assertThat(allKeys).containsAll(RandomKeys.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));

    }

    @Test
    public void clusterScanStreaming() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(RandomKeys.MAP);

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor scanCursor = null;

        do {
            if (scanCursor == null) {
                scanCursor = sync.scan(adapter);
            } else {
                scanCursor = sync.scan(adapter, scanCursor);
            }
        } while (!scanCursor.isFinished());

        assertThat(adapter.getList()).containsAll(RandomKeys.KEYS);

    }

    @Test
    public void clusterScanStreamingWithArgs() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(RandomKeys.MAP);

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor scanCursor = null;
        do {
            if (scanCursor == null) {
                scanCursor = sync.scan(adapter, ScanArgs.Builder.matches("a*"));
            } else {
                scanCursor = sync.scan(adapter, scanCursor, ScanArgs.Builder.matches("a*"));
            }
        } while (!scanCursor.isFinished());

        assertThat(adapter.getList())
                .containsAll(RandomKeys.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void clusterScanCursorFinished() throws Exception {
        syncCommands.scan(ScanCursor.FINISHED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clusterScanCursorNotReused() throws Exception {
        syncCommands.scan(ScanCursor.of("dummy"));
    }

    protected String value(int i) {
        return value + "-" + i;
    }

    protected String key(int i) {
        return key + "-" + i;
    }

    private void writeKeysToTwoNodes() {
        syncCommands.set(KEY_ON_NODE_1, value);
        syncCommands.set(KEY_ON_NODE_2, value);
    }

}
