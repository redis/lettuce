package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getNodeId;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.*;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.Connections;
import com.lambdaworks.Futures;
import com.lambdaworks.Wait;
import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Test for mutable cluster setup scenarios.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings({ "unchecked" })
@SlowTests
public class RedisClusterSetupTest extends AbstractTest {

    public static final String host = TestSettings.hostAddr();

    private static RedisClusterClient clusterClient;
    private static RedisClient client = DefaultRedisClient.get();

    private RedisClusterCommands<String, String> redis1;
    private RedisClusterCommands<String, String> redis2;

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, AbstractClusterTest.port5, AbstractClusterTest.port6);

    @BeforeClass
    public static void setupClient() {
        clusterClient = RedisClusterClient.create(RedisURI.Builder.redis(host, AbstractClusterTest.port5).build());
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void openConnection() throws Exception {
        redis1 = client.connect(RedisURI.Builder.redis(AbstractClusterTest.host, AbstractClusterTest.port5).build()).sync();
        redis2 = client.connect(RedisURI.Builder.redis(AbstractClusterTest.host, AbstractClusterTest.port6).build()).sync();
        clusterRule.clusterReset();
    }

    @After
    public void closeConnection() throws Exception {
        redis1.close();
        redis2.close();
    }

    @Test
    public void clusterMeet() throws Exception {

        clusterRule.clusterReset();

        Partitions partitionsBeforeMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsBeforeMeet.getPartitions()).hasSize(1);

        String result = redis1.clusterMeet(host, AbstractClusterTest.port6);
        assertThat(result).isEqualTo("OK");

        Wait.untilEquals(2, () -> ClusterPartitionParser.parse(redis1.clusterNodes()).size()).waitOrTimeout();

        Partitions partitionsAfterMeet = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsAfterMeet.getPartitions()).hasSize(2);
    }

    @Test
    public void clusterForget() throws Exception {

        clusterRule.clusterReset();

        String result = redis1.clusterMeet(host, AbstractClusterTest.port6);
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

        Wait.untilEquals(1, () -> ClusterPartitionParser.parse(redis1.clusterNodes()).size());

        Partitions partitionsAfterForget = ClusterPartitionParser.parse(redis1.clusterNodes());
        assertThat(partitionsAfterForget.getPartitions()).hasSize(1);
    }

    @Test
    public void clusterDelSlots() throws Exception {

        ClusterSetup.setup2Masters(clusterRule);

        redis1.clusterDelSlots(1, 2, 5, 6);

        Wait.untilEquals(16380, () -> getOwnPartition(redis1).getSlots().size());
    }

    @Test
    public void clusterSetSlots() throws Exception {

        ClusterSetup.setup2Masters(clusterRule);

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
    public void clusterSlotMigrationImport() throws Exception {

        ClusterSetup.setup2Masters(clusterRule);

        String nodeId2 = getNodeId(redis2);
        assertThat(redis1.clusterSetSlotMigrating(6, nodeId2)).isEqualTo("OK");
        assertThat(redis1.clusterSetSlotImporting(15000, nodeId2)).isEqualTo("OK");

        assertThat(redis1.clusterSetSlotStable(6)).isEqualTo("OK");
    }

    @Test
    public void clusterTopologyRefresh() throws Exception {

        clusterClient.setOptions(
                new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(5, TimeUnit.SECONDS).build());
        clusterClient.reloadPartitions();

        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();
        assertThat(clusterClient.getPartitions()).hasSize(1);

        ClusterSetup.setup2Masters(clusterRule);
        assertThat(clusterClient.getPartitions()).hasSize(2);

        clusterConnection.close();
    }

    @Test
    public void changeTopologyWhileOperations() throws Exception {

        ClusterSetup.setup2Masters(clusterRule);

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()
                .enableAllAdaptiveRefreshTriggers().build();

        clusterClient
                .setOptions(new ClusterClientOptions.Builder().topologyRefreshOptions(clusterTopologyRefreshOptions).build());
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
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

        async.close();
    }

    @Test
    public void disconnectedConnectionRejectTest() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(1, TimeUnit.SECONDS)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();
        clusterClient.setOptions(new ClusterClientOptions.Builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).refreshClusterView(false).build());
        ClusterSetup.setup2Masters(clusterRule);

        assertRoutedExecution(clusterConnection);

        RedisClusterNode partition1 = getOwnPartition(redis1);
        RedisClusterAsyncCommands<String, String> node1Connection = clusterConnection
                .getConnection(partition1.getUri().getHost(), partition1.getUri().getPort());

        shiftAllSlotsToNode1();

        suspendConnection(node1Connection);

        RedisFuture<String> set = clusterConnection.set("t", "value"); // 15891

        set.await(5, TimeUnit.SECONDS);
        try {
            set.get();
            fail("Missing RedisException");
        } catch (ExecutionException e) {
            assertThat(e).hasRootCauseInstanceOf(RedisException.class).hasMessageContaining("not connected");
        } finally {
            clusterConnection.close();
        }
    }

    @Test
    public void atLeastOnceForgetNodeFailover() throws Exception {

        clusterClient.setOptions(
                new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(1, TimeUnit.SECONDS).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();
        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(false).build());
        ClusterSetup.setup2Masters(clusterRule);

        assertRoutedExecution(clusterConnection);

        RedisClusterNode partition1 = getOwnPartition(redis1);
        RedisClusterNode partition2 = getOwnPartition(redis2);
        RedisClusterAsyncCommands<String, String> node2Connection = clusterConnection
                .getConnection(partition2.getUri().getHost(), partition2.getUri().getPort());

        shiftAllSlotsToNode1();

        suspendConnection(node2Connection);

        List<RedisFuture<String>> futures = new ArrayList<>();

        futures.add(clusterConnection.set("t", "value")); // 15891
        futures.add(clusterConnection.set("p", "value")); // 16023

        clusterConnection.set("A", "value").get(1, TimeUnit.SECONDS); // 6373

        for (RedisFuture<String> future : futures) {
            assertThat(future.isDone()).isFalse();
            assertThat(future.isCancelled()).isFalse();
        }
        redis1.clusterForget(partition2.getNodeId());
        redis2.clusterForget(partition1.getNodeId());

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).build());
        waitUntilOnlyOnePartition();

        Wait.untilTrue(() -> Futures.areAllCompleted(futures)).waitOrTimeout();

        assertRoutedExecution(clusterConnection);

        clusterConnection.close();

    }

    @Test
    public void expireStaleNodeIdConnections() throws Exception {

        clusterClient.setOptions(
                new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(1, TimeUnit.SECONDS).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();

        ClusterSetup.setup2Masters(clusterRule);

        PooledClusterConnectionProvider<String, String> clusterConnectionProvider = getPooledClusterConnectionProvider(
                clusterConnection);

        assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(0);

        assertRoutedExecution(clusterConnection);

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
        Wait.untilEquals(1, () -> clusterConnectionProvider.getConnectionCount()).waitOrTimeout();

        clusterConnection.close();

    }

    private void assertRoutedExecution(RedisClusterAsyncCommands<String, String> clusterConnection) throws Exception {
        assertExecuted(clusterConnection.set("A", "value")); // 6373
        assertExecuted(clusterConnection.set("t", "value")); // 15891
        assertExecuted(clusterConnection.set("p", "value")); // 16023
    }

    @Test
    public void doNotExpireStaleNodeIdConnections() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().closeStaleConnections(false).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();

        ClusterSetup.setup2Masters(clusterRule);

        PooledClusterConnectionProvider<String, String> clusterConnectionProvider = getPooledClusterConnectionProvider(
                clusterConnection);

        assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(0);

        assertRoutedExecution(clusterConnection);

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

        clusterConnection.close();

    }

    @Test
    public void expireStaleHostAndPortConnections() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();

        ClusterSetup.setup2Masters(clusterRule);

        final PooledClusterConnectionProvider<String, String> clusterConnectionProvider = getPooledClusterConnectionProvider(
                clusterConnection);

        assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(0);

        assertRoutedExecution(clusterConnection);
        assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(2);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            clusterConnection.getConnection(redisClusterNode.getUri().getHost(), redisClusterNode.getUri().getPort());
            clusterConnection.getConnection(redisClusterNode.getNodeId());
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
        Wait.untilEquals(2L, () -> clusterConnectionProvider.getConnectionCount()).waitOrTimeout();

        clusterConnection.close();
    }

    @Test
    public void readFromSlaveTest() throws Exception {

        ClusterSetup.setup2Masters(clusterRule);
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connect().async();
        clusterConnection.getStatefulConnection().setReadFrom(ReadFrom.SLAVE);

        clusterConnection.set(key, value).get();

        try {
            clusterConnection.get(key);
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Cannot determine a partition to read for slot");
        }

        clusterConnection.close();
    }

    @Test
    public void readFromNearestTest() throws Exception {

        ClusterSetup.setup2Masters(clusterRule);
        RedisAdvancedClusterCommands<String, String> clusterConnection = clusterClient.connect().sync();
        clusterConnection.getStatefulConnection().setReadFrom(ReadFrom.NEAREST);

        clusterConnection.set(key, value);

        assertThat(clusterConnection.get(key)).isEqualTo(value);

        clusterConnection.close();
    }

    protected PooledClusterConnectionProvider<String, String> getPooledClusterConnectionProvider(
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncConnection) {

        RedisChannelHandler<String, String> channelHandler = getChannelHandler(clusterAsyncConnection);
        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) channelHandler.getChannelWriter();
        return (PooledClusterConnectionProvider<String, String>) writer.getClusterConnectionProvider();
    }

    private RedisChannelHandler<String, String> getChannelHandler(
            RedisAdvancedClusterAsyncCommands<String, String> clusterAsyncConnection) {
        return (RedisChannelHandler<String, String>) clusterAsyncConnection.getStatefulConnection();
    }

    private void assertExecuted(RedisFuture<String> set) throws Exception {
        set.get(5, TimeUnit.SECONDS);
        assertThat(set.getError()).isNull();
        assertThat(set.get()).isEqualTo("OK");
    }

    private void waitUntilOnlyOnePartition() throws InterruptedException, TimeoutException {
        Wait.untilEquals(1, () -> clusterClient.getPartitions().size()).waitOrTimeout();
        Wait.untilTrue(() -> {
            for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
                if (redisClusterNode.getSlots().size() > 16380) {
                    return true;
                }
            }

            return false;
        }).waitOrTimeout();
    }

    private void suspendConnection(RedisClusterAsyncCommands<String, String> asyncCommands)
            throws InterruptedException, TimeoutException {
        Connections.getConnectionWatchdog(((RedisAsyncCommands<?, ?>) asyncCommands).getStatefulConnection())
                .setReconnectSuspended(true);
        asyncCommands.quit();
        WaitFor.waitOrTimeout(() -> !asyncCommands.isOpen(), timeout(seconds(6)));
    }

    protected void shiftAllSlotsToNode1() throws InterruptedException, TimeoutException {

        redis1.clusterDelSlots(AbstractClusterTest.createSlots(12000, 16384));
        redis2.clusterDelSlots(AbstractClusterTest.createSlots(12000, 16384));

        waitForSlots(redis2, 0);

        final RedisClusterNode redis2Partition = getOwnPartition(redis2);
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
                RedisClusterNode partition = partitions.getPartitionByNodeId(redis2Partition.getNodeId());

                if (!partition.getSlots().isEmpty()) {
                    removeRemaining(partition);
                }

                return partition.getSlots().size() == 0;
            }

            private void removeRemaining(RedisClusterNode partition) {
                try {
                    redis1.clusterDelSlots(toIntArray(partition.getSlots()));
                } catch (Exception o_O) {
                    // ignore
                }
            }
        }, timeout(seconds(10)));

        redis1.clusterAddSlots(RedisClusterClientTest.createSlots(12000, 16384));
        waitForSlots(redis1, 16384);

        Wait.untilTrue(clusterRule::isStable).waitOrTimeout();
    }

    private int[] toIntArray(List<Integer> list) {
        return list.parallelStream().mapToInt(Integer::intValue).toArray();
    }

    private void waitForSlots(RedisClusterCommands<String, String> connection, int slotCount)
            throws InterruptedException, TimeoutException {
        Wait.untilEquals(slotCount, () -> getOwnPartition(connection).getSlots().size()).waitOrTimeout();
    }
}
