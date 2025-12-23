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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;

/**
 * Integration tests for {@link StatefulRedisMultiDbPubSubConnection} with pubsub functionality and database switching.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class StatefulMultiDbPubSubConnectionIntegrationTests extends MultiDbTestSupport {

    private final RedisURI firstEndpoint;

    private final RedisURI secondEndpoint;

    private final StatefulRedisPubSubConnection<String, String> conn1;

    private final StatefulRedisPubSubConnection<String, String> conn2;

    @Inject
    StatefulMultiDbPubSubConnectionIntegrationTests(MultiDbClient client) {
        super(client);
        this.firstEndpoint = multiDbClient.getRedisURIs().iterator().next();
        this.secondEndpoint = multiDbClient.getRedisURIs().stream().filter(uri -> !uri.equals(firstEndpoint)).findFirst().get();
        this.conn1 = directClient1.connectPubSub();
        this.conn2 = directClient2.connectPubSub();
    }

    @BeforeEach
    void setUp() {
        directClient1.connect().sync().flushall();
        directClient2.connect().sync().flushall();
    }

    @AfterEach
    void tearDown() {
        directClient1.shutdown();
        directClient2.shutdown();
    }

    // ============ Basic PubSub Connection Tests ============

    @Test
    void shouldConnectPubSubToMultipleEndpoints() {
        StatefulRedisMultiDbPubSubConnection<String, String> connection = multiDbClient.connectPubSub();
        assertNotNull(connection);
        assertThat(connection.getEndpoints()).isNotNull();
        connection.close();
    }

    @Test
    void shouldGetCurrentEndpointForPubSub() {
        StatefulRedisMultiDbPubSubConnection<String, String> connection = multiDbClient.connectPubSub();
        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        assertNotNull(currentEndpoint);
        assertThat(currentEndpoint).isIn(connection.getEndpoints());
        connection.close();
    }

    // ============ Basic PubSub Functionality Tests ============

    @Test
    void shouldSubscribeToChannel() throws Exception {
        StatefulRedisMultiDbPubSubConnection<String, String> pubsub = multiDbClient.connectPubSub();
        BlockingQueue<String> channels = LettuceFactories.newBlockingQueue();

        pubsub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void subscribed(String channel, long count) {
                channels.add(channel);
            }

        });

        pubsub.sync().subscribe("testchannel");
        String channel = channels.take();
        assertEquals("testchannel", channel);
        pubsub.close();
    }

    @Test
    void shouldPublishAndReceiveMessage() throws Exception {
        StatefulRedisMultiDbPubSubConnection<String, String> pubsub = multiDbClient.connectPubSub();
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();

        pubsub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                messages.add(message);
            }

        });

        pubsub.sync().subscribe("msgchannel");

        // Publish message from another connection
        StatefulRedisMultiDbPubSubConnection<String, String> publisher = multiDbClient.connectPubSub();
        publisher.sync().publish("msgchannel", "Hello World");

        String message = messages.take();
        assertEquals("Hello World", message);

        pubsub.close();
        publisher.close();
    }

    // ============ PubSub with Database Switching Tests ============

    private void waitForSubscription(StatefulRedisPubSubConnection<String, String> conn, BlockingQueue<String> messages,
            String channel) {
        // Wait for subscription to be established on given conn
        Wait.untilTrue(() -> {
            AtomicInteger msgId = new AtomicInteger(0);
            try {
                String msg = "Initial message " + msgId.incrementAndGet();
                conn.sync().publish(channel, msg);
                String received = messages.poll(1, TimeUnit.SECONDS);
                return msg.equals(received);
            } catch (InterruptedException e) {
            }
            return false;
        }).waitOrTimeout();
    }

    @Test
    void shouldMaintainSubscriptionAfterDatabaseSwitch() throws Exception {
        try (StatefulRedisMultiDbPubSubConnection<String, String> multiDbConn = multiDbClient.connectPubSub()) {

            BlockingQueue<String> channels = LettuceFactories.newBlockingQueue();
            BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
            multiDbConn.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void subscribed(String channel, long count) {
                    channels.add(channel);
                }

                @Override
                public void message(String channel, String message) {
                    messages.add(message);
                }

            });

            // Subscribe on first database
            multiDbConn.sync().subscribe("channel1");
            String channel = channels.take();
            assertEquals("channel1", channel);

            waitForSubscription(conn1, messages, "channel1");

            // Switch to second database
            multiDbConn.switchToDatabase(secondEndpoint);

            // Verify subscription is re-established on second database
            waitForSubscription(conn2, messages, "channel1");

            assertThat(conn2.sync().pubsubChannels()).contains("channel1");
        }
    }

    @Test
    void shouldReceiveMessagesAfterDatabaseSwitch() throws Exception {
        try (StatefulRedisMultiDbPubSubConnection<String, String> multiDbConn = multiDbClient.connectPubSub()) {

            BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
            multiDbConn.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String message) {
                    messages.add(message);
                }

            });

            // Subscribe on first database
            multiDbConn.sync().subscribe("switchchannel");

            waitForSubscription(conn1, messages, "switchchannel");

            // Switch to second database - subscriptions are automatically re-subscribed
            multiDbConn.switchToDatabase(secondEndpoint);

            // Verify subscription is established on second database
            waitForSubscription(conn2, messages, "switchchannel");
            assertThat(conn2.sync().pubsubChannels()).contains("switchchannel");

            // Publish from second database
            conn2.sync().publish("switchchannel", "Message after switch");

            String message = messages.poll(1, TimeUnit.SECONDS);
            assertEquals("Message after switch", message);
        }
    }

    @Test
    void shouldHandleMultipleDatabaseSwitchesWithPubSub() throws Exception {
        try (StatefulRedisMultiDbPubSubConnection<String, String> multiDbConn = multiDbClient.connectPubSub()) {

            BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
            multiDbConn.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String message) {
                    messages.add(message);
                }

            });

            // Subscribe on first database
            multiDbConn.sync().subscribe("multichannel");

            waitForSubscription(conn1, messages, "multichannel");

            // Switch to second database
            multiDbConn.switchToDatabase(secondEndpoint);

            // Verify subscription on second database
            waitForSubscription(conn2, messages, "multichannel");
            assertThat(conn2.sync().pubsubChannels()).contains("multichannel");

            // Switch back to first database
            multiDbConn.switchToDatabase(firstEndpoint);

            // Verify subscription on first database again
            waitForSubscription(conn1, messages, "multichannel");
            assertThat(conn1.sync().pubsubChannels()).contains("multichannel");

            // Publish on first database
            conn1.sync().publish("multichannel", "Message from DB1");

            String message = messages.poll(1, TimeUnit.SECONDS);
            assertEquals("Message from DB1", message);
        }
    }

    @Test
    void shouldHandleListenerAdditionAfterSwitch() throws Exception {
        try (StatefulRedisMultiDbPubSubConnection<String, String> multiDbConn = multiDbClient.connectPubSub()) {

            BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();

            // Add initial listener
            multiDbConn.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String message) {
                    messages.add(message);
                }

            });

            // Subscribe on first database
            multiDbConn.sync().subscribe("listenertest");

            waitForSubscription(conn1, messages, "listenertest");

            // Switch to second database
            multiDbConn.switchToDatabase(secondEndpoint);

            // Verify subscription on second database
            waitForSubscription(conn2, messages, "listenertest");
            assertThat(conn2.sync().pubsubChannels()).contains("listenertest");

            // Add another listener after switch
            BlockingQueue<String> messages2 = LettuceFactories.newBlockingQueue();
            multiDbConn.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String message) {
                    messages2.add(message);
                }

            });

            // Publish on second database
            conn2.sync().publish("listenertest", "Listener test message");

            // Both listeners should receive the message
            String message1 = messages.poll(1, TimeUnit.SECONDS);
            assertEquals("Listener test message", message1);

            String message2 = messages2.poll(1, TimeUnit.SECONDS);
            assertEquals("Listener test message", message2);
        }
    }

    // ============ Tests for Isolation After Database Switch ============

    @Test
    void shouldNotReceiveMessagesFromOldEndpointAfterSwitch() throws Exception {
        try (StatefulRedisMultiDbPubSubConnection<String, String> multiDbConn = multiDbClient.connectPubSub()) {

            BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
            multiDbConn.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String message) {
                    messages.add(message);
                }

            });
            // Subscribe on first database
            multiDbConn.sync().subscribe("isolationtest");

            waitForSubscription(conn1, messages, "isolationtest");

            // Switch to second database
            multiDbConn.switchToDatabase(secondEndpoint);

            waitForSubscription(conn2, messages, "isolationtest");
            assertThat(conn2.sync().pubsubChannels()).contains("isolationtest");

            assertThat(conn1.sync().pubsubChannels()).doesNotContain("isolationtest");

            // Publish on the OLD endpoint (firstEndpoint) - should NOT be received
            conn1.sync().publish("isolationtest", "Message from first db");

            conn2.sync().publish("isolationtest", "Message from second db");

            // We should only receive the message from the second endpoint
            String message = messages.poll(1, TimeUnit.SECONDS);
            assertEquals("Message from second db", message);

            // Verify no additional messages are received (old endpoint message should not arrive)
            String unexpectedMessage = messages.poll(500, TimeUnit.MILLISECONDS);
            assertThat(unexpectedMessage).isNull();
        }
    }

    @Test
    void shouldUnsubscribeChannelsFromOldEndpointAfterSwitch() throws Exception {
        try (StatefulRedisMultiDbPubSubConnection<String, String> multiDbConn = multiDbClient.connectPubSub()) {

            BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
            multiDbConn.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String message) {
                    messages.add(message);
                }

            });

            // Subscribe on first database
            multiDbConn.sync().subscribe("unsubtest");

            waitForSubscription(conn1, messages, "unsubtest");

            // Switch to second database - this should trigger unsubscribe on old endpoint
            multiDbConn.switchToDatabase(secondEndpoint);

            // Verify subscription is established on second database
            waitForSubscription(conn2, messages, "unsubtest");
            assertThat(conn2.sync().pubsubChannels()).contains("unsubtest");

            // Verify that the old endpoint no longer has the subscription
            assertThat(conn1.sync().pubsubChannels()).doesNotContain("unsubtest");

            // Publish on the old endpoint - should NOT be received
            conn1.sync().publish("unsubtest", "Message from old endpoint after unsubscribe");

            // Publish on the new endpoint - should be received
            conn2.sync().publish("unsubtest", "Message from new endpoint");

            // We should only receive the message from the new endpoint
            String message = messages.poll(1, TimeUnit.SECONDS);
            assertEquals("Message from new endpoint", message);

            // Verify no additional messages are received (old endpoint message should not arrive)
            String unexpectedMessage = messages.poll(500, TimeUnit.MILLISECONDS);
            assertThat(unexpectedMessage).isNull();
        }
    }

}
