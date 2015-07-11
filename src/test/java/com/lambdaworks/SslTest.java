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
import org.junit.Test;

import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.pubsub.api.sync.RedisPubSubCommands;
import io.netty.handler.codec.DecoderException;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class SslTest extends AbstractTest {
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

        RedisConnection<String, String> connection = redisClient.connect(redisUri).sync();
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");
        connection.close();
    }

    @Test
    public void pingBeforeActivate() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();
        redisClient.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

        RedisConnection<String, String> connection = redisClient.connect(redisUri).sync();
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");

        connection.close();
    }

    @Test
    public void regularSslWithReconnect() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisConnection<String, String> connection = redisClient.connect(redisUri).sync();
        connection.set("key", "value");
        Thread.sleep(200);
        assertThat(connection.get("key")).isEqualTo("value");
        connection.close();
    }

    @Test(expected = RedisConnectionException.class)
    public void sslWithVerificationWillFail() throws Exception {

        RedisURI redisUri = RedisURI.create("rediss://" + host() + ":" + sslPort());

        RedisConnection<String, String> connection = redisClient.connect(redisUri).sync();

    }

    @Test
    public void pubSubSsl() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisPubSubCommands<String, String> connection = redisClient.connectPubSub(redisUri).sync();
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(200);

        RedisPubSubCommands<String, String> connection2 = redisClient.connectPubSub(redisUri).sync();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");
        connection.quit();
        Thread.sleep(200);
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(connection2.pubsubChannels()).contains("c1", "c2");

        connection.close();
        connection2.close();
    }

    @Test
    public void pubSubSslAndBreakConnection() throws Exception {

        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        redisClient.setOptions(new ClientOptions.Builder().suspendReconnectOnProtocolFailure(true).build());

        RedisPubSubAsyncCommands<String, String> connection = redisClient.connectPubSub(redisUri).async();
        connection.subscribe("c1").get();
        connection.subscribe("c2").get();
        Thread.sleep(200);

        RedisPubSubAsyncCommands<String, String> connection2 = redisClient.connectPubSub(redisUri).async();

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
