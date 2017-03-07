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
package io.lettuce.core.cluster.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.AbstractClusterTest;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.api.async.NodeSelectionPubSubAsyncCommands;
import io.lettuce.core.cluster.pubsub.api.async.PubSubAsyncNodeSelection;
import io.lettuce.core.cluster.pubsub.api.reactive.NodeSelectionPubSubReactiveCommands;
import io.lettuce.core.cluster.pubsub.api.reactive.PubSubReactiveNodeSelection;
import io.lettuce.core.cluster.pubsub.api.sync.NodeSelectionPubSubCommands;
import io.lettuce.core.cluster.pubsub.api.sync.PubSubNodeSelection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.PubSubTestListener;

/**
 * @author Mark Paluch
 */
public class PubSubClusterTest extends AbstractClusterTest {

    private PubSubTestListener connectionListener = new PubSubTestListener();
    private PubSubTestListener nodeListener = new PubSubTestListener();

    private StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection2;

    @Before
    public void openPubSubConnection() throws Exception {

        pubSubConnection = clusterClient.connectPubSub();
        pubSubConnection2 = clusterClient.connectPubSub();

        pubSubConnection.addListener(connectionListener);
    }

    @After
    public void closePubSubConnection() throws Exception {
        pubSubConnection.close();
        pubSubConnection2.close();
    }

    @Test
    public void testNodeIdSubscription() throws Exception {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);

        StatefulRedisPubSubConnection<String, String> node = pubSubConnection.getConnection(partition.getNodeId());
        node.addListener(nodeListener);

        node.sync().subscribe("channel");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(nodeListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().poll()).isNull();
    }

    @Test
    public void testNodeMessagePropagationSubscription() throws Exception {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);
        pubSubConnection.setNodeMessagePropagation(true);

        StatefulRedisPubSubConnection<String, String> node = pubSubConnection.getConnection(partition.getNodeId());
        node.sync().subscribe("channel");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
    }

    @Test
    public void testNodeHostAndPortMessagePropagationSubscription() throws Exception {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);
        pubSubConnection.setNodeMessagePropagation(true);

        RedisURI uri = partition.getUri();
        StatefulRedisPubSubConnection<String, String> node = pubSubConnection.getConnection(uri.getHost(), uri.getPort());
        node.sync().subscribe("channel");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
    }

    @Test
    public void testAsyncSubscription() throws Exception {

        pubSubConnection.setNodeMessagePropagation(true);

        PubSubAsyncNodeSelection<String, String> masters = pubSubConnection.async().masters();
        NodeSelectionPubSubAsyncCommands<String, String> commands = masters.commands();

        CompletableFuture.allOf(commands.psubscribe("chann*").futures()).get();

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(masters.size()).isEqualTo(2);
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().poll()).isNull();
    }

    @Test
    public void testSyncSubscription() throws Exception {

        pubSubConnection.setNodeMessagePropagation(true);

        PubSubNodeSelection<String, String> masters = pubSubConnection.sync().masters();
        NodeSelectionPubSubCommands<String, String> commands = masters.commands();

        commands.psubscribe("chann*");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(masters.size()).isEqualTo(2);
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().poll()).isNull();
    }

    @Test
    public void testReactiveSubscription() throws Exception {

        pubSubConnection.setNodeMessagePropagation(true);

        PubSubReactiveNodeSelection<String, String> masters = pubSubConnection.reactive().masters();
        NodeSelectionPubSubReactiveCommands<String, String> commands = masters.commands();

        commands.psubscribe("chann*").flux().then().block();

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(masters.size()).isEqualTo(2);
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().poll()).isNull();
    }

    @Test
    public void testClusterListener() throws Exception {

        BlockingQueue<RedisClusterNode> nodes = new LinkedBlockingQueue<>();
        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.addListener(new RedisClusterPubSubAdapter<String, String>() {

            @Override
            public void message(RedisClusterNode node, String pattern, String channel, String message) {
                nodes.add(node);
            }
        });

        PubSubNodeSelection<String, String> masters = pubSubConnection.sync().masters();
        NodeSelectionPubSubCommands<String, String> commands = masters.commands();

        commands.psubscribe("chann*");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(masters.size()).isEqualTo(2);
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().poll()).isNull();

        assertThat(nodes.take()).isNotNull();
        assertThat(nodes.take()).isNotNull();
        assertThat(nodes.poll()).isNull();
    }
}
