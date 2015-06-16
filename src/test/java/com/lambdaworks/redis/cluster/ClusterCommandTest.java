package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableList;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotRange;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotsParser;
import org.junit.*;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class ClusterCommandTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected RedisClusterAsyncConnection<String, String> redis1;

    protected RedisClusterCommands<String, String> redissync1;

    @BeforeClass
    public static void setupClient() throws Exception {
        setupClusterClient();
        client = new RedisClient(host, port1);
        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));

    }

    @AfterClass
    public static void shutdownClient() {
        shutdownClusterClient();
        client.shutdown(0, 0, TimeUnit.MILLISECONDS);
        clusterClient.shutdown();
    }

    @Before
    public void before() throws Exception {

        clusterRule.getClusterClient().reloadPartitions();
        ClusterSetup.setup2Master2Slaves(clusterRule);

        redis1 = client.connectAsync(RedisURI.Builder.redis(host, port1).build());
        redissync1 = client.connect(RedisURI.Builder.redis(host, port1).build());

    }

    @After
    public void after() throws Exception {
        redis1.close();

        redissync1.close();
    }

    @Test
    public void statefulConnectionFromSync() throws Exception {
        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        assertThat(sync.getStatefulConnection().sync()).isSameAs(sync);
    }

    @Test
    public void statefulConnectionFromAsync() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
    }

    @Test
    public void testClusterInfo() throws Exception {

        RedisFuture<String> future = redis1.clusterInfo();

        String status = future.get();

        assertThat(status).contains("cluster_known_nodes:");
        assertThat(status).contains("cluster_slots_fail:0");
        assertThat(status).contains("cluster_state:");
    }

    @Test
    public void testClusterNodes() throws Exception {

        String string = redissync1.clusterNodes();

        assertThat(string).contains("connected");
        assertThat(string).contains("master");
        assertThat(string).contains("myself");
    }

    @Test
    public void testClusterNodesSync() throws Exception {

        RedisClusterConnection<String, String> connection = clusterClient.connectCluster();

        String string = connection.clusterNodes();
        connection.close();

        assertThat(string).contains("connected");
        assertThat(string).contains("master");
        assertThat(string).contains("myself");
    }

    @Test
    public void testClusterSlaves() throws Exception {

        RedisFuture<Long> replication = redis1.waitForReplication(1, 5);
        assertThat(replication.get()).isGreaterThan(0L);
    }

    @Test
    public void testAsking() throws Exception {
        assertThat(redissync1.asking()).isEqualTo("OK");
    }

    @Test
    public void testReset() throws Exception {

        clusterClient.reloadPartitions();
        RedisAdvancedClusterAsyncCommandsImpl<String, String> connection = (RedisAdvancedClusterAsyncCommandsImpl) clusterClient
                .connectClusterAsync();

        RedisFuture<String> setA = connection.set("a", "myValue1");
        setA.get();

        connection.reset();

        setA = connection.set("a", "myValue1");

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        connection.close();

    }

    @Test
    public void testClusterSlots() throws Exception {

        List<Object> reply = redissync1.clusterSlots();
        assertThat(reply.size()).isGreaterThan(1);

        List<ClusterSlotRange> parse = ClusterSlotsParser.parse(reply);
        assertThat(parse).hasSize(2);

        ClusterSlotRange clusterSlotRange = parse.get(0);
        assertThat(clusterSlotRange.getFrom()).isEqualTo(0);
        assertThat(clusterSlotRange.getTo()).isEqualTo(11999);

        assertThat(clusterSlotRange.getMaster()).isNotNull();
        assertThat(clusterSlotRange.getSlaves()).isNotNull();
        assertThat(clusterSlotRange.toString()).contains(ClusterSlotRange.class.getSimpleName());

    }

    @Test
    public void readOnly() throws Exception {

        // cluster node 3 is a slave for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a slave for the master 1
        RedisConnection<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, port3).build());

        assertThat(connect3.readOnly()).isEqualTo("OK");
        waitUntilValueIsVisible(key, connect3);

        String resultBViewedBySlave = connect3.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);
        connect3.quit();

        resultBViewedBySlave = connect3.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);

    }

    @Test
    public void readOnlyWithReconnect() throws Exception {

        // cluster node 3 is a slave for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a slave for the master 1
        RedisConnection<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, port3).build());

        assertThat(connect3.readOnly()).isEqualTo("OK");
        connect3.quit();
        waitUntilValueIsVisible(key, connect3);

        String resultViewedBySlave = connect3.get("b");
        assertThat(resultViewedBySlave).isEqualTo(value);

    }

    protected void waitUntilValueIsVisible(String key, RedisConnection<String, String> connection) throws InterruptedException,
            TimeoutException {
        WaitFor.waitOrTimeout(() -> connection.get(key) != null, timeout(seconds(5)));
    }

    protected void prepareReadonlyTest(String key) throws InterruptedException, TimeoutException,
            java.util.concurrent.ExecutionException {

        redis1.set(key, value);

        String resultB = redis1.get(key).get();
        assertThat(resultB).isEqualTo(value);
        Thread.sleep(500); // give some time to replicate
    }

    @Test
    public void readOnlyReadWrite() throws Exception {

        // cluster node 3 is a slave for key "b"
        String key = "b";
        assertThat(SlotHash.getSlot(key)).isEqualTo(3300);
        prepareReadonlyTest(key);

        // assume cluster node 3 is a slave for the master 1
        final RedisConnection<String, String> connect3 = client.connect(RedisURI.Builder.redis(host, port3).build());

        try {
            connect3.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }

        assertThat(connect3.readOnly()).isEqualTo("OK");
        waitUntilValueIsVisible(key, connect3);

        connect3.readWrite();
        try {
            connect3.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    public void clusterSlaves() throws Exception {

        String nodeId = getNodeId(redissync1);
        List<String> result = redissync1.clusterSlaves(nodeId);

        assertThat(result.size()).isGreaterThan(0);
    }

}
