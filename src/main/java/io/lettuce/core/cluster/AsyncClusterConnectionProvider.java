/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.cluster;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterConnectionProvider.Intent;

/**
 * Asynchronous connection provider for cluster operations.
 *
 * @author Mark Paluch
 * @since 4.4
 */
interface AsyncClusterConnectionProvider extends Closeable {

    /**
     * Provide a connection for the intent and cluster slot. The underlying connection is bound to the nodeId. If the slot
     * responsibility changes, the connection will not point to the updated nodeId.
     *
     * @param intent {@link Intent#READ} or {@link Intent#WRITE}. {@literal READ} connections will be provided with
     *        {@literal READONLY} mode set.
     * @param slot the slot-hash of the key, see {@link SlotHash}.
     * @return a valid connection which handles the slot.
     * @throws RedisException if no know node can be found for the slot
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(Intent intent, int slot);

    /**
     * Provide a connection for the intent and host/port. The connection can survive cluster topology updates. The connection
     * will be closed if the node identified by {@code host} and {@code port} is no longer part of the cluster.
     *
     * @param intent {@link Intent#READ} or {@link Intent#WRITE}. {@literal READ} connections will be provided with
     *        {@literal READONLY} mode set.
     * @param host host of the node.
     * @param port port of the node.
     * @return a valid connection to the given host.
     * @throws RedisException if the host is not part of the cluster
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(Intent intent, String host, int port);

    /**
     * Provide a connection for the intent and nodeId. The connection can survive cluster topology updates. The connection will
     * be closed if the node identified by {@code nodeId} is no longer part of the cluster.
     *
     * @param intent {@link Intent#READ} or {@link Intent#WRITE}. {@literal READ} connections will be provided with
     *        {@literal READONLY} mode set.
     * @param nodeId the nodeId of the cluster node.
     * @return a valid connection to the given nodeId.
     * @throws RedisException if the {@code nodeId} is not part of the cluster
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(Intent intent, String nodeId);

    /**
     * Close the connections and free all resources.
     */
    @Override
    void close();
}
