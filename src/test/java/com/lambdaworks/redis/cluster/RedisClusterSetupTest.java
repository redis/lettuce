package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.*;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.Timeout;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.lambdaworks.redis.*;
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
        redis1 = (RedisClusterConnection<String, String>) client1.connect();
        redis2 = (RedisClusterConnection<String, String>) client2.connect();
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
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
                return partitionsAfterMeet.getPartitions().size() == 2;
            }
        }, Timeout.timeout(Duration.seconds(5)));
    }

    @Test
    public void clusterAddDelSlots() throws Exception {

        redis1.clusterMeet(host, port2);
        waitForCluster();

        add6SlotsEach();

        waitForSlots(6, 6);

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(1, 2, 3, 4, 5, 6));
            } else {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(7, 8, 9, 10, 11, 12));
            }
        }

        final Set<Integer> set1 = ImmutableSet.of(1, 2, 3, 4, 5, 6);
        final Set<Integer> set2 = ImmutableSet.of(7, 8, 9, 10, 11, 12);

        deleteSlots(redis1, set1);
        deleteSlots(redis2, set2);

        try {
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    Partitions partitions = ClusterPartitionParser.parse(redis2.clusterNodes());
                    boolean condition = partitions.getPartitions().size() == 2
                            && partitions.getPartitions().get(0).getSlots().isEmpty();
                    if (!condition) {
                        deleteSlots(redis1, set1);
                    }
                    return condition;
                }
            }, Timeout.timeout(Duration.seconds(5)));
        } catch (Exception e) {

            Partitions detail = ClusterPartitionParser.parse(redis2.clusterNodes());
            String slotsOn1 = "";
            if (detail.getPartitions().size() > 0) {
                slotsOn1 = detail.getPartitions().get(0).getSlots().toString();
            }
            fail("Slots/Partitions not deleted. Partitions: " + detail.getPartitions().size() + ", Slots on (0):" + slotsOn1, e);
        }
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

        redis1.clusterMeet(host, port2);
        waitForCluster();

        add6SlotsEach();

        waitForSlots(6, 6);

        redis1.clusterSetSlotNode(6, getNodeId(redis2));
        waitForSlots(7, 5);

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(1, 2, 3, 4, 5));
            } else {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(6, 7, 8, 9, 10, 11, 12));
            }
        }

    }

    private void add6SlotsEach() {
        for (int i = 1; i < 7; i++) {
            redis1.clusterAddSlots(i);
        }
        for (int i = 7; i < 13; i++) {
            redis2.clusterAddSlots(i);
        }
    }

    private void waitForSlots(final int expected0, final int expected1) throws InterruptedException, TimeoutException {
        try {
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
                    return partitionsAfterMeet.getPartitions().size() == 2
                            && (partitionsAfterMeet.getPartitions().get(0).getSlots().size() == expected0 || partitionsAfterMeet
                                    .getPartitions().get(0).getSlots().size() == expected1)
                            && (partitionsAfterMeet.getPartitions().get(1).getSlots().size() == expected1 || partitionsAfterMeet
                                    .getPartitions().get(1).getSlots().size() == expected0);
                }
            }, Timeout.timeout(Duration.seconds(5)));
        } catch (Exception e) {
            Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
            String detail = "";
            for (RedisClusterNode node : partitions) {
                detail += node.getNodeId() + ": " + node.getSlots() + ", ";
            }
            fail("Fail on waiting for slots counts " + expected0 + ", " + expected1 + ", detail: " + detail, e);
        }
    }

    @Test
    public void clusterSlotMigrationImport() throws Exception {

        redis1.clusterMeet(host, port2);
        waitForCluster();

        add6SlotsEach();

        waitForSlots(6, 6);

        String nodeId1 = getNodeId(redis1);
        String nodeId2 = getNodeId(redis2);
        assertThat(redis1.clusterSetSlotMigrating(6, nodeId2)).isEqualTo("OK");
        assertThat(redis1.clusterSetSlotImporting(12, nodeId2)).isEqualTo("OK");

        RedisClusterNode partition1 = getOwnPartition(redis1);
        RedisClusterNode partition2 = getOwnPartition(redis2);

        assertThat(partition1.getSlots()).hasSize(6);
        assertThat(partition2.getSlots()).hasSize(6);
    }

}
