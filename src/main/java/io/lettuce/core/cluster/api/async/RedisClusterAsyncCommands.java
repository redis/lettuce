/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.cluster.api.async;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.*;

/**
 * A complete asynchronous and thread-safe cluster Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisClusterAsyncCommands<K, V> extends BaseRedisAsyncCommands<K, V>, RedisGeoAsyncCommands<K, V>,
        RedisHashAsyncCommands<K, V>, RedisHLLAsyncCommands<K, V>, RedisKeyAsyncCommands<K, V>, RedisListAsyncCommands<K, V>,
        RedisScriptingAsyncCommands<K, V>, RedisServerAsyncCommands<K, V>, RedisSetAsyncCommands<K, V>,
        RedisSortedSetAsyncCommands<K, V>, RedisStreamAsyncCommands<K, V>, RedisStringAsyncCommands<K, V> {

    /**
     * Set the default timeout for operations. A zero timeout value indicates to not time out.
     *
     * @param timeout the timeout value.
     * @since 5.0
     */
    void setTimeout(Duration timeout);

    /**
     * Set the default timeout for operations. A zero timeout value indicates to not time out.
     *
     * @param timeout the timeout value.
     * @param unit the unit of the timeout value.
     * @deprecated since 5.0, use {@link #setTimeout(Duration)}.
     */
    @Deprecated
    void setTimeout(long timeout, TimeUnit unit);

    /**
     * Authenticate to the server.
     *
     * @param password the password.
     * @return String simple-string-reply.
     */
    String auth(String password);

    /**
     * Generate a new config epoch, incrementing the current epoch, assign the new epoch to this node, WITHOUT any consensus and
     * persist the configuration on disk before sending packets with the new configuration.
     *
     * @return String simple-string-reply If the new config epoch is generated and assigned either BUMPED (epoch) or STILL
     *         (epoch) are returned.
     */
    RedisFuture<String> clusterBumpepoch();

    /**
     * Meet another cluster node to include the node into the cluster. The command starts the cluster handshake and returns with
     * {@literal OK} when the node was added to the cluster.
     *
     * @param ip IP address of the host.
     * @param port port number.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterMeet(String ip, int port);

    /**
     * Blacklist and remove the cluster node from the cluster.
     *
     * @param nodeId the node Id.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterForget(String nodeId);

    /**
     * Adds slots to the cluster node. The current node will become the master for the specified slots.
     *
     * @param slots one or more slots from {@literal 0} to {@literal 16384}.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterAddSlots(int... slots);

    /**
     * Removes slots from the cluster node.
     *
     * @param slots one or more slots from {@literal 0} to {@literal 16384}.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterDelSlots(int... slots);

    /**
     * Assign a slot to a node. The command migrates the specified slot from the current node to the specified node in
     * {@code nodeId}
     *
     * @param slot the slot.
     * @param nodeId the id of the node that will become the master for the slot.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterSetSlotNode(int slot, String nodeId);

    /**
     * Clears migrating / importing state from the slot.
     *
     * @param slot the slot.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterSetSlotStable(int slot);

    /**
     * Flag a slot as {@literal MIGRATING} (outgoing) towards the node specified in {@code nodeId}. The slot must be handled by
     * the current node in order to be migrated.
     *
     * @param slot the slot.
     * @param nodeId the id of the node is targeted to become the master for the slot.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterSetSlotMigrating(int slot, String nodeId);

    /**
     * Flag a slot as {@literal IMPORTING} (incoming) from the node specified in {@code nodeId}.
     *
     * @param slot the slot.
     * @param nodeId the id of the node is the master of the slot.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterSetSlotImporting(int slot, String nodeId);

    /**
     * Get information and statistics about the cluster viewed by the current node.
     *
     * @return String bulk-string-reply as a collection of text lines.
     */
    RedisFuture<String> clusterInfo();

    /**
     * Obtain the nodeId for the currently connected node.
     *
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterMyId();

    /**
     * Obtain details about all cluster nodes. Can be parsed using
     * {@link io.lettuce.core.cluster.models.partitions.ClusterPartitionParser#parse}
     *
     * @return String bulk-string-reply as a collection of text lines.
     */
    RedisFuture<String> clusterNodes();

    /**
     * List replicas for a certain node identified by its {@code nodeId}. Can be parsed using
     * {@link io.lettuce.core.cluster.models.partitions.ClusterPartitionParser#parse}
     *
     * @param nodeId node id of the master node.
     * @return List&lt;String&gt; array-reply list of replicas. The command returns data in the same format as
     *         {@link #clusterNodes()} but one line per replica.
     */
    RedisFuture<List<String>> clusterSlaves(String nodeId);

    /**
     * Retrieve the list of keys within the {@code slot}.
     *
     * @param slot the slot.
     * @param count maximal number of keys.
     * @return List&lt;K&gt; array-reply list of keys.
     */
    RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count);

    /**
     * Returns the number of keys in the specified Redis Cluster hash {@code slot}.
     *
     * @param slot the slot.
     * @return Integer reply: The number of keys in the specified hash slot, or an error if the hash slot is invalid.
     */
    RedisFuture<Long> clusterCountKeysInSlot(int slot);

    /**
     * Returns the number of failure reports for the specified node. Failure reports are the way Redis Cluster uses in order to
     * promote a {@literal PFAIL} state, that means a node is not reachable, to a {@literal FAIL} state, that means that the
     * majority of masters in the cluster agreed within a window of time that the node is not reachable.
     *
     * @param nodeId the node id.
     * @return Integer reply: The number of active failure reports for the node.
     */
    RedisFuture<Long> clusterCountFailureReports(String nodeId);

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
     * @param configEpoch the config epoch.
     * @return String simple-string-reply: {@code OK} or an error if the operation fails.
     */
    RedisFuture<String> clusterSetConfigEpoch(long configEpoch);

    /**
     * Get array of cluster slots to node mappings.
     *
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply nested list of slot ranges with IP/Port mappings.
     */
    RedisFuture<List<Object>> clusterSlots();

    /**
     * The asking command is required after a {@code -ASK} redirection. The client should issue {@code ASKING} before to
     * actually send the command to the target instance. See the Redis Cluster specification for more information.
     *
     * @return String simple-string-reply.
     */
    RedisFuture<String> asking();

    /**
     * Turn this node into a replica of the node with the id {@code nodeId}.
     *
     * @param nodeId master node id.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterReplicate(String nodeId);

    /**
     * Failover a cluster node. Turns the currently connected node into a master and the master into its replica.
     *
     * @param force do not coordinate with master if {@code true}.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterFailover(boolean force);

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
     * @param hard {@code true} for hard reset. Generates a new nodeId and currentEpoch/configEpoch are set to 0.
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterReset(boolean hard);

    /**
     * Delete all the slots associated with the specified node. The number of deleted slots is returned.
     *
     * @return String simple-string-reply.
     */
    RedisFuture<String> clusterFlushslots();

    /**
     * Tells a Redis cluster replica node that the client is ok reading possibly stale data and is not interested in running
     * write queries.
     *
     * @return String simple-string-reply.
     */
    RedisFuture<String> readOnly();

    /**
     * Resets readOnly flag.
     *
     * @return String simple-string-reply.
     */
    RedisFuture<String> readWrite();

    /**
     * Delete a key with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the key.
     * @return RedisFuture&lt;Long&gt; integer-reply The number of keys that were removed.
     */
    RedisFuture<Long> del(K... keys);

    /**
     * Get the values of all the given keys with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param keys the key.
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of values at the specified keys.
     */
    RedisFuture<List<KeyValue<K, V>>> mget(K... keys);

    /**
     * Set multiple keys to multiple values with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param map the null.
     * @return RedisFuture&lt;String&gt; simple-string-reply always {@code OK} since {@code MSET} can't fail.
     */
    RedisFuture<String> mset(Map<K, V> map);

    /**
     * Set multiple keys to multiple values, only if none of the keys exist with pipelining. Cross-slot keys will result in
     * multiple calls to the particular cluster nodes.
     *
     * @param map the null.
     * @return RedisFuture&lt;Boolean&gt; integer-reply specifically:
     *
     *         {@code 1} if the all the keys were set. {@code 0} if no key was set (at least one key already existed).
     */
    RedisFuture<Boolean> msetnx(Map<K, V> map);

}
