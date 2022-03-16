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
package io.lettuce.core.cluster.topology;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 * @author Xujs
 */
class NodeTopologyView {

    private final boolean available;

    private final RedisURI redisURI;

    private Partitions partitions;

    private final int connectedClients;

    private final long replicationOffset;

    private final long latency;

    private final String clusterNodes;

    private final String info;

    private NodeTopologyView(RedisURI redisURI) {

        this.available = false;
        this.redisURI = redisURI;
        this.partitions = new Partitions();
        this.connectedClients = 0;
        this.replicationOffset = -1;
        this.clusterNodes = null;
        this.info = null;
        this.latency = 0;
    }

    NodeTopologyView(RedisURI redisURI, String clusterNodes, String info, long latency) {

        this.available = true;
        this.redisURI = redisURI;

        Properties properties = info == null ? new Properties() : parseInfo(info);
        this.partitions = ClusterPartitionParser.parse(clusterNodes);
        this.connectedClients = getClientCount(properties);
        this.replicationOffset = getReplicationOffset(properties);
        this.clusterNodes = clusterNodes;
        this.info = info;
        this.latency = latency;
    }

    private int getClientCount(Properties properties) {
        return Integer.parseInt(properties.getProperty("connected_clients", "0"));
    }

    private long getReplicationOffset(Properties properties) {
        return Long.parseLong(properties.getProperty("master_repl_offset", "-1"));
    }

    static NodeTopologyView from(RedisURI redisURI, Requests clusterNodesRequests, Requests infoRequests) {

        TimedAsyncCommand<String, String, String> nodes = clusterNodesRequests.getRequest(redisURI);
        TimedAsyncCommand<String, String, String> info = infoRequests.getRequest(redisURI);

        if (resultAvailable(nodes) && !nodes.isCompletedExceptionally() && resultAvailable(info)) {
            return new NodeTopologyView(redisURI, nodes.join(), optionallyGet(info), nodes.duration());
        }
        return new NodeTopologyView(redisURI);
    }

    private static <T> T optionallyGet(TimedAsyncCommand<?, ?, T> command) {

        if (command.isCompletedExceptionally()) {
            return null;
        }
        return command.join();
    }

    private static boolean resultAvailable(RedisFuture<?> redisFuture) {

        if (redisFuture != null && redisFuture.isDone() && !redisFuture.isCancelled()) {
            return true;
        }

        return false;
    }

    private static Properties parseInfo(String info) {

        Properties properties = new Properties();
        try {
            properties.load(new StringReader(info));
        } catch (IOException e) {
            // wtf?
        }

        return properties;
    }

    String getNodeId() {
        return getOwnPartition().getNodeId();
    }

    RedisURI getRedisURI() {

        if (partitions.isEmpty()) {
            return redisURI;
        }

        return getOwnPartition().getUri();
    }

    RedisClusterNode getOwnPartition() {
        RedisClusterNode own = findOwnPartition();

        if (own != null) {
            return own;
        }

        throw new IllegalStateException("Cannot determine own partition");
    }

    private RedisClusterNode findOwnPartition() {
        for (RedisClusterNode partition : partitions) {
            if (partition.is(RedisClusterNode.NodeFlag.MYSELF)) {
                return partition;
            }
        }

        return null;
    }

    void postProcessPartitions() {

        TopologyComparators.SortAction sortAction = TopologyComparators.SortAction.getSortAction();

        sortAction.sort(getPartitions());
        getPartitions().updateCache();
    }

    public boolean canContribute() {

        RedisClusterNode ownPartition = findOwnPartition();

        if (ownPartition == null) {
            return false;
        }
        return true;
    }

    long getLatency() {
        return latency;
    }

    boolean isAvailable() {
        return available;
    }

    Partitions getPartitions() {
        return partitions;
    }

    int getConnectedClients() {
        return connectedClients;
    }

    long getReplicationOffset() {
        return replicationOffset;
    }

    String getInfo() {
        return info;
    }

    String getClusterNodes() {
        return clusterNodes;
    }

    void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }

}
