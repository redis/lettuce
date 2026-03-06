/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.test.settings.TestSettings.host;
import static io.lettuce.test.settings.TestSettings.mtlsStandalonePort;
import static io.lettuce.test.settings.TlsSettings.MTLS_STANDALONE_CONTAINER;
import static io.lettuce.test.settings.TlsSettings.MTLS_STANDALONE_TLS_PATH;

import java.nio.file.Path;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.condition.RedisConditions;

/**
 * Integration tests for Redis 8.6+ mTLS automatic authentication (standalone mode).
 * <p>
 * This class provides the standalone implementation of mTLS auto-auth tests. The actual test methods are inherited from
 * {@link AbstractMtlsAutoAuthIntegrationTests}.
 *
 * @author Aleksandar Todorov
 */
class MtlsAutoAuthIntegrationTests extends AbstractMtlsAutoAuthIntegrationTests {

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

}
