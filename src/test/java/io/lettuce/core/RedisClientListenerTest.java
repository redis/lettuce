/*
 * Copyright 2016 the original author or authors.
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

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketAddress;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.Timeout;
import io.lettuce.TestClientResources;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * Integration tests for {@link RedisConnectionStateListener} via {@link RedisClient}.
 *
 * @author Mark Paluch
 */
public class RedisClientListenerTest extends AbstractTest {

    private static ClientResources DEFAULT_RESOURCES;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DEFAULT_RESOURCES = TestClientResources.get();
    }

    @Test
    public void shouldNotifyListener() throws Exception {

        final TestConnectionListener listener = new TestConnectionListener();

        RedisClient client = RedisClient.create(DEFAULT_RESOURCES, RedisURI.Builder.redis(host, port).build());

        client.addListener(listener);

        assertThat(listener.onConnected).isNull();
        assertThat(listener.onDisconnected).isNull();
        assertThat(listener.onException).isNull();

        StatefulRedisConnection<String, String> connection = client.connect();

        waitOrTimeout(() -> listener.onConnected != null, Timeout.timeout(seconds(2)));
        assertThat(listener.onConnectedSocketAddress).isNotNull();

        assertThat(listener.onConnected).isEqualTo(connection);
        assertThat(listener.onDisconnected).isNull();

        connection.sync().set(key, value);
        connection.close();

        waitOrTimeout(() -> listener.onDisconnected != null, Timeout.timeout(seconds(2)));

        assertThat(listener.onConnected).isEqualTo(connection);
        assertThat(listener.onDisconnected).isEqualTo(connection);

        FastShutdown.shutdown(client);
    }

    @Test
    public void shouldNotNotifyListenerAfterRemoval() throws Exception {

        final TestConnectionListener removedListener = new TestConnectionListener();
        final TestConnectionListener retainedListener = new TestConnectionListener();

        RedisClient client = RedisClient.create(DEFAULT_RESOURCES, RedisURI.Builder.redis(host, port).build());
        client.addListener(removedListener);
        client.addListener(retainedListener);
        client.removeListener(removedListener);

        // that's the sut call
        client.connect().close();

        waitOrTimeout(() -> retainedListener.onConnected != null, Timeout.timeout(seconds(2)));

        assertThat(retainedListener.onConnected).isNotNull();

        assertThat(removedListener.onConnected).isNull();
        assertThat(removedListener.onConnectedSocketAddress).isNull();
        assertThat(removedListener.onDisconnected).isNull();
        assertThat(removedListener.onException).isNull();

        FastShutdown.shutdown(client);
    }

    private class TestConnectionListener implements RedisConnectionStateListener {

        volatile SocketAddress onConnectedSocketAddress;
        volatile RedisChannelHandler<?, ?> onConnected;
        volatile RedisChannelHandler<?, ?> onDisconnected;
        volatile RedisChannelHandler<?, ?> onException;

        @Override
        public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
            onConnected = connection;
            onConnectedSocketAddress = socketAddress;
        }

        @Override
        public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
            onDisconnected = connection;
        }

        @Override
        public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
            onException = connection;
        }
    }
}
