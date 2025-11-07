/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.codec.StringCodec.UTF8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.TestFutures;
import io.lettuce.test.LettuceExtension;

/**
 * 
 * @author Ali Takavci
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class RedisMultiDbClientConnectIntegrationTests extends MultiDbTestSupport {

    @Inject
    RedisMultiDbClientConnectIntegrationTests(MultiDbClient client) {
        super(client);
    }

    @BeforeEach
    void setUp() {
        directClient1.connect().sync().flushall();
        directClient2.connect().sync().flushall();
    }

    @After
    void tearDown() {
        directClient1.shutdown();
        directClient2.shutdown();
    }

    /*
     * Standalone/Stateful
     */
    @Test
    void connectClientUri() {

        StatefulRedisConnection<String, String> connection = multiDbClient.connect();
        assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
        connection.close();
    }

    @Test
    void connectCodecClientUri() {
        StatefulRedisConnection<String, String> connection = multiDbClient.connect(UTF8);
        assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
        connection.close();
    }

    // @Test
    // void connectOwnUri() {
    // RedisURI redisURI = redis(host, port).build();
    // StatefulRedisConnection<String, String> connection = client.connect(redisURI);
    // assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
    // connection.close();
    // }

    // @Test
    // void connectMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connect(new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectSentinelMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connect(invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectCodecOwnUri() {
    // RedisURI redisURI = redis(host, port).build();
    // StatefulRedisConnection<String, String> connection = client.connect(UTF8, redisURI);
    // assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
    // connection.close();
    // }

    // @Test
    // void connectAsyncCodecOwnUri() {
    // RedisURI redisURI = redis(host, port).build();
    // ConnectionFuture<StatefulRedisConnection<String, String>> future = client.connectAsync(UTF8, redisURI);
    // StatefulRedisConnection<String, String> connection = TestFutures.getOrTimeout(future.toCompletableFuture());
    // assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
    // connection.close();
    // }

    // @Test
    // void connectCodecMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connect(UTF8, new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectcodecSentinelMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connect(UTF8, invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // @Disabled("Non-deterministic behavior. Can cause a deadlock")
    // void shutdownSyncInRedisFutureTest() {
    // try (final MultiDbClient redisFailoverClient = MultiDbClient.create();
    // final StatefulRedisConnection<String, String> connection = redisFailoverClient
    // .connect(redis(host, port).build())) {
    // CompletableFuture<String> f = connection.async().get("key1").whenComplete((result, e) -> {
    // connection.close();
    // redisFailoverClient.shutdown(0, 0, SECONDS); // deadlock expected.
    // }).toCompletableFuture();

    // assertThatThrownBy(() -> TestFutures.awaitOrTimeout(f)).isInstanceOf(TimeoutException.class);
    // }
    // }

    // @Test
    // void shutdownAsyncInRedisFutureTest() {

    // try (final MultiDbClient redisFailoverClient = MultiDbClient.create();
    // final StatefulRedisConnection<String, String> connection = redisFailoverClient
    // .connect(redis(host, port).build())) {
    // CompletableFuture<Void> f = connection.async().get("key1").thenCompose(result -> {
    // connection.close();
    // return redisFailoverClient.shutdownAsync(0, 0, SECONDS);
    // }).toCompletableFuture();

    // TestFutures.awaitOrTimeout(f);
    // }
    // }

    @Test
    void connectAndRunSimpleCommand() throws InterruptedException, ExecutionException {
        StatefulRedisConnection<String, String> connection = multiDbClient.connect();
        RedisFuture futureSet = connection.async().set("key1", "value1");
        TestFutures.awaitOrTimeout(futureSet);
        RedisFuture<String> futureGet = connection.async().get("key1");
        TestFutures.awaitOrTimeout(futureGet);
        assertEquals("value1", futureGet.get());
        connection.close();
    }

    @Test
    void connectAndRunAndSwitchAndRun() throws InterruptedException, ExecutionException {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisFuture futureSet = connection.async().set("key1", "value1");
        TestFutures.awaitOrTimeout(futureSet);
        RedisFuture<String> futureGet = connection.async().get("key1");
        TestFutures.awaitOrTimeout(futureGet);
        assertEquals(futureGet.get(), "value1");
        RedisURI other = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(connection.getCurrentEndpoint())).findFirst().get();
        connection.switchToDatabase(other);
        RedisFuture<String> futureGet2 = connection.async().get("key1");
        TestFutures.awaitOrTimeout(futureGet2);
        assertEquals(null, futureGet2.get());
        connection.close();
    }

    // /*
    // * Standalone/PubSub Stateful
    // */
    // @Test
    // void connectPubSubClientUri() {
    // StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
    // assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
    // connection.close();
    // }

    // @Test
    // void connectPubSubCodecClientUri() {
    // StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(UTF8);
    // assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
    // connection.close();
    // }

    // @Test
    // void connectPubSubOwnUri() {
    // RedisURI redisURI = redis(host, port).build();
    // StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(redisURI);
    // assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
    // connection.close();
    // }

    // @Test
    // void connectPubSubMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectPubSub(new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectPubSubSentinelMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectPubSub(invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectPubSubCodecOwnUri() {
    // RedisURI redisURI = redis(host, port).build();
    // StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(UTF8, redisURI);
    // assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
    // connection.close();
    // }

    // @Test
    // void connectPubSubAsync() {
    // RedisURI redisURI = redis(host, port).build();
    // ConnectionFuture<StatefulRedisPubSubConnection<String, String>> future = client.connectPubSubAsync(UTF8, redisURI);
    // StatefulRedisPubSubConnection<String, String> connection = future.join();
    // assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
    // connection.close();
    // }

    // @Test
    // void connectPubSubCodecMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectPubSub(UTF8, new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectPubSubCodecSentinelMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectPubSub(UTF8, invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectPubSubAsyncReauthNotSupportedWithRESP2() {
    // ClientOptions.ReauthenticateBehavior reauth = client.getOptions().getReauthenticateBehaviour();
    // ProtocolVersion protocolVersion = client.getOptions().getConfiguredProtocolVersion();
    // try {
    // client.setOptions(client.getOptions().mutate().protocolVersion(ProtocolVersion.RESP2)
    // .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build());

    // RedisURI redisURI = redis(host, port).build();
    // assertThatThrownBy(() -> client.connectPubSubAsync(UTF8, redisURI)).isInstanceOf(RedisConnectionException.class);

    // } finally {
    // client.setOptions(
    // client.getOptions().mutate().protocolVersion(protocolVersion).reauthenticateBehavior(reauth).build());
    // }
    // }

    // /*
    // * Sentinel Stateful
    // */
    // @Test
    // void connectSentinelClientUri() {
    // StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel();
    // assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
    // connection.close();
    // }

    // @Test
    // void connectSentinelCodecClientUri() {
    // StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(UTF8);
    // assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
    // connection.close();
    // }

    // @Test
    // void connectSentinelAndMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectSentinel(new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectSentinelSentinelMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectSentinel(invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectSentinelOwnUri() {
    // RedisURI redisURI = redis(host, port).build();
    // StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(redisURI);
    // assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
    // connection.close();
    // }

    // @Test
    // void connectSentinelCodecOwnUri() {

    // RedisURI redisURI = redis(host, port).build();
    // StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(UTF8, redisURI);port() +
    // assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
    // connection.close();
    // }

    // @Test
    // void connectSentinelCodecMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectSentinel(UTF8, new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    // }

    // @Test
    // void connectSentinelCodecSentinelMissingHostAndSocketUri() {
    // assertThatThrownBy(() -> client.connectSentinel(UTF8, invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    // }

    // private static RedisURI invalidSentinel() {

    // RedisURI redisURI = new RedisURI();
    // redisURI.getSentinels().add(new RedisURI());

    // return redisURI;
    // }

}
