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
package io.lettuce.core.cluster.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.lettuce.test.ReflectionTestUtils;

import io.lettuce.category.SlowTests;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.async.BaseRedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * Test for topology refreshing.
 *
 * @author Mark Paluch
 */
@SuppressWarnings({ "unchecked" })
@SlowTests
@ExtendWith(LettuceExtension.class)
class TopologyRefreshIntegrationTests extends TestSupport {

    private static final String host = TestSettings.hostAddr();
    private final RedisClient client;

    private RedisClusterClient clusterClient;
    private RedisCommands<String, String> redis1;
    private RedisCommands<String, String> redis2;

    @Inject
    TopologyRefreshIntegrationTests(RedisClient client) {
        this.client = client;
    }

    @BeforeEach
    void openConnection() {
        clusterClient = RedisClusterClient.create(client.getResources(), RedisURI.Builder
                .redis(host, ClusterTestSettings.port1).build());
        redis1 = client.connect(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1).build()).sync();
        redis2 = client.connect(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port2).build()).sync();
    }

    @AfterEach
    void closeConnection() {
        redis1.getStatefulConnection().close();
        redis2.getStatefulConnection().close();
        FastShutdown.shutdown(clusterClient);
    }

    @Test
    void changeTopologyWhileOperations() {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(true)//
                .refreshPeriod(1, TimeUnit.SECONDS)//
                .build();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterClient.getPartitions().clear();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterConnection.getStatefulConnection().close();
    }

    @Test
    void dynamicSourcesProvidesClientCountForAllNodes() {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.create();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            assertThat(redisClusterNode).isInstanceOf(RedisClusterNodeSnapshot.class);

            RedisClusterNodeSnapshot snapshot = (RedisClusterNodeSnapshot) redisClusterNode;
            assertThat(snapshot.getConnectedClients()).isNotNull().isGreaterThanOrEqualTo(0);
        }

        clusterConnection.getStatefulConnection().close();
    }

    @Test
    void staticSourcesProvidesClientCountForSeedNodes() {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .dynamicRefreshSources(false).build();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        Partitions partitions = clusterClient.getPartitions();
        RedisClusterNodeSnapshot node1 = (RedisClusterNodeSnapshot) partitions.getPartitionBySlot(0);
        assertThat(node1.getConnectedClients()).isGreaterThanOrEqualTo(1);

        RedisClusterNodeSnapshot node2 = (RedisClusterNodeSnapshot) partitions.getPartitionBySlot(15000);
        assertThat(node2.getConnectedClients()).isNull();

        clusterConnection.getStatefulConnection().close();
    }

    @Test
    void adaptiveTopologyUpdateOnDisconnectNodeIdConnection() {

        runReconnectTest((clusterConnection, node) -> {
            RedisClusterAsyncCommands<String, String> connection = clusterConnection.getConnection(node.getUri().getHost(),
                    node.getUri().getPort());

            return connection;
        });
    }

    @Test
    void adaptiveTopologyUpdateOnDisconnectHostAndPortConnection() {

        runReconnectTest((clusterConnection, node) -> {
            RedisClusterAsyncCommands<String, String> connection = clusterConnection.getConnection(node.getUri().getHost(),
                    node.getUri().getPort());

            return connection;
        });
    }

    @Test
    void adaptiveTopologyUpdateOnDisconnectDefaultConnection() {

        runReconnectTest((clusterConnection, node) -> {
            return clusterConnection;
        });
    }

    @Test
    void adaptiveTopologyUpdateIsRateLimited() {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .adaptiveRefreshTriggersTimeout(1, TimeUnit.HOURS)//
                .refreshTriggersReconnectAttempts(0)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterClient.getPartitions().clear();
        clusterConnection.quit();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterClient.getPartitions().clear();
        clusterConnection.quit();

        Delay.delay(Duration.ofMillis(200));

        assertThat(clusterClient.getPartitions()).isEmpty();

        clusterConnection.getStatefulConnection().close();
    }

    @Test
    void adaptiveTopologyUpdatetUsesTimeout() {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .adaptiveRefreshTriggersTimeout(500, TimeUnit.MILLISECONDS)//
                .refreshTriggersReconnectAttempts(0)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterConnection.quit();
        Delay.delay(Duration.ofMillis(700));

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterClient.getPartitions().clear();
        clusterConnection.quit();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterConnection.getStatefulConnection().close();
    }

    @Test
    void adaptiveTriggerDoesNotFireOnSingleReconnect() {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterClient.getPartitions().clear();

        clusterConnection.quit();
        Delay.delay(Duration.ofMillis(500));

        assertThat(clusterClient.getPartitions()).isEmpty();
        clusterConnection.getStatefulConnection().close();
    }

    @Test
    void adaptiveTriggerOnMoveRedirection() {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT)//
                .build();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = connection.async();

        Partitions partitions = connection.getPartitions();
        RedisClusterNode node1 = partitions.getPartitionBySlot(0);
        RedisClusterNode node2 = partitions.getPartitionBySlot(12000);

        List<Integer> slots = node2.getSlots();
        slots.addAll(node1.getSlots());
        node2.setSlots(slots);
        node1.setSlots(Collections.emptyList());
        partitions.updateCache();

        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node1.getNodeId()).getSlots()).hasSize(0);
        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node2.getNodeId()).getSlots()).hasSize(16384);

        clusterConnection.set("b", value); // slot 3300

        Wait.untilEquals(12000, () -> clusterClient.getPartitions().getPartitionByNodeId(node1.getNodeId()).getSlots().size())
                .waitOrTimeout();

        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node1.getNodeId()).getSlots()).hasSize(12000);
        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node2.getNodeId()).getSlots()).hasSize(4384);
        clusterConnection.getStatefulConnection().close();
    }

    private void runReconnectTest(
            BiFunction<RedisAdvancedClusterAsyncCommands<String, String>, RedisClusterNode, BaseRedisAsyncCommands> function) {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .refreshTriggersReconnectAttempts(0)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        RedisClusterNode node = clusterClient.getPartitions().getPartition(0);
        BaseRedisAsyncCommands closeable = function.apply(clusterConnection, node);
        clusterClient.getPartitions().clear();

        closeable.quit();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        if (closeable instanceof RedisAdvancedClusterCommands) {
            ((RedisAdvancedClusterCommands) closeable).getStatefulConnection().close();
        }
        clusterConnection.getStatefulConnection().close();
    }
}
