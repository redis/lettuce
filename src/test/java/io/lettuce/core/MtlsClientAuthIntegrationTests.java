/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.test.settings.TestSettings.host;
import static io.lettuce.test.settings.TestSettings.mtlsStandalonePort;
import static io.lettuce.test.settings.TlsSettings.ClientCertificate;
import static io.lettuce.test.settings.TlsSettings.MTLS_STANDALONE_CONTAINER;
import static io.lettuce.test.settings.TlsSettings.MTLS_STANDALONE_TLS_PATH;
import static io.lettuce.test.settings.TlsSettings.createMtlsSslOptions;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.condition.RedisConditions;

/**
 * Integration tests for Redis 8.6+ mTLS client authentication (standalone mode).
 * <p>
 * This class provides the standalone implementation of mTLS client auth tests. The actual test methods are inherited from
 * {@link AbstractMtlsClientAuthIntegrationTests}.
 *
 * @author Aleksandar Todorov
 */
class MtlsClientAuthIntegrationTests extends AbstractMtlsClientAuthIntegrationTests {

    private RedisClient redisClient;

    private StatefulRedisConnection<String, String> standaloneConnection;

    @Override
    protected int getPort() {
        return mtlsStandalonePort();
    }

    @Override
    protected Path getTlsPath() {
        return MTLS_STANDALONE_TLS_PATH;
    }

    @Override
    protected String getContainerName() {
        return MTLS_STANDALONE_CONTAINER;
    }

    @Override
    protected SslVerifyMode verifyPeer() {
        return SslVerifyMode.FULL;
    }

    @Override
    protected ClientOptions.Builder clientOptionsBuilder(SslOptions sslOptions) {
        return ClientOptions.builder().sslOptions(sslOptions);
    }

    @Override
    protected void initializeClient() {
        redisClient = RedisClient.create(getClientResources(), redisURI);
        redisClient.setOptions(clientOptionsBuilder(sslOptions).build());
        standaloneConnection = redisClient.connect();

        // Set base class fields for shutdown and conditions
        this.client = redisClient;
        this.connection = standaloneConnection;
    }

    @Override
    protected RedisCommands<String, String> connect() {
        return redisClient.connect(redisURI).sync();
    }

    @Override
    protected void closeCommands(RedisCommands<String, String> commands) {
        commands.getStatefulConnection().close();
    }

    @Override
    protected TestConnection connectAsDefaultUser() {
        // For standalone without password, use explicit username authentication
        RedisURI uriWithCredentials = RedisURI.builder().withHost(host()).withPort(getPort()).withSsl(true)
                .withVerifyPeer(verifyPeer()).withAuthentication("default", "").build();

        StatefulRedisConnection<String, String> conn = redisClient.connect(uriWithCredentials);
        return new TestConnection() {

            @Override
            public RedisCommands<String, String> commands() {
                return conn.sync();
            }

            @Override
            public void close() {
                conn.close();
            }

        };
    }

    @Override
    protected RedisConditions getConditions() {
        return RedisConditions.of(standaloneConnection);
    }

    // ========== Multi-user mTLS tests ==========

    @Test
    void connectWithDifferentMtlsUser() {
        // User 2: Client-test-2.p12 (CN=Client-test-2)
        SslOptions user2SslOptions = createMtlsSslOptions(getContainerName(), getTlsPath(), ClientCertificate.USER_2);
        RedisClient user2Client = RedisClient.create(getClientResources(),
                RedisURI.builder().withHost(host()).withPort(getPort()).withSsl(true).withVerifyPeer(verifyPeer()).build());
        user2Client.setOptions(clientOptionsBuilder(user2SslOptions).build());

        try (StatefulRedisConnection<String, String> conn = user2Client.connect()) {
            RedisCommands<String, String> sync = conn.sync();
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
    void caseMismatchCertificateShouldNotAuthenticateAsMtlsUser() {
        // client.p12 has CN=Client-Test-cert (uppercase T)
        // ACL user is Client-test-cert (lowercase t)
        // Redis ACL usernames are case-sensitive, so mTLS client auth should NOT work
        // The connection will fall back to default user (since standalone has no requirepass)
        SslOptions caseMismatchSslOptions = createMtlsSslOptions(getContainerName(), getTlsPath(),
                ClientCertificate.NO_ACL_USER);
        RedisClient caseMismatchClient = RedisClient.create(getClientResources(),
                RedisURI.builder().withHost(host()).withPort(getPort()).withSsl(true).withVerifyPeer(verifyPeer()).build());
        caseMismatchClient.setOptions(clientOptionsBuilder(caseMismatchSslOptions).build());

        try (StatefulRedisConnection<String, String> conn = caseMismatchClient.connect()) {
            RedisCommands<String, String> sync = conn.sync();

            // The connection should work but authenticate as "default" user,
            // NOT as the certificate CN "Client-Test-cert" (which doesn't exist as ACL user)
            String whoami = sync.aclWhoami();
            assertThat(whoami).isEqualTo("default");
            assertThat(whoami).isNotEqualTo("Client-Test-cert");
            assertThat(whoami).isNotEqualTo("Client-test-cert");
        } finally {
            caseMismatchClient.shutdown();
        }
    }

}
