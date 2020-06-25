/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.test.settings.TestSettings.sslPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.test.CanConnect;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.settings.TestSettings;
import io.netty.handler.ssl.OpenSsl;

/**
 * Tests using SSL via {@link RedisClient}.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
@ExtendWith(LettuceExtension.class)
class SslIntegrationTests extends TestSupport {

    private static final String KEYSTORE = "work/keystore.jks";

    private static final String TRUSTSTORE = "work/truststore.jks";

    private static final File TRUSTSTORE_FILE = new File(TRUSTSTORE);

    private static final File CA_CERT_FILE = new File("work/ca/certs/ca.cert.pem");

    private static final int MASTER_SLAVE_BASE_PORT_OFFSET = 2000;

    private static final RedisURI URI_VERIFY = sslURIBuilder(0) //
            .withVerifyPeer(true) //
            .build();

    private static final RedisURI URI_NO_VERIFY = sslURIBuilder(1) //
            .withVerifyPeer(false) //
            .build();

    private static final RedisURI URI_CLIENT_CERT_AUTH = sslURIBuilder(2) //
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

    @Inject
    SslIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @BeforeAll
    static void beforeClass() {

        assumeTrue(CanConnect.to(TestSettings.host(), sslPort()), "Assume that stunnel runs on port 6443");
        assertThat(TRUSTSTORE_FILE).exists();
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
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    void standaloneWithPemCert() {

        SslOptions sslOptions = SslOptions.builder() //
                .trustManager(CA_CERT_FILE) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    void standaloneWithJdkSslUsingTruststoreUrl() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL()) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    void standaloneWithClientCertificates() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .keystore(new File(KEYSTORE), "changeit".toCharArray()) //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_CLIENT_CERT_AUTH);
    }

    @Test
    void standaloneWithClientCertificatesWithoutKeystore() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyConnection(URI_CLIENT_CERT_AUTH)).isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void standaloneWithJdkSslUsingTruststoreUrlWithWrongPassword() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(), "knödel") //
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
                .truststore(TRUSTSTORE_FILE) //
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
    void pingBeforeActivate() {

        redisClient.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        verifyConnection(URI_NO_VERIFY);
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
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    void masterSlaveWithJdkSslUsingTruststoreUrl() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL()) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    void masterSlaveWithJdkSslUsingTruststoreUrlWithWrongPassword() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(), "knödel") //
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
    void masterSlavePingBeforeActivate() {

        redisClient.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_NO_VERIFY);
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
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_WITH_ONE_INVALID);
    }

    @Test
    void masterSlaveSslWithAllInvalidHostsWillFail() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        assertThatThrownBy(() -> verifyMasterSlaveConnection(MASTER_SLAVE_URIS_WITH_ALL_INVALID))
                .isInstanceOf(RedisConnectionException.class);
    }

    @Test
    void pubSubSsl() {

        RedisPubSubCommands<String, String> connection = redisClient.connectPubSub(URI_NO_VERIFY).sync();
        connection.subscribe("c1");
        connection.subscribe("c2");
        Delay.delay(Duration.ofMillis(200));

        RedisPubSubCommands<String, String> connection2 = redisClient.connectPubSub(URI_NO_VERIFY).sync();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");
        connection.quit();
        Delay.delay(Duration.ofMillis(200));
        Wait.untilTrue(connection::isOpen).waitOrTimeout();
        Wait.untilEquals(2, () -> connection2.pubsubChannels().size()).waitOrTimeout();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");

        connection.getStatefulConnection().close();
        connection2.getStatefulConnection().close();
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

    private URL truststoreURL() throws MalformedURLException {
        return TRUSTSTORE_FILE.toURI().toURL();
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
