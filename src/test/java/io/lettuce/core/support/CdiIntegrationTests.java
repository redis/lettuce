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
import static org.mockito.Mockito.mock;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Mark Paluch
 * @since 3.0
 */
class CdiIntegrationTests {

    private static SeContainer container;

    @BeforeAll
    static void setUp() {

        container = SeContainerInitializer.newInstance() //
                .disableDiscovery() //
                .addPackages(CdiIntegrationTests.class) //
                .initialize();
    }

    @AfterAll
    static void afterClass() {
        container.close();
    }

    @Produces
    RedisURI redisURI() {
        return RedisURI.Builder.redis(AbstractRedisClientTest.host, AbstractRedisClientTest.port).build();
    }

    @Produces
    ClientResources clientResources() {
        return TestClientResources.get();
    }

    @Produces
    @PersonDB
    ClientResources personClientResources() {
        return DefaultClientResources.create();
    }

    @PersonDB
    @Produces
    RedisURI redisURIQualified() {
        return RedisURI.Builder.redis(AbstractRedisClientTest.host, AbstractRedisClientTest.port + 1).build();
    }

    @Test
    void testInjection() {

        InjectedClient injectedClient = container.select(InjectedClient.class).get();
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

}
