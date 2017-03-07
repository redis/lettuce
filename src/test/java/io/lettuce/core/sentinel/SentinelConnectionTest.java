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
package io.lettuce.core.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.lettuce.TestClientResources;
import io.lettuce.Wait;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;

/**
 * @author Mark Paluch
 */
public class SentinelConnectionTest extends AbstractSentinelTest {

    private StatefulRedisSentinelConnection<String, String> connection;
    private RedisSentinelAsyncCommands<String, String> sentinelAsync;

    @BeforeClass
    public static void setupClient() {
        sentinelClient = RedisClient.create(TestClientResources.get(), RedisURI.Builder
                .sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        connection = sentinelClient.connectSentinel();
        sentinel = connection.sync();
        sentinelAsync = connection.async();
    }

    @Test
    public void testAsync() throws Exception {

        RedisFuture<List<Map<String, String>>> future = sentinelAsync.masters();

        assertThat(future.get()).isNotNull();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCancelled()).isFalse();
    }

    @Test
    public void testFuture() throws Exception {

        RedisFuture<Map<String, String>> future = sentinelAsync.master("unknown master");

        AtomicBoolean state = new AtomicBoolean();

        future.exceptionally(throwable -> {
            state.set(true);
            return null;
        });

        assertThat(future.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(state.get()).isTrue();
    }

    @Test
    public void testStatefulConnection() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        assertThat(statefulConnection).isSameAs(statefulConnection.async().getStatefulConnection());
    }

    @Test
    public void testSyncConnection() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        RedisSentinelCommands<String, String> sync = statefulConnection.sync();
        assertThat(sync.ping()).isEqualTo("PONG");
    }

    @Test
    public void testSyncAsyncConversion() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        assertThat(statefulConnection.sync().getStatefulConnection()).isSameAs(statefulConnection);
        assertThat(statefulConnection.sync().getStatefulConnection().sync()).isSameAs(statefulConnection.sync());
    }

    @Test
    public void testSyncClose() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        statefulConnection.sync().getStatefulConnection().close();

        Wait.untilTrue(() -> !sentinel.isOpen()).waitOrTimeout();

        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();
    }

    @Test
    public void testAsyncClose() throws Exception {
        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        statefulConnection.async().getStatefulConnection().close();

        Wait.untilTrue(() -> !sentinel.isOpen()).waitOrTimeout();

        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();
    }

    @Test
    public void connectToOneNode() throws Exception {
        RedisSentinelCommands<String, String> connection = sentinelClient.connectSentinel(
                RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build()).sync();
        assertThat(connection.ping()).isEqualTo("PONG");
        connection.getStatefulConnection().close();
    }

    @Test
    public void connectWithByteCodec() throws Exception {
        RedisSentinelCommands<byte[], byte[]> connection = sentinelClient.connectSentinel(new ByteArrayCodec()).sync();
        assertThat(connection.master(MASTER_ID.getBytes())).isNotNull();
        connection.getStatefulConnection().close();
    }

    @Test
    public void sentinelConnectionPingBeforeConnectShouldDiscardPassword() throws Exception {

        RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).withPassword("hello-world").build();

        sentinelClient.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
        StatefulRedisSentinelConnection<String, String> connection = sentinelClient.connectSentinel(redisURI);

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();

        sentinelClient.setOptions(ClientOptions.create());
    }

    @Test
    public void sentinelConnectionShouldSetClientName() throws Exception {

        RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).withClientName("my-client").build();

        StatefulRedisSentinelConnection<String, String> connection = sentinelClient.connectSentinel(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }

    @Test
    public void sentinelManagedConnectionShouldSetClientName() throws Exception {

        RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).withClientName("my-client").build();

        StatefulRedisConnection<String, String> connection = sentinelClient.connect(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.sync().quit();
        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }
}
