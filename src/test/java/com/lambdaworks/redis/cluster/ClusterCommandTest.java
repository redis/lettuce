/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getNodeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.*;
import org.junit.runners.MethodSorters;

import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotRange;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotsParser;
import com.lambdaworks.redis.internal.LettuceLists;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClusterCommandTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected StatefulRedisConnection<String, String> connection;

    protected RedisClusterAsyncCommands<String, String> async;

    protected RedisClusterCommands<String, String> sync;

    @BeforeClass
    public static void setupClient() throws Exception {
        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port1).build());
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                LettuceLists.newList(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() throws Exception {

        clusterRule.getClusterClient().reloadPartitions();

        connection = client.connect(RedisURI.Builder.redis(host, port1).build());
        sync = connection.sync();
        async = connection.async();

    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void statefulConnectionFromSync() throws Exception {
        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        assertThat(sync.getStatefulConnection().sync()).isSameAs(sync);
    }

    @Test
    public void statefulConnectionFromAsync() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
    }

    @Test
    public void testClusterBumpEpoch() throws Exception {

        RedisFuture<String> future = async.clusterBumpepoch();

        String result = future.get();

        assertThat(result).matches("(BUMPED|STILL).*");
    }

    @Test
    public void testClusterInfo() throws Exception {

        RedisFuture<String> future = async.clusterInfo();

        String result = future.get();

        assertThat(result).contains("cluster_known_nodes:");
        assertThat(result).contains("cluster_slots_fail:0");
        assertThat(result).contains("cluster_state:");
    }

    @Test
    public void testClusterNodes() throws Exception {

        String result = sync.clusterNodes();

        assertThat(result).contains("connected");
        assertThat(result).contains("master");
        assertThat(result).contains("myself");
    }

    @Test
    public void testClusterNodesSync() throws Exception {

        RedisClusterConnection<String, String> connection = clusterClient.connectCluster();

        String string = connection.clusterNodes();
        connection.close();

        assertThat(string).contains("connected");
        assertThat(string).contains("master");
        assertThat(string).contains("myself");
    }

    @Test
    public void testClusterSlaves() throws Exception {

        sync.set("b", value);
        RedisFuture<Long> replication = async.waitForReplication(1, 5);
        assertThat(replication.get()).isGreaterThan(0L);
    }

    @Test
    public void testAsking() throws Exception {
        assertThat(sync.asking()).isEqualTo("OK");
    }

    @Test
    public void testReset() throws Exception {

        clusterClient.reloadPartitions();
        RedisAdvancedClusterAsyncCommandsImpl<String, String> connection = (RedisAdvancedClusterAsyncCommandsImpl) clusterClient
                .connectClusterAsync();

        RedisFuture<String> setA = connection.set("a", "myValue1");
        setA.get();

        connection.reset();

        setA = connection.set("a", "myValue1");

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        connection.close();

    }

    @Test
    public void testClusterSlots() throws Exception {

        List<Object> reply = sync.clusterSlots();
        assertThat(reply.size()).isGreaterThan(1);

        List<ClusterSlotRange> parse = ClusterSlotsParser.parse(reply);
        assertThat(parse).hasSize(2);

        ClusterSlotRange clusterSlotRange = parse.get(0);
        assertThat(clusterSlotRange.getFrom()).isEqualTo(0);
        assertThat(clusterSlotRange.getTo()).isEqualTo(11999);

        assertThat(clusterSlotRange.getMaster()).isNotNull();
        assertThat(clusterSlotRange.getSlaves()).isNotNull();
        assertThat(clusterSlotRange.toString()).contains(ClusterSlotRange.class.getSimpleName());

    }

    @Test
    public void readOnly() throws Exception {

        // cluster node 3 is a slave for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a slave for the master 1
        RedisConnection<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, port3).build()).sync();

        assertThat(connect3.readOnly()).isEqualTo("OK");
        waitUntilValueIsVisible(key, connect3);

        String resultBViewedBySlave = connect3.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);
        connect3.quit();

        resultBViewedBySlave = connect3.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);

    }

    @Test
    public void readOnlyWithReconnect() throws Exception {

        // cluster node 3 is a slave for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a slave for the master 1
        RedisConnection<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, port3).build()).sync();

        assertThat(connect3.readOnly()).isEqualTo("OK");
        connect3.quit();
        waitUntilValueIsVisible(key, connect3);

        String resultViewedBySlave = connect3.get("b");
        assertThat(resultViewedBySlave).isEqualTo(value);

    }

    protected void waitUntilValueIsVisible(String key, RedisConnection<String, String> connection) throws InterruptedException,
            TimeoutException {
        WaitFor.waitOrTimeout(() -> connection.get(key) != null, timeout(seconds(5)));
    }

    protected void prepareReadonlyTest(String key) throws InterruptedException, TimeoutException,
            java.util.concurrent.ExecutionException {

        async.set(key, value);

        String resultB = async.get(key).get();
        assertThat(resultB).isEqualTo(value);
        Thread.sleep(500); // give some time to replicate
    }

    @Test
    public void readOnlyReadWrite() throws Exception {

        // cluster node 3 is a slave for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a slave for the master 1
        final RedisConnection<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, port3).build()).sync();

        try {
            connect3.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }

        assertThat(connect3.readOnly()).isEqualTo("OK");
        waitUntilValueIsVisible(key, connect3);

        connect3.readWrite();
        try {
            connect3.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    public void clusterSlaves() throws Exception {

        String nodeId = getNodeId(sync);
        List<String> result = sync.clusterSlaves(nodeId);

        assertThat(result.size()).isGreaterThan(0);
    }

}
