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
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;

import io.lettuce.TestClientResources;
import io.lettuce.Wait;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientTest extends AbstractRedisClientTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    public void openConnection() throws Exception {
        super.openConnection();
    }

    @Override
    public void closeConnection() throws Exception {
        super.closeConnection();
    }

    @Test(expected = RedisException.class)
    public void close() throws Exception {
        redis.getStatefulConnection().close();
        redis.get(key);
    }

    @Test
    public void statefulConnectionFromSync() throws Exception {
        assertThat(redis.getStatefulConnection().sync()).isSameAs(redis);
    }

    @Test
    public void statefulConnectionFromAsync() throws Exception {
        RedisAsyncCommands<String, String> async = client.connect().async();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
        async.getStatefulConnection().close();
    }

    @Test
    public void statefulConnectionFromReactive() throws Exception {
        RedisAsyncCommands<String, String> async = client.connect().async();
        assertThat(async.getStatefulConnection().reactive().getStatefulConnection()).isSameAs(async.getStatefulConnection());
        async.getStatefulConnection().close();
    }

    @Test(expected = RedisException.class)
    public void timeout() throws Exception {
        redis.setTimeout(0, TimeUnit.MICROSECONDS);
        redis.eval(" os.execute(\"sleep \" .. tonumber(1))", ScriptOutputType.STATUS);
    }

    @Test
    public void reconnect() throws Exception {

        redis.set(key, value);

        redis.quit();
        Thread.sleep(100);
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Thread.sleep(100);
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Thread.sleep(100);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 50)
    public void interrupt() throws Exception {
        Thread.currentThread().interrupt();
        redis.blpop(0, key);
    }

    @Test
    public void connectFailure() throws Exception {
        RedisClient client = RedisClient.create(TestClientResources.get(), "redis://invalid");
        exception.expect(RedisException.class);
        exception.expectMessage("Unable to connect");
        client.connect();
        FastShutdown.shutdown(client);
    }

    @Test
    public void connectPubSubFailure() throws Exception {
        RedisClient client = RedisClient.create(TestClientResources.get(), "redis://invalid");
        exception.expect(RedisException.class);
        exception.expectMessage("Unable to connect");
        client.connectPubSub();
        FastShutdown.shutdown(client);
    }

    @Test
    public void emptyClient() throws Exception {

        RedisClient client = DefaultRedisClient.get();
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
    public void testExceptionWithCause() throws Exception {
        RedisException e = new RedisException(new RuntimeException());
        assertThat(e).hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test(timeout = 20000)
    public void reset() throws Exception {
        StatefulRedisConnection<String, String> connection = client.connect();
        RedisAsyncCommands<String, String> async = connection.async();

        connection.sync().set(key, value);
        async.reset();
        connection.sync().set(key, value);
        connection.sync().flushall();

        RedisFuture<KeyValue<String, String>> eval = async.blpop(5, key);

        Command unwrapped = (Command) CommandWrapper.unwrap((RedisCommand) eval);

        Wait.untilNotEquals(0L, unwrapped::getSent).waitOrTimeout();

        assertThat(eval.isDone()).isFalse();
        assertThat(eval.isCancelled()).isFalse();

        async.reset();

        Wait.untilTrue(eval::isCancelled).waitOrTimeout();

        assertThat(eval.isCancelled()).isTrue();
        assertThat(eval.isDone()).isTrue();

        connection.close();
    }

    @Test
    public void standaloneConnectionShouldSetClientName() throws Exception {

        RedisURI redisURI = RedisURI.create(host, port);
        redisURI.setClientName("my-client");

        StatefulRedisConnection<String, String> connection = client.connect(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.sync().quit();
        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }

    @Test
    public void pubSubConnectionShouldSetClientName() throws Exception {

        RedisURI redisURI = RedisURI.create(host, port);
        redisURI.setClientName("my-client");

        StatefulRedisConnection<String, String> connection = client.connectPubSub(redisURI);

        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.sync().quit();
        assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

        connection.close();
    }

}
