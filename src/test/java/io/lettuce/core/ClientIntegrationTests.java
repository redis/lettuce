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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.New;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class ClientIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    ClientIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.redis = connection.sync();
        this.redis.flushall();
    }

    @Test
    @Inject
    void close(@New StatefulRedisConnection<String, String> connection) {

        connection.close();
        assertThatThrownBy(() -> connection.sync().get(key)).isInstanceOf(RedisException.class);
    }

    @Test
    void statefulConnectionFromSync() {
        assertThat(redis.getStatefulConnection().sync()).isSameAs(redis);
    }

    @Test
    void statefulConnectionFromAsync() {
        RedisAsyncCommands<String, String> async = client.connect().async();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
        async.getStatefulConnection().close();
    }

    @Test
    void statefulConnectionFromReactive() {
        RedisAsyncCommands<String, String> async = client.connect().async();
        assertThat(async.getStatefulConnection().reactive().getStatefulConnection()).isSameAs(async.getStatefulConnection());
        async.getStatefulConnection().close();
    }

    @Test
    void timeout() {

        redis.setTimeout(1, TimeUnit.MICROSECONDS);
        assertThatThrownBy(() -> redis.blpop(1, "unknown")).isInstanceOf(RedisCommandTimeoutException.class);

        redis.setTimeout(Duration.ofSeconds(60));
    }

    @Test
    void reconnect() {

        redis.set(key, value);

        redis.quit();
        Delay.delay(Duration.ofMillis(100));
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Delay.delay(Duration.ofMillis(100));
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Delay.delay(Duration.ofMillis(100));
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void interrupt() {

        StatefulRedisConnection<String, String> connection = client.connect();
        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> connection.sync().blpop(0, key)).isInstanceOf(RedisCommandInterruptedException.class);
        Thread.interrupted();

        connection.closeAsync();
    }

    @Test
    @Inject
    void connectFailure(ClientResources clientResources) {

        RedisClient client = RedisClient.create(clientResources, "redis://invalid");

        assertThatThrownBy(client::connect).isInstanceOf(RedisConnectionException.class)
                .hasMessageContaining("Unable to connect");

        FastShutdown.shutdown(client);
    }

    @Test
    @Inject
    void connectPubSubFailure(ClientResources clientResources) {

        RedisClient client = RedisClient.create(clientResources, "redis://invalid");

        assertThatThrownBy(client::connectPubSub).isInstanceOf(RedisConnectionException.class)
                .hasMessageContaining("Unable to connect");
        FastShutdown.shutdown(client);
    }

    @Test
    void emptyClient() {

        try {
            client.connect();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }

        try {
            client.connect().async();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }

        try {
            client.connect((RedisURI) null);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }
    }

    @Test
    void testExceptionWithCause() {
        RedisException e = new RedisException(new RuntimeException());
        assertThat(e).hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    void reset() {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisAsyncCommands<String, String> async = connection.async();

        connection.sync().set(key, value);
        async.reset();
        connection.sync().set(key, value);
        connection.sync().flushall();

        RedisFuture<KeyValue<String, String>> eval = async.blpop(5, key);

        Delay.delay(Duration.ofMillis(500));

        assertThat(eval.isDone()).isFalse();
        assertThat(eval.isCancelled()).isFalse();

        async.reset();

        Wait.untilTrue(eval::isCancelled).waitOrTimeout();

        assertThat(eval.isCancelled()).isTrue();
        assertThat(eval.isDone()).isTrue();

        connection.close();
    }

    @Test
    void standaloneConnectionShouldSetClientName() {

        RedisURI redisURI = RedisURI.create(host, port);
        redisURI.setClientName("my-client");

        StatefulRedisConnection<String, String> connection = client.connect(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.sync().quit();
        Delay.delay(Duration.ofMillis(100));
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }

    @Test
    void pubSubConnectionShouldSetClientName() {

        RedisURI redisURI = RedisURI.create(host, port);
        redisURI.setClientName("my-client");

        StatefulRedisConnection<String, String> connection = client.connectPubSub(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.sync().quit();
        Delay.delay(Duration.ofMillis(100));
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }

}
