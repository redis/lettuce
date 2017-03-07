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
package io.lettuce.core.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.FastShutdown;
import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class CdiTest {

    static CdiTestContainer container;

    @BeforeClass
    public static void setUp() throws Exception {

        container = CdiTestContainerLoader.getCdiContainer();
        container.bootContainer();
        container.startApplicationScope();
    }

    @Produces
    public RedisURI redisURI() {
        return RedisURI.Builder.redis(AbstractRedisClientTest.host, AbstractRedisClientTest.port).build();
    }

    @Produces
    @PersonDB
    public ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    public void shutdownClientResources(@Disposes ClientResources clientResources) throws Exception {
        FastShutdown.shutdown(clientResources);
    }

    @PersonDB
    @Produces
    public RedisURI redisURIQualified() {
        return RedisURI.Builder.redis(AbstractRedisClientTest.host, AbstractRedisClientTest.port + 1).build();
    }

    @Test
    public void testInjection() {

        InjectedClient injectedClient = container.getInstance(InjectedClient.class);
        assertThat(injectedClient.redisClient).isNotNull();
        assertThat(injectedClient.redisClusterClient).isNotNull();

        assertThat(injectedClient.qualifiedRedisClient).isNotNull();
        assertThat(injectedClient.qualifiedRedisClusterClient).isNotNull();

        RedisConnectionStateListener mock = mock(RedisConnectionStateListener.class);

        // do some interaction to force the container a creation of the repositories.
        injectedClient.redisClient.addListener(mock);
        injectedClient.redisClusterClient.addListener(mock);

        injectedClient.qualifiedRedisClient.addListener(mock);
        injectedClient.qualifiedRedisClusterClient.addListener(mock);

        injectedClient.pingRedis();
    }

    @AfterClass
    public static void afterClass() throws Exception {

        container.stopApplicationScope();
        container.shutdownContainer();

    }

}
