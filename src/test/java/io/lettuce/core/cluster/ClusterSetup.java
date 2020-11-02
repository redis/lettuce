/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.util.stream.Stream;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.settings.TestSettings;

/**
 * Utility to setup various Redis Cluster configurations.
 *
 * @author Mark Paluch
 * @author Mikhael Sokolov
 */
class ClusterSetup {

    /**
     * Setup a cluster consisting of two members (see {@link ClusterTestSettings#port5} to {@link ClusterTestSettings#port6}).
     * Two masters (0-11999 and 12000-16383)
     *
     * @param clusterHelper helper
     */
    public static void setup2Masters(ClusterTestHelper clusterHelper) {

        clusterHelper.clusterReset();
        clusterHelper.meet(ClusterTestSettings.host, ClusterTestSettings.port5);
        clusterHelper.meet(ClusterTestSettings.host, ClusterTestSettings.port6);

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterHelper.getClusterClient().connect().async();
        Wait.untilTrue(() -> {

            clusterHelper.getClusterClient().reloadPartitions();
            return clusterHelper.getClusterClient().getPartitions().size() == 2;

        }).waitOrTimeout();

        Partitions partitions = clusterHelper.getClusterClient().getPartitions();
        for (RedisClusterNode partition : partitions) {

            if (!partition.getSlots().isEmpty()) {
                RedisClusterAsyncCommands<String, String> nodeConnection = connection.getConnection(partition.getNodeId());

                for (Integer slot : partition.getSlots()) {
                    nodeConnection.clusterDelSlots(slot);
                }
            }
        }

        RedisClusterAsyncCommands<String, String> node1 = connection.getConnection(ClusterTestSettings.host,
                ClusterTestSettings.port5);
        node1.clusterAddSlots(ClusterTestSettings.createSlots(0, 12000));

        RedisClusterAsyncCommands<String, String> node2 = connection.getConnection(ClusterTestSettings.host,
                ClusterTestSettings.port6);
        node2.clusterAddSlots(ClusterTestSettings.createSlots(12000, 16384));

        Wait.untilTrue(clusterHelper::isStable).waitOrTimeout();

        Wait.untilEquals(2L, () -> {
            clusterHelper.getClusterClient().reloadPartitions();

            return partitionStream(clusterHelper)
                    .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.UPSTREAM)).count();
        }).waitOrTimeout();

        connection.getStatefulConnection().close();
    }

    /**
     * Setup a cluster consisting of two members (see {@link ClusterTestSettings#port5} to {@link ClusterTestSettings#port6}).
     * One master (0-16383) and one replica.
     *
     * @param clusterHelper
     */
    public static void setupMasterWithReplica(ClusterTestHelper clusterHelper) {
        clusterHelper.clusterReset();
        clusterHelper.meet(ClusterTestSettings.host, ClusterTestSettings.port5);
        clusterHelper.meet(ClusterTestSettings.host, ClusterTestSettings.port6);

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterHelper.getClusterClient().connect().async();
        StatefulRedisClusterConnection<String, String> statefulConnection = connection.getStatefulConnection();

        Wait.untilEquals(2, () -> {
            clusterHelper.getClusterClient().reloadPartitions();
            return clusterHelper.getClusterClient().getPartitions().size();
        }).waitOrTimeout();

        RedisClusterCommands<String, String> node1 = statefulConnection
                .getConnection(TestSettings.hostAddr(), ClusterTestSettings.port5).sync();
        node1.clusterAddSlots(ClusterTestSettings.createSlots(0, 16384));

        Wait.untilTrue(clusterHelper::isStable).waitOrTimeout();

        TestFutures.awaitOrTimeout(connection.getConnection(ClusterTestSettings.host, ClusterTestSettings.port6)
                .clusterReplicate(
                node1.clusterMyId()));

        clusterHelper.getClusterClient().reloadPartitions();

        Wait.untilEquals(1L, () -> {
            clusterHelper.getClusterClient().reloadPartitions();
            return partitionStream(clusterHelper)
                    .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.UPSTREAM)).count();
        }).waitOrTimeout();

        Wait.untilEquals(1L, () -> {
            clusterHelper.getClusterClient().reloadPartitions();
            return partitionStream(clusterHelper)
                    .filter(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA))
                    .count();
        }).waitOrTimeout();

        connection.getStatefulConnection().close();
    }

    private static Stream<RedisClusterNode> partitionStream(ClusterTestHelper clusterHelper) {
        return clusterHelper.getClusterClient().getPartitions().getPartitions().stream();
    }

}
