/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.test.settings.TestSettings.host;
import static io.lettuce.test.settings.TestSettings.mtlsClusterPort;
import static io.lettuce.test.settings.TlsSettings.ClientCertificate;
import static io.lettuce.test.settings.TlsSettings.MTLS_CLUSTER_CONTAINER;
import static io.lettuce.test.settings.TlsSettings.MTLS_CLUSTER_TLS_PATH;
import static io.lettuce.test.settings.TlsSettings.createMtlsSslOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractMtlsClientAuthIntegrationTests;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.condition.RedisConditions;

/**
 * Integration tests for Redis 8.6+ mTLS client authentication in cluster mode.
 * <p>
 * This class provides the cluster implementation of mTLS client auth tests. The actual test methods are inherited from
 * {@link AbstractMtlsClientAuthIntegrationTests}.
 *
 * @author Aleksandar Todorov
 */
class ClusterMtlsClientAuthIntegrationTests extends AbstractMtlsClientAuthIntegrationTests {

    private RedisClusterClient redisClusterClient;

    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @Override
    protected int getPort() {
        return mtlsClusterPort();
    }

    @Override
    protected Path getTlsPath() {
        return MTLS_CLUSTER_TLS_PATH;
    }

    @Override
    protected String getContainerName() {
        return MTLS_CLUSTER_CONTAINER;
    }

    @Override
    protected SslVerifyMode verifyPeer() {
        // Use CA mode: validates certificate chain but skips hostname verification.
        // This is necessary because cluster topology discovery returns internal container IPs
        // (e.g., 172.17.0.2) that don't match the certificate's CN/SAN.
        return SslVerifyMode.CA;
    }

    @Override
    protected String getServerPassword() {
        // Cluster has requirepass foobared configured
        return "foobared";
    }

    @Override
    protected ClientOptions.Builder clientOptionsBuilder(SslOptions sslOptions) {
        return ClusterClientOptions.builder().sslOptions(sslOptions);
    }

    @Override
    protected void initializeClient() {
        redisClusterClient = RedisClusterClient.create(getClientResources(), redisURI);
        redisClusterClient.setOptions((ClusterClientOptions) clientOptionsBuilder(sslOptions).build());
        clusterConnection = redisClusterClient.connect();

        // Set base class fields for shutdown and conditions
        this.client = redisClusterClient;
        this.connection = clusterConnection;
    }

    @Override
    protected RedisCommands<String, String> connect() {
        return ClusterTestUtil.redisCommandsOverCluster(redisClusterClient.connect());
    }

    @Override
    protected void closeCommands(RedisCommands<String, String> commands) {
        commands.getStatefulConnection().close();
    }

    @Override
    protected TestConnection connectAsDefaultUser() {
        // For cluster with password, use password-based authentication
        RedisURI uriWithPassword = RedisURI.builder().withHost(host()).withPort(getPort()).withSsl(true)
                .withVerifyPeer(verifyPeer()).withPassword(getServerPassword().toCharArray()).build();

        RedisClusterClient clientWithPassword = RedisClusterClient.create(getClientResources(), uriWithPassword);
        clientWithPassword.setOptions((ClusterClientOptions) redisClusterClient.getOptions());
        StatefulRedisClusterConnection<String, String> conn = clientWithPassword.connect();

        return new TestConnection() {

            @Override
            public RedisCommands<String, String> commands() {
                return ClusterTestUtil.redisCommandsOverCluster(conn);
            }

            @Override
            public void close() {
                conn.close();
                clientWithPassword.shutdown();
            }

        };
    }

    @Override
    protected RedisConditions getConditions() {
        return RedisConditions.of(clusterConnection);
    }

    // ========== Cluster-specific mTLS tests ==========

    @Test
    void discoverClusterNodesWithMtls() {
        // Get cluster partitions - this tests that mTLS works for topology discovery
        assertThat(redisClusterClient.getPartitions()).isNotEmpty();
        assertThat(redisClusterClient.getPartitions().size()).isGreaterThanOrEqualTo(3);

        // Verify we can see node details
        for (RedisClusterNode node : redisClusterClient.getPartitions()) {
            assertThat(node.getNodeId()).isNotEmpty();
            assertThat(node.getUri()).isNotNull();
        }
    }

    @Test
    void performCrossSlotOperationsWithMtls() {
        RedisAdvancedClusterCommands<String, String> sync = clusterConnection.sync();

        // These keys will hash to different slots
        String key1 = "mtls-cluster-key-1";
        String key2 = "mtls-cluster-key-2";
        String key3 = "mtls-cluster-key-3";

        sync.set(key1, "value1");
        sync.set(key2, "value2");
        sync.set(key3, "value3");

        assertThat(sync.get(key1)).isEqualTo("value1");
        assertThat(sync.get(key2)).isEqualTo("value2");
        assertThat(sync.get(key3)).isEqualTo("value3");

        // Cleanup
        sync.del(key1, key2, key3);
    }

    // ========== Multi-user mTLS tests ==========

    @Test
    void connectWithDifferentMtlsUser() {
        // User 2: Client-test-2.p12 (CN=Client-test-2)
        SslOptions user2SslOptions = createMtlsSslOptions(getContainerName(), getTlsPath(), ClientCertificate.USER_2);
        RedisClusterClient user2Client = RedisClusterClient.create(getClientResources(),
                RedisURI.builder().withHost(host()).withPort(getPort()).withSsl(true).withVerifyPeer(verifyPeer()).build());
        user2Client.setOptions((ClusterClientOptions) clientOptionsBuilder(user2SslOptions).build());

        try (StatefulRedisClusterConnection<String, String> conn = user2Client.connect()) {
            RedisAdvancedClusterCommands<String, String> sync = conn.sync();
            String result = sync.ping();
            assertThat(result).isEqualTo("PONG");

            // Verify authenticated as the certificate user
            String whoami = sync.aclWhoami();
            assertThat(whoami).isEqualTo("Client-test-2");
        } finally {
            user2Client.shutdown();
        }
    }

    // ========== Case sensitivity tests ==========

    @Test
    void caseMismatchCertificateShouldFailAuthentication() {
        // client.p12 has CN=Client-Test-cert (uppercase T)
        // ACL user is Client-test-cert (lowercase t)
        // Redis ACL usernames are case-sensitive, so this should fail
        SslOptions caseMismatchSslOptions = createMtlsSslOptions(getContainerName(), getTlsPath(),
                ClientCertificate.NO_ACL_USER);
        RedisClusterClient caseMismatchClient = RedisClusterClient.create(getClientResources(),
                RedisURI.builder().withHost(host()).withPort(getPort()).withSsl(true).withVerifyPeer(verifyPeer()).build());
        caseMismatchClient.setOptions((ClusterClientOptions) clientOptionsBuilder(caseMismatchSslOptions).build());

        try {
            assertThatThrownBy(caseMismatchClient::connect).isInstanceOf(RedisException.class);
        } finally {
            caseMismatchClient.shutdown();
        }
    }

}
