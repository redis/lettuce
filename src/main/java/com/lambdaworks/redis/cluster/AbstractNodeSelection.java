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
package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Abstract base class to support node selections. A node selection represents a set of Redis Cluster nodes and allows command
 * execution on the selected cluster nodes.
 * 
 * @param <API> API type.
 * @param <CMD> Command command interface type to invoke multi-node operations.
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
abstract class AbstractNodeSelection<API, CMD, K, V> implements NodeSelectionSupport<API, CMD> {

    protected ClusterDistributionChannelWriter writer;
    protected StatefulRedisClusterConnection<K, V> globalConnection;

    private ClusterConnectionProvider.Intent intent;

    public AbstractNodeSelection(StatefulRedisClusterConnection<K, V> globalConnection, ClusterConnectionProvider.Intent intent) {
        this.globalConnection = globalConnection;
        this.intent = intent;
        writer = ((StatefulRedisClusterConnectionImpl) globalConnection).getClusterDistributionChannelWriter();
    }

    protected StatefulRedisConnection<K, V> getConnection(RedisClusterNode redisClusterNode) {
        RedisURI uri = redisClusterNode.getUri();
        return writer.getClusterConnectionProvider().getConnection(intent, uri.getHost(), uri.getPort());
    }

    /**
     * @return List of involved nodes
     */
    protected abstract List<RedisClusterNode> nodes();

    @Override
    public int size() {
        return nodes().size();
    }

    public Map<RedisClusterNode, StatefulRedisConnection<K, V>> statefulMap() {
        return nodes().stream().collect(
                Collectors.toMap(redisClusterNode -> redisClusterNode, redisClusterNode1 -> getConnection(redisClusterNode1)));
    }

    @Override
    public RedisClusterNode node(int index) {
        return nodes().get(index);
    }

}
