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

import io.lettuce.TestClientResources;
import org.junit.Test;

import io.lettuce.core.resource.ClientResources;

/**
 * @author Mark Paluch
 */
public class RedisClientFactoryTest {

    private final static String URI = "redis://" + TestSettings.host() + ":" + TestSettings.port();
    private final static RedisURI REDIS_URI = RedisURI.create(URI);

    @Test
    public void plain() throws Exception {
        FastShutdown.shutdown(RedisClient.create());
    }

    @Test
    public void withStringUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withStringUriNull() throws Exception {
        RedisClient.create((String) null);
    }

    @Test
    public void withUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(REDIS_URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withUriNull() throws Exception {
        RedisClient.create((RedisURI) null);
    }

    @Test
    public void clientResources() throws Exception {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNull() throws Exception {
        RedisClient.create((ClientResources) null);
    }

    @Test
    public void clientResourcesWithStringUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(), URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithStringUriNull() throws Exception {
        RedisClient.create(TestClientResources.get(), (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNullWithStringUri() throws Exception {
        RedisClient.create(null, URI);
    }

    @Test
    public void clientResourcesWithUri() throws Exception {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(),  REDIS_URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithUriNull() throws Exception {
        RedisClient.create(TestClientResources.get(), (RedisURI) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNullWithUri() throws Exception {
        RedisClient.create(null, REDIS_URI);
    }
}
