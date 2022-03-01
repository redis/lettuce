/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.AsyncNodeSelection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
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
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        connection.async();
        connection.close();
    }

    @Test
    void standaloneReactive() {
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        connection.reactive();
        connection.close();
    }

    @Test
    void standaloneStateful() {
        redisClient.connect().close();
    }

    // PubSub
    @Test
    void pubsub() {
        redisClient.connectPubSub().close();
    }

    // Sentinel
    @Test
    void sentinel() {
        redisClient.connectSentinel().close();
    }

    // Cluster
    @Test
    void clusterSync() {
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        connection.sync();
        connection.close();
    }

    @Test
    void clusterAsync() {
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        connection.async();
        connection.close();
    }

    @Test
    void clusterReactive() {
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        connection.reactive();
        connection.close();
    }

    @Test
    void clusterStateful() {
        clusterClient.connect().close();
    }

    @Test
    void clusterPubSubSync() {
        clusterClient.connectPubSub().sync().getStatefulConnection().close();
    }

    @Test
    void clusterPubSubAsync() {
        StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub();
        connection.async();
        connection.close();
    }

    @Test
    void clusterPubSubReactive() {
        clusterClient.connectPubSub().reactive().getStatefulConnection().close();
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
