/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;

/**
 * @author Mark Paluch
 */
class RedisClusterURIUtilUnitTests {

    @Test
    void testSimpleUri() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis://host:7479"));

        assertThat(redisURIs).hasSize(1);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.getHost()).isEqualTo("host");
        assertThat(host1.getPort()).isEqualTo(7479);
    }

    @Test
    void testMultipleHosts() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis://host1,host2"));

        assertThat(redisURIs).hasSize(2);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);

        RedisURI host2 = redisURIs.get(1);
        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPort()).isEqualTo(6379);
    }

    @Test
    void testMultipleHostsWithPorts() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis://host1:6379,host2:6380"));

        assertThat(redisURIs).hasSize(2);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);

        RedisURI host2 = redisURIs.get(1);
        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPort()).isEqualTo(6380);
    }

    @Test
    void testSslWithPasswordSingleHost() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis+tls://password@host1"));

        assertThat(redisURIs).hasSize(1);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.isSsl()).isTrue();
        assertThat(host1.isStartTls()).isTrue();
        assertThat(new String(host1.getPassword())).isEqualTo("password");
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);
    }

    @Test
    void testSslWithPasswordMultipleHosts() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis+tls://password@host1:6379,host2:6380"));

        assertThat(redisURIs).hasSize(2);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.isSsl()).isTrue();
        assertThat(host1.isStartTls()).isTrue();
        assertThat(new String(host1.getPassword())).isEqualTo("password");
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);

        RedisURI host2 = redisURIs.get(1);
        assertThat(host2.isSsl()).isTrue();
        assertThat(host2.isStartTls()).isTrue();
        assertThat(new String(host2.getPassword())).isEqualTo("password");
        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPort()).isEqualTo(6380);
    }

}
