package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.lambdaworks.Wait;
import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
@SlowTests
public class RedisClusterStressScenariosTest extends AbstractTest {

    public static final String host = TestSettings.hostAddr();

    protected static RedisClient client;
    protected static RedisClusterClient clusterClient;

    protected Logger log = Logger.getLogger(getClass());

    protected StatefulRedisConnection<String, String> redis5;
    protected StatefulRedisConnection<String, String> redis6;

    protected RedisClusterCommands<String, String> redissync5;
    protected RedisClusterCommands<String, String> redissync6;

    protected String key = "key";
    protected String value = "value";

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, AbstractClusterTest.port5, AbstractClusterTest.port6);

    @BeforeClass
    public static void setupClient() throws Exception {
        client = new RedisClient(host, AbstractClusterTest.port5);
        clusterClient = new RedisClusterClient(
                ImmutableList.of(RedisURI.Builder.redis(host, AbstractClusterTest.port5).build()));
    }

    @AfterClass
    public static void shutdownClient() {
        client.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void before() throws Exception {

        ClusterSetup.setupMasterWithSlave(clusterRule);

        redis5 = client.connect(RedisURI.Builder.redis(host, AbstractClusterTest.port5).build());
        redis6 = client.connect(RedisURI.Builder.redis(host, AbstractClusterTest.port6).build());

        redissync5 = redis5.sync();
        redissync6 = redis6.sync();

        WaitFor.waitOrTimeout(() -> {
            return clusterRule.isStable();
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

    }

    @After
    public void after() throws Exception {
        redis5.close();

        redissync5.close();
        redissync6.close();
    }

    @Test
    public void testClusterFailover() throws Exception {

        log.info("Cluster node 5 is master");
        log.info("Cluster nodes seen from node 5:" + Layout.LINE_SEP + redissync5.clusterNodes());
        log.info("Cluster nodes seen from node 6:" + Layout.LINE_SEP + redissync6.clusterNodes());

        Wait.untilTrue(() -> getOwnPartition(redissync5).is(RedisClusterNode.NodeFlag.MASTER)).waitOrTimeout();
        Wait.untilTrue(() -> getOwnPartition(redissync6).is(RedisClusterNode.NodeFlag.SLAVE)).waitOrTimeout();

        String failover = redissync6.clusterFailover(true);
        assertThat(failover).isEqualTo("OK");

        Wait.untilTrue(() -> getOwnPartition(redissync6).is(RedisClusterNode.NodeFlag.MASTER)).waitOrTimeout();
        Wait.untilTrue(() -> getOwnPartition(redissync5).is(RedisClusterNode.NodeFlag.SLAVE)).waitOrTimeout();

        log.info("Cluster nodes seen from node 5 after clusterFailover:" + Layout.LINE_SEP + redissync5.clusterNodes());
        log.info("Cluster nodes seen from node 6 after clusterFailover:" + Layout.LINE_SEP + redissync6.clusterNodes());

        RedisClusterNode redis5Node = getOwnPartition(redissync5);
        RedisClusterNode redis6Node = getOwnPartition(redissync6);

        assertThat(redis5Node.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);
        assertThat(redis6Node.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);

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

        WaitFor.waitOrTimeout(() -> !slotConnection.isOpen(), timeout(seconds(5)));

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
