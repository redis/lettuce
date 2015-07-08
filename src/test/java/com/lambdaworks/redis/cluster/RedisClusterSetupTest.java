package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeoutException;

import org.junit.*;

import com.lambdaworks.Wait;
import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.*;
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
public class RedisClusterSetupTest {

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
}
