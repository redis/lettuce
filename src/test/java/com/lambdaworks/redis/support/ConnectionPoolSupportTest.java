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
package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.StatefulRedisClusterConnectionImpl;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.masterslave.MasterSlave;
import com.lambdaworks.redis.masterslave.StatefulRedisMasterSlaveConnection;

import io.netty.channel.group.ChannelGroup;

/**
 * @author Mark Paluch
 */
public class ConnectionPoolSupportTest extends AbstractTest {

    private static RedisClient client;
    private static Set<?> channels;

    @BeforeClass
    public static void setupClient() {
        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port).build());
        client.setOptions(ClientOptions.create());
        channels = (ChannelGroup) ReflectionTestUtils.getField(client, "channels");
    }

    @AfterClass
    public static void afterClass() {
        FastShutdown.shutdown(client);
    }

    @Test
    public void genericPoolShouldWorkWithWrappedConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), new GenericObjectPoolConfig());

        borrowAndReturn(pool);
        borrowAndClose(pool);
        borrowAndCloseTryWithResources(pool);

        pool.returnObject(pool.borrowObject().sync().getStatefulConnection());
        pool.returnObject(pool.borrowObject().async().getStatefulConnection());

        assertThat(channels).hasSize(1);

        pool.close();

        assertThat(channels).isEmpty();
    }

    @Test
    public void genericPoolShouldCloseConnectionsAboveMaxIdleSize() throws Exception {

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxIdle(2);

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), poolConfig);

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

        assertThat(channels).isEmpty();
    }

    @Test
    public void genericPoolShouldWorkWithPlainConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), new GenericObjectPoolConfig(), false);

        borrowAndReturn(pool);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();
        pool.returnObject(connection);

        pool.close();
    }

    @Test
    public void softReferencePoolShouldWorkWithPlainConnections() throws Exception {

        SoftReferenceObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createSoftReferenceObjectPool(() -> client.connect(), false);

        borrowAndReturn(pool);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();
        pool.returnObject(connection);

        pool.close();
    }

    @Test
    public void genericPoolUsingWrappingShouldPropagateExceptionsCorrectly() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), new GenericObjectPoolConfig());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();
        sync.set(key, value);

        try {
            sync.hgetall(key);
            fail("Missing RedisCommandExecutionException");
        } catch (RedisCommandExecutionException e) {
            assertThat(e).hasMessageContaining("WRONGTYPE");
        }

        sync.close();
        pool.close();
    }

    @Test
    public void wrappedConnectionShouldUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), new GenericObjectPoolConfig());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
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

        sync.close();
        pool.close();
    }

    @Test
    public void wrappedMasterSlaveConnectionShouldUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisMasterSlaveConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> MasterSlave.connect(client, new StringCodec(), RedisURI.create(host, port)),
                        new GenericObjectPoolConfig());

        StatefulRedisMasterSlaveConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisMasterSlaveConnection.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isTrue();

        assertThat(sync).isInstanceOf(RedisCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAsyncCommands.class).isNotInstanceOf(RedisAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisReactiveCommands.class).isNotInstanceOf(
                RedisReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisConnection.class)
                .isNotInstanceOf(StatefulRedisConnectionImpl.class).isSameAs(connection);

        sync.close();
        pool.close();
    }

    @Test
    public void wrappedClusterConnectionShouldUseWrappers() throws Exception {

        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.create(TestSettings.host(), 7379));

        GenericObjectPool<StatefulRedisClusterConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                redisClusterClient::connect, new GenericObjectPoolConfig());

        StatefulRedisClusterConnection<String, String> connection = pool.borrowObject();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisClusterConnection.class).isNotInstanceOf(
                StatefulRedisClusterConnectionImpl.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isTrue();

        assertThat(sync).isInstanceOf(RedisAdvancedClusterCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAdvancedClusterAsyncCommands.class).isNotInstanceOf(
                RedisAdvancedClusterAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisAdvancedClusterReactiveCommands.class).isNotInstanceOf(
                RedisAdvancedClusterReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisClusterConnection.class)
                .isNotInstanceOf(StatefulRedisClusterConnectionImpl.class).isSameAs(connection);

        sync.close();
        pool.close();

        FastShutdown.shutdown(redisClusterClient);
    }

    @Test
    public void plainConnectionShouldNotUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), new GenericObjectPoolConfig(), false);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();

        assertThat(connection).isInstanceOf(StatefulRedisConnection.class).isNotInstanceOf(
                StatefulRedisClusterConnectionImpl.class);
        assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();

        assertThat(sync).isInstanceOf(RedisCommands.class);
        assertThat(connection.async()).isInstanceOf(RedisAsyncCommands.class).isInstanceOf(RedisAsyncCommandsImpl.class);
        assertThat(connection.reactive()).isInstanceOf(RedisReactiveCommands.class).isInstanceOf(
                RedisReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisConnection.class).isInstanceOf(
                StatefulRedisConnectionImpl.class);

        pool.returnObject(connection);
        pool.close();
    }

    @Test
    public void softRefPoolShouldWorkWithWrappedConnections() throws Exception {

        SoftReferenceObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createSoftReferenceObjectPool(() -> client.connect());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();

        assertThat(channels).hasSize(1);

        RedisCommands<String, String> sync = connection.sync();
        sync.ping();
        sync.close();

        pool.close();

        assertThat(channels).isEmpty();
    }

    @Test
    public void wrappedObjectClosedAfterReturn() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), new GenericObjectPoolConfig(), true);

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();
        sync.ping();
        sync.close();

        try {
            connection.isMulti();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("deallocated");
        }

        pool.close();
    }

    @Test
    public void tryWithResourcesReturnsConnectionToPool() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(
                () -> client.connect(), new GenericObjectPoolConfig());

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
    public void tryWithResourcesReturnsSoftRefConnectionToPool() throws Exception {

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
            sync.close();
        }
    }
}
