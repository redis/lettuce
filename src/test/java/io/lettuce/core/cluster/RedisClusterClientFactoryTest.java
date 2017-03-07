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
package io.lettuce.core.cluster;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.lettuce.TestClientResources;
import io.lettuce.core.FastShutdown;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSettings;
import io.lettuce.core.internal.LettuceLists;

/**
 * @author Mark Paluch
 */
public class RedisClusterClientFactoryTest {

    private final static String URI = "redis://" + TestSettings.host() + ":" + TestSettings.port();
    private final static RedisURI REDIS_URI = RedisURI.create(URI);
    private static final List<RedisURI> REDIS_URIS = LettuceLists.newList(REDIS_URI);

    @Test
    public void withStringUri() throws Exception {
        FastShutdown.shutdown(RedisClusterClient.create(TestClientResources.get(), URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withStringUriNull() throws Exception {
        RedisClusterClient.create((String) null);
    }

    @Test
    public void withUri() throws Exception {
        FastShutdown.shutdown(RedisClusterClient.create(REDIS_URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withUriUri() throws Exception {
        RedisClusterClient.create((RedisURI) null);
    }

    @Test
    public void withUriIterable() throws Exception {
        FastShutdown.shutdown(RedisClusterClient.create(LettuceLists.newList(REDIS_URI)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void withUriIterableNull() throws Exception {
        RedisClusterClient.create((Iterable<RedisURI>) null);
    }

    @Test
    public void clientResourcesWithStringUri() throws Exception {
        FastShutdown.shutdown(RedisClusterClient.create(TestClientResources.get(), URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithStringUriNull() throws Exception {
        RedisClusterClient.create(TestClientResources.get(), (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNullWithStringUri() throws Exception {
        RedisClusterClient.create(null, URI);
    }

    @Test
    public void clientResourcesWithUri() throws Exception {
        FastShutdown.shutdown(RedisClusterClient.create(TestClientResources.get(), REDIS_URI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithUriNull() throws Exception {
        RedisClusterClient.create(TestClientResources.get(), (RedisURI) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithUriUri() throws Exception {
        RedisClusterClient.create(null, REDIS_URI);
    }

    @Test
    public void clientResourcesWithUriIterable() throws Exception {
        FastShutdown.shutdown(RedisClusterClient.create(TestClientResources.get(), LettuceLists.newList(REDIS_URI)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesWithUriIterableNull() throws Exception {
        RedisClusterClient.create(TestClientResources.get(), (Iterable<RedisURI>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientResourcesNullWithUriIterable() throws Exception {
        RedisClusterClient.create(null, REDIS_URIS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientWithDifferentSslSettings() throws Exception {
        RedisClusterClient.create(Arrays.asList(RedisURI.create("redis://host1"), RedisURI.create("redis+ssl://host1")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientWithDifferentTlsSettings() throws Exception {
        RedisClusterClient.create(Arrays.asList(RedisURI.create("rediss://host1"), RedisURI.create("redis+tls://host1")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientWithDifferentVerifyPeerSettings() throws Exception {
        RedisURI redisURI = RedisURI.create("rediss://host1");
        redisURI.setVerifyPeer(false);

        RedisClusterClient.create(Arrays.asList(redisURI, RedisURI.create("rediss://host1")));
    }
}
