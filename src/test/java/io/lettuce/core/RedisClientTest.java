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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DefaultEventLoopGroupProvider;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Mark Paluch
 */
public class RedisClientTest {

    @Test
    public void reuseClientConnections() throws Exception {

        // given
        DefaultClientResources clientResources = DefaultClientResources.create();
        Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> eventLoopGroups = getExecutors(clientResources);

        RedisClient redisClient1 = newClient(clientResources);
        RedisClient redisClient2 = newClient(clientResources);
        connectAndClose(redisClient1);
        connectAndClose(redisClient2);

        // when
        EventExecutorGroup executor = eventLoopGroups.values().iterator().next();
        redisClient1.shutdown(0, 0, TimeUnit.MILLISECONDS);

        // then
        connectAndClose(redisClient2);

        clientResources.shutdown(0, 0, TimeUnit.MILLISECONDS).get();

        assertThat(eventLoopGroups).isEmpty();
        assertThat(executor.isShuttingDown()).isTrue();
        assertThat(clientResources.eventExecutorGroup().isShuttingDown()).isTrue();
    }

    @Test
    public void reuseClientConnectionsShutdownTwoClients() throws Exception {

        // given
        DefaultClientResources clientResources = DefaultClientResources.create();
        Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> eventLoopGroups = getExecutors(clientResources);

        RedisClient redisClient1 = newClient(clientResources);
        RedisClient redisClient2 = newClient(clientResources);
        connectAndClose(redisClient1);
        connectAndClose(redisClient2);

        // when
        EventExecutorGroup executor = eventLoopGroups.values().iterator().next();

        redisClient1.shutdown(0, 0, TimeUnit.MILLISECONDS);
        assertThat(executor.isShutdown()).isFalse();
        connectAndClose(redisClient2);
        redisClient2.shutdown(0, 0, TimeUnit.MILLISECONDS);

        // then
        assertThat(eventLoopGroups).isEmpty();
        assertThat(executor.isShutdown()).isTrue();
        assertThat(clientResources.eventExecutorGroup().isShuttingDown()).isFalse();

        // cleanup
        clientResources.shutdown(0, 0, TimeUnit.MILLISECONDS).get();
        assertThat(clientResources.eventExecutorGroup().isShuttingDown()).isTrue();
    }

    @Test
    public void managedClientResources() throws Exception {

        // given
        RedisClient redisClient1 = RedisClient.create(RedisURI.create(TestSettings.host(), TestSettings.port()));
        ClientResources clientResources = redisClient1.getResources();
        Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> eventLoopGroups = getExecutors(clientResources);
        connectAndClose(redisClient1);

        // when
        EventExecutorGroup executor = eventLoopGroups.values().iterator().next();

        redisClient1.shutdown(0, 0, TimeUnit.MILLISECONDS);

        // then
        assertThat(eventLoopGroups).isEmpty();
        assertThat(executor.isShuttingDown()).isTrue();
        assertThat(clientResources.eventExecutorGroup().isShuttingDown()).isTrue();
    }

    private void connectAndClose(RedisClient client) {
        client.connect().close();
    }

    private RedisClient newClient(DefaultClientResources clientResources) {
        return RedisClient.create(clientResources, RedisURI.create(TestSettings.host(), TestSettings.port()));
    }

    private Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> getExecutors(ClientResources clientResources)
            throws Exception {
        Field eventLoopGroupsField = DefaultEventLoopGroupProvider.class.getDeclaredField("eventLoopGroups");
        eventLoopGroupsField.setAccessible(true);
        return (Map) eventLoopGroupsField.get(clientResources.eventLoopGroupProvider());
    }
}
