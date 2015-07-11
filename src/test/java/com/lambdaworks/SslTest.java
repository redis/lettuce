package com.lambdaworks;

import static com.lambdaworks.redis.TestSettings.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;
import io.netty.handler.codec.DecoderException;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class SslTest {
    public static final String KEYSTORE = "work/keystore.jks";
    public static RedisClient redisClient = new RedisClient();

    @Before
    public void before() throws Exception {
        assumeTrue("Assume that stunnel runs on port 6443", Sockets.isOpen(host(), sslPort()));
        assertThat(new File(KEYSTORE)).exists();
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE);
    }

    @AfterClass
    public static void afterClass() {
        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void regularSsl() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisConnection<String, String> connection = redisClient.connect(redisUri);
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");

        connection.close();
    }

    @Test
    public void pingBeforeActivate() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();
        redisClient.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

        RedisConnection<String, String> connection = redisClient.connect(redisUri);
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");

        connection.close();
    }

    @Test
    public void regularSslWithReconnect() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisConnection<String, String> connection = redisClient.connect(redisUri);
        connection.set("key", "value");
        connection.quit();
        assertThat(connection.get("key")).isEqualTo("value");
        connection.close();
    }

    @Test(expected = RedisConnectionException.class)
    public void sslWithVerificationWillFail() throws Exception {

        assumeTrue(JavaRuntime.AT_LEAST_JDK_7);
        RedisURI redisUri = RedisURI.create("rediss://" + host() + ":" + sslPort());

        RedisConnection<String, String> connection = redisClient.connect(redisUri);

    }

    @Test
    public void pubSubSsl() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisPubSubConnection<String, String> connection = redisClient.connectPubSub(redisUri);
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(200);

        RedisPubSubConnection<String, String> connection2 = redisClient.connectPubSub(redisUri);

        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");
        connection.quit();
        Thread.sleep(200);

        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");

        connection.close();
        connection2.close();
    }

    @Test
    public void pubSubSslAndBreakConnection() throws Exception {
        assumeTrue(JavaRuntime.AT_LEAST_JDK_7);

        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        redisClient.setOptions(new ClientOptions.Builder().suspendReconnectOnProtocolFailure(true).build());

        RedisPubSubConnection<String, String> connection = redisClient.connectPubSub(redisUri);
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(200);

        RedisPubSubConnection<String, String> connection2 = redisClient.connectPubSub(redisUri);
        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");

        redisUri.setVerifyPeer(true);

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

}
