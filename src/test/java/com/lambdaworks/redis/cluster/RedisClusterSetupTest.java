package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.AbstractClusterTest.createSlots;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getNodeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.Timeout;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class RedisClusterSetupTest {

    public static final String host = TestSettings.hostAddr();
    public static final int port1 = 7383;
    public static final int port2 = 7384;

    private static RedisClusterClient clusterClient;
    private static RedisClient client1;
    private static RedisClient client2;

    private RedisClusterConnection<String, String> redis1;
    private RedisClusterConnection<String, String> redis2;

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2);

    @BeforeClass
    public static void setupClient() {
        clusterClient = new RedisClusterClient(RedisURI.Builder.redis(host, port1).build());
        client1 = new RedisClient(host, port1);
        client2 = new RedisClient(host, port2);
    }

    @AfterClass
    public static void shutdownClient() {
        clusterClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
        client1.shutdown(0, 0, TimeUnit.MILLISECONDS);
        client2.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void openConnection() throws Exception {
        redis1 = client1.connect();
        redis2 = client2.connect();
        clusterRule.clusterReset();
    }

    @After
    public void closeConnection() throws Exception {
        redis1.close();
        redis2.close();
    }

    @Test
    public void clusterMeet() throws Exception {

        Partitions partitionsBeforeMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsBeforeMeet.getPartitions()).hasSize(1);

        String result = redis1.clusterMeet(host, port2);

        assertThat(result).isEqualTo("OK");
        waitForCluster();

        Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsAfterMeet.getPartitions()).hasSize(2);
    }

    @Test
    public void clusterForget() throws Exception {

        String result = redis1.clusterMeet(host, port2);

        assertThat(result).isEqualTo("OK");
        waitForCluster();

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (!redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                redis1.clusterForget(redisClusterNode.getNodeId());
            }
        }
        Thread.sleep(300);

        Partitions partitionsAfterForget = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsAfterForget.getPartitions()).hasSize(1);
    }

    private void waitForCluster() throws InterruptedException, TimeoutException {
        List<RedisClusterConnection<String, String>> connections = ImmutableList.of(redis1, redis2);
        WaitFor.waitOrTimeout(() -> {

            Set<String> nodeIds = Sets.newHashSet();
            for (RedisClusterConnection<String, String> connection : connections) {
                Partitions partitionsAfterMeet = ClusterPartitionParser.parse(connection.clusterNodes());
                partitionsAfterMeet.forEach(redisClusterNode -> nodeIds.add(redisClusterNode.getNodeId()));
            }

            return nodeIds.size() == 2;
        }, Timeout.timeout(Duration.seconds(5)));
    }

    @Test
    public void clusterAddDelSlots() throws Exception {

        recreateCluster();

        addAllSlots();
        waitForSlots(redis1, 12001);
        waitForSlots(redis2, 4383);

        Set<Integer> set1 = ImmutableSet.of(1, 2, 5, 6);

        deleteSlots(redis1, set1);

        waitForSlots(redis1, 11997);
    }

    protected void recreateCluster() throws InterruptedException, TimeoutException {
        clusterRule.clusterReset();
        redis1.clusterMeet(host, port2);
        redis2.clusterMeet(host, port1);
        waitForCluster();
    }

    private void deleteSlots(RedisClusterConnection<String, String> connection, Set<Integer> slots) {
        for (Integer slot : slots) {
            try {
                connection.clusterDelSlots(slot);
            } catch (RedisException e) {
            }
        }
    }

    @Test
    public void clusterSetSlots() throws Exception {

        recreateCluster();

        addAllSlots();
        waitForSlots(redis1, 12001);
        waitForSlots(redis2, 4383);

        redis1.clusterSetSlotNode(6, getNodeId(redis2));

        waitForSlots(redis1, 12000);
        waitForSlots(redis2, 4383);

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                assertThat(redisClusterNode.getSlots()).contains(1, 2, 3, 4, 5).doesNotContain(6);
            }
        }
    }

    private void addAllSlots() {
        redis1.clusterAddSlots(createSlots(0, 12001));
        redis2.clusterAddSlots(createSlots(12001, 16384));
    }

    private void waitForSlots(RedisClusterConnection<String, String> connection, int slotCount) throws InterruptedException,
            TimeoutException {
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                Partitions partitionsAfterMeet = ClusterPartitionParser.parse(connection.clusterNodes());
                Optional<RedisClusterNode> first = partitionsAfterMeet.stream()
                        .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).findFirst();
                return first.isPresent() && first.get().getSlots().size() == slotCount;
            }
        }, Timeout.timeout(Duration.seconds(5)));
    }

    @Test
    public void clusterSlotMigrationImport() throws Exception {

        redis1.clusterMeet(host, port2);
        waitForCluster();

        addAllSlots();
        waitForSlots(redis1, 12001);
        waitForSlots(redis2, 4383);

        String nodeId1 = getNodeId(redis1);
        String nodeId2 = getNodeId(redis2);
        assertThat(redis1.clusterSetSlotMigrating(6, nodeId2)).isEqualTo("OK");
        assertThat(redis1.clusterSetSlotImporting(15000, nodeId2)).isEqualTo("OK");
    }
}
