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
package io.lettuce.core.masterslave;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.masterreplica.MasterReplica;

/**
 * Master-Slave connection API.
 * <p>
 * This API allows connections to Redis Master/Slave setups which run either in a static Master/Slave setup or are managed by
 * Redis Sentinel. Master-Slave connections can discover topologies and select a source for read operations using
 * {@link io.lettuce.core.ReadFrom}.
 * </p>
 * <p>
 *
 * Connections can be obtained by providing the {@link RedisClient}, a {@link RedisURI} and a {@link RedisCodec}.
 *
 * <pre class="code">
 * RedisClient client = RedisClient.create();
 * StatefulRedisMasterSlaveConnection&lt;String, String&gt; connection = MasterSlave.connect(client,
 *         RedisURI.create(&quot;redis://localhost&quot;), StringCodec.UTF8);
 * // ...
 *
 * connection.close();
 * client.shutdown();
 * </pre>
 *
 * </p>
 * <h3>Topology Discovery</h3>
 * <p />
 * Master-Slave topologies are either static or semi-static. Redis Standalone instances with attached slaves provide no
 * failover/HA mechanism. Redis Sentinel managed instances are controlled by Redis Sentinel and allow failover (which include
 * master promotion). The {@link MasterSlave} API supports both mechanisms. The topology is provided by a
 * {@code TopologyProvider}:
 *
 * <ul>
 * <li>{@code MasterReplicaTopologyProvider}: Dynamic topology lookup using the {@code INFO REPLICATION} output. Slaves are
 * listed as {@code slaveN=...} entries. The initial connection can either point to a master or a replica and the topology
 * provider will discover nodes. The connection needs to be re-established outside of lettuce in a case of Master/Slave failover
 * or topology changes.</li>
 * <li>{@code StaticMasterReplicaTopologyProvider}: Topology is defined by the list of {@link RedisURI URIs} and the
 * {@code ROLE} output. MasterSlave uses only the supplied nodes and won't discover additional nodes in the setup. The
 * connection needs to be re-established outside of lettuce in a case of Master/Slave failover or topology changes.</li>
 * <li>{@code SentinelTopologyProvider}: Dynamic topology lookup using the Redis Sentinel API. In particular,
 * {@code SENTINEL MASTER} and {@code SENTINEL SLAVES} output. Master/Slave failover is handled by lettuce.</li>
 * </ul>
 *
 * <h3>Topology Updates</h4>
 * <ul>
 * <li>Standalone Master/Slave: Performs a one-time topology lookup which remains static afterward</li>
 * <li>Redis Sentinel: Subscribes to all Sentinels and listens for Pub/Sub messages to trigger topology refreshing</li>
 * </ul>
 *
 * <h3>Connection Fault-Tolerance</h3> Connecting to Master/Slave bears the possibility that individual nodes are not reachable.
 * {@link MasterSlave} can still connect to a partially-available set of nodes.
 *
 * <ul>
 * <li>Redis Sentinel: At least one Sentinel must be reachable, the masterId must be registered and at least one host must be
 * available (master or slave). Allows for runtime-recovery based on Sentinel Events.</li>
 * <li>Static Setup (auto-discovery): The initial endpoint must be reachable. No recovery/reconfiguration during runtime.</li>
 * <li>Static Setup (provided hosts): All endpoints must be reachable. No recovery/reconfiguration during runtime.</li>
 * </ul>
 *
 * @author Mark Paluch
 * @since 4.1
 * @deprecated since 5.2, use {@link io.lettuce.core.masterreplica.MasterReplica}
 */
@Deprecated
public class MasterSlave {

    /**
     * Open a new connection to a Redis Master-Slave server/servers using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link MasterSlave} performs auto-discovery of nodes using either Redis Sentinel or Master/Slave. A {@link RedisURI}
     * can point to either a master or a replica host.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection.
     */
    public static <K, V> StatefulRedisMasterSlaveConnection<K, V> connect(RedisClient redisClient, RedisCodec<K, V> codec,
            RedisURI redisURI) {

        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");

        return new MasterSlaveConnectionWrapper<>(MasterReplica.connect(redisClient, codec, redisURI));
    }

    /**
     * Open asynchronously a new connection to a Redis Master-Slave server/servers using the supplied {@link RedisURI} and the
     * supplied {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link MasterSlave} performs auto-discovery of nodes using either Redis Sentinel or Master/Slave. A {@link RedisURI}
     * can point to either a master or a replica host.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return {@link CompletableFuture} that is notified once the connect is finished.
     * @since
     */
    public static <K, V> CompletableFuture<StatefulRedisMasterSlaveConnection<K, V>> connectAsync(RedisClient redisClient,
            RedisCodec<K, V> codec, RedisURI redisURI) {
        return MasterReplica.connectAsync(redisClient, codec, redisURI).thenApply(MasterSlaveConnectionWrapper::new);
    }

    /**
     * Open a new connection to a Redis Master-Slave server/servers using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link MasterSlave} performs auto-discovery of nodes if the URI is a Redis Sentinel URI. Master/Slave URIs will be
     * treated as static topology and no additional hosts are discovered in such case. Redis Standalone Master/Slave will
     * discover the roles of the supplied {@link RedisURI URIs} and issue commands to the appropriate node.
     * </p>
     * <p>
     * When using Redis Sentinel, ensure that {@link Iterable redisURIs} contains only a single entry as only the first URI is
     * considered. {@link RedisURI} pointing to multiple Sentinels can be configured through
     * {@link RedisURI.Builder#withSentinel}.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURIs the Redis server(s) to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection.
     */
    public static <K, V> StatefulRedisMasterSlaveConnection<K, V> connect(RedisClient redisClient, RedisCodec<K, V> codec,
            Iterable<RedisURI> redisURIs) {
        return new MasterSlaveConnectionWrapper<>(MasterReplica.connect(redisClient, codec, redisURIs));
    }

    /**
     * Open asynchronously a new connection to a Redis Master-Slave server/servers using the supplied {@link RedisURI} and the
     * supplied {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link MasterSlave} performs auto-discovery of nodes if the URI is a Redis Sentinel URI. Master/Slave URIs will be
     * treated as static topology and no additional hosts are discovered in such case. Redis Standalone Master/Slave will
     * discover the roles of the supplied {@link RedisURI URIs} and issue commands to the appropriate node.
     * </p>
     * <p>
     * When using Redis Sentinel, ensure that {@link Iterable redisURIs} contains only a single entry as only the first URI is
     * considered. {@link RedisURI} pointing to multiple Sentinels can be configured through
     * {@link RedisURI.Builder#withSentinel}.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURIs the Redis server(s) to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return {@link CompletableFuture} that is notified once the connect is finished.
     */
    public static <K, V> CompletableFuture<StatefulRedisMasterSlaveConnection<K, V>> connectAsync(RedisClient redisClient,
            RedisCodec<K, V> codec, Iterable<RedisURI> redisURIs) {
        return MasterReplica.connectAsync(redisClient, codec, redisURIs).thenApply(MasterSlaveConnectionWrapper::new);
    }

}
