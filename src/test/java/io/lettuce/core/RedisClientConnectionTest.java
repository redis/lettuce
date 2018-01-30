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
package io.lettuce.core;

import static io.lettuce.core.RedisURI.Builder.redis;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;

/**
 * @author Mark Paluch
 * @author Jongyeol Choi
 */
public class RedisClientConnectionTest extends AbstractRedisClientTest {

    public static final Duration EXPECTED_TIMEOUT = Duration.ofMillis(500);

    @Before
    public void before() {
        client.setDefaultTimeout(EXPECTED_TIMEOUT);
    }

    /*
     * Standalone/Stateful
     */
    @Test
    public void connectClientUri() {

        StatefulRedisConnection<String, String> connection = client.connect();
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    public void connectCodecClientUri() {
        StatefulRedisConnection<String, String> connection = client.connect(Utf8StringCodec.UTF8);
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    public void connectOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisConnection<String, String> connection = client.connect(redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectMissingHostAndSocketUri() {
        client.connect(new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelMissingHostAndSocketUri() {
        client.connect(invalidSentinel());
    }

    @Test
    public void connectCodecOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisConnection<String, String> connection = client.connect(Utf8StringCodec.UTF8, redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    public void connectAsyncCodecOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        ConnectionFuture<StatefulRedisConnection<String, String>> future = client.connectAsync(Utf8StringCodec.UTF8, redisURI);
        StatefulRedisConnection<String, String> connection = future.join();
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectCodecMissingHostAndSocketUri() {
        client.connect(Utf8StringCodec.UTF8, new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectcodecSentinelMissingHostAndSocketUri() {
        client.connect(Utf8StringCodec.UTF8, invalidSentinel());
    }

    @Test(expected = TimeoutException.class)
    @Ignore("Non-deterministic behavior. Can cause a deadlock")
    public void shutdownSyncInRedisFutureTest() throws Exception {

        RedisClient redisClient = RedisClient.create();
        StatefulRedisConnection<String, String> connection = redisClient.connect(redis(host, port).build());

        CompletableFuture<String> f = connection.async().get("key1").whenComplete((result, e) -> {
            connection.close();
            redisClient.shutdown(0, 0, TimeUnit.SECONDS); // deadlock expected.
            }).toCompletableFuture();

        f.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void shutdownAsyncInRedisFutureTest() throws Exception {

        RedisClient redisClient = RedisClient.create();
        StatefulRedisConnection<String, String> connection = redisClient.connect(redis(host, port).build());
        CompletableFuture<Void> f = connection.async().get("key1").thenCompose(result -> {
            connection.close();
            return redisClient.shutdownAsync(0, 0, TimeUnit.SECONDS);
        }).toCompletableFuture();

        f.get(5, TimeUnit.SECONDS);
    }

    /*
     * Standalone/PubSub Stateful
     */
    @Test
    public void connectPubSubClientUri() {
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    public void connectPubSubCodecClientUri() {
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(Utf8StringCodec.UTF8);
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    public void connectPubSubOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubMissingHostAndSocketUri() {
        client.connectPubSub(new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubSentinelMissingHostAndSocketUri() {
        client.connectPubSub(invalidSentinel());
    }

    @Test
    public void connectPubSubCodecOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub(Utf8StringCodec.UTF8, redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test
    public void connectPubSubAsync() {
        RedisURI redisURI = redis(host, port).build();
        ConnectionFuture<StatefulRedisPubSubConnection<String, String>> future = client.connectPubSubAsync(
                Utf8StringCodec.UTF8, redisURI);
        StatefulRedisPubSubConnection<String, String> connection = future.join();
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubCodecMissingHostAndSocketUri() {
        client.connectPubSub(Utf8StringCodec.UTF8, new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPubSubCodecSentinelMissingHostAndSocketUri() {
        client.connectPubSub(Utf8StringCodec.UTF8, invalidSentinel());
    }

    /*
     * Sentinel Stateful
     */
    @Test
    public void connectSentinelClientUri() {
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel();
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test
    public void connectSentinelCodecClientUri() {
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(Utf8StringCodec.UTF8);
        assertThat(connection.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
        connection.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelAndMissingHostAndSocketUri() {
        client.connectSentinel(new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelSentinelMissingHostAndSocketUri() {
        client.connectSentinel(invalidSentinel());
    }

    @Test
    public void connectSentinelOwnUri() {
        RedisURI redisURI = redis(host, port).build();
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(redisURI);
        assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        connection.close();
    }

    @Test
    public void connectSentinelCodecOwnUri() {

        RedisURI redisURI = redis(host, port).build();
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(Utf8StringCodec.UTF8, redisURI);
        assertThat(connection.getTimeout()).isEqualTo(redisURI.getTimeout());
        connection.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelCodecMissingHostAndSocketUri() {
        client.connectSentinel(Utf8StringCodec.UTF8, new RedisURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectSentinelCodecSentinelMissingHostAndSocketUri() {
        client.connectSentinel(Utf8StringCodec.UTF8, invalidSentinel());
    }

    private static RedisURI invalidSentinel() {

        RedisURI redisURI = new RedisURI();
        redisURI.getSentinels().add(new RedisURI());

        return redisURI;
    }
}
