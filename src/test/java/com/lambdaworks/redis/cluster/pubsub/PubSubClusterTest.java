package com.lambdaworks.redis.cluster.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.AbstractClusterTest;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.LettuceFactories;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;

/**
 * @author Mark Paluch
 */
public class PubSubClusterTest extends AbstractClusterTest implements RedisPubSubListener<String, String> {

    private BlockingQueue<String> channels;
    private BlockingQueue<String> patterns;
    private BlockingQueue<String> messages;
    private BlockingQueue<Long> counts;

    private StatefulRedisClusterConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection2;

    @Before
    public void openPubSubConnection() throws Exception {
        connection = clusterClient.connect();
        pubSubConnection = clusterClient.connectPubSub();
        pubSubConnection2 = clusterClient.connectPubSub();
        channels = LettuceFactories.newBlockingQueue();
        patterns = LettuceFactories.newBlockingQueue();
        messages = LettuceFactories.newBlockingQueue();
        counts = LettuceFactories.newBlockingQueue();
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
        pubSubConnection.addListener(this);

        connection.getConnection(nodeId).sync().publish(key, value);
        assertThat(messages.take()).isEqualTo(value);

        connection.getConnection(otherNode.getNodeId()).sync().publish(key, value);
        assertThat(messages.take()).isEqualTo(value);
    }


    @Test
    public void testPubSubClientPublish() throws Exception {

        String nodeId = pubSubConnection.sync().clusterMyId();
        pubSubConnection.sync().subscribe(key);
        pubSubConnection.addListener(this);

        assertThat(pubSubConnection2.sync().clusterMyId()).isEqualTo(nodeId);

        pubSubConnection2.sync().publish(key, value);
        assertThat(messages.take()).isEqualTo(value);
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
        pubSubConnection.addListener(this);

        List<String> channelsOnSubscribedNode = connection.getConnection(nodeId).sync().pubsubChannels();
        assertThat(channelsOnSubscribedNode).hasSize(1);

        RedisCommands<String, String> otherNodeConnection = connection.getConnection(otherNode.getNodeId()).sync();
        otherNodeConnection.publish(key, value);
        assertThat(channels.take()).isEqualTo(key);

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

    // RedisPubSubListener implementation

    @Override
    public void message(String channel, String message) {
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        patterns.add(pattern);
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void subscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }

    @Override
    public void unsubscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }

}
