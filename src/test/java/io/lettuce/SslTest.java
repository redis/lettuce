/*
 * Copyright 2011-2018 the original author or authors.
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

import static io.lettuce.core.TestSettings.host;
import static io.lettuce.core.TestSettings.sslPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.OpenSsl;

/**
 * @author Mark Paluch
 */
public class SslTest extends AbstractTest {

    private static final String KEYSTORE = "work/keystore.jks";
    private static final String TRUSTSTORE = "work/truststore.jks";
    private static RedisClient redisClient;

    private static final RedisURI URI_NO_VERIFY = RedisURI.Builder.redis(host(), sslPort()) //
            .withSsl(true) //
            .withVerifyPeer(false) //
            .build();

    private static final RedisURI URI_VERIFY = RedisURI.Builder.redis(host(), sslPort(1)) //
            .withSsl(true) //
            .withVerifyPeer(true) //
            .build();

    private static final RedisURI URI_CLIENT_CERT_AUTH = RedisURI.Builder.redis(host(), sslPort(2)) //
            .withSsl(true) //
            .withVerifyPeer(true) //
            .build();

    @BeforeClass
    public static void beforeClass() {

        assumeTrue("Assume that stunnel runs on port 6443", Sockets.isOpen(host(), sslPort()));
        assertThat(new File(TRUSTSTORE)).exists();

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
                .truststore(new File(TRUSTSTORE)) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    public void standaloneWithJdkSslUsingTruststoreUrl() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(new File(TRUSTSTORE)) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_VERIFY);
    }

    @Test
    public void standaloneWithClientCertificates() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .keystore(new File(KEYSTORE), "changeit".toCharArray()) //
                .truststore(new File(TRUSTSTORE)) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_CLIENT_CERT_AUTH);
    }

    @Test(expected = RedisConnectionException.class)
    public void standaloneWithClientCertificatesWithoutKeystore() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(new File(TRUSTSTORE)) //
                .build();
        setOptions(sslOptions);

        verifyConnection(URI_CLIENT_CERT_AUTH);
    }

    @Test(expected = RedisConnectionException.class)
    public void standaloneWithJdkSslUsingTruststoreUrlWithWrongPassword() throws Exception {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(new File(TRUSTSTORE), "knödel") //
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
                .truststore(new File(TRUSTSTORE)) //
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

        RedisFuture<List<String>> defectFuture = connection.pubsubChannels();

        try {
            assertThat(defectFuture.get()).doesNotContain("c1", "c2");
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

    private void setOptions(SslOptions sslOptions) {
        ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();
        redisClient.setOptions(clientOptions);
    }

    private void verifyConnection(RedisURI redisUri) {
        StatefulRedisConnection<String, String> connection = redisClient.connect(redisUri);
        connection.sync().ping();
        connection.close();
    }
}
