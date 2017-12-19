/*
 * Copyright 2011-2017 the original author or authors.
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
package io.lettuce.core.cluster;

import static io.lettuce.core.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.test.util.ReflectionTestUtils;

import io.lettuce.TestClientResources;
import io.lettuce.Wait;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterClientTest extends AbstractClusterTest {

    protected static RedisClient client;

    private StatefulRedisConnection<String, String> redis1;
    private StatefulRedisConnection<String, String> redis2;
    private StatefulRedisConnection<String, String> redis3;
    private StatefulRedisConnection<String, String> redis4;

    private RedisCommands<String, String> redissync1;
    private RedisCommands<String, String> redissync2;
    private RedisCommands<String, String> redissync3;
    private RedisCommands<String, String> redissync4;

    protected RedisAdvancedClusterCommands<String, String> sync;
    protected StatefulRedisClusterConnection<String, String> connection;

    @BeforeClass
    public static void setupClient() {

        setupClusterClient();

        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port1).build());
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                Collections.singletonList(RedisURI.Builder.redis(host, port1).withClientName("my-client").build()));
    }

    @AfterClass
    public static void shutdownClient() {
        shutdownClusterClient();
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() {

        clusterClient.setOptions(ClusterClientOptions.create());
        clusterRule.getClusterClient().reloadPartitions();

        redis1 = client.connect(RedisURI.Builder.redis(host, port1).build());
        redis2 = client.connect(RedisURI.Builder.redis(host, port2).build());
        redis3 = client.connect(RedisURI.Builder.redis(host, port3).build());
        redis4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        redissync1 = redis1.sync();
        redissync2 = redis2.sync();
        redissync3 = redis3.sync();
        redissync4 = redis4.sync();

        clusterClient.reloadPartitions();
        connection = clusterClient.connect();
        sync = connection.sync();
    }

    @After
    public void after() {
        connection.close();
        redis1.close();

        redissync1.getStatefulConnection().close();
        redissync2.getStatefulConnection().close();
        redissync3.getStatefulConnection().close();
        redissync4.getStatefulConnection().close();
    }

    @Test
    public void statefulConnectionFromSync() {
        RedisAdvancedClusterCommands<String, String> sync = clusterClient.connect().sync();
        assertThat(sync.getStatefulConnection().sync()).isSameAs(sync);
        connection.close();
    }

    @Test
    public void statefulConnectionFromAsync() {
        RedisAdvancedClusterAsyncCommands<String, String> async = clusterClient.connect().async();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
        connection.close();
    }

    @Test
    public void shouldApplyTimeoutOnRegularConnection() {

        clusterClient.setDefaultTimeout(1, TimeUnit.MINUTES);

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(connection.getConnection(host, port1).getTimeout()).isEqualTo(Duration.ofMinutes(1));

        connection.close();
    }

    @Test
    public void shouldApplyTimeoutOnRegularConnectionUsingCodec() {

        clusterClient.setDefaultTimeout(1, TimeUnit.MINUTES);

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect(Utf8StringCodec.UTF8);

        assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(connection.getConnection(host, port1).getTimeout()).isEqualTo(Duration.ofMinutes(1));

        connection.close();
    }

    @Test
    public void shouldApplyTimeoutOnPubSubConnection() {

        clusterClient.setDefaultTimeout(1, TimeUnit.MINUTES);

        StatefulRedisPubSubConnection<String, String> connection = clusterClient.connectPubSub();

        assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        connection.close();
    }

    @Test
    public void shouldApplyTimeoutOnPubSubConnectionUsingCodec() {

        clusterClient.setDefaultTimeout(1, TimeUnit.MINUTES);

        StatefulRedisPubSubConnection<String, String> connection = clusterClient.connectPubSub(Utf8StringCodec.UTF8);

        assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        connection.close();
    }

    @Test
    public void clusterConnectionShouldSetClientName() throws Exception {

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");
        Thread.sleep(10);
        connection.sync().quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();
        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");

        StatefulRedisConnection<String, String> nodeConnection = connection.getConnection(connection.getPartitions()
                .getPartition(0).getNodeId());
        assertThat(nodeConnection.sync().clientGetname()).isEqualTo("my-client");

        connection.close();
    }

    @Test
    public void pubSubclusterConnectionShouldSetClientName() throws InterruptedException {

        StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub();

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");
        Thread.sleep(10);
        connection.sync().quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");

        StatefulRedisConnection<String, String> nodeConnection = connection.getConnection(connection.getPartitions()
                .getPartition(0).getNodeId());
        assertThat(nodeConnection.sync().clientGetname()).isEqualTo("my-client");

        connection.close();
    }

    @Test
    public void reloadPartitions() {
        assertThat(clusterClient.getPartitions()).hasSize(4);

        assertThat(clusterClient.getPartitions().getPartition(0).getUri());
        assertThat(clusterClient.getPartitions().getPartition(1).getUri());
        assertThat(clusterClient.getPartitions().getPartition(2).getUri());
        assertThat(clusterClient.getPartitions().getPartition(3).getUri());

        clusterClient.reloadPartitions();

        assertThat(clusterClient.getPartitions().getPartition(0).getUri());
        assertThat(clusterClient.getPartitions().getPartition(1).getUri());
        assertThat(clusterClient.getPartitions().getPartition(2).getUri());
        assertThat(clusterClient.getPartitions().getPartition(3).getUri());
    }

    @Test
    public void testClusteredOperations() {

        SlotHash.getSlot(KEY_B.getBytes()); // 3300 -> Node 1 and Slave (Node 3)
        SlotHash.getSlot(KEY_A.getBytes()); // 15495 -> Node 2

        RedisFuture<String> result = redis1.async().set(KEY_B, value);
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redissync1.set(KEY_B, "value")).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.async().set(KEY_A, value);
        try {
            resultMoved.get();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("MOVED 15495");
        }

        clusterClient.reloadPartitions();
        RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync();

        assertThat(connection.set(KEY_A, value)).isEqualTo("OK");
        assertThat(connection.set(KEY_B, "myValue2")).isEqualTo("OK");
        assertThat(connection.set(KEY_D, "myValue2")).isEqualTo("OK");

        connection.getStatefulConnection().close();

    }

    @Test
    public void testReset() {

        clusterClient.reloadPartitions();
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        connection.sync().set(KEY_A, value);
        connection.reset();

        assertThat(connection.sync().set(KEY_A, value)).isEqualTo("OK");
        connection.close();
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testClusterCommandRedirection() throws Exception {

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterClient.connect().async();

        // Command on node within the default connection
        assertThat(connection.set(KEY_B, value).get()).isEqualTo("OK");

        // gets redirection to node 3
        assertThat(connection.set(KEY_A, value).get()).isEqualTo("OK");
        connection.getStatefulConnection().close();
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testClusterRedirection() throws Exception {

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterClient.connect().async();
        Partitions partitions = clusterClient.getPartitions();

        for (RedisClusterNode partition : partitions) {
            partition.setSlots(new ArrayList<>());
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {

                int[] slots = createSlots(0, 16384);
                for (int i = 0; i < slots.length; i++) {
                    partition.getSlots().add(i);
                }
            }
        }
        partitions.updateCache();

        // appropriate cluster node
        RedisFuture<String> setB = connection.set(KEY_B, value);

        assertThat(setB).isInstanceOf(AsyncCommand.class);

        setB.get(10, TimeUnit.SECONDS);
        assertThat(setB.getError()).isNull();
        assertThat(setB.get()).isEqualTo("OK");

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set(KEY_A, value);

        assertThat(setA instanceof AsyncCommand).isTrue();

        setA.get(10, TimeUnit.SECONDS);
        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        connection.getStatefulConnection().close();
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testClusterRedirectionLimit() throws Exception {

        clusterClient.setOptions(ClusterClientOptions.builder().maxRedirects(0).build());
        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterClient.connect().async();
        Partitions partitions = clusterClient.getPartitions();

        for (RedisClusterNode partition : partitions) {

            if (partition.getSlots().contains(15495)) {
                partition.setSlots(new ArrayList<>());
            } else {
                partition.setSlots(new ArrayList<>());
                int[] slots = createSlots(0, 16384);
                for (int i = 0; i < slots.length; i++) {
                    partition.getSlots().add(i);
                }
            }

        }
        partitions.updateCache();

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set(KEY_A, value);

        assertThat(setA instanceof AsyncCommand).isTrue();

        setA.await(10, TimeUnit.SECONDS);
        assertThat(setA.getError()).isEqualTo("MOVED 15495 127.0.0.1:7380");

        connection.getStatefulConnection().close();
    }

    @Test(expected = RedisException.class)
    public void closeConnection() {

        RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync();

        List<String> time = connection.time();
        assertThat(time).hasSize(2);

        connection.getStatefulConnection().close();

        connection.time();
    }

    @Test
    public void clusterAuth() throws Exception {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), port7).withPassword("foobared").build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        List<String> time = sync.time();
        assertThat(time).hasSize(2);

        connection.async().quit().get();

        time = sync.time();
        assertThat(time).hasSize(2);

        char[] password = (char[]) ReflectionTestUtils.getField(connection, "password");
        assertThat(new String(password)).isEqualTo("foobared");

        connection.close();
        FastShutdown.shutdown(clusterClient);
    }

    @Test
    public void clusterAuthPingBeforeConnect() throws Exception {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), port7).withPassword("foobared").build());
        clusterClient.setOptions(ClusterClientOptions.builder().pingBeforeActivateConnection(true).build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        List<String> time = sync.time();
        assertThat(time).hasSize(2);

        connection.async().quit().get();

        time = sync.time();
        assertThat(time).hasSize(2);

        connection.close();
        FastShutdown.shutdown(clusterClient);
    }

    @Test(expected = RedisException.class)
    public void clusterNeedsAuthButNotSupplied() {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), port7).build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        try {
            List<String> time = connection.sync().time();
            assertThat(time).hasSize(2);
        } finally {
            connection.close();
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    public void noClusterNodeAvailable() {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, 40400).build());
        try {
            clusterClient.connect();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisException.class);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    public void getClusterNodeConnection() {

        RedisClusterNode redis1Node = getOwnPartition(redissync2);

        RedisClusterCommands<String, String> connection = sync.getConnection(TestSettings.hostAddr(), port2);

        String result = connection.clusterMyId();
        assertThat(result).isEqualTo(redis1Node.getNodeId());

    }

    @Test
    public void operateOnNodeConnection() {

        sync.set(KEY_A, value);
        sync.set(KEY_B, "d");

        StatefulRedisConnection<String, String> statefulRedisConnection = connection.getConnection(TestSettings.hostAddr(),
                port2);

        RedisClusterCommands<String, String> connection = statefulRedisConnection.sync();

        assertThat(connection.get(KEY_A)).isEqualTo(value);
        try {
            connection.get(KEY_B);
            fail("missing RedisCommandExecutionException: MOVED");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    public void testGetConnectionAsyncByNodeId() {

        RedisClusterNode partition = connection.getPartitions().getPartition(0);

        StatefulRedisConnection<String, String> node = connection.getConnectionAsync(partition.getNodeId()).join();

        assertThat(node.sync().ping()).isEqualTo("PONG");
    }

    @Test
    public void testGetConnectionAsyncByHostAndPort() {

        RedisClusterNode partition = connection.getPartitions().getPartition(0);

        RedisURI uri = partition.getUri();
        StatefulRedisConnection<String, String> node = connection.getConnectionAsync(uri.getHost(), uri.getPort()).join();

        assertThat(node.sync().ping()).isEqualTo("PONG");
    }

    @Test
    public void testStatefulConnection() throws Exception {
        RedisAdvancedClusterAsyncCommands<String, String> async = connection.async();

        assertThat(async.ping().get()).isEqualTo("PONG");
    }

    @Test(expected = RedisException.class)
    public void getButNoPartitionForSlothash() {

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            redisClusterNode.setSlots(new ArrayList<>());
        }
        RedisChannelHandler rch = (RedisChannelHandler) connection;
        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) rch.getChannelWriter();
        writer.setPartitions(clusterClient.getPartitions());
        clusterClient.getPartitions().reload(clusterClient.getPartitions().getPartitions());

        sync.get(key);
    }

    @Test
    public void readOnlyOnCluster() throws Exception {

        sync.readOnly();
        // commands are dispatched to a different connection, therefore it works for us.
        sync.set(KEY_B, value);

        connection.async().quit().get();

        assertThat(ReflectionTestUtils.getField(connection, "readOnly")).isEqualTo(Boolean.TRUE);

        sync.readWrite();

        assertThat(ReflectionTestUtils.getField(connection, "readOnly")).isEqualTo(Boolean.FALSE);
        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, 40400).build());
        try {
            clusterClient.connect();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisException.class);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    public void getKeysInSlot() {

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        List<String> keysA = sync.clusterGetKeysInSlot(SLOT_A, 10);
        assertThat(keysA).isEqualTo(Collections.singletonList(KEY_A));

        List<String> keysB = sync.clusterGetKeysInSlot(SLOT_B, 10);
        assertThat(keysB).isEqualTo(Collections.singletonList(KEY_B));

    }

    @Test
    public void countKeysInSlot() {

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        Long result = sync.clusterCountKeysInSlot(SLOT_A);
        assertThat(result).isEqualTo(1L);

        result = sync.clusterCountKeysInSlot(SLOT_B);
        assertThat(result).isEqualTo(1L);

        int slotZZZ = SlotHash.getSlot("ZZZ".getBytes());
        result = sync.clusterCountKeysInSlot(slotZZZ);
        assertThat(result).isEqualTo(0L);

    }

    @Test
    public void testClusterCountFailureReports() {
        RedisClusterNode ownPartition = getOwnPartition(redissync1);
        assertThat(redissync1.clusterCountFailureReports(ownPartition.getNodeId())).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testClusterKeyslot() {
        assertThat(redissync1.clusterKeyslot(KEY_A)).isEqualTo(SLOT_A);
        assertThat(SlotHash.getSlot(KEY_A)).isEqualTo(SLOT_A);
    }

    @Test
    public void testClusterSaveconfig() {
        assertThat(redissync1.clusterSaveconfig()).isEqualTo("OK");
    }

    @Test
    public void testClusterSetConfigEpoch() {
        try {
            redissync1.clusterSetConfigEpoch(1L);
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("ERR The user can assign a config epoch only");
        }
    }

    @Test
    public void testReadFrom() {
        StatefulRedisClusterConnection<String, String> statefulConnection = connection;

        assertThat(statefulConnection.getReadFrom()).isEqualTo(ReadFrom.MASTER);

        statefulConnection.setReadFrom(ReadFrom.NEAREST);
        assertThat(statefulConnection.getReadFrom()).isEqualTo(ReadFrom.NEAREST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadFromNull() {
        connection.setReadFrom(null);
    }

    @Test
    public void testPfmerge() {

        RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync();

        assertThat(SlotHash.getSlot("key2660")).isEqualTo(SlotHash.getSlot("key7112")).isEqualTo(SlotHash.getSlot("key8885"));

        connection.pfadd("key2660", "rand", "mat");
        connection.pfadd("key7112", "mat", "perrin");

        connection.pfmerge("key8885", "key2660", "key7112");

        assertThat(connection.pfcount("key8885")).isEqualTo(3);

        connection.getStatefulConnection().close();
    }
}
