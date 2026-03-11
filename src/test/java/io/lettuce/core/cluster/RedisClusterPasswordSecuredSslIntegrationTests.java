package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.test.settings.TestSettings.*;
import static io.lettuce.test.settings.TlsSettings.ClientCertificate;
import static io.lettuce.test.settings.TlsSettings.MTLS_CLUSTER_CONTAINER;
import static io.lettuce.test.settings.TlsSettings.MTLS_CLUSTER_TLS_PATH;
import static io.lettuce.test.settings.TlsSettings.createMtlsSslOptions;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.test.CanConnect;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisClusterPasswordSecuredSslIntegrationTests extends TestSupport {

    private static final int CLUSTER_PORT_SSL_1 = 7443;

    private static final int CLUSTER_PORT_SSL_2 = 7444; // replica cannot replicate properly with upstream

    private static final int CLUSTER_PORT_SSL_3 = 7445;

    private static final String SLOT_1_KEY = "8HMdi";

    private static final String SLOT_16352_KEY = "UyAa4KqoWgPGKa";

    private RedisURI redisURI;

    private RedisClusterClient redisClient;

    @BeforeAll
    void beforeAll() {
        // Check if mTLS certificate files exist (only available on Redis 8.0+)
        Path keystorePath = Paths.get(System.getenv().getOrDefault("TEST_WORK_FOLDER", "work/docker"),
                MTLS_CLUSTER_TLS_PATH.toString(), ClientCertificate.DEFAULT.getFilename());
        assumeTrue(Files.exists(keystorePath),
                "mTLS certificates not available (requires Redis 8.0+), skipping SSL cluster tests");

        redisURI = RedisURI.Builder.redis(host(), CLUSTER_PORT_SSL_1).withPassword("foobared").withSsl(true)
                .withVerifyPeer(SslVerifyMode.CA).build();

        redisClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

        SslOptions sslOptions = createMtlsSslOptions(MTLS_CLUSTER_CONTAINER, MTLS_CLUSTER_TLS_PATH, ClientCertificate.DEFAULT);
        redisClient.setOptions(ClusterClientOptions.builder().sslOptions(sslOptions).build());
    }

    @BeforeEach
    void before() {
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_1), "Assume that stunnel runs on port 7442");
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_2), "Assume that stunnel runs on port 7444");
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_3), "Assume that stunnel runs on port 7445");
        assumeTrue(CanConnect.to(host(), 7479), "Assume that Redis runs on port 7479");
        assumeTrue(CanConnect.to(host(), 7480), "Assume that Redis runs on port 7480");
        assumeTrue(CanConnect.to(host(), 7481), "Assume that Redis runs on port 7481");
    }

    @AfterAll
    void afterClass() {
        if (redisClient != null) {
            FastShutdown.shutdown(redisClient);
        }
    }

    @Test
    void defaultClusterConnectionShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    void partitionViewShouldContainClusterPorts() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        List<Integer> ports = connection.getPartitions().stream().map(redisClusterNode -> redisClusterNode.getUri().getPort())
                .collect(Collectors.toList());
        connection.close();

        assertThat(ports).contains(CLUSTER_PORT_SSL_1, CLUSTER_PORT_SSL_3);
    }

    @Test
    void routedOperationsAreWorking() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        sync.set(SLOT_1_KEY, "value1");
        sync.set(SLOT_16352_KEY, "value2");

        assertThat(sync.get(SLOT_1_KEY)).isEqualTo("value1");
        assertThat(sync.get(SLOT_16352_KEY)).isEqualTo("value2");

        connection.close();
    }

    @Test
    void nodeConnectionsShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        // master 2
        StatefulRedisConnection<String, String> node2Connection = connection.getConnection(hostAddr(), 7445);

        try {
            node2Connection.sync().get(SLOT_1_KEY);
        } catch (RedisCommandExecutionException e) {
            assertThat(e).hasMessage("MOVED 1 127.0.0.1:" + CLUSTER_PORT_SSL_1);
        }

        connection.close();
    }

    @Test
    void nodeSelectionApiShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        Executions<String> ping = connection.sync().all().commands().ping();
        assertThat(ping).hasSize(3).contains("PONG");

        connection.close();
    }

    @Test
    void connectionWithoutPasswordShouldFail() {
        // mTLS with NO_ACL_USER certificate only available on Redis 8.0+
        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assumeTrue(RedisConditions.of(conn).hasVersionGreaterOrEqualsTo("8.0"), "Requires Redis 8.0+");
        }

        RedisURI redisURI = RedisURI.Builder.redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(SslVerifyMode.CA)
                .build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(), redisURI);
        // Use certificate without matching ACL user to ensure auth fails
        redisClusterClient.setOptions(ClusterClientOptions.builder()
                .sslOptions(createMtlsSslOptions(MTLS_CLUSTER_CONTAINER, MTLS_CLUSTER_TLS_PATH, ClientCertificate.NO_ACL_USER))
                .build());

        try {
            redisClusterClient.refreshPartitions();
            fail("Expected RedisException for missing password");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Cannot reload Redis Cluster topology");
        } finally {
            FastShutdown.shutdown(redisClusterClient);
        }
    }

    @Test
    void connectionWithoutPasswordShouldFail2() {
        // mTLS with NO_ACL_USER certificate only available on Redis 8.0+
        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assumeTrue(RedisConditions.of(conn).hasVersionGreaterOrEqualsTo("8.0"), "Requires Redis 8.0+");
        }

        RedisURI redisURI = RedisURI.Builder.redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(SslVerifyMode.CA)
                .build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(), redisURI);
        // Use certificate without matching ACL user to ensure auth fails
        redisClusterClient.setOptions(ClusterClientOptions.builder()
                .sslOptions(createMtlsSslOptions(MTLS_CLUSTER_CONTAINER, MTLS_CLUSTER_TLS_PATH, ClientCertificate.NO_ACL_USER))
                .build());

        try {
            redisClusterClient.connect();
            fail("Expected RedisConnectionException for missing password");
        } catch (RedisConnectionException e) {
            assertThat(e).hasMessageContaining("Unable to establish a connection to Redis Cluster");
        } finally {
            FastShutdown.shutdown(redisClusterClient);
        }
    }

    @Test
    void clusterNodeRefreshWorksForMultipleIterations() {

        redisClient.refreshPartitions();
        redisClient.refreshPartitions();
        redisClient.refreshPartitions();
        redisClient.refreshPartitions();
    }

}
