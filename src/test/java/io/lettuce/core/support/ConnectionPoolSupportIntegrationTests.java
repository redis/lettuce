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
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.StatefulRedisClusterConnectionImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnection;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.group.ChannelGroup;

/**
 * @author Mark Paluch
 */
class ConnectionPoolSupportIntegrationTests extends TestSupport {

    private static RedisClient client;

    private static Set<?> channels;

    @BeforeAll
    static void setupClient() {
        client = RedisClient.create(TestClientResources.create(), RedisURI.Builder.redis(host, port).build());
        client.setOptions(ClientOptions.create());
        channels = (ChannelGroup) ReflectionTestUtils.getField(client, "channels");
    }

    @AfterAll
    static void afterClass() {
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(client.getResources());
    }

    @Test
    void genericPoolShouldWorkWithWrappedConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig<>());

        borrowAndReturn(pool);
        borrowAndClose(pool);
        borrowAndCloseTryWithResources(pool);

        pool.returnObject(pool.borrowObject().sync().getStatefulConnection());
        pool.returnObject(pool.borrowObject().async().getStatefulConnection());

        assertThat(channels).hasSize(1);

        pool.close();

        Wait.untilTrue(channels::isEmpty).waitOrTimeout();

        assertThat(channels).isEmpty();
    }

    @Test
    void genericPoolShouldCloseConnectionsAboveMaxIdleSize() throws Exception {

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxIdle(2);

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), poolConfig);

        borrowAndReturn(pool);
        borrowAndClose(pool);
        borrowAndCloseTryWithResources(pool);

        StatefulRedisConnection<String, String> c1 = pool.borrowObject();
        StatefulRedisConnection<String, String> c2 = pool.borrowObject();
        StatefulRedisConnection<String, String> c3 = pool.borrowObject();

        assertThat(channels).hasSize(3);

        pool.returnObject(c1);
        pool.returnObject(c2);
        pool.returnObject(c3);

        assertThat(channels).hasSize(2);

        pool.close();

        Wait.untilTrue(channels::isEmpty).waitOrTimeout();

        assertThat(channels).isEmpty();
    }

    @Test
    void genericPoolShouldWorkWithPlainConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig<>(), false);

        borrowAndReturn(pool);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();
        pool.returnObject(connection);

        pool.close();
    }

    @Test
    void softReferencePoolShouldWorkWithPlainConnections() throws Exception {

        SoftReferenceObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createSoftReferenceObjectPool(() -> client.connect(), false);

        borrowAndReturn(pool);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();
        pool.returnObject(connection);

        connection.close();
        pool.close();
    }

    @Test
    void genericPoolUsingWrappingShouldPropagateExceptionsCorrectly() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig<>());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
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
    void wrappedConnectionShouldUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig<>());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisConnection.class)
                .isNotInstanceOf(StatefulRedisClusterConnectionImpl.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isTrue();

        assertThat(sync).isInstanceOf(RedisCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAsyncCommands.class).isNotInstanceOf(RedisAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisReactiveCommands.class)
                .isNotInstanceOf(RedisReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisConnection.class)
                .isNotInstanceOf(StatefulRedisConnectionImpl.class).isSameAs(connection);

        connection.close();
        pool.close();
    }

    @Test
    void wrappedMasterSlaveConnectionShouldUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisMasterSlaveConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> MasterSlave.connect(client, new StringCodec(), RedisURI.create(host, port)),
                        new GenericObjectPoolConfig<>());

        StatefulRedisMasterSlaveConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisMasterSlaveConnection.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isTrue();

        assertThat(sync).isInstanceOf(RedisCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAsyncCommands.class).isNotInstanceOf(RedisAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisReactiveCommands.class)
                .isNotInstanceOf(RedisReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisConnection.class)
                .isNotInstanceOf(StatefulRedisConnectionImpl.class).isSameAs(connection);

        connection.close();
        pool.close();
    }

    @Test
    void wrappedClusterConnectionShouldUseWrappers() throws Exception {

        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.create(TestSettings.host(), 7379));

        GenericObjectPool<StatefulRedisClusterConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(redisClusterClient::connect, new GenericObjectPoolConfig<>());

        StatefulRedisClusterConnection<String, String> connection = pool.borrowObject();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisClusterConnection.class)
                .isNotInstanceOf(StatefulRedisClusterConnectionImpl.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isTrue();

        assertThat(sync).isInstanceOf(RedisAdvancedClusterCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAdvancedClusterAsyncCommands.class)
                .isNotInstanceOf(RedisAdvancedClusterAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisAdvancedClusterReactiveCommands.class)
                .isNotInstanceOf(RedisAdvancedClusterReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisClusterConnection.class)
                .isNotInstanceOf(StatefulRedisClusterConnectionImpl.class).isSameAs(connection);

        connection.close();
        pool.close();

        FastShutdown.shutdown(redisClusterClient);
    }

    @Test
    void plainConnectionShouldNotUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig<>(), false);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisConnection.class)
                .isNotInstanceOf(StatefulRedisClusterConnectionImpl.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();

        assertThat(sync).isInstanceOf(RedisCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAsyncCommands.class).isInstanceOf(RedisAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisReactiveCommands.class)
                .isInstanceOf(RedisReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisConnection.class)
                .isInstanceOf(StatefulRedisConnectionImpl.class);

        pool.returnObject(connection);
        pool.close();
    }

    @Test
    void softRefPoolShouldWorkWithWrappedConnections() throws Exception {

        SoftReferenceObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createSoftReferenceObjectPool(() -> client.connect());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();

        assertThat(channels).hasSize(1);

        RedisCommands<String, String> sync = connection.sync();
        sync.ping();

        connection.close();
        pool.close();

        Wait.untilTrue(channels::isEmpty).waitOrTimeout();

        assertThat(channels).isEmpty();
    }

    @Test
    void wrappedObjectClosedAfterReturn() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig<>(), true);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
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
    void tryWithResourcesReturnsConnectionToPool() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig<>());

        StatefulRedisConnection<String, String> usedConnection = null;
        try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {

            RedisCommands<String, String> sync = connection.sync();
            sync.ping();

            usedConnection = connection;
        }

        try {
            usedConnection.isMulti();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("deallocated");
        }

        pool.close();
    }

    @Test
    void tryWithResourcesReturnsSoftRefConnectionToPool() throws Exception {

        SoftReferenceObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createSoftReferenceObjectPool(() -> client.connect());

        StatefulRedisConnection<String, String> usedConnection = null;
        try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {

            RedisCommands<String, String> sync = connection.sync();
            sync.ping();

            usedConnection = connection;
        }

        try {
            usedConnection.isMulti();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("deallocated");
        }

        pool.close();
    }

    private void borrowAndReturn(ObjectPool<StatefulRedisConnection<String, String>> pool) throws Exception {

        for (int i = 0; i < 10; i++) {
            StatefulRedisConnection<String, String> connection = pool.borrowObject();
            RedisCommands<String, String> sync = connection.sync();
            sync.ping();
            pool.returnObject(connection);
        }
    }

    private void borrowAndCloseTryWithResources(ObjectPool<StatefulRedisConnection<String, String>> pool) throws Exception {

        for (int i = 0; i < 10; i++) {
            try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {
                RedisCommands<String, String> sync = connection.sync();
                sync.ping();
            }
        }
    }

    private void borrowAndClose(ObjectPool<StatefulRedisConnection<String, String>> pool) throws Exception {

        for (int i = 0; i < 10; i++) {
            StatefulRedisConnection<String, String> connection = pool.borrowObject();
            RedisCommands<String, String> sync = connection.sync();
            sync.ping();
            connection.close();
        }
    }

}
