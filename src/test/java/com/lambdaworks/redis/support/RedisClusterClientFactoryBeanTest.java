package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;

import org.junit.Test;

import com.lambdaworks.redis.RedisURI;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisClusterClientFactoryBeanTest {

    private RedisClusterClientFactoryBean sut = new RedisClusterClientFactoryBean();

    @Test(expected = IllegalArgumentException.class)
    public void invalidUri() throws Exception {

        sut.setUri(URI.create("http://www.web.de"));
        sut.afterPropertiesSet();
    }

    @Test(expected = IllegalArgumentException.class)
    public void sentinelUri() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS_SENTINEL + "://www.web.de"));
        sut.afterPropertiesSet();
    }

    @Test
    public void validUri() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS + "://password@host"));
        sut.afterPropertiesSet();
        assertThat(sut.getRedisURI().getHost()).isEqualTo("host");
        assertThat(sut.getRedisURI().getPassword()).isEqualTo("password".toCharArray());
    }

    @Test
    public void validUriPasswordOverride() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS + "://password@host"));
        sut.setPassword("thepassword");

        sut.afterPropertiesSet();
        assertThat(sut.getRedisURI().getHost()).isEqualTo("host");
        assertThat(sut.getRedisURI().getPassword()).isEqualTo("thepassword".toCharArray());
    }
}