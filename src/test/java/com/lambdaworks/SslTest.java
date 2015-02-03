package com.lambdaworks;

import static com.lambdaworks.redis.TestSettings.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;

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
        redisClient.shutdown();
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
        RedisURI redisUri = RedisURI.create("rediss://" + host() + ":" + sslPort());

        RedisConnection<String, String> connection = redisClient.connect(redisUri);
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");

        connection.close();

    }

    @Test
    public void pubSubSsl() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisPubSubConnection<String, String> connection = redisClient.connectPubSub(redisUri);
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(100);

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
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(false).build();

        RedisPubSubConnection<String, String> connection = redisClient.connectPubSub(redisUri);
        connection.subscribe("c1");
        connection.subscribe("c2");
        Thread.sleep(100);

        RedisPubSubConnection<String, String> connection2 = redisClient.connectPubSub(redisUri);
        assertThat(connection2.pubsubChannels().get()).contains("c1", "c2");

        redisUri.setStartTls(true);
        redisUri.setVerifyPeer(true);

        connection.quit();
        Thread.sleep(200);

        assertThat(connection2.pubsubChannels().get()).doesNotContain("c1", "c2");

        connection.close();
        connection2.close();
    }
}
