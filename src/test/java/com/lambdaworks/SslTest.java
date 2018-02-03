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
package com.lambdaworks;

import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.sslPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.*;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.masterslave.MasterSlave;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.pubsub.api.sync.RedisPubSubCommands;

import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.OpenSsl;

/**
 * Tests using SSL via {@link RedisClient}.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
public class SslTest extends AbstractTest {

    private static final String KEYSTORE = "work/keystore.jks";
    private static final String TRUSTSTORE = "work/truststore.jks";
    private static final File TRUSTSTORE_FILE = new File(TRUSTSTORE);
    private static final int MASTER_SLAVE_BASE_PORT_OFFSET = 2000;

    private static final RedisURI URI_NO_VERIFY = sslURIBuilder(0) //
            .withVerifyPeer(false) //
            .build();

    private static final RedisURI URI_VERIFY = sslURIBuilder(1) //
            .withVerifyPeer(true) //
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

    @After
    public void tearDown() {

        ChannelGroup group = (ChannelGroup) ReflectionTestUtils.getField(redisClient, "channels");

        assertThat((Iterable) group).as("Test completed without connection cleanup").isEmpty();
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
        connection.close();
    }

    @Test(expected = RedisConnectionException.class)
    public void sslWithVerificationWillFail() {

        RedisURI redisUri = RedisURI.create("rediss://" + host() + ":" + sslPort());
        redisClient.connect(redisUri).sync();
    }

    @Test
    public void masterSlaveWithSsl() {

        RedisCommands<String, String> connection = MasterSlave.connect(redisClient, StringCodec.UTF8,
                MASTER_SLAVE_URIS_NO_VERIFY).sync();
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
    public void masterSlavePingBeforeActivate() {

        redisClient.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_NO_VERIFY);
    }

    @Test
    public void masterSlaveSslWithReconnect() throws Exception {
        RedisCommands<String, String> connection = MasterSlave.connect(redisClient, StringCodec.UTF8,
                MASTER_SLAVE_URIS_NO_VERIFY).sync();
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
    public void masterSlaveSslWithOneInvalidHostWillSucceed() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_WITH_ONE_INVALID);
    }

    @Test(expected = RedisConnectionException.class)
    public void masterSlaveSslWithAllInvalidHostsWillFail() {

        SslOptions sslOptions = SslOptions.builder() //
                .jdkSslProvider() //
                .truststore(TRUSTSTORE_FILE) //
                .build();
        setOptions(sslOptions);

        verifyMasterSlaveConnection(MASTER_SLAVE_URIS_WITH_ALL_INVALID);
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

        RedisChannelHandler channelHandler = (RedisChannelHandler) connection.getStatefulConnection();
        RedisChannelWriter channelWriter = channelHandler.getChannelWriter();
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

        connection.close();
        connection2.close();
    }

    private static RedisURI.Builder sslURIBuilder(int portOffset) {
        return RedisURI.Builder.redis(host(), sslPort(portOffset)).withSsl(true);
    }

    private static List<RedisURI> sslUris(IntStream masterSlaveOffsets,
            Function<RedisURI.Builder, RedisURI.Builder> builderCustomizer) {

        return masterSlaveOffsets.map(it -> it + MASTER_SLAVE_BASE_PORT_OFFSET)
                .mapToObj(offset -> RedisURI.Builder.redis(host(), sslPort(offset)).withSsl(true)).map(builderCustomizer)
                .map(RedisURI.Builder::build).collect(Collectors.toList());
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

        try (StatefulRedisConnection<String, String> connection = MasterSlave.connect(redisClient, StringCodec.UTF8, redisUris)) {
            connection.sync().ping();
        }
    }
}
