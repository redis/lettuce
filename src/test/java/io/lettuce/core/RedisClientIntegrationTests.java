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

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DefaultEventLoopGroupProvider;
import io.lettuce.test.Futures;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Mark Paluch
 */
class RedisClientIntegrationTests extends TestSupport {

    private final ClientResources clientResources = TestClientResources.get();

    @Test
    void shouldNotifyListener() {

        final TestConnectionListener listener = new TestConnectionListener();

        RedisClient client = RedisClient.create(clientResources, RedisURI.Builder.redis(host, port).build());

        client.addListener(listener);

        assertThat(listener.onConnected).isNull();
        assertThat(listener.onDisconnected).isNull();
        assertThat(listener.onException).isNull();

        StatefulRedisConnection<String, String> connection = client.connect();

        Wait.untilTrue(() -> listener.onConnected != null).waitOrTimeout();
        assertThat(listener.onConnectedSocketAddress).isNotNull();

        assertThat(listener.onConnected).isEqualTo(connection);
        assertThat(listener.onDisconnected).isNull();

        connection.sync().set(key, value);
        connection.close();

        Wait.untilTrue(() -> listener.onDisconnected != null).waitOrTimeout();

        assertThat(listener.onConnected).isEqualTo(connection);
        assertThat(listener.onDisconnected).isEqualTo(connection);

        FastShutdown.shutdown(client);
    }

    @Test
    void shouldNotNotifyListenerAfterRemoval() {

        final TestConnectionListener removedListener = new TestConnectionListener();
        final TestConnectionListener retainedListener = new TestConnectionListener();

        RedisClient client = RedisClient.create(clientResources, RedisURI.Builder.redis(host, port).build());
        client.addListener(removedListener);
        client.addListener(retainedListener);
        client.removeListener(removedListener);

        // that's the sut call
        client.connect().close();

        Wait.untilTrue(() -> retainedListener.onConnected != null).waitOrTimeout();

        assertThat(retainedListener.onConnected).isNotNull();

        assertThat(removedListener.onConnected).isNull();
        assertThat(removedListener.onConnectedSocketAddress).isNull();
        assertThat(removedListener.onDisconnected).isNull();
        assertThat(removedListener.onException).isNull();

        FastShutdown.shutdown(client);
    }

    @Test
    void reuseClientConnections() throws Exception {

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

        Futures.await(clientResources.shutdown(0, 0, TimeUnit.MILLISECONDS));

        assertThat(eventLoopGroups).isEmpty();
        assertThat(executor.isShuttingDown()).isTrue();
        assertThat(clientResources.eventExecutorGroup().isShuttingDown()).isTrue();
    }

    @Test
    void reuseClientConnectionsShutdownTwoClients() throws Exception {

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
        Futures.await(clientResources.shutdown(0, 0, TimeUnit.MILLISECONDS));
        assertThat(clientResources.eventExecutorGroup().isShuttingDown()).isTrue();
    }

    @Test
    void managedClientResources() throws Exception {

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

    private class TestConnectionListener implements RedisConnectionStateListener {

        volatile SocketAddress onConnectedSocketAddress;

        volatile RedisChannelHandler<?, ?> onConnected;

        volatile RedisChannelHandler<?, ?> onDisconnected;

        volatile RedisChannelHandler<?, ?> onException;

        @Override
        public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
            onConnected = connection;
            onConnectedSocketAddress = socketAddress;
        }

        @Override
        public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
            onDisconnected = connection;
        }

        @Override
        public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
            onException = connection;
        }

    }

}
