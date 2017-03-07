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
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Proxy;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.junit.Test;

import io.lettuce.TestClientResources;
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

/**
 * @author Mark Paluch
 */
public class ConnectionPoolSupportTest extends AbstractRedisClientTest {

    @Test
    public void genericPoolShouldWorkWithWrappedConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

        borrowAndReturn(pool);
        borrowAndClose(pool);
        borrowAndCloseTryWithResources(pool);

        pool.returnObject(pool.borrowObject().sync().getStatefulConnection());
        pool.returnObject(pool.borrowObject().async().getStatefulConnection());

        pool.close();
    }

    @Test
    public void softReferencePoolShouldWorkWithWrappedConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

        borrowAndReturn(pool);
        borrowAndClose(pool);
        borrowAndCloseTryWithResources(pool);

        pool.returnObject(pool.borrowObject().sync().getStatefulConnection());
        pool.returnObject(pool.borrowObject().async().getStatefulConnection());

        pool.close();
    }

    @Test
    public void genericPoolShouldWorkWithPlainConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig(), false);

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

        connection.close();
        pool.close();
    }

    @Test
    public void genericPoolUsingWrappingShouldPropagateExceptionsCorrectly() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

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
    public void wrappedConnectionShouldUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

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
        assertThat(connection.reactive()).isInstanceOf(RedisReactiveCommands.class)
                .isNotInstanceOf(RedisReactiveCommandsImpl.class);
        assertThat(sync.getStatefulConnection()).isInstanceOf(StatefulRedisConnection.class)
                .isNotInstanceOf(StatefulRedisConnectionImpl.class).isSameAs(connection);

        connection.close();
        pool.close();
    }

    @Test
    public void wrappedClusterConnectionShouldUseWrappers() throws Exception {

        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.create(TestSettings.host(), 7379));

        GenericObjectPool<StatefulRedisClusterConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(redisClusterClient::connect, new GenericObjectPoolConfig());

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
    public void plainConnectionShouldNotUseWrappers() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig(), false);

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
    public void softRefPoolShouldWorkWithWrappedConnections() throws Exception {

        SoftReferenceObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createSoftReferenceObjectPool(() -> client.connect());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();
        sync.ping();

        connection.close();
        pool.close();
    }

    @Test
    public void wrappedObjectClosedAfterReturn() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig(), true);

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
    public void tryWithResourcesReturnsConnectionToPool() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

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
            connection.close();
        }
    }
}
