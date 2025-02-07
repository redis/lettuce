/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.test.CanConnect;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.settings.TestSettings;
import io.netty.handler.ssl.OpenSsl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.test.settings.TestSettings.sslPort;
import static io.lettuce.test.settings.TlsSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests using SSL via {@link RedisClient}.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class SslIntegrationTests extends TestSupport {

    private static final String KEYSTORE = "work/keystore.jks";

    private static File truststoreFile0;

    private static File truststoreFile1;

    private static File truststoreFile2;

    private static File cacertFile;

    private static final int MASTER_SLAVE_BASE_PORT_OFFSET = 2000;

    private static final RedisURI URI_VERIFY = sslURIBuilder(0) //
            .withVerifyPeer(true) //
            .build();

    private static final RedisURI URI_VERIFY_IMPOSSIBLE_TIMEOUT = sslURIBuilder(0) //
            .withVerifyPeer(true) //
            .withTimeout(Duration.ZERO).build();

    private static final RedisURI URI_NO_VERIFY = sslURIBuilder(1) //
            .withVerifyPeer(false) //
            .build();

    private static final RedisURI URI_CLIENT_CERT_AUTH = sslURIBuilder(1) //
            .withVerifyPeer(true) //
            .build();

    private static final List<RedisURI> MASTER_SLAVE_URIS_NO_VERIFY = sslUris(IntStream.of(0, 1),
            builder -> builder.withVerifyPeer(false));

    private static final List<RedisURI> MASTER_SLAVE_URIS_VERIFY = sslUris(IntStream.of(0, 1),
            builder -> builder.withVerifyPeer(true));

    private static final List<RedisURI> MASTER_SLAVE_URIS_WITH_ONE_INVALID = sslUris(IntStream.of(0, 1, 2),
            builder -> builder.withVerifyPeer(true));

    private static final List<RedisURI> MASTER_SLAVE_URIS_WITH_ALL_INVALID = sslUris(IntStream.of(2, 3),
            builder -> builder.withVerifyPeer(true));

    private final RedisClient redisClient;

    private static File keystore;

    @Inject
    SslIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @BeforeAll
    static void beforeClass() {
        Path path0 = createAndSaveTestTruststore("redis-standalone-0", Paths.get("redis-standalone-0/work/tls"), "changeit");
        truststoreFile0 = path0.toFile();
        cacertFile = envCa(Paths.get("redis-standalone-0/work/tls")).toFile();

        Path path = createAndSaveTestTruststore("redis-standalone-1", Paths.get("redis-standalone-1/work/tls"), "changeit");
        truststoreFile1 = path.toFile();
        cacertFile = envCa(Paths.get("redis-standalone-1/work/tls")).toFile();

        Path path2 = createAndSaveTestTruststore("redis-standalone-sentinel-controlled",
                Paths.get("redis-standalone-sentinel-controlled/work/tls"), "changeit");
        truststoreFile2 = path2.toFile();
        cacertFile = envCa(Paths.get("redis-standalone-sentinel-controlled/work/tls")).toFile();

        try {
            generateCertificates(testGenCertPath("redis-standalone-0/work/tls").toString(), "redis-standalone-0/work/tls");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        keystore = Paths.get("redis-standalone-0/work/tls/keystore.jks").toFile();
        assumeTrue(CanConnect.to(TestSettings.host(), sslPort()), "Assume that stunnel runs on port 6443");
        // Maybe we should do a list.
        assertThat(truststoreFile0).exists();
        assertThat(truststoreFile1).exists();
        assertThat(truststoreFile2).exists();
    }

    @Test
    void standaloneWithSsl() {

        RedisCommands<String, String> connection = redisClient.connect(URI_NO_VERIFY).sync();
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");
        connection.getStatefulConnection().close();
    }

    @Test
    void standaloneWithJdkSsl() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreFile1, "changeit") //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    void standaloneWithVerifyCaOnly() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreFile0, "changeit") //
                .build();
        setOptions(sslOptions);

        verifyConnection(sslURIBuilder(1).withVerifyPeer(SslVerifyMode.CA).build());
    }

    @Test
    void standaloneWithPemCert() {

        SslOptions sslOptions = SslOptions.builder() //
                .trustManager(envCa(Paths.get("redis-standalone-1/work/tls")).toFile()) //
                .build();
        setOptions(sslOptions);
        verifyConnection(URI_VERIFY);
    }

    @Test
    void standaloneWithPemCertAndImpossibleTimeout() {

        Assertions.setMaxStackTraceElementsDisplayed(30);
        SslOptions sslOptions = SslOptions.builder() //
                .trustManager(envCa(Paths.get("redis-standalone-1/work/tls")).toFile()) //
                .build();
        setOptions(sslOptions);
        redisClient.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).sslOptions(sslOptions).build());

        try {
            redisClient.connect(URI_VERIFY_IMPOSSIBLE_TIMEOUT).close();
        } catch (Exception e) {

            assertThat(e).isInstanceOf(RedisConnectionException.class)
                    .hasRootCauseInstanceOf(RedisCommandTimeoutException.class);
        }
    }

    @Test
    void standaloneWithJdkSslUsingTruststoreUrl() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(truststoreFile1), "changeit") //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    void standaloneWithClientCertificates() {
        // 6444
        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .keystore(keystore, "changeit".toCharArray()) //
                .truststore(truststoreFile0, "changeit") //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_CLIENT_CERT_AUTH);
    }

    @Test
    void standaloneWithClientCertificatesWithoutKeystore() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreFile1, "changeit") //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyConnection(URI_CLIENT_CERT_AUTH)).isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void standaloneWithJdkSslUsingTruststoreUrlWithWrongPassword() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(truststoreFile0), "knödel") //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyConnection(URI_VERIFY)).isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void standaloneWithJdkSslFailsWithWrongTruststore() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyConnection(URI_VERIFY)).isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void standaloneWithOpenSsl() {

        assumeTrue(OpenSsl.isAvailable());

        SslOptions sslOptions = SslOptions.builder() //
                .openSslProvider() //
                .truststore(truststoreFile0, "changeit") //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    void standaloneWithOpenSslFailsWithWrongTruststore() {

        assumeTrue(OpenSsl.isAvailable());

        SslOptions sslOptions = SslOptions.builder() //
                .openSslProvider() //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyConnection(URI_VERIFY)).isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void regularSslWithReconnect() {

        RedisCommands<String, String> connection = redisClient.connect(URI_NO_VERIFY).sync();
        connection.quit();
        Delay.delay(Duration.ofMillis(200));
        assertThat(connection.ping()).isEqualTo("PONG");
        connection.getStatefulConnection().close();
    }

    @Test
    void sslWithVerificationWillFail() {

        RedisURI redisUri = RedisURI.create("rediss://" + TestSettings.host() + ":" + sslPort());

        assertThatThrownBy(() -> redisClient.connect(redisUri).sync()).isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void masterSlaveWithSsl() {

        RedisCommands<String, String> connection = MasterSlave
                .connect(redisClient, StringCodec.UTF8, MASTER_SLAVE_URIS_NO_VERIFY).sync();
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");
        connection.getStatefulConnection().close();
    }

    @Test
    void masterSlaveWithJdkSsl() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreFile2, "changeit") //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    void masterSlaveWithJdkSslUsingTruststoreUrl() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(truststoreFile2), "changeit") //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    void masterSlaveWithJdkSslUsingTruststoreUrlWithWrongPassword() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(truststoreFile0), "knödel") //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY))
                .isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void masterSlaveWithJdkSslFailsWithWrongTruststore() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY))
                .isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void masterSlaveSslWithReconnect() {
        RedisCommands<String, String> connection = MasterSlave
                .connect(redisClient, StringCodec.UTF8, MASTER_SLAVE_URIS_NO_VERIFY).sync();
        connection.quit();
        Delay.delay(Duration.ofMillis(200));
        assertThat(connection.ping()).isEqualTo("PONG");
        connection.getStatefulConnection().close();
    }

    @Test
    void masterSlaveSslWithVerificationWillFail() {
        assertThatThrownBy(() -> MasterSlave.connect(redisClient, StringCodec.UTF8, MASTER_SLAVE_URIS_VERIFY))
                .isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void masterSlaveSslWithOneInvalidHostWillSucceed() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreFile2, "changeit") //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_WITH_ONE_INVALID);
    }

    @Test
    void masterSlaveSslWithAllInvalidHostsWillFail() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreFile0, "changeit") //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyMasterSlaveConnection(MASTER_SLAVE_URIS_WITH_ALL_INVALID))
                .isInstanceOf(RedisConnectionException.class);
    }

    @Test
    @Disabled
    // This test is frequently failing on the pipeline and passing locally, so is considered very unstable
    // The 120 seconds timeout used to stabilize it somewhat, but no longer
    void pubSubSsl() {

        RedisPubSubCommands<String, String> connection = redisClient.connectPubSub(URI_NO_VERIFY).sync();
        connection.subscribe("c1");
        connection.subscribe("c2");

        RedisPubSubCommands<String, String> connection2 = redisClient.connectPubSub(URI_NO_VERIFY).sync();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");
        connection.quit();
        Wait.untilTrue(connection.getStatefulConnection()::isOpen).waitOrTimeout();
        Wait.untilEquals(2, () -> connection2.pubsubChannels().size()).during(Duration.ofSeconds(120)).waitOrTimeout();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");
    }

    private static RedisURI.Builder sslURIBuilder(int portOffset) {
        return RedisURI.Builder.redis(TestSettings.host(), sslPort(portOffset)).withSsl(true);
    }

    private static List<RedisURI> sslUris(IntStream masterSlaveOffsets,
            Function<RedisURI.Builder, RedisURI.Builder> builderCustomizer) {

        return masterSlaveOffsets.map(it -> it + MASTER_SLAVE_BASE_PORT_OFFSET)
                .mapToObj(offset -> RedisURI.Builder.redis(TestSettings.host(), sslPort(offset)).withSsl(true))
                .map(builderCustomizer).map(RedisURI.Builder::build).collect(Collectors.toList());
    }

    private URL truststoreURL(File truststoreFile) throws MalformedURLException {
        return truststoreFile.toURI().toURL();
    }

    private void setOptions(SslOptions sslOptions) {
        ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();
        redisClient.setOptions(clientOptions);
    }

    private void verifyConnection(RedisURI redisUri) {

        try (StatefulRedisConnection<String, String> connection = redisClient.connect(redisUri)) {
            connection.sync().ping();
        }
    }

    private void verifyMasterSlaveConnection(List<RedisURI> redisUris) {

        try (StatefulRedisConnection<String, String> connection = MasterSlave.connect(redisClient, StringCodec.UTF8,
                redisUris)) {
            connection.sync().ping();
        }
    }

}
