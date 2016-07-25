package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.TestSettings.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.lambdaworks.TestClientResources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.Sockets;
import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.sync.Executions;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;

/**
 * @author Mark Paluch
 */
public class RedisClusterPasswordSecuredSslTest extends AbstractTest {

    public static final String KEYSTORE = "work/keystore.jks";

    public static final int CLUSTER_PORT_SSL_1 = 7443;
    public static final int CLUSTER_PORT_SSL_2 = 7444;
    public static final int CLUSTER_PORT_SSL_3 = 7445;

    public static final String SLOT_1_KEY = "8HMdi";
    public static final String SLOT_16352_KEY = "UyAa4KqoWgPGKa";

    public static RedisURI redisURI = RedisURI.builder().redis(host(), CLUSTER_PORT_SSL_1).withPassword("foobared")
            .withSsl(true).withVerifyPeer(false).build();
    public static RedisClusterClient redisClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

    @Before
    public void before() throws Exception {
        assumeTrue("Assume that stunnel runs on port 7443", Sockets.isOpen(host(), CLUSTER_PORT_SSL_1));
        assumeTrue("Assume that stunnel runs on port 7444", Sockets.isOpen(host(), CLUSTER_PORT_SSL_2));
        assumeTrue("Assume that stunnel runs on port 7445", Sockets.isOpen(host(), CLUSTER_PORT_SSL_3));
        assertThat(new File(KEYSTORE)).exists();
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE);
    }

    @AfterClass
    public static void afterClass() {
        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void defaultClusterConnectionShouldWork() throws Exception {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    public void partitionViewShouldContainClusterPorts() throws Exception {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        List<Integer> ports = connection.getPartitions().stream().map(redisClusterNode -> redisClusterNode.getUri().getPort())
                .collect(Collectors.toList());
        connection.close();

        assertThat(ports).contains(CLUSTER_PORT_SSL_1, CLUSTER_PORT_SSL_2, CLUSTER_PORT_SSL_3);
    }

    @Test
    public void routedOperationsAreWorking() throws Exception {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        sync.set(SLOT_1_KEY, "value1");
        sync.set(SLOT_16352_KEY, "value2");

        assertThat(sync.get(SLOT_1_KEY)).isEqualTo("value1");
        assertThat(sync.get(SLOT_16352_KEY)).isEqualTo("value2");

        connection.close();
    }

    @Test
    public void nodeConnectionsShouldWork() throws Exception {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        // slave
        StatefulRedisConnection<String, String> node2Connection = connection.getConnection(hostAddr(), 7444);

        try {
            node2Connection.sync().get(SLOT_1_KEY);
        } catch (RedisCommandExecutionException e) {
            assertThat(e).hasMessage("MOVED 1 127.0.0.1:7443");
        }

        connection.close();
    }

    @Test
    public void nodeSelectionApiShouldWork() throws Exception {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        Executions<String> ping = connection.sync().all().commands().ping();
        assertThat(ping).hasSize(3).contains("PONG");

        connection.close();
    }

    @Test
    public void connectionWithoutPasswordShouldFail() throws Exception {

        RedisURI redisURI = RedisURI.builder().redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(false)
                .build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

        try {
            redisClusterClient.reloadPartitions();
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Cannot retrieve initial cluster");
        } finally {
            FastShutdown.shutdown(redisClusterClient);
        }
    }

    @Test
    public void connectionWithoutPasswordShouldFail2() throws Exception {

        RedisURI redisURI = RedisURI.builder().redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(false)
                .build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

        try {
            redisClusterClient.connect();
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Cannot retrieve initial cluster");
        } finally {
            FastShutdown.shutdown(redisClusterClient);
        }
    }

    @Test
    public void clusterNodeRefreshWorksForMultipleIterations() throws Exception {

        redisClient.reloadPartitions();
        redisClient.reloadPartitions();
        redisClient.reloadPartitions();
        redisClient.reloadPartitions();
    }
}
