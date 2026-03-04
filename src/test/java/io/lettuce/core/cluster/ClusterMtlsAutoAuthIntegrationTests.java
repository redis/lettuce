/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.test.settings.TestSettings.host;
import static io.lettuce.test.settings.TestSettings.mtlsClusterPort;
import static io.lettuce.test.settings.TlsSettings.MTLS_CLUSTER_CONTAINER;
import static io.lettuce.test.settings.TlsSettings.MTLS_CLUSTER_TLS_PATH;

import java.nio.file.Path;

import io.lettuce.core.AbstractMtlsAutoAuthIntegrationTests;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.test.condition.RedisConditions;

/**
 * Integration tests for Redis 8.6+ mTLS automatic authentication in cluster mode.
 * <p>
 * This class provides the cluster implementation of mTLS auto-auth tests. The actual test methods are inherited from
 * {@link AbstractMtlsAutoAuthIntegrationTests}.
 *
 * @author Aleksandar Todorov
 */
class ClusterMtlsAutoAuthIntegrationTests extends AbstractMtlsAutoAuthIntegrationTests {

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

}
