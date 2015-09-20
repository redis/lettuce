package com.lambdaworks.redis;

import org.junit.Test;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisClientFactoryTest {

    private final static String URI = "redis://" + TestSettings.host() + ":" + TestSettings.port();
    private final static RedisURI REDIS_URI = RedisURI.create(URI);

    @Test
    public void plain() throws Exception {
        FastShutdown.shutdown(RedisClient.create());
    }

    @Test
    public void withStringUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withStringUriNull() throws Exception {
        RedisClient.create((String) null);
    }

    @Test
    public void withUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(REDIS_URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withUriNull() throws Exception {
        RedisClient.create((RedisURI) null);
    }
}
