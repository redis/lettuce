package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.*;

import com.google.code.tempusfugit.temporal.Condition;
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
        FastShutdown.shutdown(clusterClient);
        FastShutdown.shutdown(client1);
        FastShutdown.shutdown(client2);
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
        waitForCluster(redis1);

        Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsAfterMeet.getPartitions()).hasSize(2);
    }

    @Test
    public void clusterForget() throws Exception {

        String result = redis1.clusterMeet(host, port2);

        assertThat(result).isEqualTo("OK");
        waitForCluster(redis1);

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
                for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
                    if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.HANDSHAKE)) {
                        return false;
                    }
                }
                return true;
            }
        }, timeout(seconds(5)));

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

    private void waitForCluster(final RedisClusterConnection<String, String> connection) throws InterruptedException,
            TimeoutException {
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                Partitions partitionsAfterMeet = ClusterPartitionParser.parse(connection.clusterNodes());
                return partitionsAfterMeet.getPartitions().size() == 2;
            }
        }, timeout(seconds(5)));
    }

    @Test
    public void clusterAddDelSlots() throws Exception {

        redis1.clusterMeet(host, port2);
        waitForCluster(redis1);
        waitForCluster(redis2);

        add6SlotsEach();

        waitForSlots(redis1, 6);
        waitForSlots(redis2, 6);

        final Set<Integer> set1 = ImmutableSet.of(1, 2, 3, 4, 5, 6);
        final Set<Integer> set2 = ImmutableSet.of(7, 8, 9, 10, 11, 12);

        deleteSlots(redis1, set1);
        deleteSlots(redis2, set2);

        verifyDeleteSlots(redis1, set1);
        verifyDeleteSlots(redis2, set2);
    }

    protected void verifyDeleteSlots(final RedisClusterConnection<String, String> connection, final Set<Integer> slots) {
        try {
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    RedisClusterNode ownPartition = getOwnPartition(connection);
                    boolean condition = ownPartition.getSlots().isEmpty();
                    if (!ownPartition.getSlots().isEmpty()) {
                        deleteSlots(connection, slots);
                    }
                    return condition;
                }
            }, timeout(seconds(5)));
        } catch (Exception e) {

            RedisClusterNode ownPartition = getOwnPartition(connection);
            fail("Slots not deleted, Slots on " + ownPartition.getUri() + ":" + ownPartition.getSlots(), e);
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
        waitForCluster(redis1);
        waitForCluster(redis2);

        add6SlotsEach();

        waitForSlots(redis1, 6);
        waitForSlots(redis2, 6);

        redis1.clusterSetSlotNode(6, getNodeId(redis2));

        waitForSlots(redis1, 5);

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(1, 2, 3, 4, 5));
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

    private void waitForSlots(final RedisClusterConnection<String, String> nodeConnection, final int expectedCount)
            throws InterruptedException, TimeoutException {
        try {
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    RedisClusterNode ownPartition = getOwnPartition(nodeConnection);
                    return ownPartition.getSlots().size() == expectedCount;
                }
            }, timeout(seconds(10)));
        } catch (Exception e) {
            RedisClusterNode ownPartition = getOwnPartition(nodeConnection);
            fail("Fail on waiting for slots on " + ownPartition.getUri() + ", expected count " + expectedCount + ", actual: "
                    + ownPartition.getSlots(), e);
        }
    }

    @Test
    public void clusterSlotMigrationImport() throws Exception {

        redis1.clusterMeet(host, port2);
        waitForCluster(redis1);
        waitForCluster(redis2);

        add6SlotsEach();

        waitForSlots(redis1, 6);
        waitForSlots(redis2, 6);

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
