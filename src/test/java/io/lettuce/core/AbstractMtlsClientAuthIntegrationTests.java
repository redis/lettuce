/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.test.settings.TestSettings.host;
import static io.lettuce.test.settings.TlsSettings.ClientCertificate;
import static io.lettuce.test.settings.TlsSettings.createMtlsSslOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.lettuce.core.api.BaseRedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.CanConnect;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.resource.TestClientResources;

/**
 * Abstract base class for Redis 8.6+ mTLS client authentication integration tests.
 * <p>
 * When mTLS is configured and the client certificate's Common Name (CN) matches an existing ACL user, the client is
 * automatically authenticated as that user without needing to send an AUTH command.
 * <p>
 * Subclasses provide the concrete client implementation (standalone or cluster) and configuration.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractMtlsClientAuthIntegrationTests extends TestSupport {

    // The CN in the client certificate - must match an ACL user for client auth
    // Note: Case-sensitive - must exactly match the CN in the certificate
    protected static final String CLIENT_CERT_CN = "Client-test-cert";

    protected SslOptions sslOptions;

    protected RedisURI redisURI;

    /**
     * The Redis client (standalone or cluster). Subclasses set this in {@link #initializeClient()}.
     */
    protected BaseRedisClient client;

    /**
     * The primary connection used for version checking. Subclasses set this in {@link #initializeClient()}.
     */
    protected StatefulConnection<String, String> connection;

    /**
     * @return the port to connect to
     */
    protected abstract int getPort();

    /**
     * @return the path to TLS certificates
     */
    protected abstract Path getTlsPath();

    /**
     * @return the container/environment name for truststore creation
     */
    protected abstract String getContainerName();

    /**
     * @return the SSL verification mode. Use {@link SslVerifyMode#FULL} for standalone (validates CA and hostname), or
     *         {@link SslVerifyMode#CA} for cluster (validates CA only, allows internal IPs from topology discovery).
     */
    protected abstract SslVerifyMode verifyPeer();

    /**
     * @return the server password, or null if no password is configured. Subclasses can override this for use in
     *         {@link #connectAsDefaultUser()}.
     */
    protected String getServerPassword() {
        return null;
    }

    /**
     * Builds ClientOptions with the given SslOptions. Subclasses can override to customize (e.g., set protocol version).
     *
     * @param sslOptions the SSL options to include
     * @return the ClientOptions builder to use
     */
    protected abstract ClientOptions.Builder clientOptionsBuilder(SslOptions sslOptions);

    /**
     * Initialize the client and establish the initial connection for version checking.
     */
    protected abstract void initializeClient();

    /**
     * Create a new connection and return its sync commands.
     *
     * @return RedisCommands for the new connection
     */
    protected abstract RedisCommands<String, String> connect();

    /**
     * Close a connection obtained from {@link #connect()}.
     *
     * @param commands the commands instance to close
     */
    protected abstract void closeCommands(RedisCommands<String, String> commands);

    /**
     * Create a new connection that explicitly authenticates as the 'default' user (not via mTLS client auth). The caller is
     * responsible for closing the returned TestConnection.
     * <p>
     * For servers with password: should use password-based authentication. For servers without password: should use explicit
     * username-only authentication.
     *
     * @return TestConnection wrapping the commands and cleanup logic
     */
    protected abstract TestConnection connectAsDefaultUser();

    /**
     * Shutdown the client and close all connections. Uses the {@link #connection} and {@link #client} fields set by
     * {@link #initializeClient()}.
     */
    protected void shutdownClient() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * @return RedisConditions for version checking
     */
    protected abstract RedisConditions getConditions();

    /**
     * @return the ClientResources to use for creating clients
     */
    protected ClientResources getClientResources() {
        return TestClientResources.get();
    }

    @BeforeAll
    void beforeAll() {
        assumeTrue(CanConnect.to(host(), getPort()), "Assume that Redis with mTLS runs on port " + getPort());

        // Check if mTLS certificate files exist (only available on Redis 8.0+)
        Path keystorePath = Paths.get(System.getenv().getOrDefault("TEST_WORK_FOLDER", "work/docker"), getTlsPath().toString(),
                ClientCertificate.DEFAULT.getFilename());
        assumeTrue(Files.exists(keystorePath), "mTLS certificate file does not exist (requires Redis 8.0+): " + keystorePath);

        sslOptions = createMtlsSslOptions(getContainerName(), getTlsPath(), ClientCertificate.DEFAULT);

        redisURI = RedisURI.builder().withHost(host()).withPort(getPort()).withSsl(true).withVerifyPeer(verifyPeer()).build();

        try {
            initializeClient();
        } catch (Exception e) {
            // Connection may fail on Redis versions < 8.6 that don't support mTLS client auth
            // Tests will be skipped in beforeEach() due to null connection
        }
    }

    @AfterAll
    void afterAll() {
        shutdownClient();
    }

    @BeforeEach
    void setUp() {
        // Skip tests if connection could not be established (e.g., Redis < 8.6 without mTLS support)
        assumeTrue(connection != null, "mTLS connection not available - requires Redis 8.6+ with mTLS client auth");

        RedisConditions conditions = getConditions();
        assumeTrue(conditions.hasVersionGreaterOrEqualsTo("8.6"), "mTLS client authentication requires Redis 8.6+");
    }

    @Test
    void mtlsClientAuthWithClientCertificate() {
        RedisCommands<String, String> commands = connect();
        try {
            // Verify connection works
            assertThat(commands.ping()).isEqualTo("PONG");

            // Verify we're authenticated as the certificate CN user
            assertThat(commands.aclWhoami()).isEqualTo(CLIENT_CERT_CN);
        } finally {
            closeCommands(commands);
        }
    }

    @Test
    void mtlsClientAuthWithReconnect() {
        RedisCommands<String, String> commands = connect();
        try {
            // Verify initial authentication
            assertThat(commands.aclWhoami()).isEqualTo(CLIENT_CERT_CN);

            // Force reconnection
            commands.quit();

            // After reconnect, should still be authenticated via mTLS
            assertThat(commands.ping()).isEqualTo("PONG");
            assertThat(commands.aclWhoami()).isEqualTo(CLIENT_CERT_CN);
        } finally {
            closeCommands(commands);
        }
    }

    @Test
    void explicitAuthReturnsDefaultUser() {
        try (TestConnection testConn = connectAsDefaultUser()) {
            // Verify connection works
            assertThat(testConn.commands().ping()).isEqualTo("PONG");

            // Verify we're authenticated as 'default' user (explicit auth, not mTLS client auth)
            assertThat(testConn.commands().aclWhoami()).isEqualTo("default");
        }
    }

    /**
     * Wrapper for a test connection that handles cleanup of both connection and client (if needed).
     */
    protected interface TestConnection extends AutoCloseable {

        RedisCommands<String, String> commands();

        @Override
        void close();

    }

}
