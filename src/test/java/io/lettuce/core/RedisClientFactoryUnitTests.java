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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
class RedisClientFactoryUnitTests {

    private static final String URI = "redis://" + TestSettings.host() + ":" + TestSettings.port();

    private static final RedisURI REDIS_URI = RedisURI.create(URI);

    @Test
    void plain() {
        FastShutdown.shutdown(RedisClient.create());
    }

    @Test
    void withStringUri() {
        FastShutdown.shutdown(RedisClient.create(URI));
    }

    @Test
    void withStringUriNull() {
        assertThatThrownBy(() -> RedisClient.create((String) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withUri() {
        FastShutdown.shutdown(RedisClient.create(REDIS_URI));
    }

    @Test
    void withUriNull() {
        assertThatThrownBy(() -> RedisClient.create((RedisURI) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResources() {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get()));
    }

    @Test
    void clientResourcesNull() {
        assertThatThrownBy(() -> RedisClient.create((ClientResources) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesWithStringUri() {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(), URI));
    }

    @Test
    void clientResourcesWithStringUriNull() {
        assertThatThrownBy(() -> RedisClient.create(TestClientResources.get(), (String) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesNullWithStringUri() {
        assertThatThrownBy(() -> RedisClient.create(null, URI)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesWithUri() {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(), REDIS_URI));
    }

    @Test
    void clientResourcesWithUriNull() {
        assertThatThrownBy(() -> RedisClient.create(TestClientResources.get(), (RedisURI) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesNullWithUri() {
        assertThatThrownBy(() -> RedisClient.create(null, REDIS_URI)).isInstanceOf(IllegalArgumentException.class);
    }

}
