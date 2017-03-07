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
package io.lettuce.core.cluster.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.AbstractClusterTest;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.PubSubTestListener;

/**
 * @author Mark Paluch
 */
public class PubSubConnectionTest extends AbstractClusterTest {

    private PubSubTestListener listener = new PubSubTestListener();

    private StatefulRedisClusterConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection2;

    @Before
    public void openPubSubConnection() throws Exception {
        connection = clusterClient.connect();
        pubSubConnection = clusterClient.connectPubSub();
        pubSubConnection2 = clusterClient.connectPubSub();

    }

    @After
    public void closePubSubConnection() throws Exception {
        connection.close();
        pubSubConnection.close();
        pubSubConnection2.close();
    }

    @Test
    public void testRegularClientPubSubChannels() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        RedisClusterNode otherNode = getOtherThan(nodeId);
        pubSubConnection.sync().subscribe(key);

        List<String> channelsOnSubscribedNode = connection.getConnection(nodeId).sync().pubsubChannels();
        assertThat(channelsOnSubscribedNode).hasSize(1);

        List<String> channelsOnOtherNode = connection.getConnection(otherNode.getNodeId()).sync().pubsubChannels();
        assertThat(channelsOnOtherNode).isEmpty();
    }

    @Test
    public void testRegularClientPublish() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        RedisClusterNode otherNode = getOtherThan(nodeId);
        pubSubConnection.sync().subscribe(key);
        pubSubConnection.addListener(listener);

        connection.getConnection(nodeId).sync().publish(key, value);
        assertThat(listener.getMessages().take()).isEqualTo(value);

        connection.getConnection(otherNode.getNodeId()).sync().publish(key, value);
        assertThat(listener.getMessages().take()).isEqualTo(value);
    }

    @Test
    public void testPubSubClientPublish() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        pubSubConnection.sync().subscribe(key);
        pubSubConnection.addListener(listener);

        assertThat(pubSubConnection2.sync().clusterMyId()).isEqualTo(nodeId);

        pubSubConnection2.sync().publish(key, value);
        assertThat(listener.getMessages().take()).isEqualTo(value);
    }

    @Test
    public void testConnectToLeastClientsNode() throws Exception {

        clusterClient.reloadPartitions();
        String nodeId = pubSubConnection.sync().clusterMyId();

        StatefulRedisPubSubConnection<String, String> connectionAfterPartitionReload = clusterClient.connectPubSub();
        String newConnectionNodeId = connectionAfterPartitionReload.sync().clusterMyId();
        connectionAfterPartitionReload.close();

        assertThat(nodeId).isNotEqualTo(newConnectionNodeId);
    }

    @Test
    public void testRegularClientPubSubPublish() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        RedisClusterNode otherNode = getOtherThan(nodeId);
        pubSubConnection.sync().subscribe(key);
        pubSubConnection.addListener(listener);

        List<String> channelsOnSubscribedNode = connection.getConnection(nodeId).sync().pubsubChannels();
        assertThat(channelsOnSubscribedNode).hasSize(1);

        RedisCommands<String, String> otherNodeConnection = connection.getConnection(otherNode.getNodeId()).sync();
        otherNodeConnection.publish(key, value);
        assertThat(listener.getChannels().take()).isEqualTo(key);

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
