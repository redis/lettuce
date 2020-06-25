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
package io.lettuce.core.cluster;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;

/**
 * Connection provider for cluster operations.
 *
 * @author Mark Paluch
 * @since 3.0
 */
interface ClusterConnectionProvider extends Closeable {

    /**
     * Provide a connection for the intent and cluster slot. The underlying connection is bound to the nodeId. If the slot
     * responsibility changes, the connection will not point to the updated nodeId.
     *
     * @param intent {@link Intent#READ} or {@link ClusterConnectionProvider.Intent#WRITE}. {@literal READ} connections will be
     *        provided with {@literal READONLY} mode set.
     * @param slot the slot-hash of the key, see {@link SlotHash}.
     * @return a valid connection which handles the slot.
     * @throws RedisException if no know node can be found for the slot
     */
    <K, V> StatefulRedisConnection<K, V> getConnection(Intent intent, int slot);

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
    <K, V> StatefulRedisConnection<K, V> getConnection(Intent intent, String host, int port);

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
    <K, V> StatefulRedisConnection<K, V> getConnection(Intent intent, String nodeId);

    /**
     * Close the connections and free all resources.
     */
    @Override
    void close();

    /**
     * Close the connections and free all resources asynchronously.
     *
     * @since 5.1
     */
    CompletableFuture<Void> closeAsync();

    /**
     * Reset the writer state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection.
     */
    void reset();

    /**
     * Close connections that are not in use anymore/not part of the cluster.
     */
    void closeStaleConnections();

    /**
     * Update partitions.
     *
     * @param partitions the new partitions
     */
    void setPartitions(Partitions partitions);

    /**
     * Disable or enable auto-flush behavior. Default is {@code true}. If autoFlushCommands is disabled, multiple commands
     * can be issued without writing them actually to the transport. Commands are buffered until a {@link #flushCommands()} is
     * issued. After calling {@link #flushCommands()} commands are sent to the transport and executed by Redis.
     *
     * @param autoFlush state of autoFlush.
     */
    void setAutoFlushCommands(boolean autoFlush);

    /**
     * Flush pending commands. This commands forces a flush on the channel and can be used to buffer ("pipeline") commands to
     * achieve batching. No-op if channel is not connected.
     */
    void flushCommands();

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@code null}
     */
    void setReadFrom(ReadFrom readFrom);

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#UPSTREAM} if not set.
     *
     * @return the read from setting
     */
    ReadFrom getReadFrom();

    enum Intent {
        READ, WRITE;
    }

}
