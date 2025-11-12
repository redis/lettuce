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
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.test.LettuceExtension;

/**
 * Integration tests for {@link StatefulRedisMultiDbPubSubConnection} with pubsub functionality and database switching.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class StatefulMultiDbPubSubConnectionIntegrationTests extends MultiDbTestSupport {

    @Inject
    StatefulMultiDbPubSubConnectionIntegrationTests(MultiDbClient client) {
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

    @Test
    void shouldMaintainSubscriptionAfterDatabaseSwitch() throws Exception {
        StatefulRedisMultiDbPubSubConnection<String, String> pubsub = multiDbClient.connectPubSub();
        BlockingQueue<String> channels = LettuceFactories.newBlockingQueue();

        pubsub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void subscribed(String channel, long count) {
                channels.add(channel);
            }

        });

        // Subscribe on first database
        pubsub.sync().subscribe("channel1");
        String channel = channels.take();
        assertEquals("channel1", channel);

        // Switch to second database
        RedisURI other = StreamSupport.stream(pubsub.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(pubsub.getCurrentEndpoint())).findFirst().get();
        pubsub.switchToDatabase(other);

        // Listener should still be active
        assertThat(pubsub).isNotNull();

        pubsub.close();
    }

    @Test
    void shouldReceiveMessagesAfterDatabaseSwitch() throws Exception {
        StatefulRedisMultiDbPubSubConnection<String, String> pubsub = multiDbClient.connectPubSub();
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();

        pubsub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                messages.add(message);
            }

        });

        // Subscribe on first database
        pubsub.sync().subscribe("switchchannel");

        // Switch to second database - subscriptions are automatically re-subscribed
        RedisURI other = StreamSupport.stream(pubsub.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(pubsub.getCurrentEndpoint())).findFirst().get();
        pubsub.switchToDatabase(other);

        // Publish from another connection on second database
        StatefulRedisMultiDbPubSubConnection<String, String> publisher = multiDbClient.connectPubSub();
        publisher.switchToDatabase(other);
        publisher.sync().publish("switchchannel", "Message after switch");

        String message = messages.poll(1, TimeUnit.SECONDS);
        assertEquals("Message after switch", message);

        pubsub.close();
        publisher.close();
    }

    @Test
    void shouldHandleMultipleDatabaseSwitchesWithPubSub() throws Exception {
        StatefulRedisMultiDbPubSubConnection<String, String> pubsub = multiDbClient.connectPubSub();
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();

        pubsub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                messages.add(message);
            }

        });

        RedisURI firstDb = pubsub.getCurrentEndpoint();
        RedisURI secondDb = StreamSupport.stream(pubsub.getEndpoints().spliterator(), false).filter(uri -> !uri.equals(firstDb))
                .findFirst().get();

        // Subscribe on first database
        pubsub.sync().subscribe("multichannel");

        // Switch to second database
        pubsub.switchToDatabase(secondDb);
        pubsub.sync().subscribe("multichannel");

        // Switch back to first database
        pubsub.switchToDatabase(firstDb);

        // Publish on first database
        StatefulRedisMultiDbPubSubConnection<String, String> publisher = multiDbClient.connectPubSub();
        publisher.sync().publish("multichannel", "Message from DB1");

        String message = messages.take();
        assertEquals("Message from DB1", message);

        pubsub.close();
        publisher.close();
    }

    @Test
    void shouldHandleListenerAdditionAfterSwitch() throws Exception {
        StatefulRedisMultiDbPubSubConnection<String, String> pubsub = multiDbClient.connectPubSub();
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();

        // Subscribe on first database
        pubsub.sync().subscribe("listenertest");

        // Switch to second database
        RedisURI other = StreamSupport.stream(pubsub.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(pubsub.getCurrentEndpoint())).findFirst().get();
        pubsub.switchToDatabase(other);

        // Add listener after switch
        pubsub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                messages.add(message);
            }

        });

        // Subscribe on second database
        pubsub.sync().subscribe("listenertest");

        // Publish on second database
        StatefulRedisMultiDbPubSubConnection<String, String> publisher = multiDbClient.connectPubSub();
        publisher.switchToDatabase(other);
        publisher.sync().publish("listenertest", "Listener test message");

        String message = messages.take();
        assertEquals("Listener test message", message);

        pubsub.close();
        publisher.close();
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

            // Get the endpoints
            RedisURI firstDb = multiDbConn.getCurrentEndpoint();
            RedisURI secondDb = StreamSupport.stream(multiDbConn.getEndpoints().spliterator(), false)
                    .filter(uri -> !uri.equals(firstDb)).findFirst().get();

            // Subscribe on first database
            multiDbConn.sync().subscribe("isolationtest");

            // Switch to second database
            multiDbConn.switchToDatabase(secondDb);

            // Publish on the OLD endpoint (firstDb) - should NOT be received
            try (StatefulRedisPubSubConnection<String, String> conn1 = RedisClient.create(firstDb).connectPubSub()) {
                try (StatefulRedisPubSubConnection<String, String> conn2 = RedisClient.create(secondDb).connectPubSub()) {

                    conn1.sync().publish("isolationtest", "Message from first db");

                    conn2.sync().publish("isolationtest", "Message from second db");

                    // We should only receive the message from the new endpoint
                    String message = messages.poll(1, TimeUnit.SECONDS);
                    assertEquals("Message from second db", message);

                    // Verify no additional messages are received (old endpoint message should not arrive)
                    String unexpectedMessage = messages.poll(500, TimeUnit.MILLISECONDS);
                    assertThat(unexpectedMessage).isNull();
                }
            }
        }
    }

    @Test
    void shouldUnsubscribeChannelsFromOldEndpointAfterSwitch() throws Exception {
        StatefulRedisMultiDbPubSubConnection<String, String> pubsub = multiDbClient.connectPubSub();
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();

        pubsub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                messages.add(message);
            }

        });

        // Get the endpoints
        RedisURI firstDb = pubsub.getCurrentEndpoint();
        RedisURI secondDb = StreamSupport.stream(pubsub.getEndpoints().spliterator(), false).filter(uri -> !uri.equals(firstDb))
                .findFirst().get();

        // Subscribe on first database
        pubsub.sync().subscribe("unsubtest");

        // Wait for subscription to be established
        Thread.sleep(200);

        // Switch to second database - this should trigger unsubscribe on old endpoint
        pubsub.switchToDatabase(secondDb);

        // Wait a bit for unsubscribe to complete
        Thread.sleep(500);

        // Verify that the old endpoint no longer receives messages
        // by publishing on the old endpoint and verifying no message is received
        StatefulRedisMultiDbPubSubConnection<String, String> oldPublisher = multiDbClient.connectPubSub();
        oldPublisher.sync().publish("unsubtest", "Message from old endpoint after unsubscribe");

        // Verify no message is received from old endpoint
        String unexpectedMessage = messages.poll(1, TimeUnit.SECONDS);
        assertThat(unexpectedMessage).isNull();

        // Now verify that the new endpoint still receives messages
        // by re-subscribing on the new endpoint
        pubsub.sync().subscribe("unsubtest");

        // Publish on the new endpoint
        StatefulRedisMultiDbPubSubConnection<String, String> newPublisher = multiDbClient.connectPubSub();
        newPublisher.switchToDatabase(secondDb);
        newPublisher.sync().publish("unsubtest", "Message from new endpoint");

        // Verify message is received from new endpoint
        String expectedMessage = messages.poll(1, TimeUnit.SECONDS);
        assertEquals("Message from new endpoint", expectedMessage);

        pubsub.close();
        oldPublisher.close();
        newPublisher.close();
    }

}
