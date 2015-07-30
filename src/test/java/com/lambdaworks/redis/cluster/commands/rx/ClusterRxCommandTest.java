package com.lambdaworks.redis.cluster.commands.rx;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getNodeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeoutException;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.AbstractClusterTest;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.SlotHash;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotRange;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotsParser;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClusterRxCommandTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected StatefulRedisConnection<String, String> connection;

    protected RedisReactiveCommands<String, String> rx;

    @BeforeClass
    public static void setupClient() throws Exception {
        client = new RedisClient(host, port1);
        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));

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
        rx = connection.reactive();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void testClusterInfo() throws Exception {

        String status = rx.clusterInfo().toBlocking().first();

        assertThat(status).contains("cluster_known_nodes:");
        assertThat(status).contains("cluster_slots_fail:0");
        assertThat(status).contains("cluster_state:");
    }

    @Test
    public void testClusterNodes() throws Exception {

        String result = rx.clusterNodes().toBlocking().first();

        assertThat(result).contains("connected");
        assertThat(result).contains("master");
        assertThat(result).contains("myself");
    }

    @Test
    public void testClusterSlaves() throws Exception {

        rx.set("b", value).subscribe();
        Long replication = rx.waitForReplication(1, 5).toBlocking().first();
        assertThat(replication).isGreaterThan(0L);
    }

    @Test
    public void testAsking() throws Exception {
        assertThat(rx.asking().toBlocking().first()).isEqualTo("OK");
    }

    @Test
    public void testClusterSlots() throws Exception {

        List<Object> reply = rx.clusterSlots().toList().toBlocking().first();
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
    public void clusterSlaves() throws Exception {

        String nodeId = getNodeId(rx.getStatefulConnection().sync());
        List<String> result = rx.clusterSlaves(nodeId).toList().toBlocking().first();

        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    public void getKeysInSlot() throws Exception {

        connection.sync().set("b", value);
        List<String> keys = rx.clusterGetKeysInSlot(SlotHash.getSlot("b".getBytes()), 10).toList().toBlocking().first();
        assertThat(keys).isEqualTo(ImmutableList.of("b"));
    }

    @Test
    public void countKeysInSlot() throws Exception {

        connection.sync().set("b", value);
        Long result = rx.clusterCountKeysInSlot(SlotHash.getSlot("b".getBytes())).toBlocking().first();
        assertThat(result).isEqualTo(1L);

        result = rx.clusterCountKeysInSlot(SlotHash.getSlot("ZZZ".getBytes())).toBlocking().first();
        assertThat(result).isEqualTo(0L);
    }

}
