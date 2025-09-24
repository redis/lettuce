/*
 * Copyright 2017-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.cluster;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.ConnectionIntent;

/**
 * Asynchronous connection provider for cluster operations.
 *
 * @author Mark Paluch
 * @since 4.4
 */
interface AsyncClusterConnectionProvider extends Closeable {

    /**
     * Obtain a node-scoped connection for the given {@link ConnectionIntent} by first selecting a random upstream (master)
     * shard to ensure even distribution across partitions and then delegating to the slot-based selection logic.
     * <p>
     * For READ intents, the subsequent slot-based path applies the configured {@link ReadFrom} policy (including
     * order-sensitivity and replica eligibility) and reuses cached reader connections where available. For WRITE intents, the
     * slot-based path selects the upstream node responsible for the chosen slot. If no master with assigned slots is available,
     * this method falls back to selecting a random slot across the full slot space.
     *
     * @param connectionIntent the desired {@link ConnectionIntent} (READ or WRITE), used to determine eligibility and routing
     *        behavior.
     * @return a {@link CompletableFuture} that completes with a node-scoped {@link StatefulRedisConnection} for the selected
     *         node.
     *
     * @implNote This method uses the current {@code partitions} snapshot for shard/slot selection and delegates to
     *           {@link #getConnectionAsync(ConnectionIntent, int)} to perform the actual connection acquisition, thereby
     *           centralizing membership validation, caching, and ReadFrom handling.
     *
     * @throws CompletionException if the connection cannot be obtained. The underlying cause can be a
     *         {@link PartitionSelectorException} when no eligible partition/slot can be determined, or a
     *         {@link RedisConnectionException} originating from the connection attempt.
     *
     * @see #getConnectionAsync(ConnectionIntent, int)
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionIntent connectionIntent);

    /**
     * Provide a connection for the connectionIntent and cluster slot. The underlying connection is bound to the nodeId. If the
     * slot responsibility changes, the connection will not point to the updated nodeId.
     *
     * @param connectionIntent {@link ConnectionIntent#READ} or {@link ConnectionIntent#WRITE}. {@literal READ} connections will
     *        be provided with {@literal READONLY} mode set.
     * @param slot the slot-hash of the key, see {@link SlotHash}.
     * @return a valid connection which handles the slot.
     * @throws RedisException if no know node can be found for the slot
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionIntent connectionIntent, int slot);

    /**
     * Provide a connection for the connectionIntent and host/port. The connection can survive cluster topology updates. The
     * connection will be closed if the node identified by {@code host} and {@code port} is no longer part of the cluster.
     *
     * @param connectionIntent {@link ConnectionIntent#READ} or {@link ConnectionIntent#WRITE}. {@literal READ} connections will
     *        be provided with {@literal READONLY} mode set.
     * @param host host of the node.
     * @param port port of the node.
     * @return a valid connection to the given host.
     * @throws RedisException if the host is not part of the cluster
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionIntent connectionIntent, String host,
            int port);

    /**
     * Provide a connection for the connectionIntent and nodeId. The connection can survive cluster topology updates. The
     * connection will be closed if the node identified by {@code nodeId} is no longer part of the cluster.
     *
     * @param connectionIntent {@link ConnectionIntent#READ} or {@link ConnectionIntent#WRITE}. {@literal READ} connections will
     *        be provided with {@literal READONLY} mode set.
     * @param nodeId the nodeId of the cluster node.
     * @return a valid connection to the given nodeId.
     * @throws RedisException if the {@code nodeId} is not part of the cluster
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionIntent connectionIntent,
            String nodeId);

    /**
     * Close the connections and free all resources.
     */
    @Override
    void close();

}
