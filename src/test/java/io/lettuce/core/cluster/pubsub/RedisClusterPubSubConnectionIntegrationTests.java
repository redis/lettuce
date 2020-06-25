/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.cluster.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.api.async.NodeSelectionPubSubAsyncCommands;
import io.lettuce.core.cluster.pubsub.api.async.PubSubAsyncNodeSelection;
import io.lettuce.core.cluster.pubsub.api.reactive.NodeSelectionPubSubReactiveCommands;
import io.lettuce.core.cluster.pubsub.api.reactive.PubSubReactiveNodeSelection;
import io.lettuce.core.cluster.pubsub.api.sync.NodeSelectionPubSubCommands;
import io.lettuce.core.cluster.pubsub.api.sync.PubSubNodeSelection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.PubSubTestListener;
import io.lettuce.test.Futures;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class RedisClusterPubSubConnectionIntegrationTests extends TestSupport {

    private final RedisClusterClient clusterClient;

    private final PubSubTestListener connectionListener = new PubSubTestListener();

    private final PubSubTestListener nodeListener = new PubSubTestListener();

    private StatefulRedisClusterConnection<String, String> connection;

    private StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;

    private StatefulRedisClusterPubSubConnection<String, String> pubSubConnection2;

    @Inject
    RedisClusterPubSubConnectionIntegrationTests(RedisClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    @BeforeEach
    void openPubSubConnection() {
        connection = clusterClient.connect();
        pubSubConnection = clusterClient.connectPubSub();
        pubSubConnection2 = clusterClient.connectPubSub();

    }

    @AfterEach
    void closePubSubConnection() {
        connection.close();
        pubSubConnection.close();
        pubSubConnection2.close();
    }

    @Test
    void testRegularClientPubSubChannels() {

        String nodeId = pubSubConnection.sync().clusterMyId();
        RedisClusterNode otherNode = getOtherThan(nodeId);
        pubSubConnection.sync().subscribe(key);

        List<String> channelsOnSubscribedNode = connection.getConnection(nodeId).sync().pubsubChannels();
        assertThat(channelsOnSubscribedNode).hasSize(1);

        List<String> channelsOnOtherNode = connection.getConnection(otherNode.getNodeId()).sync().pubsubChannels();
        assertThat(channelsOnOtherNode).isEmpty();
    }

    @Test
    void testRegularClientPublish() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        RedisClusterNode otherNode = getOtherThan(nodeId);
        pubSubConnection.sync().subscribe(key);
        pubSubConnection.addListener(connectionListener);

        connection.getConnection(nodeId).sync().publish(key, value);
        assertThat(connectionListener.getMessages().take()).isEqualTo(value);

        connection.getConnection(otherNode.getNodeId()).sync().publish(key, value);
        assertThat(connectionListener.getMessages().take()).isEqualTo(value);
    }

    @Test
    void testPubSubClientPublish() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        pubSubConnection.sync().subscribe(key);
        pubSubConnection.addListener(connectionListener);

        assertThat(pubSubConnection2.sync().clusterMyId()).isEqualTo(nodeId);

        pubSubConnection2.sync().publish(key, value);
        assertThat(connectionListener.getMessages().take()).isEqualTo(value);
    }

    @Test
    void testConnectToLeastClientsNode() {

        clusterClient.reloadPartitions();
        String nodeId = pubSubConnection.sync().clusterMyId();

        StatefulRedisPubSubConnection<String, String> connectionAfterPartitionReload = clusterClient.connectPubSub();
        String newConnectionNodeId = connectionAfterPartitionReload.sync().clusterMyId();
        connectionAfterPartitionReload.close();

        assertThat(nodeId).isNotEqualTo(newConnectionNodeId);
    }

    @Test
    void testRegularClientPubSubPublish() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        RedisClusterNode otherNode = getOtherThan(nodeId);
        pubSubConnection.sync().subscribe(key);
        pubSubConnection.addListener(connectionListener);

        List<String> channelsOnSubscribedNode = connection.getConnection(nodeId).sync().pubsubChannels();
        assertThat(channelsOnSubscribedNode).hasSize(1);

        RedisCommands<String, String> otherNodeConnection = connection.getConnection(otherNode.getNodeId()).sync();
        otherNodeConnection.publish(key, value);
        assertThat(connectionListener.getChannels().take()).isEqualTo(key);
    }

    @Test
    void testGetConnectionAsyncByNodeId() {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);

        StatefulRedisPubSubConnection<String, String> node = Futures
                .get(pubSubConnection.getConnectionAsync(partition.getNodeId()));

        assertThat(node.sync().ping()).isEqualTo("PONG");
    }

    @Test
    void testGetConnectionAsyncByHostAndPort() {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);

        RedisURI uri = partition.getUri();
        StatefulRedisPubSubConnection<String, String> node = Futures
                .get(pubSubConnection.getConnectionAsync(uri.getHost(), uri.getPort()));

        assertThat(node.sync().ping()).isEqualTo("PONG");
    }

    @Test
    void testNodeIdSubscription() throws Exception {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);

        StatefulRedisPubSubConnection<String, String> node = pubSubConnection.getConnection(partition.getNodeId());
        node.addListener(nodeListener);

        node.sync().subscribe("channel");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(nodeListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().poll()).isNull();
    }

    @Test
    void testNodeMessagePropagationSubscription() throws Exception {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);
        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.addListener(connectionListener);

        StatefulRedisPubSubConnection<String, String> node = pubSubConnection.getConnection(partition.getNodeId());
        node.sync().subscribe("channel");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
    }

    @Test
    void testNodeHostAndPortMessagePropagationSubscription() throws Exception {

        RedisClusterNode partition = pubSubConnection.getPartitions().getPartition(0);
        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.addListener(connectionListener);

        RedisURI uri = partition.getUri();
        StatefulRedisPubSubConnection<String, String> node = pubSubConnection.getConnection(uri.getHost(), uri.getPort());
        node.sync().subscribe("channel");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
    }

    @Test
    void testAsyncSubscription() throws Exception {

        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.addListener(connectionListener);

        PubSubAsyncNodeSelection<String, String> masters = pubSubConnection.async().masters();
        NodeSelectionPubSubAsyncCommands<String, String> commands = masters.commands();

        Futures.await(commands.psubscribe("chann*"));

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(masters.size()).isEqualTo(2);
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
    }

    @Test
    void testSyncSubscription() throws Exception {

        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.addListener(connectionListener);

        PubSubNodeSelection<String, String> masters = pubSubConnection.sync().masters();
        NodeSelectionPubSubCommands<String, String> commands = masters.commands();

        commands.psubscribe("chann*");

        pubSubConnection2.sync().publish("channel", "message");

        assertThat(masters.size()).isEqualTo(2);
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
        assertThat(connectionListener.getMessages().take()).isEqualTo("message");
    }

    @Test
    void testReactiveSubscription() throws Exception {

        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.addListener(connectionListener);

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
    void testClusterListener() throws Exception {

        BlockingQueue<RedisClusterNode> nodes = new LinkedBlockingQueue<>();
        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.addListener(connectionListener);
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

    private RedisClusterNode getOtherThan(String nodeId) {
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            if (redisClusterNode.getNodeId().equals(nodeId)) {
                continue;
            }
            return redisClusterNode;
        }

        throw new IllegalStateException("No other nodes than " + nodeId + " available");
    }

}
