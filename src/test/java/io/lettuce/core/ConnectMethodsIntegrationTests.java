package io.lettuce.core;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.AsyncNodeSelection;
import io.lettuce.test.LettuceExtension;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ConnectMethodsIntegrationTests {

    private final RedisClient redisClient;

    private final RedisClusterClient clusterClient;

    @Inject
    ConnectMethodsIntegrationTests(RedisClient redisClient, RedisClusterClient clusterClient) {
        this.redisClient = redisClient;
        this.clusterClient = clusterClient;
    }

    // Standalone
    @Test
    void standaloneSync() {
        redisClient.connect().close();
    }

    @Test
    void standaloneAsync() {
        try(final StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            connection.async();
        }
    }

    @Test
    void standaloneReactive() {
        try(final StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            connection.reactive();
        }
    }

    @Test
    void standaloneStateful() {
        redisClient.connect().close();
    }

    // PubSub
    @Test
    void pubsubSync() {
        redisClient.connectPubSub().close();
    }

    @Test
    void pubsubAsync() {
        redisClient.connectPubSub().close();
    }

    @Test
    void pubsubReactive() {
        redisClient.connectPubSub().close();
    }

    @Test
    void pubsubStateful() {
        redisClient.connectPubSub().close();
    }

    // Sentinel
    @Test
    void sentinelSync() {
        try(final StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
            connection.sync();
        }
    }

    @Test
    void sentinelAsync() {
        try(final StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
            connection.async();
        }
    }

    @Test
    void sentinelReactive() {
        try(final StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
            connection.reactive();
        }
    }

    @Test
    void sentinelStateful() {
        redisClient.connectSentinel().close();
    }

    // Cluster
    @Test
    void clusterSync() {
        try(final StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            connection.sync();
        }
    }

    @Test
    void clusterAsync() {
        try(final StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            connection.async();
        }
    }

    @Test
    void clusterReactive() {
        try(final StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            connection.reactive();
        }
    }

    @Test
    void clusterStateful() {
        clusterClient.connect().close();
    }

    @Test
    void clusterPubSubSync() {
        try(final StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub()) {
            connection.sync();
        }
    }

    @Test
    void clusterPubSubAsync() {
        try(final StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub()) {
            connection.async();
        }
    }

    @Test
    void clusterPubSubReactive() {
        try(final StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub()) {
            connection.reactive();
        }
    }

    @Test
    void clusterPubSubStateful() {
        clusterClient.connectPubSub().close();
    }

    // Advanced Cluster
    @Test
    void advancedClusterSync() {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        RedisURI uri = clusterClient.getPartitions().getPartition(0).getUri();
        statefulConnection.getConnection(uri.getHost(), uri.getPort()).sync();
        statefulConnection.close();
    }

    @Test
    void advancedClusterAsync() {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        RedisURI uri = clusterClient.getPartitions().getPartition(0).getUri();
        statefulConnection.getConnection(uri.getHost(), uri.getPort()).sync();
        statefulConnection.close();
    }

    @Test
    void advancedClusterReactive() {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        RedisURI uri = clusterClient.getPartitions().getPartition(0).getUri();
        statefulConnection.getConnection(uri.getHost(), uri.getPort()).reactive();
        statefulConnection.close();
    }

    @Test
    void advancedClusterStateful() {
        clusterClient.connect().close();
    }

    // Cluster node selection
    @Test
    void nodeSelectionClusterAsync() {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        AsyncNodeSelection<String, String> masters = statefulConnection.async().masters();
        statefulConnection.close();
    }

}
