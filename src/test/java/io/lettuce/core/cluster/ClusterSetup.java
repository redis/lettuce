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
package io.lettuce.core.cluster;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.lettuce.Wait;
import io.lettuce.core.TestSettings;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
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
    public static void setup2Masters(ClusterRule clusterRule)
            throws InterruptedException, ExecutionException, TimeoutException {

        clusterRule.clusterReset();
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port5);
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port6);

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterRule.getClusterClient().connect().async();
        Wait.untilTrue(() -> {

            clusterRule.getClusterClient().reloadPartitions();
            return clusterRule.getClusterClient().getPartitions().size() == 2;

        }).waitOrTimeout();

        Partitions partitions = clusterRule.getClusterClient().getPartitions();
        for (RedisClusterNode partition : partitions) {

            if (!partition.getSlots().isEmpty()) {
                RedisClusterAsyncCommands<String, String> nodeConnection = connection.getConnection(partition.getNodeId());

                for (Integer slot : partition.getSlots()) {
                    nodeConnection.clusterDelSlots(slot);
                }
            }
        }

        RedisClusterAsyncCommands<String, String> node1 = connection.getConnection(AbstractClusterTest.host,
                AbstractClusterTest.port5);
        node1.clusterAddSlots(AbstractClusterTest.createSlots(0, 12000));

        RedisClusterAsyncCommands<String, String> node2 = connection.getConnection(AbstractClusterTest.host,
                AbstractClusterTest.port6);
        node2.clusterAddSlots(AbstractClusterTest.createSlots(12000, 16384));

        Wait.untilTrue(clusterRule::isStable).waitOrTimeout();

        Wait.untilEquals(2L, () -> {
            clusterRule.getClusterClient().reloadPartitions();

            return partitionStream(clusterRule)
                    .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER)).count();
        }).waitOrTimeout();

        connection.getStatefulConnection().close();
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
    public static void setupMasterWithSlave(ClusterRule clusterRule)
            throws InterruptedException, ExecutionException, TimeoutException {

        clusterRule.clusterReset();
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port5);
        clusterRule.meet(AbstractClusterTest.host, AbstractClusterTest.port6);

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterRule.getClusterClient().connect().async();
        StatefulRedisClusterConnection<String, String> statefulConnection = connection.getStatefulConnection();

        Wait.untilEquals(2, () -> {
            clusterRule.getClusterClient().reloadPartitions();
            return clusterRule.getClusterClient().getPartitions().size();
        }).waitOrTimeout();

        RedisClusterCommands<String, String> node1 = statefulConnection
                .getConnection(TestSettings.hostAddr(), AbstractClusterTest.port5).sync();
        node1.clusterAddSlots(AbstractClusterTest.createSlots(0, 16384));

        Wait.untilTrue(clusterRule::isStable).waitOrTimeout();

        connection.getConnection(AbstractClusterTest.host, AbstractClusterTest.port6).clusterReplicate(node1.clusterMyId())
                .get();

        clusterRule.getClusterClient().reloadPartitions();

        Wait.untilEquals(1L, () -> {
            clusterRule.getClusterClient().reloadPartitions();
            return partitionStream(clusterRule)
                    .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER)).count();
        }).waitOrTimeout();

        Wait.untilEquals(1L, () -> {
            clusterRule.getClusterClient().reloadPartitions();
            return partitionStream(clusterRule).filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE))
                    .count();
        }).waitOrTimeout();

        connection.getStatefulConnection().close();
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
