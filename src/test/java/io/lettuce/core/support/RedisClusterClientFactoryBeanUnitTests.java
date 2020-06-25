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
package io.lettuce.core.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;

/**
 * @author Mark Paluch
 */
class RedisClusterClientFactoryBeanUnitTests {

    private RedisClusterClientFactoryBean sut = new RedisClusterClientFactoryBean();

    @Test
    void invalidUri() {

        sut.setUri(URI.create("http://www.web.de"));
        assertThatThrownBy(() -> sut.afterPropertiesSet()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sentinelUri() {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS_SENTINEL + "://www.web.de"));
        assertThatThrownBy(() -> sut.afterPropertiesSet()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validUri() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS + "://password@host"));
        sut.afterPropertiesSet();
        assertThat(getRedisURI().getHost()).isEqualTo("host");
        assertThat(getRedisURI().getPassword()).isEqualTo("password".toCharArray());

        sut.destroy();
    }

    @Test
    void validUriPasswordOverride() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS + "://password@host"));
        sut.setPassword("thepassword");

        sut.afterPropertiesSet();
        assertThat(getRedisURI().getHost()).isEqualTo("host");
        assertThat(getRedisURI().getPassword()).isEqualTo("thepassword".toCharArray());

        sut.destroy();
    }

    @Test
    void multiNodeUri() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS + "://password@host1,host2"));
        sut.afterPropertiesSet();

        Collection<RedisURI> redisUris = sut.getRedisURIs();
        assertThat(redisUris).hasSize(2);

        Iterator<RedisURI> iterator = redisUris.iterator();
        RedisURI host1 = iterator.next();
        RedisURI host2 = iterator.next();

        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPassword()).isEqualTo("password".toCharArray());

        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPassword()).isEqualTo("password".toCharArray());

        sut.destroy();
    }

    @Test
    void multiNodeUriPasswordOverride() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS + "://password@host1,host2"));
        sut.setPassword("thepassword");

        sut.afterPropertiesSet();

        Collection<RedisURI> redisUris = sut.getRedisURIs();
        assertThat(redisUris).hasSize(2);

        Iterator<RedisURI> iterator = redisUris.iterator();
        RedisURI host1 = iterator.next();
        RedisURI host2 = iterator.next();

        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPassword()).isEqualTo("thepassword".toCharArray());

        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPassword()).isEqualTo("thepassword".toCharArray());

        sut.destroy();
    }

    @Test
    void supportsSsl() throws Exception {

        sut.setUri(URI.create(RedisURI.URI_SCHEME_REDIS_SECURE + "://password@host"));
        sut.afterPropertiesSet();

        assertThat(getRedisURI().getHost()).isEqualTo("host");
        assertThat(getRedisURI().getPassword()).isEqualTo("password".toCharArray());
        assertThat(getRedisURI().isVerifyPeer()).isFalse();
        assertThat(getRedisURI().isSsl()).isTrue();

        sut.destroy();
    }

    private RedisURI getRedisURI() {
        return sut.getRedisURIs().iterator().next();
    }

}
