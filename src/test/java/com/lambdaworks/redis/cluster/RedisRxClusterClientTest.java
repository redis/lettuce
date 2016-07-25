package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import com.lambdaworks.TestClientResources;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import rx.Observable;

import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import rx.Single;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisRxClusterClientTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected StatefulRedisClusterConnection<String, String> connection;
    protected RedisAdvancedClusterCommands<String, String> sync;
    protected RedisAdvancedClusterReactiveCommands<String, String> rx;

    @BeforeClass
    public static void setupClient() throws Exception {
        setupClusterClient();
        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port1).build());
        clusterClient = RedisClusterClient.create(TestClientResources.get(), Collections.singletonList(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClient() {
        shutdownClusterClient();
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() throws Exception {

        clusterRule.getClusterClient().reloadPartitions();

        clusterClient.reloadPartitions();
        connection = clusterClient.connect();
        sync = connection.sync();
        rx = connection.reactive();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void testClusterCommandRedirection() throws Exception {
        // Command on node within the default connection
        assertThat(block(rx.set(KEY_B, "myValue1"))).isEqualTo("OK");

        // gets redirection to node 3
        assertThat(block(rx.set(KEY_A, "myValue1"))).isEqualTo("OK");
    }

    @Test
    public void getKeysInSlot() throws Exception {

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        List<String> keysA = block(rx.clusterGetKeysInSlot(SLOT_A, 10).toList());
        assertThat(keysA).isEqualTo(Collections.singletonList(KEY_A));

        List<String> keysB = block(rx.clusterGetKeysInSlot(SLOT_B, 10).toList());
        assertThat(keysB).isEqualTo(Collections.singletonList(KEY_B));
    }

    @Test
    public void countKeysInSlot() throws Exception {

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        Long result = block(rx.clusterCountKeysInSlot(SLOT_A));
        assertThat(result).isEqualTo(1L);

        result = block(rx.clusterCountKeysInSlot(SLOT_B));
        assertThat(result).isEqualTo(1L);

        int slotZZZ = SlotHash.getSlot("ZZZ".getBytes());
        result = block(rx.clusterCountKeysInSlot(slotZZZ));
        assertThat(result).isEqualTo(0L);
    }

    @Test
    public void testClusterCountFailureReports() throws Exception {
        RedisClusterNode ownPartition = getOwnPartition(sync);
        assertThat(block(rx.clusterCountFailureReports(ownPartition.getNodeId()))).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testClusterKeyslot() throws Exception {
        assertThat(block(rx.clusterKeyslot(KEY_A))).isEqualTo(SLOT_A);
        assertThat(SlotHash.getSlot(KEY_A)).isEqualTo(SLOT_A);
    }

    @Test
    public void testClusterSaveconfig() throws Exception {
        assertThat(block(rx.clusterSaveconfig())).isEqualTo("OK");
    }

    @Test
    public void testClusterSetConfigEpoch() throws Exception {
        try {
            block(rx.clusterSetConfigEpoch(1L));
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("ERR The user can assign a config epoch only");
        }
    }

    private <T> T block(Observable<T> observable) {
        return observable.toBlocking().single();
    }

    private <T> T block(Single<T> observable) {
        return observable.toBlocking().value();
    }

}
