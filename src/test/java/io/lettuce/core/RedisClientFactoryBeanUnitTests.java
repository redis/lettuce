/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.support.RedisClientFactoryBean;
import io.lettuce.test.resource.FastShutdown;

/**
 * @author Mark Paluch
 */
class RedisClientFactoryBeanUnitTests {

    private RedisClientFactoryBean sut = new RedisClientFactoryBean();

    @AfterEach
    void tearDown() throws Exception {
        FastShutdown.shutdown(sut.getObject());
        sut.destroy();
    }

    @Test
    void testSimpleUri() throws Exception {
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
    void testSimpleUriWithoutDB() throws Exception {
        String uri = "redis://localhost/";

        sut.setUri(URI.create(uri));
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(0);
    }

    @Test
    void testSimpleUriWithoutDB2() throws Exception {
        String uri = "redis://localhost/";

        sut.setUri(URI.create(uri));
        sut.afterPropertiesSet();

        RedisURI redisURI = sut.getRedisURI();

        assertThat(redisURI.getDatabase()).isEqualTo(0);
    }

    @Test
    void testSimpleUriWithPort() throws Exception {
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
    void testSentinelUri() throws Exception {
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
    void testSentinelUriWithPort() throws Exception {
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
    void testMultipleSentinelUri() throws Exception {
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
    void testMultipleSentinelUriWithPorts() throws Exception {
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
