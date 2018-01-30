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

import java.net.SocketAddress;
import java.util.function.Supplier;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.SocketAddressResolver;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Supporting class for {@link ClusterNodeConnectionFactory} implementations.
 * <p>
 * Provides utility methods to resolve {@link SocketAddress} and {@link Partitions}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
abstract class AbstractClusterNodeConnectionFactory<K, V> implements ClusterNodeConnectionFactory<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(PooledClusterConnectionProvider.DefaultClusterNodeConnectionFactory.class);

    private final ClientResources clientResources;

    private volatile Partitions partitions;

    /**
     * Create a new {@link AbstractClusterNodeConnectionFactory} given {@link ClientResources}.
     *
     * @param clientResources must not be {@literal null}.
     */
    public AbstractClusterNodeConnectionFactory(ClientResources clientResources) {
        this.clientResources = clientResources;
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }

    public Partitions getPartitions() {
        return partitions;
    }

    /**
     * Get a {@link Supplier} of {@link SocketAddress} for a
     * {@link io.lettuce.core.cluster.ClusterNodeConnectionFactory.ConnectionKey}.
     * <p>
     * This {@link Supplier} resolves the requested endpoint on each {@link Supplier#get()}.
     *
     * @param connectionKey must not be {@literal null}.
     * @return
     */
    protected Supplier<SocketAddress> getSocketAddressSupplier(ConnectionKey connectionKey) {

        return () -> {

            if (connectionKey.nodeId != null) {

                SocketAddress socketAddress = getSocketAddress(connectionKey.nodeId);
                logger.debug("Resolved SocketAddress {} using for Cluster node {}", socketAddress, connectionKey.nodeId);
                return socketAddress;
            }

            SocketAddress socketAddress = resolve(RedisURI.create(connectionKey.host, connectionKey.port));
            logger.debug("Resolved SocketAddress {} using for Cluster node at {}:{}", socketAddress, connectionKey.host,
                    connectionKey.port);
            return socketAddress;
        };
    }

    /**
     * Get the {@link SocketAddress} for a {@code nodeId} from {@link Partitions}.
     *
     * @param nodeId
     * @return the {@link SocketAddress}.
     * @throws IllegalArgumentException if {@code nodeId} cannot be looked up.
     */
    protected SocketAddress getSocketAddress(String nodeId) {

        for (RedisClusterNode partition : partitions) {
            if (partition.getNodeId().equals(nodeId)) {
                return resolve(partition.getUri());
            }
        }

        throw new IllegalArgumentException(String.format("Cannot resolve a RedisClusterNode for nodeId %s", nodeId));
    }

    private SocketAddress resolve(RedisURI redisURI) {
        return SocketAddressResolver.resolve(redisURI, clientResources.dnsResolver());
    }
}
