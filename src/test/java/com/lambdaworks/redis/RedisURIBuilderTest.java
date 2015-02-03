package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class RedisURIBuilderTest {

    @Test
    public void sentinel() throws Exception {
        RedisURI result = RedisURI.Builder.sentinel("localhost").withTimeout(2, TimeUnit.HOURS).build();
        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getTimeout()).isEqualTo(2);
        assertThat(result.getUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    public void sentinelWithPort() throws Exception {
        RedisURI result = RedisURI.Builder.sentinel("localhost", 1).withTimeout(2, TimeUnit.HOURS).build();
        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getTimeout()).isEqualTo(2);
        assertThat(result.getUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    public void redisWithPort() throws Exception {
        RedisURI result = RedisURI.Builder.redis("localhost").withPort(1234).build();

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(1234);
    }

    @Test
    public void redisFromUrl() throws Exception {
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS + "://password@localhost/1");

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(result.getPassword()).isEqualTo("password".toCharArray());
        assertThat(result.isSsl()).isFalse();
    }

    @Test
    public void redisSslFromUrl() throws Exception {
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SECURE + "://:password@localhost/1");

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(result.getPassword()).isEqualTo("password".toCharArray());
        assertThat(result.isSsl()).isTrue();
    }

    @Test
    public void redisSentinelFromUrl() throws Exception {
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SENTINEL + "://password@localhost/1#master");

        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(0);
        assertThat(result.getPassword()).isEqualTo("password".toCharArray());
        assertThat(result.getSentinelMasterId()).isEqualTo("master");

        result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SENTINEL + "://password@host1:1,host2:3423,host3/1#master");

        assertThat(result.getSentinels()).hasSize(3);
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(0);
        assertThat(result.getPassword()).isEqualTo("password".toCharArray());
        assertThat(result.getSentinelMasterId()).isEqualTo("master");

        RedisURI sentinel1 = result.getSentinels().get(0);
        assertThat(sentinel1.getPort()).isEqualTo(1);
        assertThat(sentinel1.getHost()).isEqualTo("host1");

        RedisURI sentinel2 = result.getSentinels().get(1);
        assertThat(sentinel2.getPort()).isEqualTo(3423);
        assertThat(sentinel2.getHost()).isEqualTo("host2");

        RedisURI sentinel3 = result.getSentinels().get(2);
        assertThat(sentinel3.getPort()).isEqualTo(RedisURI.DEFAULT_SENTINEL_PORT);
        assertThat(sentinel3.getHost()).isEqualTo("host3");

    }
}
