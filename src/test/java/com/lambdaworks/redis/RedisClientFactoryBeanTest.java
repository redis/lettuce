package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.support.RedisClientFactoryBean;

public class RedisClientFactoryBeanTest {
    private RedisClientFactoryBean sut = new RedisClientFactoryBean();

    @After
    public void tearDown() throws Exception {
        FastShutdown.shutdown(sut.getObject());
        sut.destroy();
    }

    @Test
    public void testSimpleUri() throws Exception {
        String uri = "redis://localhost/2";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(2);
        assertThat(redisURI.getHost()).isEqualTo("localhost");
        assertThat(redisURI.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(new String(redisURI.getPassword())).isEqualTo("password");
    }

    @Test
    public void testSimpleUriWithoutDB() throws Exception {
        String uri = "redis://localhost/";

        sut.setUri(URI.create(uri));
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(0);
    }

    @Test
    public void testSimpleUriWithoutDB2() throws Exception {
        String uri = "redis://localhost/";

        sut.setUri(URI.create(uri));
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(0);
    }

    @Test
    public void testSimpleUriWithPort() throws Exception {
        String uri = "redis://localhost:1234/0";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(0);
        assertThat(redisURI.getHost()).isEqualTo("localhost");
        assertThat(redisURI.getPort()).isEqualTo(1234);
        assertThat(new String(redisURI.getPassword())).isEqualTo("password");
    }

    @Test
    public void testSentinelUri() throws Exception {
        String uri = "redis-sentinel://localhost/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(1);

        RedisURI sentinelUri = redisURI.getSentinels().get(0);
        assertThat(sentinelUri.getHost()).isEqualTo("localhost");
        assertThat(sentinelUri.getPort()).isEqualTo(RedisURI.DEFAULT_SENTINEL_PORT);
        assertThat(new String(redisURI.getPassword())).isEqualTo("password");
        assertThat(redisURI.getSentinelMasterId()).isEqualTo("myMaster");
    }

    @Test
    public void testSentinelUriWithPort() throws Exception {
        String uri = "redis-sentinel://localhost:1234/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(1);

        RedisURI sentinelUri = redisURI.getSentinels().get(0);
        assertThat(sentinelUri.getHost()).isEqualTo("localhost");
        assertThat(sentinelUri.getPort()).isEqualTo(1234);
        assertThat(new String(redisURI.getPassword())).isEqualTo("password");
        assertThat(redisURI.getSentinelMasterId()).isEqualTo("myMaster");
    }

    @Test
    public void testMultipleSentinelUri() throws Exception {
        String uri = "redis-sentinel://localhost,localhost2,localhost3/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(1);
        assertThat(redisURI.getSentinels()).hasSize(3);

        RedisURI sentinelUri = redisURI.getSentinels().get(0);
        assertThat(sentinelUri.getHost()).isEqualTo("localhost");
        assertThat(sentinelUri.getPort()).isEqualTo(RedisURI.DEFAULT_SENTINEL_PORT);
        assertThat(redisURI.getSentinelMasterId()).isEqualTo("myMaster");
    }

    @Test
    public void testMultipleSentinelUriWithPorts() throws Exception {
        String uri = "redis-sentinel://localhost,localhost2:1234,localhost3/1#myMaster";

        sut.setUri(URI.create(uri));
        sut.setPassword("password");
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(1);
        assertThat(redisURI.getSentinels()).hasSize(3);

        RedisURI sentinelUri1 = redisURI.getSentinels().get(0);
        assertThat(sentinelUri1.getHost()).isEqualTo("localhost");
        assertThat(sentinelUri1.getPort()).isEqualTo(RedisURI.DEFAULT_SENTINEL_PORT);

        RedisURI sentinelUri2 = redisURI.getSentinels().get(1);
        assertThat(sentinelUri2.getHost()).isEqualTo("localhost2");
        assertThat(sentinelUri2.getPort()).isEqualTo(1234);
        assertThat(redisURI.getSentinelMasterId()).isEqualTo("myMaster");
    }
}
