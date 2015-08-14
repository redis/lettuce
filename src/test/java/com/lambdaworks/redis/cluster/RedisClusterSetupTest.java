package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getNodeId;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.Lists;
import com.lambdaworks.Wait;
import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.DefaultRedisClient;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@SuppressWarnings("unchecked")
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
        clusterClient = new RedisClusterClient(RedisURI.Builder.redis(host, AbstractClusterTest.port5).build());
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

    private void waitForSlots(RedisClusterCommands<String, String> connection, int slotCount) throws InterruptedException,
            TimeoutException {
        Wait.untilEquals(slotCount, () -> getOwnPartition(connection).getSlots().size()).waitOrTimeout();
    }

    @Test
    public void clusterSlotMigrationImport() throws Exception {

        ClusterSetup.setup2Masters(clusterRule);

        String nodeId2 = getNodeId(redis2);
        assertThat(redis1.clusterSetSlotMigrating(6, nodeId2)).isEqualTo("OK");
        assertThat(redis1.clusterSetSlotImporting(15000, nodeId2)).isEqualTo("OK");
    }

    @Test
    public void clusterTopologyRefresh() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(5, TimeUnit.SECONDS)
                .build());
        clusterClient.reloadPartitions();

        RedisAdvancedClusterAsyncConnection<String, String> clusterConnection = clusterClient.connectClusterAsync();

        assertThat(clusterClient.getPartitions()).hasSize(1);

        ClusterSetup.setup2Masters(clusterRule);

        assertThat(clusterClient.getPartitions()).hasSize(2);

        clusterConnection.close();

    }

    @Test
    public void changeTopologyWhileOperations() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(1, TimeUnit.SECONDS)
                .build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();

        ClusterSetup.setup2Masters(clusterRule);

        assertRoutedExecution(clusterConnection);

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(false).build());

        shiftAllSlotsToNode1();

        assertRoutedExecution(clusterConnection);

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).build());

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

        assertRoutedExecution(clusterConnection);
    }

    @Test
    public void atLeastOnceForgetNodeFailover() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(1, TimeUnit.SECONDS)
                .build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();
        clusterClient.setOptions(new ClientOptions.Builder().build());
        ClusterSetup.setup2Masters(clusterRule);

        assertRoutedExecution(clusterConnection);

        RedisClusterNode partition1 = getOwnPartition(redis1);
        RedisClusterNode partition2 = getOwnPartition(redis2);
        RedisClusterAsyncCommands<String, String> node2Connection = clusterConnection.getConnection(partition2.getNodeId());

        shiftAllSlotsToNode1();

        suspendConnection((RedisAsyncCommands<String, String>) node2Connection);

        final List<RedisFuture<String>> futures = Lists.newArrayList();

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

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                for (RedisFuture<String> future : futures) {
                    if (!future.isDone()) {
                        return false;
                    }
                }
                return true;
            }
        }, timeout(seconds(6)));

        assertRoutedExecution(clusterConnection);

    }

    private void waitUntilOnlyOnePartition() throws InterruptedException, TimeoutException {
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                if (clusterClient.getPartitions().size() == 1) {
                    for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
                        if (redisClusterNode.getSlots().size() > 16380) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }, timeout(seconds(10)));
    }

    private void suspendConnection(final RedisAsyncCommands<String, String> node2Connection) throws InterruptedException,
            TimeoutException {
        suspendAutoReconnect(node2Connection.getStatefulConnection());
        node2Connection.quit();
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return !node2Connection.isOpen();
            }
        }, timeout(seconds(6)));
    }

    private void suspendAutoReconnect(StatefulConnection<String, String> connection) {
        ClusterNodeCommandHandler channelWriter = (ClusterNodeCommandHandler) ((RedisChannelHandler) connection)
                .getChannelWriter();
        channelWriter.prepareClose();
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
                    int[] ints = toIntArray(partition.getSlots());
                    redis1.clusterDelSlots(ints);
                } catch (Exception e) {

                }
            }
        }, timeout(seconds(10)));

        redis1.clusterAddSlots(RedisClusterClientTest.createSlots(12000, 16384));
        waitForSlots(redis1, 16384);

        Wait.untilTrue(clusterRule::isStable).waitOrTimeout();
    }

    private int[] toIntArray(List<Integer> source) {
        int[] result = new int[source.size()];
        for (int i = 0; i < source.size(); i++) {
            result[i] = source.get(i);
        }
        return result;
    }

    @Test
    public void expireStaleNodeIdConnections() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(1, TimeUnit.SECONDS)
                .build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();

        ClusterSetup.setup2Masters(clusterRule);

        PooledClusterConnectionProvider clusterConnectionProvider = getPooledClusterConnectionProvider(clusterConnection);

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

        assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(1);

    }

    private void assertRoutedExecution(RedisClusterAsyncCommands<String, String> clusterConnection) throws Exception {
        assertExecuted(clusterConnection.set("A", "value")); // 6373
        assertExecuted(clusterConnection.set("t", "value")); // 15891
        assertExecuted(clusterConnection.set("p", "value")); // 16023
    }

    @Test
    public void doNotExpireStaleNodeIdConnections() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).closeStaleConnections(false)
                .refreshPeriod(1, TimeUnit.SECONDS).build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();

        ClusterSetup.setup2Masters(clusterRule);

        PooledClusterConnectionProvider<?, ?> clusterConnectionProvider = getPooledClusterConnectionProvider(clusterConnection);

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

        Thread.sleep(2000);

        assertThat(clusterConnectionProvider.getConnectionCount()).isEqualTo(2);

    }

    @Test
    public void expireStaleHostAndPortConnections() throws Exception {

        clusterClient.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).refreshPeriod(1, TimeUnit.SECONDS)
                .build());
        RedisAdvancedClusterAsyncCommands<String, String> clusterConnection = clusterClient.connectClusterAsync();

        ClusterSetup.setup2Masters(clusterRule);

        final PooledClusterConnectionProvider clusterConnectionProvider = getPooledClusterConnectionProvider(clusterConnection);

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

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterClient.getPartitions().size() == 1;
            }
        }, timeout(seconds(6)));

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterConnectionProvider.getConnectionCount() == 2;
            }
        }, timeout(seconds(6)));

    }

    protected PooledClusterConnectionProvider getPooledClusterConnectionProvider(
            RedisAdvancedClusterAsyncConnection clusterAsyncConnection) {

        RedisChannelHandler<?, ?> channelHandler = (RedisChannelHandler<?, ?>) clusterAsyncConnection.getStatefulConnection();
        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) channelHandler.getChannelWriter();
        return (PooledClusterConnectionProvider) writer.getClusterConnectionProvider();
    }

    private void assertExecuted(RedisFuture<String> set) throws Exception {
        set.get();
        assertThat(set.getError()).isNull();
        assertThat(set.get()).isEqualTo("OK");
    }
}
