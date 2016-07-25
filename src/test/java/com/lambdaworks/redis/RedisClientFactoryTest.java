package com.lambdaworks.redis;

import com.lambdaworks.TestClientResources;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;

/**
 * @author Mark Paluch
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

    @Test
    public void clientResources() throws Exception {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNull() throws Exception {
        RedisClient.create((ClientResources) null);
    }

    @Test
    public void clientResourcesWithStringUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(), URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithStringUriNull() throws Exception {
        RedisClient.create(TestClientResources.get(), (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNullWithStringUri() throws Exception {
        RedisClient.create(null, URI);
    }

    @Test
    public void clientResourcesWithUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(),  REDIS_URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithUriNull() throws Exception {
        RedisClient.create(TestClientResources.get(), (RedisURI) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNullWithUri() throws Exception {
        RedisClient.create(null, REDIS_URI);
    }
}
