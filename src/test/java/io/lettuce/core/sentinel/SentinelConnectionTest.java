/*
 * Copyright 2011-2018 the original author or authors.
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.Futures;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class SentinelConnectionTest extends AbstractSentinelTest {

    private StatefulRedisSentinelConnection<String, String> connection;
    private RedisSentinelAsyncCommands<String, String> sentinelAsync;

    @BeforeAll
    static void setupClient() {
        sentinelClient = RedisClient.create(TestClientResources.get(), RedisURI.Builder
                .sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @BeforeEach
    public void openConnection() {
        connection = sentinelClient.connectSentinel();
        sentinel = connection.sync();
        sentinelAsync = connection.async();
    }

    @Test
    void testAsync() {

        RedisFuture<List<Map<String, String>>> future = sentinelAsync.masters();

        assertThat(Futures.get(future)).isNotNull();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCancelled()).isFalse();
    }

    @Test
    void testFuture() throws Exception {

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
    void testStatefulConnection() {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        assertThat(statefulConnection).isSameAs(statefulConnection.async().getStatefulConnection());
    }

    @Test
    void testSyncConnection() {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        RedisSentinelCommands<String, String> sync = statefulConnection.sync();
        assertThat(sync.ping()).isEqualTo("PONG");
    }

    @Test
    void testSyncAsyncConversion() {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        assertThat(statefulConnection.sync().getStatefulConnection()).isSameAs(statefulConnection);
        assertThat(statefulConnection.sync().getStatefulConnection().sync()).isSameAs(statefulConnection.sync());
    }

    @Test
    void testSyncClose() {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        statefulConnection.sync().getStatefulConnection().close();

        Wait.untilTrue(() -> !sentinel.isOpen()).waitOrTimeout();

        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();
    }

    @Test
    void testAsyncClose() {
        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        statefulConnection.async().getStatefulConnection().close();

        Wait.untilTrue(() -> !sentinel.isOpen()).waitOrTimeout();

        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();
    }

    @Test
    void connectToOneNode() {
        RedisSentinelCommands<String, String> connection = sentinelClient.connectSentinel(
                RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build()).sync();
        assertThat(connection.ping()).isEqualTo("PONG");
        connection.getStatefulConnection().close();
    }

    @Test
    void connectWithByteCodec() {
        RedisSentinelCommands<byte[], byte[]> connection = sentinelClient.connectSentinel(new ByteArrayCodec()).sync();
        assertThat(connection.master(MASTER_ID.getBytes())).isNotNull();
        connection.getStatefulConnection().close();
    }

    @Test
    void sentinelConnectionPingBeforeConnectShouldDiscardPassword() {

        RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).withPassword("hello-world").build();

        sentinelClient.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
        StatefulRedisSentinelConnection<String, String> connection = sentinelClient.connectSentinel(redisURI);

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();

        sentinelClient.setOptions(ClientOptions.create());
    }

    @Test
    void sentinelConnectionShouldSetClientName() {

        RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).withClientName("my-client").build();

        StatefulRedisSentinelConnection<String, String> connection = sentinelClient.connectSentinel(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }

    @Test
    void sentinelManagedConnectionShouldSetClientName() {

        RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).withClientName("my-client").build();

        StatefulRedisConnection<String, String> connection = sentinelClient.connect(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.sync().quit();
        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }
}
