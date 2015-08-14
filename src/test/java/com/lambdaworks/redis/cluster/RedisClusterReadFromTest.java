package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.lambdaworks.redis.BaseRedisConnection;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterReadFromTest {

    public static final String host = TestSettings.hostAddr();
    public static final int port1 = 7383;
    public static final int port2 = 7384;

    private static RedisClusterClient clusterClient;
    private static boolean initialized = false;

    protected RedisAdvancedClusterConnection<String, String> connection;

    protected String key = "key";
    protected String value = "value";

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2);

    @BeforeClass
    public static void setupClient() throws Exception {
        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));
        clusterClient.setOptions(new ClusterClientOptions.Builder().validateClusterNodeMembership(false).build());
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() throws Exception {
        connection = clusterClient.connectCluster();

        if (!initialized) {
            setup1Master1Slave();
            initialized = true;
        }
    }

    protected void setup1Master1Slave() throws InterruptedException, TimeoutException {
        clusterRule.clusterReset();
        clusterRule.meet(host, port1);
        clusterRule.meet(host, port2);

        clusterRule.getClusterClient().reloadPartitions();

        final RedisClusterConnection<String, String> redis1 = this.connection.getConnection(host, port1);
        RedisClusterConnection<String, String> redis2 = this.connection.getConnection(host, port2);
        redis1.clusterAddSlots(RedisClusterClientTest.createSlots(0, 16384));

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterRule.isStable();
            }
        }, timeout(seconds(5)));

        redis2.clusterReplicate(redis1.clusterMyId());

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                clusterRule.getClusterClient().reloadPartitions();
                Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes());
                for (RedisClusterNode partition : partitions) {
                    if (partition.getFlags().contains(RedisClusterNode.NodeFlag.SLAVE)) {
                        return true;
                    }
                }
                return false;
            }
        }, timeout(seconds(5)));
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void defaultTest() throws Exception {
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.MASTER);
    }

    @Test
    public void readWriteMaster() throws Exception {
        connection.setReadFrom(ReadFrom.MASTER);
        connection.set(key, value);
        assertThat(connection.get(key)).isEqualTo(value);
    }

    @Test
    public void readWriteMasterPreferred() throws Exception {
        connection.setReadFrom(ReadFrom.MASTER_PREFERRED);
        connection.set(key, value);
        assertThat(connection.get(key)).isEqualTo(value);
    }

    @Test
    public void readWriteSlave() throws Exception {
        connection.setReadFrom(ReadFrom.SLAVE);

        connection.set(key, "value1");

        ((BaseRedisConnection<String, String>) connection.getConnection(host, port2)).waitForReplication(1, 1000);
        assertThat(connection.get(key)).isEqualTo("value1");
    }

    @Test
    public void readWriteNearest() throws Exception {
        connection.setReadFrom(ReadFrom.NEAREST);

        connection.set(key, "value1");

        ((BaseRedisConnection<String, String>) connection.getConnection(host, port2)).waitForReplication(1, 1000);
        assertThat(connection.get(key)).isEqualTo("value1");
    }
}
