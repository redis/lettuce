package com.lambdaworks;

import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.port;
import static com.lambdaworks.redis.TestSettings.sslPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisURI;

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

    @Test()
    @Ignore("Need strategy how to deal with broken SSL connections.")
    public void sslWithVerificationWillFail() throws Exception {
        RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort()).withSsl(true).withVerifyPeer(true).build();

        RedisConnection<String, String> connection = redisClient.connect(redisUri);
        connection.set("key", "value");
        assertThat(connection.get("key")).isEqualTo("value");

        connection.close();

    }
}
