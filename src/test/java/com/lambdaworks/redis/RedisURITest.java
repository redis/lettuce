package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisURITest {

    @Test
    public void equalsTest() throws Exception {

        RedisURI redisURI1 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI2 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI3 = RedisURI.create("redis://auth@localhost:1231/5");

        assertThat(redisURI1).isEqualTo(redisURI2);
        assertThat(redisURI1.hashCode()).isEqualTo(redisURI2.hashCode());
        assertThat(redisURI1.toString()).contains("localhost").contains("1234");

        assertThat(redisURI3).isNotEqualTo(redisURI2);
        assertThat(redisURI3.hashCode()).isNotEqualTo(redisURI2.hashCode());
    }

    @Test
    public void setUsage() throws Exception {

        RedisURI redisURI1 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI2 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI3 = RedisURI.create("redis://auth@localhost:1234/6");

        Set<RedisURI> set = ImmutableSet.of(redisURI1, redisURI2, redisURI3);

        assertThat(set).hasSize(2);
    }

    @Test
    public void mapUsage() throws Exception {

        RedisURI redisURI1 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI2 = RedisURI.create("redis://auth@localhost:1234/5");

        Map<RedisURI, String> map = new HashMap<>();
        map.put(redisURI1, "something");

        assertThat(map.get(redisURI2)).isEqualTo("something");

    }

    @Test
    public void sentinelEqualsTest() throws Exception {

        RedisURI redisURI1 = RedisURI.create("redis-sentinel://auth@h1,h2,h3:1234/5#masterId");
        RedisURI redisURI2 = RedisURI.create("redis-sentinel://auth@h1,h2,h3:1234/5#masterId");
        RedisURI redisURI3 = RedisURI.create("redis-sentinel://auth@h1,h2,h3:1234/5#OtherMasterId");

        assertThat(redisURI1).isEqualTo(redisURI2);
        assertThat(redisURI1.hashCode()).isEqualTo(redisURI2.hashCode());
        assertThat(redisURI1.toString()).contains("h1");

        assertThat(redisURI3).isNotEqualTo(redisURI2);
        assertThat(redisURI3.hashCode()).isNotEqualTo(redisURI2.hashCode());

    }

    @Test
    public void socketEqualsTest() throws Exception {

        RedisURI redisURI1 = RedisURI.create("redis-socket:///var/tmp/socket");
        RedisURI redisURI2 = RedisURI.create("redis-socket:///var/tmp/socket");
        RedisURI redisURI3 = RedisURI.create("redis-socket:///var/tmp/other-socket");

        assertThat(redisURI1).isEqualTo(redisURI2);
        assertThat(redisURI1.hashCode()).isEqualTo(redisURI2.hashCode());
        assertThat(redisURI1.toString()).contains("/var/tmp/socket");

        assertThat(redisURI3).isNotEqualTo(redisURI2);
        assertThat(redisURI3.hashCode()).isNotEqualTo(redisURI2.hashCode());
    }
}
