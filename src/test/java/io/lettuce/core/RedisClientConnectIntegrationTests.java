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

import static io.lettuce.core.RedisURI.Builder.redis;
import static io.lettuce.core.codec.StringCodec.UTF8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.Futures;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 * @author Jongyeol Choi
 */
@ExtendWith(LettuceExtension.class)
class RedisClientConnectIntegrationTests extends TestSupport {

    private static final Duration EXPECTED_TIMEOUT = Duration.ofMillis(500);

    private final RedisClient client;

    @Inject
    RedisClientConnectIntegrationTests(RedisClient client) {
        this.client = client;
    }

    @BeforeEach
    void before() {
        client.setDefaultTimeout(EXPECTED_TIMEOUT);
    }

    /*
     * Standalone/Stateful
     */
    @Test
    void connectClientUri() {

        StatefulRedisConnection<String, String> connection = client.connect();
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    void connectCodecClientUri() {
        StatefulRedisConnection<String, String> connection = client.connect(UTF8);
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    void connectOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisConnection<String, String> connection = client.connect(redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    void connectMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connect(new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectSentinelMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connect(invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectCodecOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisConnection<String, String> connection = client.connect(UTF8, redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    void connectAsyncCodecOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        ConnectionFuture<StatefulRedisConnection<String, String>> future = client.connectAsync(UTF8, redisURI);
        StatefulRedisConnection<String, String> connection = Futures.get(future.toCompletableFuture());
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    void connectCodecMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connect(UTF8, new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectcodecSentinelMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connect(UTF8, invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Disabled("Non-deterministic behavior. Can cause a deadlock")
    void shutdownSyncInRedisFutureTest() {

        RedisClient redisClient = RedisClient.create();
        StatefulRedisConnection<String, String> connection = redisClient.connect(redis(host, port).build());

        CompletableFuture<String> f = connection.async().get("key1").whenComplete((result, e) -> {
            connection.close();
            redisClient.shutdown(0, 0, SECONDS); // deadlock expected.
        }).toCompletableFuture();

        assertThatThrownBy(() -> Futures.await(f)).isInstanceOf(TimeoutException.class);
    }

    @Test
    void shutdownAsyncInRedisFutureTest() {

        RedisClient redisClient = RedisClient.create();
        StatefulRedisConnection<String, String> connection = redisClient.connect(redis(host, port).build());
        CompletableFuture<Void> f = connection.async().get("key1").thenCompose(result -> {
            connection.close();
            return redisClient.shutdownAsync(0, 0, SECONDS);
        }).toCompletableFuture();

        Futures.await(f);
    }

    /*
     * Standalone/PubSub Stateful
     */
    @Test
    void connectPubSubClientUri() {
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    void connectPubSubCodecClientUri() {
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(UTF8);
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    void connectPubSubOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    void connectPubSubMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectPubSub(new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectPubSubSentinelMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectPubSub(invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectPubSubCodecOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(UTF8, redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    void connectPubSubAsync() {
        RedisURI redisURI = redis(host, port).build();
        ConnectionFuture<StatefulRedisPubSubConnection<String, String>> future = client.connectPubSubAsync(UTF8, redisURI);
        StatefulRedisPubSubConnection<String, String> connection = future.join();
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    void connectPubSubCodecMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectPubSub(UTF8, new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectPubSubCodecSentinelMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectPubSub(UTF8, invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    }

    /*
     * Sentinel Stateful
     */
    @Test
    void connectSentinelClientUri() {
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel();
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    void connectSentinelCodecClientUri() {
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(UTF8);
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    void connectSentinelAndMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectSentinel(new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectSentinelSentinelMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectSentinel(invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectSentinelOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(redisURI);
        assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        connection.close();
    }

    @Test
    void connectSentinelCodecOwnUri() {

        RedisURI redisURI = redis(host, port).build();
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(UTF8, redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    void connectSentinelCodecMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectSentinel(UTF8, new RedisURI())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectSentinelCodecSentinelMissingHostAndSocketUri() {
        assertThatThrownBy(() -> client.connectSentinel(UTF8, invalidSentinel())).isInstanceOf(IllegalArgumentException.class);
    }

    private static RedisURI invalidSentinel() {

        RedisURI redisURI = new RedisURI();
        redisURI.getSentinels().add(new RedisURI());

        return redisURI;
    }

}
