/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class RedisURIBuilderTest {

    @Test
    public void sentinel() throws Exception {
        RedisURI result = RedisURI.Builder.sentinel("localhost").withTimeout(2, TimeUnit.HOURS).build();
        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getTimeout()).isEqualTo(2);
        assertThat(result.getUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test(expected = IllegalStateException.class)
    public void sentinelWithHostShouldFail() throws Exception {
        RedisURI.builder().sentinel("localhost").withHost("localhost");
    }

    @Test
    public void sentinelWithPort() throws Exception {
        RedisURI result = RedisURI.Builder.sentinel("localhost", 1).withTimeout(2, TimeUnit.HOURS).build();
        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getTimeout()).isEqualTo(2);
        assertThat(result.getUnit()).isEqualTo(TimeUnit.HOURS);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailIfBuilderIsEmpty() throws Exception {
        RedisURI.builder().build();
    }

    @Test
    public void redisWithHostAndPort() throws Exception {
        RedisURI result = RedisURI.builder().withHost("localhost").withPort(1234).build();

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(1234);
    }

    @Test
    public void redisWithPort() throws Exception {
        RedisURI result = RedisURI.Builder.redis("localhost").withPort(1234).build();

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(1234);
    }

    @Test
    public void redisWithClientName() throws Exception {
        RedisURI result = RedisURI.Builder.redis("localhost").withClientName("hello").build();

        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getClientName()).isEqualTo("hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void redisHostAndPortWithInvalidPort() throws Exception {
        RedisURI.Builder.redis("localhost", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void redisWithInvalidPort() throws Exception {
        RedisURI.Builder.redis("localhost").withPort(65536);
    }

    @Test
    public void redisFromUrl() throws Exception {
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS + "://password@localhost/21");

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(result.getPassword()).isEqualTo("password".toCharArray());
        assertThat(result.getDatabase()).isEqualTo(21);
        assertThat(result.isSsl()).isFalse();
    }

    @Test
    public void redisFromUrlNoPassword() throws Exception {
        RedisURI redisURI = RedisURI.create("redis://localhost:1234/5");
        assertThat(redisURI.getPassword()).isNull();

        redisURI = RedisURI.create("redis://h:@localhost.com:14589");
        assertThat(redisURI.getPassword()).isNull();
    }

    @Test
    public void redisFromUrlPassword() throws Exception {
        RedisURI redisURI = RedisURI.create("redis://h:password@localhost.com:14589");
        assertThat(redisURI.getPassword()).isEqualTo("password".toCharArray());
    }

    @Test
    public void redisWithSSL() throws Exception {
        RedisURI result = RedisURI.Builder.redis("localhost").withSsl(true).withStartTls(true).build();

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.isSsl()).isTrue();
        assertThat(result.isStartTls()).isTrue();
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
        assertThat(result.toString()).contains("master");

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

    @Test(expected = IllegalArgumentException.class)
    public void redisSentinelWithInvalidPort() throws Exception {
        RedisURI.Builder.sentinel("a", 65536);
    }

    @Test(expected = IllegalArgumentException.class)
    public void redisSentinelWithMasterIdAndInvalidPort() throws Exception {
        RedisURI.Builder.sentinel("a", 65536, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void redisSentinelWithNullMasterId() throws Exception {
        RedisURI.Builder.sentinel("a", 1, null);
    }

    @Test(expected = IllegalStateException.class)
    public void redisSentinelWithSSLNotPossible() throws Exception {
        RedisURI.Builder.sentinel("a", 1, "master").withSsl(true);
    }

    @Test(expected = IllegalStateException.class)
    public void redisSentinelWithTLSNotPossible() throws Exception {
        RedisURI.Builder.sentinel("a", 1, "master").withStartTls(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidScheme() throws Exception {
        RedisURI.create("http://www.web.de");
    }

    @Test
    public void redisSocket() throws Exception {
        File file = new File("work/socket-6479").getCanonicalFile();
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());

        assertThat(result.getSocket()).isEqualTo(file.getCanonicalPath());
        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getPassword()).isNull();
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(0);
        assertThat(result.isSsl()).isFalse();
    }

    @Test
    public void redisSocketWithPassword() throws Exception {
        File file = new File("work/socket-6479").getCanonicalFile();
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://password@" + file.getCanonicalPath());

        assertThat(result.getSocket()).isEqualTo(file.getCanonicalPath());
        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getPassword()).isEqualTo("password".toCharArray());
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(0);
        assertThat(result.isSsl()).isFalse();
    }

}
