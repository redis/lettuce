package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.slots.ClusterSlotRange;
import io.lettuce.core.cluster.models.slots.ClusterSlotsParser;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ClusterCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisClusterClient clusterClient;

    private final StatefulRedisConnection<String, String> connection;

    private final RedisClusterAsyncCommands<String, String> async;

    private final RedisClusterCommands<String, String> sync;

    @Inject
    ClusterCommandIntegrationTests(RedisClient client, RedisClusterClient clusterClient) {

        this.client = client;
        this.clusterClient = clusterClient;

        this.connection = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port1).build());
        this.sync = connection.sync();
        this.async = connection.async();
    }

    @AfterEach
    void after() {
        connection.close();
    }

    @Test
    void testClusterBumpEpoch() {

        RedisFuture<String> future = async.clusterBumpepoch();

        String result = TestFutures.getOrTimeout(future);

        assertThat(result).matches("(BUMPED|STILL).*");
    }

    @Test
    void testClusterInfo() {

        String result = sync.clusterInfo();

        assertThat(result).contains("cluster_known_nodes:");
        assertThat(result).contains("cluster_slots_fail:0");
        assertThat(result).contains("cluster_state:");
    }

    @Test
    void testClusterNodes() {

        String result = sync.clusterNodes();

        assertThat(result).contains("connected");
        assertThat(result).contains("master");
        assertThat(result).contains("myself");
    }

    @Test
    @EnabledOnCommand("EXPIRETIME") // Redis 7.0
    void testClusterShards() {

        List<Object> result = sync.clusterShards();
        assertThat(result).hasSize(2);

        result = connection.reactive().clusterShards().block(Duration.ofSeconds(5));
        assertThat(result).hasSize(2);
    }

    @Test
    @EnabledOnCommand("EXPIRETIME") // Redis 7.0
    void testClusterShardsParsing() {

        List<Object> result = sync.clusterShards();

        Partitions partitions = ClusterPartitionParser.parse(result);

        assertThat(partitions).hasSizeGreaterThan(2); // Redis 7.2 doesn't include the self-partition anymore
        assertThat(partitions.getPartitionBySlot(1).getUri().getPort()).isIn(7379, 7381);
        assertThat(partitions.getPartitionBySlot(12001).getUri().getPort()).isIn(7380, 7382);
        assertThat(partitions.getPartition("127.0.0.1", 7382).is(RedisClusterNode.NodeFlag.REPLICA)).isTrue();
        assertThat(partitions.getPartition("127.0.0.1", 7382).is(RedisClusterNode.NodeFlag.SLAVE)).isTrue();
    }

    @Test
    void testClusterNodesSync() {

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        String string = connection.sync().clusterNodes();
        connection.close();

        assertThat(string).contains("connected");
        assertThat(string).contains("master");
        assertThat(string).contains("myself");
    }

    @Test
    void testClusterReplicas() {

        sync.set("b", value);
        RedisFuture<Long> replication = async.waitForReplication(1, 5);
        assertThat(TestFutures.getOrTimeout(replication)).isGreaterThan(0L);
    }

    @Test
    void testAsking() {
        assertThat(sync.asking()).isEqualTo("OK");
    }

    @Test
    void testClusterSlots() {

        List<Object> reply = sync.clusterSlots();
        assertThat(reply.size()).isGreaterThan(1);

        List<ClusterSlotRange> parse = ClusterSlotsParser.parse(reply);
        assertThat(parse).hasSize(2);

        ClusterSlotRange clusterSlotRange = parse.get(0);
        assertThat(clusterSlotRange.getFrom()).isEqualTo(0);
        assertThat(clusterSlotRange.getTo()).isEqualTo(11999);

        assertThat(clusterSlotRange.toString()).contains(ClusterSlotRange.class.getSimpleName());
    }

    @Test
    void readOnly() throws Exception {

        // cluster node 3 is a replica for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a replica for the master 1
        RedisCommands<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port3).build())
                .sync();

        assertThat(connect3.readOnly()).isEqualTo("OK");
        waitUntilValueIsVisible(key, connect3);

        String resultBViewedByReplica = connect3.get("b");
        assertThat(resultBViewedByReplica).isEqualTo(value);
        connect3.quit();

        resultBViewedByReplica = connect3.get("b");
        assertThat(resultBViewedByReplica).isEqualTo(value);
    }

    @Test
    void readOnlyWithReconnect() throws Exception {

        // cluster node 3 is a replica for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a replica for the master 1
        RedisCommands<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port3).build())
                .sync();

        assertThat(connect3.readOnly()).isEqualTo("OK");
        connect3.quit();
        waitUntilValueIsVisible(key, connect3);

        String resultViewedByReplica = connect3.get("b");
        assertThat(resultViewedByReplica).isEqualTo(value);
    }

    @Test
    void readOnlyReadWrite() throws Exception {

        // cluster node 3 is a replica for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a replica for the master 1
        final RedisCommands<String, String> connect3 = client
                .connect(RedisURI.Builder.redis(host, ClusterTestSettings.port3).build()).sync();

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
    void clusterSlaves() {

        String nodeId = getNodeId(sync);
        List<String> result = sync.clusterSlaves(nodeId);

        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void clusterReplicas() {

        String nodeId = getNodeId(sync);
        List<String> result = sync.clusterReplicas(nodeId);

        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void testClusterLinks() {
        List<Map<String, Object>> values = sync.clusterLinks();
        assertThat(values).isNotEmpty();
        for (Map<String, Object> value : values) {
            assertThat(value).containsKeys("direction", "node", "create-time", "events", "send-buffer-allocated",
                    "send-buffer-used");
        }
    }

    @Test
    void testClusterLinksAsync() throws Exception {
        RedisFuture<List<Map<String, Object>>> futureLinks = async.clusterLinks();
        List<Map<String, Object>> values = futureLinks.get();
        for (Map<String, Object> value : values) {
            assertThat(value).containsKeys("direction", "node", "create-time", "events", "send-buffer-allocated",
                    "send-buffer-used");
        }
    }

    private void prepareReadonlyTest(String key) {

        async.set(key, value);

        String resultB = TestFutures.getOrTimeout(async.get(key));
        assertThat(resultB).isEqualTo(value);
        Delay.delay(Duration.ofMillis(500)); // give some time to replicate
    }

    private static void waitUntilValueIsVisible(String key, RedisCommands<String, String> commands) {
        Wait.untilTrue(() -> commands.get(key) != null).waitOrTimeout();
    }

}
