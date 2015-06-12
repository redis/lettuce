package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ClusterSetup {

    /**
     * Setup a cluster consisting of four members (see {@link AbstractClusterTest#port1} to {@link AbstractClusterTest#port4}).
     * Two masters (0-11999 and 12000-16383) and two slaves
     * 
     * @param clusterRule
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void setup2Master2Slaves(ClusterRule clusterRule) throws InterruptedException, ExecutionException,
            TimeoutException {

        if (is2Masters2Slaves(clusterRule)) {
            return;
        }

        clusterRule.clusterReset();
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port1);
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port2);
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port3);
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port4);

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterRule.getClusterClient().connectClusterAsync();

        RedisClusterAsyncCommands<String, String> node1 = connection.getConnection(AbstractClusterTest.host,
                AbstractClusterTest.port1);
        node1.clusterAddSlots(AbstractClusterTest.createSlots(0, 12000)).get();

        RedisClusterAsyncCommands<String, String> node2 = connection.getConnection(AbstractClusterTest.host,
                AbstractClusterTest.port2);
        node2.clusterAddSlots(AbstractClusterTest.createSlots(12000, 16384)).get();
        WaitFor.waitOrTimeout(() -> {
            return clusterRule.isStable();
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));
        clusterRule.getClusterClient().reloadPartitions();

        connection.getConnection(AbstractClusterTest.host, AbstractClusterTest.port3).clusterReplicate(
                AbstractClusterTest.clusterClient.getPartitions().getPartitionBySlot(1).getNodeId());
        connection.getConnection(AbstractClusterTest.host, AbstractClusterTest.port4).clusterReplicate(
                AbstractClusterTest.clusterClient.getPartitions().getPartitionBySlot(12002).getNodeId());

        WaitFor.waitOrTimeout(
                () -> {

                    clusterRule.getClusterClient().reloadPartitions();

                    return clusterRule.getClusterClient().getPartitions().getPartitions().stream()
                            .filter(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.SLAVE))
                            .count() == 2;
                }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

        connection.close();
    }

    private static boolean is2Masters2Slaves(ClusterRule clusterRule) {
        RedisClusterClient clusterClient = clusterRule.getClusterClient();

        long slaves = clusterClient.getPartitions().stream()
                .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE)).count();
        long masters = clusterClient.getPartitions().stream()
                .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER)).count();

        return slaves == 2 && masters == 2;
    }

}
