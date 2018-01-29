/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.OpenSsl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static io.lettuce.core.TestSettings.host;
import static io.lettuce.core.TestSettings.sslPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

/**
 * @author Mark Paluch
 */
public class SslTest extends AbstractTest {

    private static final String KEYSTORE = "work/keystore.jks";
    private static final String TRUSTSTORE = "work/truststore.jks";
    private static final File TRUSTSTORE_FILE = new File(TRUSTSTORE);
    private static final int MASTER_SLAVE_BASE_PORT_OFFSET = 2000;

    private static final RedisURI URI_NO_VERIFY = sslURIBuilder(0)
            .withVerifyPeer(false) //
            .build();

    private static final List<RedisURI> MASTER_SLAVE_URIS_NO_VERIFY = Arrays.asList(
            masterSlaveSSLURIBuilder(MASTER_SLAVE_BASE_PORT_OFFSET)
                    .withVerifyPeer(false)
                    .build(),
            masterSlaveSSLURIBuilder(MASTER_SLAVE_BASE_PORT_OFFSET + 1)
                    .withVerifyPeer(false)
                    .build());

    private static final RedisURI URI_VERIFY = sslURIBuilder(1) //
            .withVerifyPeer(true) //
            .build();

    private static final List<RedisURI> MASTER_SLAVE_URIS_VERIFY = Arrays.asList(
            masterSlaveSSLURIBuilder(MASTER_SLAVE_BASE_PORT_OFFSET)
                    .withVerifyPeer(true)
                    .build(),
            masterSlaveSSLURIBuilder(MASTER_SLAVE_BASE_PORT_OFFSET + 1)
                    .withVerifyPeer(true)
                    .build());

    private static final RedisURI URI_CLIENT_CERT_AUTH = sslURIBuilder(2) //
            .withVerifyPeer(true) //
            .build();

    private static final List<RedisURI> MASTER_SLAVE_URIS_CLIENT_CERT_AUTH = Arrays.asList(
            masterSlaveSSLURIBuilder(MASTER_SLAVE_BASE_PORT_OFFSET + 2)
                    .withVerifyPeer(true)
                    .build(),
            masterSlaveSSLURIBuilder(MASTER_SLAVE_BASE_PORT_OFFSET + 3)
                    .withVerifyPeer(true)
                    .build());

    private static RedisClient redisClient;

    @BeforeClass
    public static void beforeClass() {

        assumeTrue("Assume that stunnel runs on port 6443", Sockets.isOpen(host(), sslPort()));
        assertThat(TRUSTSTORE_FILE).exists();

        redisClient = RedisClient.create(TestClientResources.get());
    }

    @Before
    public void before() {
        redisClient.setOptions(ClientOptions.create());
    }

    @AfterClass
    public static void afterClass() {
        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void standaloneWithSsl() {

        RedisCommands<String, String> connection = redisClient.connect(URI_NO_VERIFY).sync();
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");
        connection.getStatefulConnection().close();
    }

    @Test
    public void standaloneWithJdkSsl() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    public void standaloneWithJdkSslUsingTruststoreUrl() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL()) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    public void standaloneWithClientCertificates() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .keystore(new File(KEYSTORE), "changeit".toCharArray()) //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_CLIENT_CERT_AUTH);
    }

    @Test(expected = RedisConnectionException.class)
    public void standaloneWithClientCertificatesWithoutKeystore() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_CLIENT_CERT_AUTH);
    }

    @Test(expected = RedisConnectionException.class)
    public void standaloneWithJdkSslUsingTruststoreUrlWithWrongPassword() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(), "knödel") //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test(expected = RedisConnectionException.class)
    public void standaloneWithJdkSslFailsWithWrongTruststore() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    public void standaloneWithOpenSsl() {

        assumeTrue(OpenSsl.isAvailable());

        SslOptions sslOptions = SslOptions.builder() //
                .openSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test(expected = RedisConnectionException.class)
    public void standaloneWithOpenSslFailsWithWrongTruststore() {

        assumeTrue(OpenSsl.isAvailable());

        SslOptions sslOptions = SslOptions.builder() //
                .openSslProvider() //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    public void pingBeforeActivate() {

        redisClient.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        verifyConnection(URI_NO_VERIFY);
    }

    @Test
    public void regularSslWithReconnect() throws Exception {

        RedisCommands<String, String> connection = redisClient.connect(URI_NO_VERIFY).sync();
        connection.quit();
        Thread.sleep(200);
        assertThat(connection.ping()).isEqualTo("PONG");
        connection.getStatefulConnection().close();
    }

    @Test(expected = RedisConnectionException.class)
    public void sslWithVerificationWillFail() {

        RedisURI redisUri = RedisURI.create("rediss://" + host() + ":" + sslPort());
        redisClient.connect(redisUri).sync();
    }

    @Test
    public void masterSlaveWithSsl() {

        RedisCommands<String, String> connection =
                MasterSlave.connect(redisClient, StringCodec.UTF8, MASTER_SLAVE_URIS_NO_VERIFY).sync();
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");
        connection.getStatefulConnection().close();
    }

    @Test
    public void masterSlaveWithJdkSsl() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    public void masterSlaveWithJdkSslUsingTruststoreUrl() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL()) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    public void masterSlaveWithClientCertificates() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .keystore(new File(KEYSTORE), "changeit".toCharArray()) //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_CLIENT_CERT_AUTH);
    }


    @Test(expected = RedisConnectionException.class)
    public void masterSlaveWithClientCertificatesWithoutKeystore() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_CLIENT_CERT_AUTH);
    }

    @Test(expected = RedisConnectionException.class)
    public void masterSlaveWithJdkSslUsingTruststoreUrlWithWrongPassword() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(truststoreURL(), "knödel") //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test(expected = RedisConnectionException.class)
    public void masterSlaveWithJdkSslFailsWithWrongTruststore() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    public void masterSlaveWithOpenSsl() {

        assumeTrue(OpenSsl.isAvailable());

        SslOptions sslOptions = SslOptions.builder() //
                .openSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test(expected = RedisConnectionException.class)
    public void masterSlaveWithOpenSslFailsWithWrongTruststore() {

        assumeTrue(OpenSsl.isAvailable());

        SslOptions sslOptions = SslOptions.builder() //
                .openSslProvider() //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    public void masterSlavePingBeforeActivate() {

        redisClient.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_NO_VERIFY);
    }

    @Test
    public void masterSlaveSslWithReconnect() throws Exception {
        RedisCommands<String, String> connection =
                MasterSlave.connect(redisClient, StringCodec.UTF8, MASTER_SLAVE_URIS_NO_VERIFY).sync();
        connection.quit();
        Thread.sleep(200);
        assertThat(connection.ping()).isEqualTo("PONG");
        connection.getStatefulConnection().close();
    }

    @Test(expected = RedisConnectionException.class)
    public void masterSlaveSslWithVerificationWillFail() {
        MasterSlave.connect(redisClient, StringCodec.UTF8, MASTER_SLAVE_URIS_VERIFY);
    }

    @Test
    public void pubSubSsl() throws Exception {

        RedisPubSubCommands<String, String> connection = redisClient.connectPubSub(URI_NO_VERIFY).sync();
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(200);

        RedisPubSubCommands<String, String> connection2 = redisClient.connectPubSub(URI_NO_VERIFY).sync();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");
        connection.quit();
        Thread.sleep(200);
        Wait.untilTrue(connection::isOpen).waitOrTimeout();
        Wait.untilEquals(2, () -> connection2.pubsubChannels().size()).waitOrTimeout();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");

        connection.getStatefulConnection().close();
        connection2.getStatefulConnection().close();
    }

    @Test(timeout = 10000)
    public void pubSubSslAndBreakConnection() throws Exception {

        RedisURI redisURI = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();
        redisClient.setOptions(ClientOptions.builder().suspendReconnectOnProtocolFailure(true).build());

        RedisPubSubAsyncCommands<String, String> connection = redisClient.connectPubSub(redisURI).async();
        RedisPubSubAsyncCommands<String, String> connection2 = redisClient.connectPubSub(redisURI).async();

        redisURI.setVerifyPeer(true);
        connection.subscribe("c1");
        connection.subscribe("c2");

        Wait.untilTrue(() -> connection2.pubsubChannels().get().containsAll(Arrays.asList("c1", "c2"))).waitOrTimeout();

        try {
            connection.quit().get();
        } catch (Exception e) {
        }

        List<String> future = connection2.pubsubChannels().get();
        assertThat(future).doesNotContain("c1", "c2");

        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection.getStatefulConnection());
        Wait.untilNotEquals(null, () -> ReflectionTestUtils.getField(channelWriter, "connectionError")).waitOrTimeout();

        RedisFuture<Void> defectFuture = connection.subscribe("foo");

        try {
            defectFuture.get();
            fail("Missing ExecutionException with nested SSLHandshakeException");
        } catch (InterruptedException e) {
            fail("Missing ExecutionException with nested SSLHandshakeException");
        } catch (ExecutionException e) {
            assertThat(e).hasCauseInstanceOf(DecoderException.class);
            assertThat(e).hasRootCauseInstanceOf(CertificateException.class);
        }

        assertThat(defectFuture).isDone();

        connection.getStatefulConnection().close();
        connection2.getStatefulConnection().close();
    }


    private static RedisURI.Builder sslURIBuilder(int portOffset) {
        return RedisURI.Builder.redis(host(), sslPort(portOffset))
                .withSsl(true);
    }

    private static RedisURI.Builder masterSlaveSSLURIBuilder(int portOffset) {
        return sslURIBuilder(portOffset)
                // TODO - why do the master/slave tests need this but the standalone ones don't???
                .withTimeout(Duration.of(500, ChronoUnit.MILLIS));
    }

    private URL truststoreURL() throws MalformedURLException {
        return TRUSTSTORE_FILE.toURI().toURL();
    }

    private void setOptions(SslOptions sslOptions) {
        ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();
        redisClient.setOptions(clientOptions);
    }

    private void verifyConnection(RedisURI redisUri) {
        StatefulRedisConnection<String, String> connection = redisClient.connect(redisUri);
        connection.sync().ping();
        connection.close();
    }

    private void verifyMasterSlaveConnection(List<RedisURI> redisUris) {
        StatefulRedisConnection<String, String> connection =
                MasterSlave.connect(redisClient, StringCodec.UTF8, redisUris);
        connection.sync().ping();
        connection.close();
    }
}
