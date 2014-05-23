package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import com.lambdaworks.redis.support.RedisClientFactoryBean;
import org.junit.Test;

import com.lambdaworks.redis.RedisURI;

public class RedisClientFactoryBeanTest {
    private RedisClientFactoryBean sut = new RedisClientFactoryBean();

    @Test
    public void testSimpleUri() throws Exception {
        String uri = "redis://localhost/2";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(2, redisURI.getDatabase());
        assertEquals("localhost", redisURI.getHost());
        assertEquals(RedisURI.DEFAULT_REDIS_PORT, redisURI.getPort());
        assertEquals("password", new String(redisURI.getPassword()));

    }

    @Test
    public void testSimpleUriWithoutDB() throws Exception {
        String uri = "redis://localhost/";

        sut.setUri(URI.create(uri));
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(0, redisURI.getDatabase());

    }

    @Test
    public void testSimpleUriWithoutDB2() throws Exception {
        String uri = "redis://localhost/";

        sut.setUri(URI.create(uri));
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(0, redisURI.getDatabase());

    }

    @Test
    public void testSimpleUriWithPort() throws Exception {
        String uri = "redis://localhost:1234/0";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(0, redisURI.getDatabase());
        assertEquals("localhost", redisURI.getHost());
        assertEquals(1234, redisURI.getPort());
        assertEquals("password", new String(redisURI.getPassword()));
    }

    @Test
    public void testSentinelUri() throws Exception {
        String uri = "redis-sentinel://localhost/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(1, redisURI.getDatabase());

        RedisURI sentinelUri = redisURI.getSentinels().get(0);
        assertEquals("localhost", sentinelUri.getHost());
        assertEquals(RedisURI.DEFAULT_SENTINEL_PORT, sentinelUri.getPort());
        assertEquals("password", new String(redisURI.getPassword()));
        assertEquals("myMaster", redisURI.getSentinelMasterId());
    }

    @Test
    public void testSentinelUriWithPort() throws Exception {
        String uri = "redis-sentinel://localhost:1234/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(1, redisURI.getDatabase());

        RedisURI sentinelUri = redisURI.getSentinels().get(0);
        assertEquals("localhost", sentinelUri.getHost());
        assertEquals(1234, sentinelUri.getPort());
        assertEquals("password", new String(redisURI.getPassword()));
        assertEquals("myMaster", redisURI.getSentinelMasterId());
    }

    @Test
    public void testMultipleSentinelUri() throws Exception {
        String uri = "redis-sentinel://localhost,localhost2,localhost3/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(1, redisURI.getDatabase());
        assertEquals(3, redisURI.getSentinels().size());

        RedisURI sentinelUri = redisURI.getSentinels().get(0);
        assertEquals("localhost", sentinelUri.getHost());
        assertEquals(RedisURI.DEFAULT_SENTINEL_PORT, sentinelUri.getPort());
        assertEquals("myMaster", redisURI.getSentinelMasterId());
    }

    @Test
    public void testMultipleSentinelUriWithPorts() throws Exception {
        String uri = "redis-sentinel://localhost,localhost2:1234,localhost3/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertEquals(1, redisURI.getDatabase());
        assertEquals(3, redisURI.getSentinels().size());

        RedisURI sentinelUri1 = redisURI.getSentinels().get(0);
        assertEquals("localhost", sentinelUri1.getHost());
        assertEquals(RedisURI.DEFAULT_SENTINEL_PORT, sentinelUri1.getPort());

        RedisURI sentinelUri2 = redisURI.getSentinels().get(1);
        assertEquals("localhost2", sentinelUri2.getHost());
        assertEquals(1234, sentinelUri2.getPort());
        assertEquals("myMaster", redisURI.getSentinelMasterId());
    }
}
