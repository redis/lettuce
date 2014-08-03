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
}
