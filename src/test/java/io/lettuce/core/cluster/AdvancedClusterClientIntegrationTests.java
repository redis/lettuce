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
package io.lettuce.core.cluster;

import static io.lettuce.test.LettuceExtension.Connection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.inject.New;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.*;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for {@link StatefulRedisClusterConnection}.
 *
 * @author Mark Paluch
 * @author Jon Chambers
 */
@SuppressWarnings("rawtypes")
@ExtendWith(LettuceExtension.class)
class AdvancedClusterClientIntegrationTests extends TestSupport {

    private static final String KEY_ON_NODE_1 = "a";

    private static final String KEY_ON_NODE_2 = "b";

    private final RedisClusterClient clusterClient;

    private final StatefulRedisClusterConnection<String, String> clusterConnection;

    private final RedisAdvancedClusterAsyncCommands<String, String> async;

    private final RedisAdvancedClusterCommands<String, String> sync;

    @Inject
    AdvancedClusterClientIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> clusterConnection) {
        this.clusterClient = clusterClient;

        this.clusterConnection = clusterConnection;
        this.async = clusterConnection.async();
        this.sync = clusterConnection.sync();
    }

    @BeforeEach
    void setUp() {
        this.sync.flushall();
    }

    @Test
    void nodeConnections() {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncCommands<String, String> nodeConnection = async.getConnection(redisClusterNode.getNodeId());

            String myid = Futures.get(nodeConnection.clusterMyId());
            assertThat(myid).isEqualTo(redisClusterNode.getNodeId());
        }
    }

    @Test
    void unknownNodeId() {
        assertThatThrownBy(() -> async.getConnection("unknown")).isInstanceOf(RedisException.class);
    }

    @Test
    void invalidHost() {
        assertThatThrownBy(() -> async.getConnection("invalid-host", -1)).isInstanceOf(RedisException.class);
    }

    @Test
    void partitions() {

        Partitions partitions = async.getStatefulConnection().getPartitions();
        assertThat(partitions).hasSize(4);
    }

    @Test
    void differentConnections() {

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncCommands<String, String> nodeId = async.getConnection(redisClusterNode.getNodeId());
            RedisClusterAsyncCommands<String, String> hostAndPort = async.getConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }

        StatefulRedisClusterConnection<String, String> statefulConnection = async.getStatefulConnection();
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
    void msetRegular() {

        Map<String, String> mset = Collections.singletonMap(key, value);

        String result = sync.mset(mset);

        assertThat(result).isEqualTo("OK");
        assertThat(sync.get(key)).isEqualTo(value);
    }

    @Test
    void msetCrossSlot() {

        Map<String, String> mset = prepareMset();

        String result = sync.mset(mset);

        assertThat(result).isEqualTo("OK");

        for (String mykey : mset.keySet()) {
            String s1 = sync.get(mykey);
            assertThat(s1).isEqualTo("value-" + mykey);
        }
    }

    @Test
    void msetnxCrossSlot() {

        Map<String, String> mset = prepareMset();

        String key = mset.keySet().iterator().next();
        Map<String, String> submap = Collections.singletonMap(key, mset.get(key));

        assertThat(sync.msetnx(submap)).isTrue();
        assertThat(sync.msetnx(mset)).isFalse();

        for (String mykey : mset.keySet()) {
            String s1 = sync.get(mykey);
            assertThat(s1).isEqualTo("value-" + mykey);
        }
    }

    @Test
    void mgetRegular() {

        msetRegular();
        List<KeyValue<String, String>> result = sync.mget(key);

        assertThat(result).hasSize(1);
    }

    @Test
    void mgetCrossSlot() {

        msetCrossSlot();
        List<String> keys = new ArrayList<>();
        List<KeyValue<String, String>> expectation = new ArrayList<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            keys.add(key);
            expectation.add(kv(key, "value-" + key));
        }

        List<KeyValue<String, String>> result = sync.mget(keys.toArray(new String[keys.size()]));

        assertThat(result).hasSize(keys.size());
        assertThat(result).isEqualTo(expectation);
    }

    @Test
    @EnabledOnCommand("UNLINK")
    void delRegular() {

        msetRegular();
        Long result = sync.unlink(key);

        assertThat(result).isEqualTo(1);
        assertThat(Futures.get(async.get(key))).isNull();
    }

    @Test
    void delCrossSlot() {

        List<String> keys = prepareKeys();

        Long result = sync.del(keys.toArray(new String[keys.size()]));

        assertThat(result).isEqualTo(25);

        for (String mykey : keys) {
            String s1 = sync.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    @EnabledOnCommand("UNLINK")
    void unlinkRegular() {

        msetRegular();
        Long result = sync.unlink(key);

        assertThat(result).isEqualTo(1);
        assertThat(sync.get(key)).isNull();
    }

    @Test
    @EnabledOnCommand("UNLINK")
    void unlinkCrossSlot() {

        List<String> keys = prepareKeys();

        Long result = sync.unlink(keys.toArray(new String[keys.size()]));

        assertThat(result).isEqualTo(25);

        for (String mykey : keys) {
            String s1 = sync.get(mykey);
            assertThat(s1).isNull();
        }
    }

    private List<String> prepareKeys() {

        msetCrossSlot();
        List<String> keys = new ArrayList<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            keys.add(key);
        }
        return keys;
    }

    @Test
    void clientSetname() {

        String name = "test-cluster-client";

        assertThat(clusterClient.getPartitions().size()).isGreaterThan(0);

        sync.clientSetname(name);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterCommands<String, String> nodeConnection = async.getStatefulConnection().sync()
                    .getConnection(redisClusterNode.getNodeId());
            assertThat(nodeConnection.clientList()).contains(name);
        }

        assertThat(sync.clientGetname()).isEqualTo(name);
    }

    @Test
    void clientSetnameRunOnError() {
        assertThatThrownBy(() -> sync.clientSetname("not allowed")).isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void dbSize() {

        writeKeysToTwoNodes();

        RedisClusterCommands<String, String> nodeConnection1 = clusterConnection
                .getConnection(ClusterTestSettings.host, ClusterTestSettings.port1).sync();
        RedisClusterCommands<String, String> nodeConnection2 = clusterConnection
                .getConnection(ClusterTestSettings.host, ClusterTestSettings.port1).sync();

        assertThat(nodeConnection1.dbsize()).isEqualTo(1);
        assertThat(nodeConnection2.dbsize()).isEqualTo(1);

        Long dbsize = sync.dbsize();
        assertThat(dbsize).isEqualTo(2);
    }

    @Test
    void flushall() {

        writeKeysToTwoNodes();

        assertThat(sync.flushall()).isEqualTo("OK");

        Long dbsize = sync.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void flushallAsync() {

        writeKeysToTwoNodes();

        assertThat(sync.flushallAsync()).isEqualTo("OK");

        Wait.untilTrue(() -> sync.get(KEY_ON_NODE_1) == null).waitOrTimeout();
        Wait.untilTrue(() -> sync.get(KEY_ON_NODE_2) == null).waitOrTimeout();

        assertThat(sync.get(KEY_ON_NODE_1)).isNull();
        assertThat(sync.get(KEY_ON_NODE_2)).isNull();
    }

    @Test
    void flushdb() {

        writeKeysToTwoNodes();

        assertThat(sync.flushdb()).isEqualTo("OK");

        Long dbsize = sync.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void keys() {

        writeKeysToTwoNodes();

        assertThat(sync.keys("*")).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    void keysStreaming() {

        writeKeysToTwoNodes();
        ListStreamingAdapter<String> result = new ListStreamingAdapter<>();

        assertThat(sync.keys(result, "*")).isEqualTo(2);
        assertThat(result.getList()).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    void randomKey() {

        writeKeysToTwoNodes();

        assertThat(sync.randomkey()).isIn(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    void scriptFlush() {
        assertThat(sync.scriptFlush()).isEqualTo("OK");
    }

    @Test
    void scriptKill() {
        assertThatThrownBy(sync::scriptKill).hasMessageContaining("NOTBUSY");
    }

    @Test
    void scriptLoad() {

        assertThat(sync.scriptFlush()).isEqualTo("OK");

        String script = "return true";

        String sha = LettuceStrings.digest(script.getBytes());
        assertThat(sync.scriptExists(sha)).contains(false);

        String returnedSha = sync.scriptLoad(script);

        assertThat(returnedSha).isEqualTo(sha);
        assertThat(sync.scriptExists(sha)).contains(true);
    }

    @Test
    @Disabled("Run me manually, I will shutdown all your cluster nodes so you need to restart the Redis Cluster after this test")
    void shutdown() {
        sync.shutdown(true);
    }

    @Test
    void testSync() {

        RedisAdvancedClusterCommands<String, String> sync = async.getStatefulConnection().sync();
        sync.set(key, value);
        assertThat(sync.get(key)).isEqualTo(value);

        RedisClusterCommands<String, String> node2Connection = sync.getConnection(ClusterTestSettings.host,
                ClusterTestSettings.port2);
        assertThat(node2Connection.get(key)).isEqualTo(value);

        assertThat(sync.getStatefulConnection()).isSameAs(async.getStatefulConnection());
    }

    @Test
    @Inject
    void routeCommandToNoAddrPartition(@New StatefulRedisClusterConnection<String, String> connectionUnderTest) {

        RedisAdvancedClusterCommands<String, String> sync = connectionUnderTest.sync();
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
    }

    @Test
    @Inject
    void routeCommandToForbiddenHostOnRedirect(
            @Connection(requiresNew = true) StatefulRedisClusterConnection<String, String> connectionUnderTest) {

        RedisAdvancedClusterCommands<String, String> sync = connectionUnderTest.sync();
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
    }

    @Test
    void getConnectionToNotAClusterMemberForbidden() {

        StatefulRedisClusterConnection<String, String> sync = clusterClient.connect();
        try {
            sync.getConnection(TestSettings.host(), TestSettings.port());
        } catch (RedisException e) {
            assertThat(e).hasRootCauseExactlyInstanceOf(IllegalArgumentException.class);
        }
        sync.close();
    }

    @Test
    void getConnectionToNotAClusterMemberAllowed() {

        clusterClient.setOptions(ClusterClientOptions.builder().validateClusterNodeMembership(false).build());
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        connection.getConnection(TestSettings.host(), TestSettings.port());
        connection.close();
    }

    @Test
    @Inject
    void pipelining(@New StatefulRedisClusterConnection<String, String> connectionUnderTest) {

        RedisAdvancedClusterAsyncCommands<String, String> async = connectionUnderTest.async();
        // preheat the first connection
        Futures.await(async.get(key(0)));

        int iterations = 1000;
        async.setAutoFlushCommands(false);
        List<RedisFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            futures.add(async.set(key(i), value(i)));
        }

        for (int i = 0; i < iterations; i++) {
            assertThat(this.sync.get(key(i))).as("Key " + key(i) + " must be null").isNull();
        }

        async.flushCommands();

        boolean result = Futures.awaitAll(futures);
        assertThat(result).isTrue();

        for (int i = 0; i < iterations; i++) {
            assertThat(this.sync.get(key(i))).as("Key " + key(i) + " must be " + value(i)).isEqualTo(value(i));
        }
    }

    @Test
    void clusterScan() {

        RedisAdvancedClusterCommands<String, String> sync = async.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

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

        assertThat(allKeys).containsAll(KeysAndValues.KEYS);
    }

    @Test
    void clusterScanWithArgs() {

        RedisAdvancedClusterCommands<String, String> sync = async.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

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

        assertThat(allKeys)
                .containsAll(KeysAndValues.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));
    }

    @Test
    void clusterScanStreaming() {

        RedisAdvancedClusterCommands<String, String> sync = async.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor scanCursor = null;

        do {
            if (scanCursor == null) {
                scanCursor = sync.scan(adapter);
            } else {
                scanCursor = sync.scan(adapter, scanCursor);
            }
        } while (!scanCursor.isFinished());

        assertThat(adapter.getList()).containsAll(KeysAndValues.KEYS);

    }

    @Test
    void clusterScanStreamingWithArgs() {

        RedisAdvancedClusterCommands<String, String> sync = async.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

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
                .containsAll(KeysAndValues.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));

    }

    @Test
    void clusterScanCursorFinished() {
        assertThatThrownBy(() -> sync.scan(ScanCursor.FINISHED)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clusterScanCursorNotReused() {
        assertThatThrownBy(() -> sync.scan(ScanCursor.of("dummy"))).isInstanceOf(IllegalArgumentException.class);
    }

    String value(int i) {
        return value + "-" + i;
    }

    String key(int i) {
        return key + "-" + i;
    }

    private void writeKeysToTwoNodes() {
        sync.set(KEY_ON_NODE_1, value);
        sync.set(KEY_ON_NODE_2, value);
    }

    Map<String, String> prepareMset() {
        Map<String, String> mset = new HashMap<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            mset.put(key, "value-" + key);
        }
        return mset;
    }

}
