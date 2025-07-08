/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.cluster.ClusterTestSettings.*;
import static io.lettuce.core.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.category.SlowTests;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.Range;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.DefaultRedisClient;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Test for mutable cluster setup scenarios.
 *
 * @author Mark Paluch
 * @author dengliming
 * @author Hari Mani
 * @since 3.0
 */
@Tag(INTEGRATION_TEST)
@SuppressWarnings({ "unchecked" })
@SlowTests
public class RedisClusterSetupIntegrationTests extends TestSupport {

    private static final String host = TestSettings.hostAddr();

    private static final ClusterTopologyRefreshOptions PERIODIC_REFRESH_ENABLED = ClusterTopologyRefreshOptions.builder()
            .enablePeriodicRefresh(Duration.ofSeconds(1)).dynamicRefreshSources(false).build();

    private static RedisClusterClient clusterClient;

    private static final RedisClient client = DefaultRedisClient.get();

    private static ClusterTestHelper clusterHelper;

    private StatefulRedisConnection<String, String> redisConnection1;

    private StatefulRedisConnection<String, String> redisConnection2;

    private RedisCommands<String, String> redis1;

    private RedisCommands<String, String> redis2;

    @BeforeAll
    public static void setupClient() {
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, ClusterTestSettings.port5).build());
        clusterHelper = new ClusterTestHelper(clusterClient, ClusterTestSettings.port5, ClusterTestSettings.port6);
    }

    @AfterAll
    public static void shutdownClient() {
        FastShutdown.shutdown(clusterClient);
    }

    @BeforeEach
    public void openConnection() {
        clusterHelper.flushdb();
        redisConnection1 = client.connect(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port5).build());
        redis1 = redisConnection1.sync();
        redisConnection2 = client.connect(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port6).build());
        redis2 = redisConnection2.sync();
        clusterHelper.clusterReset();
    }

    @AfterEach
    public void closeConnection() {
        redisConnection1.close();
        redisConnection2.close();
    }

    @Test
    public void clusterMeet() {

        clusterHelper.clusterReset();

        Partitions partitionsBeforeMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsBeforeMeet.getPartitions()).hasSize(1);

        String result = redis1.clusterMeet(host, ClusterTestSettings.port6);
        assertThat(result).isEqualTo("OK");

        Wait.untilEquals(2, () -> ClusterPartitionParser.parse(redis1.clusterNodes()).size()).waitOrTimeout();

        Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsAfterMeet.getPartitions()).hasSize(2);
    }

    @Test
    public void clusterForget() {

        clusterHelper.clusterReset();

        String result = redis1.clusterMeet(host, ClusterTestSettings.port6);
        assertThat(result).isEqualTo("OK");
        Wait.untilTrue(() -> redis1.clusterNodes().contains(redis2.clusterMyId())).waitOrTimeout();
        Wait.untilTrue(() -> redis2.clusterNodes().contains(redis1.clusterMyId())).waitOrTimeout();
        Wait.untilTrue(() -> {
            Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
            if (partitions.size() != 2) {
                return false;
            }
            for (RedisClusterNode redisClusterNode : partitions) {
                if (redisClusterNode.is(RedisClusterNode.NodeFlag.HANDSHAKE)) {
                    return false;
                }
            }
            return true;
        }).waitOrTimeout();

        redis1.clusterForget(redis2.clusterMyId());

        Wait.untilEquals(1, () -> ClusterPartitionParser.parse(redis1.clusterNodes()).size()).waitOrTimeout();

        Partitions partitionsAfterForget = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsAfterForget.getPartitions()).hasSize(1);
    }

    @Test
    public void clusterDelSlots() {

        ClusterSetup.setup2Masters(clusterHelper);

        redis1.clusterDelSlots(1, 2, 5, 6);

        Wait.untilEquals(11996, () -> getOwnPartition(redis1).getSlots().size()).waitOrTimeout();
    }

    @Test
    @EnabledOnCommand("SINTERCARD") // Redis 7
    public void clusterDelSlotsRange() {

        ClusterSetup.setup2Masters(clusterHelper);

        redis1.clusterDelSlotsRange(Range.create(1, 4), Range.create(5, 6));

        Wait.untilEquals(11994, () -> getOwnPartition(redis1).getSlots().size()).waitOrTimeout();
    }

    @Test
    public void clusterSetSlots() {

        ClusterSetup.setup2Masters(clusterHelper);

        redis1.clusterSetSlotNode(6, getNodeId(redis2));

        waitForSlots(redis1, 11999);
        waitForSlots(redis2, 4384);

        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
        for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                assertThat(redisClusterNode.getSlots()).contains(1, 2, 3, 4, 5).doesNotContain(6);
            }
        }
    }

    @Test
    public void clusterSlotMigrationImport() {

        ClusterSetup.setup2Masters(clusterHelper);

        String nodeId2 = getNodeId(redis2);
        assertThat(redis1.clusterSetSlotMigrating(6, nodeId2)).isEqualTo("OK");
        assertThat(redis1.clusterSetSlotImporting(15000, nodeId2)).isEqualTo("OK");

        assertThat(redis1.clusterSetSlotStable(6)).isEqualTo("OK");
    }

    @Test
    public void clusterTopologyRefresh() {
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(PERIODIC_REFRESH_ENABLED).build());
        clusterClient.reloadPartitions();
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            assertThat(clusterClient.getPartitions()).hasSize(1);
            ClusterSetup.setup2Masters(clusterHelper);
            assertThat(clusterClient.getPartitions()).hasSize(2);
        }
    }

    @Test
    public void changeTopologyWhileOperations() {

        ClusterSetup.setup2Masters(clusterHelper);

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enableAllAdaptiveRefreshTriggers().build();

        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(clusterTopologyRefreshOptions).build());
        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            RedisAdvancedClusterCommands<String, String> sync = connection.sync();
            RedisAdvancedClusterAsyncCommands<String, String> async = connection.async();

            Partitions partitions = connection.getPartitions();
            assertThat(partitions.getPartitionBySlot(0).getSlots().size()).isEqualTo(12000);
            assertThat(partitions.getPartitionBySlot(16380).getSlots().size()).isEqualTo(4384);
            assertRoutedExecution(async);

            sync.del("A");
            sync.del("t");
            sync.del("p");

            shiftAllSlotsToNode1();
            assertRoutedExecution(async);

            Wait.untilTrue(() -> {
                if (clusterClient.getPartitions().size() == 2) {
                    for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
                        if (redisClusterNode.getSlots().size() > 16380) {
                            return true;
                        }
                    }
                }

                return false;
            }).waitOrTimeout();

            assertThat(partitions.getPartitionBySlot(0).getSlots().size()).isEqualTo(16384);

            assertThat(sync.get("A")).isEqualTo("value");
            assertThat(sync.get("t")).isEqualTo("value");
            assertThat(sync.get("p")).isEqualTo("value");
        }
    }

    @Test
    public void slotMigrationShouldUseAsking() {
        ClusterSetup.setup2Masters(clusterHelper);
        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {

            RedisAdvancedClusterCommands<String, String> sync = connection.sync();
            RedisAdvancedClusterAsyncCommands<String, String> async = connection.async();

            Partitions partitions = connection.getPartitions();
            assertThat(partitions.getPartitionBySlot(0).getSlots().size()).isEqualTo(12000);
            assertThat(partitions.getPartitionBySlot(16380).getSlots().size()).isEqualTo(4384);

            redis1.clusterSetSlotMigrating(3300, redis2.clusterMyId());
            redis2.clusterSetSlotImporting(3300, redis1.clusterMyId());

            assertThat(sync.get("b")).isNull();
        }
    }

    @Test
    public void disconnectedConnectionRejectTest() throws Exception {

        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(PERIODIC_REFRESH_ENABLED)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncCommands = clusterConnection.async();
            clusterClient.setOptions(
                    ClusterClientOptions.builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                            .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());
            ClusterSetup.setup2Masters(clusterHelper);

            assertRoutedExecution(clusterAsyncCommands);

            RedisClusterNode partition1 = getOwnPartition(redis1);
            StatefulRedisConnection<String, String> node1Connection = clusterConnection
                    .getConnection(partition1.getUri().getHost(), partition1.getUri().getPort());

            shiftAllSlotsToNode1();

            suspendConnection(node1Connection);

            RedisFuture<String> set = clusterAsyncCommands.set("t", "value"); // 15891

            set.await(5, TimeUnit.SECONDS);

            assertThatThrownBy(() -> TestFutures.awaitOrTimeout(set)).hasRootCauseInstanceOf(RedisException.class);
        }
    }

    @Test
    public void atLeastOnceForgetNodeFailover() throws Exception {

        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(PERIODIC_REFRESH_ENABLED)
                .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncCommands = clusterConnection.async();
            clusterClient.setOptions(ClusterClientOptions.builder()
                    .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());
            ClusterSetup.setup2Masters(clusterHelper);

            assertRoutedExecution(clusterAsyncCommands);

            RedisClusterNode partition1 = getOwnPartition(redis1);
            RedisClusterNode partition2 = getOwnPartition(redis2);
            RedisClusterAsyncCommands<String, String> node1Connection = clusterAsyncCommands
                    .getConnection(partition1.getUri().getHost(), partition1.getUri().getPort());

            StatefulRedisConnection<String, String> node2Connection = clusterConnection
                    .getConnection(partition2.getUri().getHost(), partition2.getUri().getPort());

            shiftAllSlotsToNode1();

            suspendConnection(node2Connection);

            List<RedisFuture<String>> futures = new ArrayList<>();

            futures.add(clusterAsyncCommands.set("t", "value")); // 15891
            futures.add(clusterAsyncCommands.set("p", "value")); // 16023

            clusterAsyncCommands.set("A", "value").get(1, TimeUnit.SECONDS); // 6373

            for (RedisFuture<String> future : futures) {
                assertThat(future.isDone()).isFalse();
                assertThat(future.isCancelled()).isFalse();
            }
            redis1.clusterForget(partition2.getNodeId());
            redis2.clusterForget(partition1.getNodeId());

            Partitions partitions = clusterClient.getPartitions();
            partitions.remove(partition2);
            partitions.getPartition(0).setSlots(Arrays.stream(createSlots(0, 16384)).boxed().collect(Collectors.toList()));
            partitions.updateCache();
            clusterClient.updatePartitionsInConnections();

            Wait.untilTrue(() -> TestFutures.areAllDone(futures)).waitOrTimeout();

            assertRoutedExecution(clusterAsyncCommands);
        }
    }

    @Test
    public void expireStaleDefaultConnection() {

        ClusterSetup.setup2Masters(clusterHelper);

        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, port5).build());
        redisClusterClient.setOptions(ClusterClientOptions.builder()
                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder().dynamicRefreshSources(false).build()).build());

        StatefulRedisClusterConnectionImpl<String, String> connection = (StatefulRedisClusterConnectionImpl<String, String>) redisClusterClient
                .connect();
        String firstMaster = connection.sync().clusterMyId();

        RedisClusterNode firstMasterNode = connection.getPartitions().getPartitionByNodeId(firstMaster);

        assertThat(firstMasterNode.getUri().getPort()).isEqualTo(port5);

        redis2.clusterForget(redis1.clusterMyId());
        redis1.clusterForget(redis2.clusterMyId());

        Partitions partitions = ClusterPartitionParser.parse(redis2.clusterNodes());
        connection.setPartitions(partitions);

        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        String secondMaster = connection.sync().clusterMyId();
        assertThat(secondMaster).isEqualTo(redis2.clusterMyId());
        connection.close();

        FastShutdown.shutdown(redisClusterClient);
    }

    @Test
    public void expireStaleDefaultPubSubConnection() {

        ClusterSetup.setup2Masters(clusterHelper);

        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, port5).build());
        redisClusterClient.setOptions(ClusterClientOptions.builder()
                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder().dynamicRefreshSources(false).build()).build());

        StatefulRedisClusterPubSubConnectionImpl<String, String> connection = (StatefulRedisClusterPubSubConnectionImpl<String, String>) redisClusterClient
                .connectPubSub();
        String firstMaster = connection.sync().clusterMyId();

        RedisClusterNode firstMasterNode = connection.getPartitions().getPartitionByNodeId(firstMaster);

        assertThat(firstMasterNode.getUri().getPort()).isEqualTo(port5);

        redis2.clusterForget(redis1.clusterMyId());
        redis1.clusterForget(redis2.clusterMyId());

        Partitions partitions = ClusterPartitionParser.parse(redis2.clusterNodes());
        connection.setPartitions(partitions);

        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        String secondMaster = connection.sync().clusterMyId();
        assertThat(secondMaster).isEqualTo(redis2.clusterMyId());
        connection.close();

        FastShutdown.shutdown(redisClusterClient);
    }

    @Test
    public void expireStaleNodeIdConnections() {

        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(PERIODIC_REFRESH_ENABLED).build());
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncCommands = clusterConnection.async();

            ClusterSetup.setup2Masters(clusterHelper);

            PooledClusterConnectionProvider<String, String> clusterConnectionProvider = getPooledClusterConnectionProvider(
                    clusterConnection);

            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(0);

            assertRoutedExecution(clusterAsyncCommands);

            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(2);

            Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
            for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
                if (!redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                    redis1.clusterForget(redisClusterNode.getNodeId());
                }
            }

            partitions = ClusterPartitionParser.parse(redis2.clusterNodes());
            for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
                if (!redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                    redis2.clusterForget(redisClusterNode.getNodeId());
                }
            }

            Wait.untilEquals(1, () -> clusterClient.getPartitions().size()).waitOrTimeout();
            Wait.untilEquals(1, clusterConnectionProvider::getConnectionCount).waitOrTimeout();
        }
    }

    private void assertRoutedExecution(RedisClusterAsyncCommands<String, String> clusterConnection) {
        assertExecuted(clusterConnection.set("A", "value")); // 6373
        assertExecuted(clusterConnection.set("t", "value")); // 15891
        assertExecuted(clusterConnection.set("p", "value")); // 16023
    }

    @Test
    public void doNotExpireStaleNodeIdConnections() {

        clusterClient.setOptions(ClusterClientOptions.builder()
                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder().closeStaleConnections(false).build()).build());
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncCommands = clusterConnection.async();

            ClusterSetup.setup2Masters(clusterHelper);

            PooledClusterConnectionProvider<String, String> clusterConnectionProvider = getPooledClusterConnectionProvider(
                    clusterConnection);

            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(0);

            assertRoutedExecution(clusterAsyncCommands);

            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(2);

            Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
            for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
                if (!redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                    redis1.clusterForget(redisClusterNode.getNodeId());
                }
            }

            partitions = ClusterPartitionParser.parse(redis2.clusterNodes());
            for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
                if (!redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                    redis2.clusterForget(redisClusterNode.getNodeId());
                }
            }

            clusterClient.reloadPartitions();

            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(2);
        }
    }

    @Test
    public void expireStaleHostAndPortConnections() {
        clusterClient.setOptions(ClusterClientOptions.builder().build());
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncCommands = clusterConnection.async();

            ClusterSetup.setup2Masters(clusterHelper);

            final PooledClusterConnectionProvider<String, String> clusterConnectionProvider = getPooledClusterConnectionProvider(
                    clusterConnection);

            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(0);

            assertRoutedExecution(clusterAsyncCommands);
            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(2);

            for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
                clusterAsyncCommands.getConnection(redisClusterNode.getUri().getHost(), redisClusterNode.getUri().getPort());
                clusterAsyncCommands.getConnection(redisClusterNode.getNodeId());
            }

            assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(4);

            Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
            for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
                if (!redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                    redis1.clusterForget(redisClusterNode.getNodeId());
                }
            }

            partitions = ClusterPartitionParser.parse(redis2.clusterNodes());
            for (RedisClusterNode redisClusterNode : partitions.getPartitions()) {
                if (!redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                    redis2.clusterForget(redisClusterNode.getNodeId());
                }
            }

            clusterClient.reloadPartitions();

            Wait.untilEquals(1, () -> clusterClient.getPartitions().size()).waitOrTimeout();
            Wait.untilEquals(2L, clusterConnectionProvider::getConnectionCount).waitOrTimeout();
        }
    }

    @Test
    public void readFromReplicaTest() {
        ClusterSetup.setup2Masters(clusterHelper);
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncCommands = clusterConnection.async();
            clusterConnection.setReadFrom(ReadFrom.REPLICA);

            TestFutures.awaitOrTimeout(clusterAsyncCommands.set(key, value));

            try {
                clusterAsyncCommands.get(key);
            } catch (RedisException e) {
                assertThat(e).hasMessageContaining("Cannot determine a partition to read for slot");
            }
        }
    }

    @Test
    public void readFromNearestTest() {
        ClusterSetup.setup2Masters(clusterHelper);
        try (StatefulRedisClusterConnection<String, String> clusterConnection = clusterClient.connect()) {
            RedisAdvancedClusterCommands<String, String> clusterCommands = clusterConnection.sync();
            clusterConnection.setReadFrom(ReadFrom.NEAREST);

            clusterCommands.set(key, value);

            assertThat(clusterCommands.get(key)).isEqualTo(value);
        }
    }

    private PooledClusterConnectionProvider<String, String> getPooledClusterConnectionProvider(
            StatefulRedisClusterConnection<String, String> clusterAsyncConnection) {

        RedisChannelHandler<String, String> channelHandler = getChannelHandler(clusterAsyncConnection);
        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) channelHandler.getChannelWriter();
        return (PooledClusterConnectionProvider<String, String>) writer.getClusterConnectionProvider();
    }

    private RedisChannelHandler<String, String> getChannelHandler(
            StatefulRedisClusterConnection<String, String> clusterAsyncConnection) {
        return (RedisChannelHandler<String, String>) clusterAsyncConnection;
    }

    private void assertExecuted(RedisFuture<String> set) {
        TestFutures.awaitOrTimeout(set);
        assertThat(set.getError()).isNull();
        assertThat(TestFutures.getOrTimeout(set)).isEqualTo("OK");
    }

    private void suspendConnection(final StatefulRedisConnection<String, String> statefulRedisConnection) {
        ConnectionTestUtil.getConnectionWatchdog(statefulRedisConnection).setReconnectSuspended(true);
        statefulRedisConnection.sync().quit();
        Wait.untilTrue(() -> !statefulRedisConnection.isOpen()).waitOrTimeout();
    }

    private void shiftAllSlotsToNode1() {

        redis1.clusterDelSlots(createSlots(12000, 16384));
        redis2.clusterDelSlots(createSlots(12000, 16384));

        waitForSlots(redis2, 0);

        RedisClusterNode redis2Partition = getOwnPartition(redis2);

        Wait.untilTrue(new Supplier<Boolean>() {

            @Override
            public Boolean get() {
                Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
                RedisClusterNode partition = partitions.getPartitionByNodeId(redis2Partition.getNodeId());

                if (!partition.getSlots().isEmpty()) {
                    removeRemaining(partition);
                }

                return partition.getSlots().isEmpty();
            }

            private void removeRemaining(RedisClusterNode partition) {
                try {
                    redis1.clusterDelSlots(toIntArray(partition.getSlots()));
                } catch (Exception o_O) {
                    // ignore
                }
            }

        }).waitOrTimeout();

        redis1.clusterAddSlots(createSlots(12000, 16384));
        waitForSlots(redis1, 16384);

        Wait.untilTrue(clusterHelper::isStable).waitOrTimeout();
    }

    private int[] toIntArray(List<Integer> list) {
        return list.parallelStream().mapToInt(Integer::intValue).toArray();
    }

    private void waitForSlots(RedisClusterCommands<String, String> connection, int slotCount) {
        Wait.untilEquals(slotCount, () -> getOwnPartition(connection).getSlots().size()).waitOrTimeout();
    }

}
