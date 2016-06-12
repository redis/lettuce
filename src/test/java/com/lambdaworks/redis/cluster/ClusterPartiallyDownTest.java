package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.resource.ClientResources;

/**
 * @author Mark Paluch
 */
public class ClusterPartiallyDownTest {
    private static ClientResources clientResources = TestClientResources.create();

    private static int port1 = 7579;
    private static int port2 = 7580;
    private static int port3 = 7581;
    private static int port4 = 7582;

    private static final RedisURI URI_1 = RedisURI.Builder.redis("127.0.0.1", port1).build();
    private static final RedisURI URI_2 = RedisURI.Builder.redis("127.0.0.1", port2).build();
    private static final RedisURI URI_3 = RedisURI.Builder.redis("127.0.0.1", port3).build();
    private static final RedisURI URI_4 = RedisURI.Builder.redis("127.0.0.1", port4).build();

    private RedisClusterClient redisClusterClient;

    @Before
    public void before() throws Exception {

    }

    @After
    public void after() throws Exception {
        redisClusterClient.shutdown();
    }

    @Test
    public void connectToPartiallyDownCluster() throws Exception {

        List<RedisURI> seed = Arrays.asList(URI_1, URI_2, URI_3, URI_4);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);
        RedisAdvancedClusterConnection<String, String> connection = redisClusterClient.connectCluster();

        assertThat(connection.ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    public void operateOnPartiallyDownCluster() throws Exception {

        List<RedisURI> seed = Arrays.asList(URI_1, URI_2, URI_3, URI_4);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);
        RedisAdvancedClusterConnection<String, String> connection = redisClusterClient.connectCluster();

        String key_10439 = "aaa";
        assertThat(SlotHash.getSlot(key_10439)).isEqualTo(10439);

        try {
            connection.get(key_10439);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasCauseExactlyInstanceOf(RedisConnectionException.class)
                    .hasRootCauseInstanceOf(ConnectException.class);
        }

        connection.close();
    }

    @Test
    public void seedNodesAreOffline() throws Exception {

        List<RedisURI> seed = Arrays.asList(URI_1, URI_2, URI_3);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);

        try {
            redisClusterClient.connectCluster();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasNoCause();
        }
    }

    @Test
    public void partitionNodesAreOffline() throws Exception {

        List<RedisURI> seed = Arrays.asList(URI_1, URI_2, URI_3);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);

        Partitions partitions = new Partitions();
        partitions.addPartition(new RedisClusterNode(URI_1, "a", true, null, 0, 0, 0, new ArrayList<Integer>(),
                new HashSet<RedisClusterNode.NodeFlag>()));
        partitions.addPartition(new RedisClusterNode(URI_2, "b", true, null, 0, 0, 0, new ArrayList<Integer>(),
                new HashSet<RedisClusterNode.NodeFlag>()));

        redisClusterClient.setPartitions(partitions);

        try {
            redisClusterClient.connectCluster();
            fail("Missing RedisConnectionException");
        } catch (RedisConnectionException e) {
            assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
        }
    }
}
