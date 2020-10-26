/*
 * Copyright 2017-2020 the original author or authors.
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
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.StatefulRedisClusterConnectionImpl;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.TestFutures;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.netty.channel.group.ChannelGroup;

/**
 * Integration tests for {@link BoundedAsyncPool}.
 *
 * @author Mark Paluch
 */
class AsyncConnectionPoolSupportIntegrationTests extends TestSupport {

    private static RedisClient client;
    private static Set<?> channels;
    private static RedisURI uri = RedisURI.Builder.redis(host, port).build();

    @BeforeAll
    static void setupClient() {

        client = RedisClient.create(TestClientResources.create(), uri);
        client.setOptions(ClientOptions.create());
        channels = (ChannelGroup) ReflectionTestUtils.getField(client, "channels");
    }

    @AfterAll
    static void afterClass() {
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(client.getResources());
    }

    @Test
    void asyncPoolShouldWorkWithWrappedConnections() {

        BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, uri), BoundedPoolConfig.create());

        borrowAndReturn(pool);
        borrowAndClose(pool);
        borrowAndCloseAsync(pool);

        TestFutures.awaitOrTimeout(pool.release(TestFutures.getOrTimeout(pool.acquire()).sync().getStatefulConnection()));
        TestFutures.awaitOrTimeout(pool.release(TestFutures.getOrTimeout(pool.acquire()).async().getStatefulConnection()));

        assertThat(channels).hasSize(1);

        pool.close();

        assertThat(channels).isEmpty();
    }

    @Test
    void asyncPoolWithAsyncCreationWorkWithWrappedConnections() {

        BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport
                .createBoundedObjectPoolAsync(() -> client.connectAsync(StringCodec.ASCII, uri), BoundedPoolConfig.create(),
                        true)
                .toCompletableFuture().join();

        borrowAndReturn(pool);
        borrowAndClose(pool);
        borrowAndCloseAsync(pool);

        TestFutures.awaitOrTimeout(pool.release(TestFutures.getOrTimeout(pool.acquire()).sync().getStatefulConnection()));
        TestFutures.awaitOrTimeout(pool.release(TestFutures.getOrTimeout(pool.acquire()).async().getStatefulConnection()));

        assertThat(channels).hasSize(1);

        pool.close();

        assertThat(channels).isEmpty();
    }

    @Test
    void asyncPoolShouldCloseConnectionsAboveMaxIdleSize() {

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxIdle(2);

        BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, uri), CommonsPool2ConfigConverter.bounded(poolConfig));

        borrowAndReturn(pool);
        borrowAndClose(pool);

        StatefulRedisConnection<String, String> c1 = TestFutures.getOrTimeout(pool.acquire());
        StatefulRedisConnection<String, String> c2 = TestFutures.getOrTimeout(pool.acquire());
        StatefulRedisConnection<String, String> c3 = TestFutures.getOrTimeout(pool.acquire());

        assertThat(channels).hasSize(3);

        CompletableFuture.allOf(pool.release(c1), pool.release(c2), pool.release(c3)).join();

        assertThat(channels).hasSize(2);

        pool.close();

        assertThat(channels).isEmpty();
    }

    @Test
    void asyncPoolShouldWorkWithPlainConnections() {

        AsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, uri), BoundedPoolConfig.create(), false);

        borrowAndReturn(pool);

        StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(pool.acquire());
        assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();
        pool.release(connection);

        pool.close();
    }

    @Test
    void asyncPoolUsingWrappingShouldPropagateExceptionsCorrectly() {

        AsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, uri), BoundedPoolConfig.create());

        StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(pool.acquire());
        RedisCommands<String, String> sync = connection.sync();
        sync.set(key, value);

        try {
            sync.hgetall(key);
            fail("Missing RedisCommandExecutionException");
        } catch (RedisCommandExecutionException e) {
            assertThat(e).hasMessageContaining("WRONGTYPE");
        }

        connection.close();
        pool.close();
    }

    @Test
    void wrappedConnectionShouldUseWrappers() {

        AsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, uri), BoundedPoolConfig.create());

        StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(pool.acquire());
        RedisCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisConnection.class).isNotInstanceOf(
                StatefulRedisClusterConnectionImpl.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isTrue();

        assertThat(sync).isInstanceOf(RedisCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAsyncCommands.class).isNotInstanceOf(RedisAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisReactiveCommands.class).isNotInstanceOf(
                RedisReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisConnection.class)
                .isNotInstanceOf(StatefulRedisConnectionImpl.class).isSameAs(connection);

        connection.close();
        pool.close();
    }

    @Test
    void wrappedObjectClosedAfterReturn() {

        AsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, uri), BoundedPoolConfig.create(), true);

        StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(pool.acquire());
        RedisCommands<String, String> sync = connection.sync();
        sync.ping();

        connection.close();

        try {
            connection.isMulti();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("deallocated");
        }

        pool.close();
    }

    @Test
    void shouldPropagateAsyncFlow() {

        AsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, uri), BoundedPoolConfig.create());

        CompletableFuture<String> pingResponse = pool.acquire().thenCompose(c -> {
            return c.async().ping().whenComplete((s, throwable) -> pool.release(c));
        });

        TestFutures.awaitOrTimeout(pingResponse);
        assertThat(pingResponse).isCompletedWithValue("PONG");

        pool.close();
    }

    private void borrowAndReturn(AsyncPool<StatefulRedisConnection<String, String>> pool) {

        for (int i = 0; i < 10; i++) {
            StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(pool.acquire());
            RedisCommands<String, String> sync = connection.sync();
            sync.ping();
            TestFutures.awaitOrTimeout(pool.release(connection));
        }
    }

    private void borrowAndClose(AsyncPool<StatefulRedisConnection<String, String>> pool) {

        for (int i = 0; i < 10; i++) {
            StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(pool.acquire());
            RedisCommands<String, String> sync = connection.sync();
            sync.ping();
            connection.close();
        }
    }

    private void borrowAndCloseAsync(AsyncPool<StatefulRedisConnection<String, String>> pool) {

        for (int i = 0; i < 10; i++) {
            StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(pool.acquire());
            RedisCommands<String, String> sync = connection.sync();
            sync.ping();
            TestFutures.getOrTimeout(connection.closeAsync());
        }
    }
}
