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
package com.lambdaworks;

import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.sslPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.pubsub.api.sync.RedisPubSubCommands;

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
        connection.close();
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
        connection.close();
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

        connection.close();
        connection2.close();
    }

    @Test
    public void pubSubSslAndBreakConnection() throws Exception {

        RedisURI redisURI = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();
        redisClient.setOptions(ClientOptions.builder().suspendReconnectOnProtocolFailure(true).build());

        RedisPubSubAsyncCommands<String, String> connection = redisClient.connectPubSub(redisURI).async();
        connection.subscribe("c1").get();
        connection.subscribe("c2").get();
        Thread.sleep(200);

        RedisPubSubAsyncCommands<String, String> connection2 = redisClient.connectPubSub(redisURI).async();

        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");

        redisURI.setVerifyPeer(true);

        connection.quit();
        Thread.sleep(500);

        RedisFuture<List<String>> future = connection2.pubsubChannels();
        assertThat(future.get()).doesNotContain("c1", "c2");
        assertThat(future.isDone()).isEqualTo(true);

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

        assertThat(defectFuture.isDone()).isEqualTo(true);

        connection.close();
        connection2.close();
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
