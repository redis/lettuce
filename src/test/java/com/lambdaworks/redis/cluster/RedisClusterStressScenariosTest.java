package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getNodeId;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterStressScenariosTest {

    public static final String host = TestSettings.hostAddr();
    public static final int port1 = 7379;
    public static final int port2 = 7380;
    public static final int port3 = 7381;
    public static final int port4 = 7382;

    protected static RedisClient client;
    protected static RedisClusterClient clusterClient;

    protected Logger log = Logger.getLogger(getClass());

    protected RedisClusterAsyncCommands<String, String> redis1;

    protected RedisClusterCommands<String, String> redissync1;
    protected RedisClusterCommands<String, String> redissync4;

    protected String key = "key";
    protected String value = "value";

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    @BeforeClass
    public static void setupClient() throws Exception {
        client = new RedisClient(host, port1);

        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));

    }

    @AfterClass
    public static void shutdownClient() {

        client.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void before() throws Exception {

        redis1 = client.connectAsync(RedisURI.Builder.redis(host, port1).build());

        redissync1 = client.connect(RedisURI.Builder.redis(host, port1).build());
        redissync4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        WaitFor.waitOrTimeout(() -> {
            return clusterRule.isStable();
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

    }

    @After
    public void after() throws Exception {
        redis1.close();

        redissync1.close();
        redissync4.close();
    }

    @Test
    public void testClusterFailover() throws Exception {

        redissync4.clusterReplicate(getNodeId(redissync1));

        RedisClusterNode redis1Node = getOwnPartition(redissync1);
        RedisClusterNode redis4Node = getOwnPartition(redissync4);

        if (redis1Node.getFlags().contains(RedisClusterNode.NodeFlag.MASTER)) {

            log.info("Cluster node 1 is master");
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync4).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            log.info("Cluster nodes seen from node 1:" + Layout.LINE_SEP + redissync1.clusterNodes());

            RedisFuture<String> future = redis1.clusterFailover(false);
            try {
                future.get();
            } catch (Exception e) {
                assertThat(e)
                        .hasMessage(
                                "com.lambdaworks.redis.RedisCommandExecutionException: ERR You should send CLUSTER FAILOVER to a slave");
            }

            String failover = redissync4.clusterFailover(true);
            assertThat(failover).isEqualTo("OK");
            new ThreadSleep(seconds(2));
            log.info("Cluster nodes seen from node 1 after clusterFailover:" + Layout.LINE_SEP + redissync1.clusterNodes());
            log.info("Cluster nodes seen from node 4 after clusterFailover:" + Layout.LINE_SEP + redissync4.clusterNodes());

            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync1).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            redis1Node = getOwnPartition(redissync1);
            redis4Node = getOwnPartition(redissync4);

            assertThat(redis1Node.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);
            assertThat(redis4Node.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);
        }

        if (redis4Node.getFlags().contains(RedisClusterNode.NodeFlag.MASTER)) {

            log.info("Cluster node 4 is master");
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync1).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            log.info("Cluster nodes seen from node 1:" + Layout.LINE_SEP + redissync1.clusterNodes());
            try {
                redissync4.clusterFailover(false);
            } catch (Exception e) {
                assertThat(e).hasMessage("ERR You should send CLUSTER FAILOVER to a slave");
            }

            RedisFuture<String> failover = redis1.clusterFailover(true);
            String result = failover.get();
            assertThat(failover.getError()).isNull();
            assertThat(result).isEqualTo("OK");

            new ThreadSleep(seconds(2));
            log.info("Cluster nodes seen from node 1 after clusterFailover:" + Layout.LINE_SEP + redissync1.clusterNodes());
            log.info("Cluster nodes seen from node 4 after clusterFailover:" + Layout.LINE_SEP + redissync4.clusterNodes());

            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync4).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            redis1Node = getOwnPartition(redissync1);
            redis4Node = getOwnPartition(redissync4);

            assertThat(redis1Node.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);
            assertThat(redis4Node.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);
        }

    }

    @Test
    public void testClusterConnectionStability() throws Exception {

        RedisAdvancedClusterAsyncCommandsImpl<String, String> connection = (RedisAdvancedClusterAsyncCommandsImpl<String, String>) clusterClient
                .connectClusterAsync();

        RedisChannelHandler<String, String> statefulConnection = (RedisChannelHandler) connection.getStatefulConnection();

        connection.set("a", "b");
        ClusterDistributionChannelWriter<String, String> writer = (ClusterDistributionChannelWriter) statefulConnection
                .getChannelWriter();

        StatefulRedisConnectionImpl<Object, Object> statefulSlotConnection = (StatefulRedisConnectionImpl) writer
                .getClusterConnectionProvider().getConnection(ClusterConnectionProvider.Intent.WRITE, 3300);

        final RedisAsyncConnection<Object, Object> slotConnection = statefulSlotConnection.async();

        slotConnection.set("a", "b");
        slotConnection.close();

        WaitFor.waitOrTimeout(() -> statefulSlotConnection.isClosed() && !statefulSlotConnection.isOpen(), timeout(seconds(2)));

        assertThat(statefulSlotConnection.isClosed()).isTrue();
        assertThat(statefulSlotConnection.isOpen()).isFalse();

        assertThat(connection.isOpen()).isTrue();
        assertThat(statefulConnection.isOpen()).isTrue();
        assertThat(statefulConnection.isClosed()).isFalse();

        connection.set("a", "b");

        StatefulRedisConnectionImpl<Object, Object> backendConnection2 = (StatefulRedisConnectionImpl) writer
                .getClusterConnectionProvider().getConnection(ClusterConnectionProvider.Intent.WRITE, 3300);

        assertThat(backendConnection2.isOpen()).isTrue();
        assertThat(backendConnection2.isClosed()).isFalse();

        assertThat(backendConnection2).isNotSameAs(statefulSlotConnection);

        connection.close();

    }

    @Test(timeout = 20000)
    public void distributedClusteredAccessAsync() throws Exception {

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        List<RedisFuture<?>> futures = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            futures.add(connection.set("a" + i, "myValue1" + i));
            futures.add(connection.set("b" + i, "myValue2" + i));
            futures.add(connection.set("d" + i, "myValue3" + i));
        }

        for (RedisFuture<?> future : futures) {
            future.get();
        }

        for (int i = 0; i < 100; i++) {
            RedisFuture<String> setA = connection.get("a" + i);
            RedisFuture<String> setB = connection.get("b" + i);
            RedisFuture<String> setD = connection.get("d" + i);

            setA.get();
            setB.get();
            setD.get();

            assertThat(setA.getError()).isNull();
            assertThat(setB.getError()).isNull();
            assertThat(setD.getError()).isNull();

            assertThat(setA.get()).isEqualTo("myValue1" + i);
            assertThat(setB.get()).isEqualTo("myValue2" + i);
            assertThat(setD.get()).isEqualTo("myValue3" + i);
        }

        connection.close();
    }

    @Test
    public void distributedClusteredAccessSync() throws Exception {

        RedisClusterConnection<String, String> connection = clusterClient.connectCluster();

        for (int i = 0; i < 100; i++) {
            connection.set("a" + i, "myValue1" + i);
            connection.set("b" + i, "myValue2" + i);
            connection.set("d" + i, "myValue3" + i);
        }

        for (int i = 0; i < 100; i++) {

            assertThat(connection.get("a" + i)).isEqualTo("myValue1" + i);
            assertThat(connection.get("b" + i)).isEqualTo("myValue2" + i);
            assertThat(connection.get("d" + i)).isEqualTo("myValue3" + i);
        }

        connection.close();
    }

}
