package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.lambdaworks.Sockets;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
public class RedisClusterPasswordSecuredSslTest extends AbstractClusterTest {

    public static final String KEYSTORE = "work/keystore.jks";

    public static final int CLUSTER_PORT_SSL_1 = 7443;
    public static final int CLUSTER_PORT_SSL_2 = 7444;
    public static final int CLUSTER_PORT_SSL_3 = 7445;

    public static final String SLOT_1_KEY = "8HMdi";
    public static final String SLOT_16352_KEY = "UyAa4KqoWgPGKa";

    public static RedisURI redisURI = new RedisURI.Builder().redis(host(), CLUSTER_PORT_SSL_1).withPassword("foobared")
            .withSsl(true).withVerifyPeer(false).build();
    public static RedisClusterClient redisClient = RedisClusterClient.create(redisURI);

    @Before
    public void before() throws Exception {
        assumeTrue("Assume that stunnel runs on port 7443", Sockets.isOpen(host(), CLUSTER_PORT_SSL_1));
        assumeTrue("Assume that stunnel runs on port 7444", Sockets.isOpen(host(), CLUSTER_PORT_SSL_2));
        assumeTrue("Assume that stunnel runs on port 7445", Sockets.isOpen(host(), CLUSTER_PORT_SSL_3));
        assertThat(new File(KEYSTORE)).exists();
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE);

        redisClient.setOptions(ClusterClientOptions.create());
    }

    @AfterClass
    public static void afterClass() {
        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void defaultClusterConnectionShouldWork() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection = redisClient.connectCluster();
        assertThat(connection.ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    public void connectWithoutPassword() throws Exception {

        RedisURI redisURI = new RedisURI.Builder().redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(false)
                .build();

        RedisClusterClient redisClusterClient = RedisClusterClient.create(redisURI);

        try {
            redisClusterClient.reloadPartitions();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Cannot retrieve initial cluster partitions");
        }

        try {
            redisClusterClient.connectCluster();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Cannot retrieve initial cluster partitions");
        }

        FastShutdown.shutdown(redisClusterClient);
    }

    @Test
    public void defaultClusterConnectionShouldWork1() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection = redisClient.connectCluster();
        assertThat(connection.ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    public void partitionViewShouldContainClusterPorts() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection = redisClient.connectCluster();

        Partitions partitions = redisClient.getPartitions();
        List<Integer> ports = Lists.newArrayList();
        for (RedisClusterNode partition : partitions) {
            ports.add(partition.getUri().getPort());
        }
        connection.close();

        assertThat(ports).contains(CLUSTER_PORT_SSL_1, CLUSTER_PORT_SSL_2, CLUSTER_PORT_SSL_3);
    }

    @Test
    public void routedOperationsAreWorking() throws Exception {

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder()
                .pingBeforeActivateConnection(true).build();
        redisClient.setOptions(clusterClientOptions);
        RedisAdvancedClusterConnection<String, String> connection = redisClient.connectCluster();

        connection.set(SLOT_1_KEY, "value1");
        connection.set(SLOT_16352_KEY, "value2");

        assertThat(connection.get(SLOT_1_KEY)).isEqualTo("value1");
        assertThat(connection.get(SLOT_16352_KEY)).isEqualTo("value2");

        connection.close();
    }

    @Test
    public void nodeConnectionsShouldWork() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection = redisClient.connectCluster();

        // slave
        RedisClusterConnection<String, String> node2Connection = connection.getConnection(hostAddr(), 7444);

        try {
            node2Connection.get(SLOT_1_KEY);
        } catch (RedisCommandExecutionException e) {
            assertThat(e).hasMessage("MOVED 1 127.0.0.1:7443");
        }

        connection.close();
    }

    @Test
    public void connectionWithoutPasswordShouldFail() throws Exception {

        RedisURI redisURI = new RedisURI.Builder().redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(false)
                .build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(redisURI);

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

        RedisURI redisURI = new RedisURI.Builder().redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(false)
                .build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(redisURI);

        try {
            redisClusterClient.connectCluster();
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