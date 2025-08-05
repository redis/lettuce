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
package io.lettuce.core.cluster.api.async;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.lettuce.core.Range;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.*;
import io.lettuce.core.json.JsonParser;

/**
 * A complete asynchronous and thread-safe cluster Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author dengliming
 * @since 4.0
 */
public interface RedisClusterAsyncCommands<K, V> extends BaseRedisAsyncCommands<K, V>, RedisAclAsyncCommands<K, V>,
        RedisFunctionAsyncCommands<K, V>, RedisGeoAsyncCommands<K, V>, RedisHashAsyncCommands<K, V>,
        RedisHLLAsyncCommands<K, V>, RedisKeyAsyncCommands<K, V>, RedisListAsyncCommands<K, V>,
        RedisScriptingAsyncCommands<K, V>, RedisServerAsyncCommands<K, V>, RedisSetAsyncCommands<K, V>,
        RedisSortedSetAsyncCommands<K, V>, RedisStreamAsyncCommands<K, V>, RedisStringAsyncCommands<K, V>,
        RedisJsonAsyncCommands<K, V>, RedisVectorSetAsyncCommands<K, V>, RediSearchAsyncCommands<K, V> {

    /**
     * Set the default timeout for operations. A zero timeout value indicates to not time out.
     *
     * @param timeout the timeout value
     * @since 5.0
     * @deprecated since 6.2. Use the corresponding {@link io.lettuce.core.api.StatefulConnection#setTimeout(Duration)} method
     *             on the connection interface. To be removed with Lettuce 7.0.
     */
    @Deprecated
    void setTimeout(Duration timeout);

    /**
     * The asking command is required after a {@code -ASK} redirection. The client should issue {@code ASKING} before to
     * actually send the command to the target instance. See the Redis Cluster specification for more information.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> asking();

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    RedisFuture<String> auth(CharSequence password);

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    RedisFuture<String> auth(String username, CharSequence password);

    /**
     * Adds slots to the cluster node. The current node will become the upstream for the specified slots.
     *
     * @param slots one or more slots from {@literal 0} to {@literal 16384}
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterAddSlots(int... slots);

    /**
     * Generate a new config epoch, incrementing the current epoch, assign the new epoch to this node, WITHOUT any consensus and
     * persist the configuration on disk before sending packets with the new configuration.
     *
     * @return String simple-string-reply If the new config epoch is generated and assigned either BUMPED (epoch) or STILL
     *         (epoch) are returned.
     */
    RedisFuture<String> clusterBumpepoch();

    /**
     * Returns the number of failure reports for the specified node. Failure reports are the way Redis Cluster uses in order to
     * promote a {@literal PFAIL} state, that means a node is not reachable, to a {@literal FAIL} state, that means that the
     * majority of masters in the cluster agreed within a window of time that the node is not reachable.
     *
     * @param nodeId the node id
     * @return Integer reply: The number of active failure reports for the node.
     */
    RedisFuture<Long> clusterCountFailureReports(String nodeId);

    /**
     * Returns the number of keys in the specified Redis Cluster hash {@code slot}.
     *
     * @param slot the slot
     * @return Integer reply: The number of keys in the specified hash slot, or an error if the hash slot is invalid.
     */
    RedisFuture<Long> clusterCountKeysInSlot(int slot);

    /**
     * Takes a list of slot ranges (specified by start and end slots) to assign to the node.
     *
     * @param ranges a list of slot ranges (specified by start and end slots)
     * @return String simple-string-reply
     * @since 6.2
     */
    RedisFuture<String> clusterAddSlotsRange(Range<Integer>... ranges);

    /**
     * Removes slots from the cluster node.
     *
     * @param slots one or more slots from {@literal 0} to {@literal 16384}
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterDelSlots(int... slots);

    /**
     * Takes a list of slot ranges (specified by start and end slots) to remove to the node.
     *
     * @param ranges a list of slot ranges (specified by start and end slots)
     * @return String simple-string-reply
     * @since 6.2
     */
    RedisFuture<String> clusterDelSlotsRange(Range<Integer>... ranges);

    /**
     * Failover a cluster node. Turns the currently connected node into a master and the master into its replica.
     *
     * @param force do not coordinate with master if {@code true}
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterFailover(boolean force);

    /**
     * Failover a cluster node. Turns the currently connected node into a master and the master into its replica.
     *
     * @param force do not coordinate with master if {@code true}
     * @param takeOver do not coordinate with the rest of the cluster if {@code true} force will take precedence over takeOver
     *        if both are set.
     * @return String simple-string-reply
     * @since 6.2.3
     */
    RedisFuture<String> clusterFailover(boolean force, boolean takeOver);

    /**
     * Delete all the slots associated with the specified node. The number of deleted slots is returned.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterFlushslots();

    /**
     * Disallow connections and remove the cluster node from the cluster.
     *
     * @param nodeId the node Id
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterForget(String nodeId);

    /**
     * Retrieve the list of keys within the {@code slot}.
     *
     * @param slot the slot
     * @param count maximal number of keys
     * @return List&lt;K&gt; array-reply list of keys
     */
    RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count);

    /**
     * Get information and statistics about the cluster viewed by the current node.
     *
     * @return String bulk-string-reply as a collection of text lines.
     */
    RedisFuture<String> clusterInfo();

    /**
     * Returns an integer identifying the hash slot the specified key hashes to. This command is mainly useful for debugging and
     * testing, since it exposes via an API the underlying Redis implementation of the hashing algorithm. Basically the same as
     * {@link io.lettuce.core.cluster.SlotHash#getSlot(byte[])}. If not, call Houston and report that we've got a problem.
     *
     * @param key the key.
     * @return Integer reply: The hash slot number.
     */
    RedisFuture<Long> clusterKeyslot(K key);

    /**
     * Meet another cluster node to include the node into the cluster. The command starts the cluster handshake and returns with
     * {@literal OK} when the node was added to the cluster.
     *
     * @param ip IP address of the host
     * @param port port number.
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterMeet(String ip, int port);

    /**
     * Obtain the nodeId for the currently connected node.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterMyId();

    /**
     * Obtain the shard ID for the currently connected node.
     * <p>
     * The CLUSTER MYSHARDID command returns the unique, auto-generated identifier that is associated with the shard to which
     * the connected cluster node belongs.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterMyShardId();

    /**
     * Obtain details about all cluster nodes. Can be parsed using
     * {@link io.lettuce.core.cluster.models.partitions.ClusterPartitionParser#parse}
     *
     * @return String bulk-string-reply as a collection of text lines
     */
    RedisFuture<String> clusterNodes();

    /**
     * Turn this node into a replica of the node with the id {@code nodeId}.
     *
     * @param nodeId master node id
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterReplicate(String nodeId);

    /**
     * List replicas for a certain node identified by its {@code nodeId}. Can be parsed using
     * {@link io.lettuce.core.cluster.models.partitions.ClusterPartitionParser#parse}
     *
     * @param nodeId node id of the master node
     * @return List&lt;String&gt; array-reply list of replicas. The command returns data in the same format as
     *         {@link #clusterNodes()} but one line per replica.
     * @since 6.1.7
     */
    RedisFuture<List<String>> clusterReplicas(String nodeId);

    /**
     * Reset a node performing a soft or hard reset:
     * <ul>
     * <li>All other nodes are forgotten</li>
     * <li>All the assigned / open slots are released</li>
     * <li>If the node is a replica, it turns into a master</li>
     * <li>Only for hard reset: a new Node ID is generated</li>
     * <li>Only for hard reset: currentEpoch and configEpoch are set to 0</li>
     * <li>The new configuration is saved and the cluster state updated</li>
     * <li>If the node was a replica, the whole data set is flushed away</li>
     * </ul>
     *
     * @param hard {@code true} for hard reset. Generates a new nodeId and currentEpoch/configEpoch are set to 0
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterReset(boolean hard);

    /**
     * Forces a node to save the nodes.conf configuration on disk.
     *
     * @return String simple-string-reply: {@code OK} or an error if the operation fails.
     */
    RedisFuture<String> clusterSaveconfig();

    /**
     * This command sets a specific config epoch in a fresh node. It only works when:
     * <ul>
     * <li>The nodes table of the node is empty.</li>
     * <li>The node current config epoch is zero.</li>
     * </ul>
     *
     * @param configEpoch the config epoch
     * @return String simple-string-reply: {@code OK} or an error if the operation fails.
     */
    RedisFuture<String> clusterSetConfigEpoch(long configEpoch);

    /**
     * Flag a slot as {@literal IMPORTING} (incoming) from the node specified in {@code nodeId}.
     *
     * @param slot the slot
     * @param nodeId the id of the node is the master of the slot
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterSetSlotImporting(int slot, String nodeId);

    /**
     * Flag a slot as {@literal MIGRATING} (outgoing) towards the node specified in {@code nodeId}. The slot must be handled by
     * the current node in order to be migrated.
     *
     * @param slot the slot
     * @param nodeId the id of the node is targeted to become the master for the slot
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterSetSlotMigrating(int slot, String nodeId);

    /**
     * Assign a slot to a node. The command migrates the specified slot from the current node to the specified node in
     * {@code nodeId}
     *
     * @param slot the slot
     * @param nodeId the id of the node that will become the master for the slot
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterSetSlotNode(int slot, String nodeId);

    /**
     * Clears migrating / importing state from the slot.
     *
     * @param slot the slot
     * @return String simple-string-reply
     */
    RedisFuture<String> clusterSetSlotStable(int slot);

    /**
     * Get array of cluster shards
     *
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply nested list of the shards response.
     * @since 6.2
     */
    RedisFuture<List<Object>> clusterShards();

    /**
     * List replicas for a certain node identified by its {@code nodeId}. Can be parsed using
     * {@link io.lettuce.core.cluster.models.partitions.ClusterPartitionParser#parse}
     *
     * @param nodeId node id of the master node
     * @return List&lt;String&gt; array-reply list of replicas. The command returns data in the same format as
     *         {@link #clusterNodes()} but one line per replica.
     * @deprecated since 6.1.7, use {@link #clusterReplicas(String)} instead.
     */
    @Deprecated
    RedisFuture<List<String>> clusterSlaves(String nodeId);

    /**
     * Get array of cluster slots to node mappings.
     *
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply nested list of slot ranges with IP/Port mappings.
     */
    RedisFuture<List<Object>> clusterSlots();

    /**
     * Set multiple keys to multiple values, only if none of the keys exist with pipelining. Cross-slot keys will result in
     * multiple calls to the particular cluster nodes.
     *
     * @param map the map
     * @return RedisFuture&lt;Boolean&gt; integer-reply specifically:
     *
     *         {@code 1} if the all the keys were set. {@code 0} if no key was set (at least one key already existed).
     */
    RedisFuture<Boolean> msetnx(Map<K, V> map);

    /**
     * Tells a Redis cluster replica node that the client is ok reading possibly stale data and is not interested in running
     * write queries.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> readOnly();

    /**
     * Resets readOnly flag.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> readWrite();

    /**
     * Retrieves information about the TCP links between nodes in a Redis Cluster.
     *
     * @return List of maps containing attributes and values for each peer link.
     */
    RedisFuture<List<Map<String, Object>>> clusterLinks();

    /**
     * @return the currently configured instance of the {@link JsonParser}
     * @since 6.5
     */
    JsonParser getJsonParser();

}
