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
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Proxy;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.junit.Test;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.reactive.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.StatefulRedisClusterConnectionImpl;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.masterslave.MasterSlave;
import com.lambdaworks.redis.masterslave.StatefulRedisMasterSlaveConnection;

/**
 * @author Mark Paluch
 */
public class ConnectionPoolSupportTest extends AbstractRedisClientTest {

    @Test
    public void genericPoolShouldWorkWithWrappedConnections() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

        StatefulRedisConnection<String, String> connection = pool.borrowObject();
        RedisCommands<String, String> sync = connection.sync();
        sync.ping();

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

        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.create(),
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
}
