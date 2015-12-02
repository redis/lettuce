package com.lambdaworks.redis.cluster;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import com.lambdaworks.Wait;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ClusterSetup {

    /**
     * Setup a cluster consisting of two members (see {@link AbstractClusterTest#port5} to {@link AbstractClusterTest#port6}).
     * Two masters (0-11999 and 12000-16383)
     *
     * @param clusterRule
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void setup2Masters(ClusterRule clusterRule) throws InterruptedException, ExecutionException, TimeoutException {

        clusterRule.clusterReset();
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port5);
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port6);

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterRule.getClusterClient().connectClusterAsync();
        Wait.untilTrue(() -> {

            clusterRule.getClusterClient().reloadPartitions();
            return clusterRule.getClusterClient().getPartitions().size() == 2;

        }).waitOrTimeout();

        RedisClusterAsyncCommands<String, String> node1 = connection.getConnection(AbstractClusterTest.host,
                AbstractClusterTest.port5);
        node1.clusterAddSlots(AbstractClusterTest.createSlots(0, 12000));

        RedisClusterAsyncCommands<String, String> node2 = connection.getConnection(AbstractClusterTest.host,
                AbstractClusterTest.port6);
        node2.clusterAddSlots(AbstractClusterTest.createSlots(12000, 16384));

        Wait.untilTrue(clusterRule::isStable).waitOrTimeout();

        Wait.untilEquals(
                2L,
                () -> {
                    clusterRule.getClusterClient().reloadPartitions();

                    return partitionStream(clusterRule).filter(
                            redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER)).count();
                }).waitOrTimeout();

        connection.close();
    }

    /**
     * Setup a cluster consisting of two members (see {@link AbstractClusterTest#port5} to {@link AbstractClusterTest#port6}).
     * One master (0-16383) and one slave.
     *
     * @param clusterRule
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void setupMasterWithSlave(ClusterRule clusterRule) throws InterruptedException, ExecutionException,
            TimeoutException {

        clusterRule.clusterReset();
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port5);
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port6);

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterRule.getClusterClient().connectClusterAsync();
        StatefulRedisClusterConnection<String, String> statefulConnection = connection.getStatefulConnection();

        Wait.untilEquals(2, () -> {
            clusterRule.getClusterClient().reloadPartitions();
            return clusterRule.getClusterClient().getPartitions().size();
        }).waitOrTimeout();

        RedisClusterCommands<String, String> node1 = statefulConnection.getConnection(TestSettings.hostAddr(),
                AbstractClusterTest.port5).sync();
        node1.clusterAddSlots(AbstractClusterTest.createSlots(0, 16384));

        Wait.untilTrue(clusterRule::isStable).waitOrTimeout();

        connection.getConnection(AbstractClusterTest.host, AbstractClusterTest.port6).clusterReplicate(node1.clusterMyId())
                .get();

        clusterRule.getClusterClient().reloadPartitions();

        Wait.untilEquals(
                1L,
                () -> {
                    clusterRule.getClusterClient().reloadPartitions();
                    return partitionStream(clusterRule).filter(
                            redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER)).count();
                }).waitOrTimeout();

        Wait.untilEquals(
                1L,
                () -> {
                    clusterRule.getClusterClient().reloadPartitions();
                    return partitionStream(clusterRule).filter(
                            redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE)).count();
                }).waitOrTimeout();

        connection.close();
    }

    protected static Stream<RedisClusterNode> partitionStream(ClusterRule clusterRule) {
        return clusterRule.getClusterClient().getPartitions().getPartitions().stream();
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
