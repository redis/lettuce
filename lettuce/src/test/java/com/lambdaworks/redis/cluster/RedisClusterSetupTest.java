package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.Timeout;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterConnection;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 28.07.14 08:24
 */
public class RedisClusterSetupTest {
    public static final String host = "127.0.0.1";
    public static final int port1 = 7383;
    public static final int port2 = 7384;

    private static RedisClient client1;
    private static RedisClient client2;

    private RedisClusterConnection<String, String> redis1;
    private RedisClusterConnection<String, String> redis2;

    @BeforeClass
    public static void setupClient() {
        client1 = new RedisClient(host, port1);
        client2 = new RedisClient(host, port2);
    }

    @AfterClass
    public static void shutdownClient() {
        client1.shutdown();
        client2.shutdown();
    }

    @Before
    public void openConnection() throws Exception {
        redis1 = (RedisClusterConnection) client1.connect();
        redis1.flushall();
        redis1.flushdb();

        redis2 = (RedisClusterConnection) client2.connect();
        redis2.flushall();
        redis2.flushdb();

        redis1.clusterReset(true);
        redis2.clusterReset(true);
        Thread.sleep(100);
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

        for (int i = 1; i < 7; i++) {
            redis1.clusterAddSlots(i);
        }
        for (int i = 7; i < 13; i++) {
            redis2.clusterAddSlots(i);
        }

        waitForSlots();

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(1, 2, 3, 4, 5, 6));
            } else {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(7, 8, 9, 10, 11, 12));
            }
        }

        redis1.clusterDelSlots(1, 2, 3, 4, 5, 6);
        redis2.clusterDelSlots(7, 8, 9, 10, 11, 12);

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis2.clusterNodes());
                return partitionsAfterMeet.getPartitions().size() == 2
                        && partitionsAfterMeet.getPartitions().get(0).getSlots().isEmpty();
            }
        }, Timeout.timeout(Duration.seconds(5)));

    }

    @Test
    public void clusterSetSlots() throws Exception {

        redis1.clusterMeet(host, port2);
        waitForCluster();

        for (int i = 1; i < 7; i++) {
            redis1.clusterAddSlots(i);
        }
        for (int i = 7; i < 13; i++) {
            redis2.clusterAddSlots(i);
        }

        waitForSlots();

        redis1.clusterSetSlotNode(6, getNodeId(redis2));
        waitForSlots();

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(1, 2, 3, 4, 5));
            } else {
                assertThat(redisClusterNode.getSlots()).isEqualTo(ImmutableList.of(6, 7, 8, 9, 10, 11, 12));
            }
        }

    }

    private void waitForSlots() throws InterruptedException, TimeoutException {
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
                return partitionsAfterMeet.getPartitions().size() == 2
                        && !partitionsAfterMeet.getPartitions().get(0).getSlots().isEmpty()
                        && !partitionsAfterMeet.getPartitions().get(1).getSlots().isEmpty();
            }
        }, Timeout.timeout(Duration.seconds(5)));
    }

    @Test
    public void clusterSlotMigrationImport() throws Exception {

        redis1.clusterMeet(host, port2);
        waitForCluster();

        for (int i = 1; i < 7; i++) {
            redis1.clusterAddSlots(i);
        }
        for (int i = 7; i < 13; i++) {
            redis2.clusterAddSlots(i);
        }

        waitForSlots();

        String nodeId1 = getNodeId(redis1);
        String nodeId2 = getNodeId(redis2);
        assertThat(redis1.clusterSetSlotMigrating(6, nodeId2)).isEqualTo("OK");
        assertThat(redis1.clusterSetSlotImporting(12, nodeId2)).isEqualTo("OK");

        RedisClusterNode partition1 = getOwnPartition(redis1);
        RedisClusterNode partition2 = getOwnPartition(redis2);

        assertThat(partition1.getSlots()).hasSize(6);
        assertThat(partition2.getSlots()).hasSize(6);
    }

    private String getNodeId(RedisClusterConnection<String, String> connection) {
        RedisClusterNode ownPartition = getOwnPartition(connection);
        if (ownPartition != null) {
            return ownPartition.getNodeId();
        }

        return null;
    }

    private RedisClusterNode getOwnPartition(RedisClusterConnection<String, String> connection) {
        Partitions partitions = ClusterPartitionParser.parse(connection.clusterNodes());

        for (RedisClusterNode partition : partitions) {
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                return partition;
            }
        }
        return null;
    }

}
