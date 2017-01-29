/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis;

import static com.lambdaworks.ConnectionTestUtil.getChannel;
import static com.lambdaworks.ConnectionTestUtil.getConnectionWatchdog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.lambdaworks.Wait;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
public class ClientOptionsTest extends AbstractRedisClientTest {

    @Test
    public void testNew() throws Exception {
        checkAssertions(ClientOptions.create());
    }

    @Test
    public void testBuilder() throws Exception {
        checkAssertions(ClientOptions.builder().build());
    }

    @Test
    public void testCopy() throws Exception {
        checkAssertions(ClientOptions.copyOf(ClientOptions.builder().build()));
    }

    protected void checkAssertions(ClientOptions sut) {
        assertThat(sut.isAutoReconnect()).isEqualTo(true);
        assertThat(sut.isCancelCommandsOnReconnectFailure()).isEqualTo(false);
        assertThat(sut.isPingBeforeActivateConnection()).isEqualTo(false);
        assertThat(sut.isSuspendReconnectOnProtocolFailure()).isEqualTo(false);
        assertThat(sut.getDisconnectedBehavior()).isEqualTo(ClientOptions.DisconnectedBehavior.DEFAULT);
    }

    @Test
    public void variousClientOptions() throws Exception {

        StatefulRedisConnection<String, String> connection1 = client.connect();

        assertThat(connection1.getOptions().isAutoReconnect()).isTrue();
        connection1.close();

        client.setOptions(ClientOptions.builder().autoReconnect(false).build());

        StatefulRedisConnection<String, String> connection2 = client.connect();
        assertThat(connection2.getOptions().isAutoReconnect()).isFalse();

        assertThat(connection1.getOptions().isAutoReconnect()).isTrue();

        connection1.close();
        connection2.close();
    }

    @Test
    public void requestQueueSize() throws Exception {

        client.setOptions(ClientOptions.builder().requestQueueSize(10).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();
        getConnectionWatchdog(connection.getStatefulConnection()).setListenOnChannelInactive(false);

        connection.quit();

        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();

        for (int i = 0; i < 10; i++) {
            connection.ping();
        }

        try {
            connection.ping();
            fail("missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Request queue size exceeded");
        }

        connection.getStatefulConnection().close();
    }

    @Test
    public void disconnectedWithoutReconnect() throws Exception {

        client.setOptions(ClientOptions.builder().autoReconnect(false).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        connection.quit();
        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();
        try {
            connection.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test
    public void disconnectedRejectCommands() throws Exception {

        client.setOptions(ClientOptions.builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        getConnectionWatchdog(connection.getStatefulConnection()).setListenOnChannelInactive(false);
        connection.quit();
        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();
        try {
            connection.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test
    public void disconnectedAcceptCommands() throws Exception {

        client.setOptions(ClientOptions.builder().autoReconnect(false)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        connection.quit();
        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();
        connection.get(key);
        connection.getStatefulConnection().close();
    }

    @Test(timeout = 10000)
    public void pingBeforeConnect() throws Exception {

        redis.set(key, value);
        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
        RedisCommands<String, String> connection = client.connect().sync();

        try {
            String result = connection.get(key);
            assertThat(result).isEqualTo(value);
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test
    public void pingBeforeConnectWithAuthentication() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.Builder.redis(host, port).withPassword(passwd).build();

                RedisCommands<String, String> connection = client.connect(redisURI).sync();

                try {
                    String result = connection.info();
                    assertThat(result).contains("memory");
                } finally {
                    connection.getStatefulConnection().close();
                }

            }
        };
    }

    @Test
    public void pingBeforeConnectWithSslAndAuthentication() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.Builder.redis(host, 6443).withPassword(passwd).withVerifyPeer(false).withSsl(true)
                        .build();

                RedisCommands<String, String> connection = client.connect(redisURI).sync();

                try {
                    String result = connection.info();
                    assertThat(result).contains("memory");
                } finally {
                    connection.getStatefulConnection().close();
                }

            }
        };
    }

    @Test
    public void pingBeforeConnectWithAuthenticationFails() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.builder().redis(host, port).build();

                try {
                    client.connect(redisURI);
                    fail("Missing RedisConnectionException");
                } catch (RedisConnectionException e) {
                    assertThat(e).hasRootCauseInstanceOf(RedisCommandExecutionException.class);
                }
            }
        };
    }

    @Test
    public void pingBeforeConnectWithSslAndAuthenticationFails() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.builder().redis(host, 6443).withVerifyPeer(false).withSsl(true).build();

                try {
                    client.connect(redisURI);
                    fail("Missing RedisConnectionException");
                } catch (RedisConnectionException e) {
                    assertThat(e).hasRootCauseInstanceOf(RedisCommandExecutionException.class);
                }
            }
        };
    }

    @Test(timeout = 10000)
    public void pingBeforeConnectWithQueuedCommandsAndReconnect() throws Exception {

        StatefulRedisConnection<String, String> controlConnection = client.connect();

        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        Utf8StringCodec codec = new Utf8StringCodec();

        StatefulRedisConnection<String, String> redisConnection = client.connect(RedisURI.create("redis://localhost:6479/5"));
        redisConnection.async().set("key1", "value1");
        redisConnection.async().set("key2", "value2");

        RedisFuture<String> sleep = (RedisFuture<String>) controlConnection.dispatch(new AsyncCommand<>(new Command<>(
                CommandType.DEBUG, new StatusOutput<>(codec), new CommandArgs<>(codec).add("SLEEP").add(2))));

        sleep.await(100, TimeUnit.MILLISECONDS);

        Channel channel = getChannel(redisConnection);
        ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
        connectionWatchdog.setReconnectSuspended(true);

        channel.close().get();
        sleep.get();

        redisConnection.async().get(key).cancel(true);

        RedisFuture<String> getFuture1 = redisConnection.async().get("key1");
        RedisFuture<String> getFuture2 = redisConnection.async().get("key2");
        getFuture1.await(100, TimeUnit.MILLISECONDS);

        connectionWatchdog.setReconnectSuspended(false);
        connectionWatchdog.scheduleReconnect();

        assertThat(getFuture1.get()).isEqualTo("value1");
        assertThat(getFuture2.get()).isEqualTo("value2");

        controlConnection.close();
        redisConnection.close();
    }

    @Test(timeout = 10000)
    public void authenticatedPingBeforeConnectWithQueuedCommandsAndReconnect() throws Exception {

        new WithPasswordRequired() {

            @Override
            protected void run(RedisClient client) throws Exception {

                RedisURI redisURI = RedisURI.Builder.redis(host, port).withPassword(passwd).withDatabase(5).build();
                StatefulRedisConnection<String, String> controlConnection = client.connect(redisURI);

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

                Utf8StringCodec codec = new Utf8StringCodec();

                StatefulRedisConnection<String, String> redisConnection = client.connect(redisURI);
                redisConnection.async().set("key1", "value1");
                redisConnection.async().set("key2", "value2");

                RedisFuture<String> sleep = (RedisFuture<String>) controlConnection.dispatch(new AsyncCommand<>(new Command<>(
                        CommandType.DEBUG, new StatusOutput<>(codec), new CommandArgs<>(codec).add("SLEEP").add(2))));

                sleep.await(100, TimeUnit.MILLISECONDS);

                Channel channel = getChannel(redisConnection);
                ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
                connectionWatchdog.setReconnectSuspended(true);

                channel.close().get();
                sleep.get();

                redisConnection.async().get(key).cancel(true);

                RedisFuture<String> getFuture1 = redisConnection.async().get("key1");
                RedisFuture<String> getFuture2 = redisConnection.async().get("key2");
                getFuture1.await(100, TimeUnit.MILLISECONDS);

                connectionWatchdog.setReconnectSuspended(false);
                connectionWatchdog.scheduleReconnect();

                assertThat(getFuture1.get()).isEqualTo("value1");
                assertThat(getFuture2.get()).isEqualTo("value2");

                controlConnection.close();
                redisConnection.close();
            }
        };

    }
}
