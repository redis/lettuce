package com.lambdaworks.redis.cluster.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.Wait;
import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.async.BaseRedisAsyncCommands;
import com.lambdaworks.redis.cluster.AbstractClusterTest;
import com.lambdaworks.redis.cluster.ClusterClientOptions;
import com.lambdaworks.redis.cluster.ClusterTopologyRefreshOptions;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Test for topology refreshing.
 *
 * @author Mark Paluch
 */
@SuppressWarnings({ "unchecked" })
@SlowTests
public class TopologyRefreshTest extends AbstractTest {

    public static final String host = TestSettings.hostAddr();
    private static RedisClient client = DefaultRedisClient.get();

    private RedisClusterClient clusterClient;
    private RedisClusterCommands<String, String> redis1;
    private RedisClusterCommands<String, String> redis2;

    @Before
    public void openConnection() throws Exception {
        clusterClient = RedisClusterClient.create(client.getResources(),
                RedisURI.Builder.redis(host, AbstractClusterTest.port1).build());
        redis1 = client.connect(RedisURI.Builder.redis(AbstractClusterTest.host, AbstractClusterTest.port1).build()).sync();
        redis2 = client.connect(RedisURI.Builder.redis(AbstractClusterTest.host, AbstractClusterTest.port2).build()).sync();
    }

    @After
    public void closeConnection() throws Exception {
        redis1.close();
        redis2.close();
        FastShutdown.shutdown(clusterClient);
    }

    @Test
    public void changeTopologyWhileOperations() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()
                .enablePeriodicRefresh(true)//
                .refreshPeriod(1, TimeUnit.SECONDS)//
                .build();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterClient.getPartitions().clear();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterConnection.close();
    }

    @Test
    public void dynamicSourcesProvidesClientCountForAllNodes() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.create();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            assertThat(redisClusterNode).isInstanceOf(RedisClusterNodeSnapshot.class);

            RedisClusterNodeSnapshot snapshot = (RedisClusterNodeSnapshot) redisClusterNode;
            assertThat(snapshot.getConnectedClients()).isNotNull().isGreaterThanOrEqualTo(0);
        }

        clusterConnection.close();
    }

    @Test
    public void staticSourcesProvidesClientCountForSeedNodes() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()
                .dynamicRefreshSources(false).build();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        Partitions partitions = clusterClient.getPartitions();
        RedisClusterNodeSnapshot node1 = (RedisClusterNodeSnapshot) partitions.getPartitionBySlot(0);
        assertThat(node1.getConnectedClients()).isGreaterThanOrEqualTo(1);

        RedisClusterNodeSnapshot node2 = (RedisClusterNodeSnapshot) partitions.getPartitionBySlot(15000);
        assertThat(node2.getConnectedClients()).isNull();

        clusterConnection.close();
    }

    @Test
    public void adaptiveTopologyUpdateOnDisconnectNodeIdConnection() throws Exception {

        runReconnectTest((clusterConnection, node) -> {
            RedisClusterAsyncCommands<String, String> connection = clusterConnection.getConnection(node.getUri().getHost(),
                    node.getUri().getPort());

            return connection;
        });
    }

    @Test
    public void adaptiveTopologyUpdateOnDisconnectHostAndPortConnection() throws Exception {

        runReconnectTest((clusterConnection, node) -> {
            RedisClusterAsyncCommands<String, String> connection = clusterConnection.getConnection(node.getUri().getHost(),
                    node.getUri().getPort());

            return connection;
        });
    }

    @Test
    public void adaptiveTopologyUpdateOnDisconnectDefaultConnection() throws Exception {

        runReconnectTest((clusterConnection, node) -> {
            return clusterConnection;
        });
    }

    @Test
    public void adaptiveTopologyUpdateIsRateLimited() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()//
                .adaptiveRefreshTriggersTimeout(1, TimeUnit.HOURS)//
                .refreshTriggersReconnectAttempts(0)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterClient.getPartitions().clear();
        clusterConnection.quit();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterClient.getPartitions().clear();
        clusterConnection.quit();

        Thread.sleep(1000);

        assertThat(clusterClient.getPartitions()).isEmpty();

        clusterConnection.close();
    }

    @Test
    public void adaptiveTopologyUpdatetUsesTimeout() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()//
                .adaptiveRefreshTriggersTimeout(500, TimeUnit.MILLISECONDS)//
                .refreshTriggersReconnectAttempts(0)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterConnection.quit();
        Thread.sleep(1000);

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterClient.getPartitions().clear();
        clusterConnection.quit();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterConnection.close();
    }

    @Test
    public void adaptiveTriggerDoesNotFireOnSingleReconnect() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        clusterClient.getPartitions().clear();

        clusterConnection.quit();
        Thread.sleep(500);

        assertThat(clusterClient.getPartitions()).isEmpty();
        clusterConnection.close();
    }

    @Test
    public void adaptiveTriggerOnMoveRedirection() throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()//
                .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT)//
                .build();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = connection.async();

        Partitions partitions = connection.getPartitions();
        RedisClusterNode node1 = partitions.getPartitionBySlot(0);
        RedisClusterNode node2 = partitions.getPartitionBySlot(12000);

        node2.getSlots().addAll(node1.getSlots());
        node1.getSlots().clear();
        partitions.updateCache();

        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node1.getNodeId()).getSlots()).hasSize(0);
        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node2.getNodeId()).getSlots()).hasSize(16384);

        clusterConnection.set("b", value); // slot 3300

        Wait.untilEquals(12000, new Wait.Supplier<Integer>() {
            @Override
            public Integer get() throws Exception {
                return clusterClient.getPartitions().getPartitionByNodeId(node1.getNodeId()).getSlots().size();
            }
        }).waitOrTimeout();

        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node1.getNodeId()).getSlots()).hasSize(12000);
        assertThat(clusterClient.getPartitions().getPartitionByNodeId(node2.getNodeId()).getSlots()).hasSize(4384);
        clusterConnection.close();
    }

    private void runReconnectTest(
            BiFunction<RedisAdvancedClusterAsyncCommands<String, String>, RedisClusterNode, BaseRedisAsyncCommands> function)
            throws Exception {

        ClusterTopologyRefreshOptions topologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()//
                .refreshTriggersReconnectAttempts(0)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();
        clusterClient.setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(topologyRefreshOptions).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        RedisClusterNode node = clusterClient.getPartitions().getPartition(0);
        BaseRedisAsyncCommands closeable = function.apply(clusterConnection, node);
        clusterClient.getPartitions().clear();

        closeable.quit();

        Wait.untilTrue(() -> {
            return !clusterClient.getPartitions().isEmpty();
        }).waitOrTimeout();

        clusterConnection.close();
    }
}
