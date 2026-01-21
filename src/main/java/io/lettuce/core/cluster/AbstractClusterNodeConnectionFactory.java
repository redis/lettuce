package io.lettuce.core.cluster;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.resource.ClientResources;
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
     * @param clientResources must not be {@code null}.
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
     * Get a {@link Supplier} of {@link CompletionStage} of {@link SocketAddress} for a
     * {@link io.lettuce.core.cluster.ClusterNodeConnectionFactory.ConnectionKey}.
     * <p>
     * This {@link Supplier} resolves the requested endpoint on each {@link Supplier#get()}.
     *
     * @param connectionKey must not be {@code null}.
     * @return
     */
    Supplier<CompletionStage<SocketAddress>> getSocketAddressSupplier(ConnectionKey connectionKey) {

        return () -> {

            if (connectionKey.nodeId != null) {

                SocketAddress socketAddress = getSocketAddress(connectionKey.nodeId);
                logger.debug("Resolved SocketAddress {} using for Cluster node {}", socketAddress, connectionKey.nodeId);
                return CompletableFuture.completedFuture(socketAddress);
            }

            SocketAddress socketAddress = resolve(RedisURI.create(connectionKey.host, connectionKey.port));
            logger.debug("Resolved SocketAddress {} using for Cluster node at {}:{}", socketAddress, connectionKey.host,
                    connectionKey.port);
            return CompletableFuture.completedFuture(socketAddress);
        };
    }

    /**
     * Get the {@link SocketAddress} for a {@code nodeId} from {@link Partitions}.
     *
     * @param nodeId
     * @return the {@link SocketAddress}.
     * @throws IllegalArgumentException if {@code nodeId} cannot be looked up.
     */
    private SocketAddress getSocketAddress(String nodeId) {

        for (RedisClusterNode partition : partitions) {
            if (partition.getNodeId().equals(nodeId)) {
                return resolve(partition.getUri());
            }
        }

        throw new IllegalArgumentException(String.format("Cannot resolve a RedisClusterNode for nodeId %s", nodeId));
    }

    private SocketAddress resolve(RedisURI redisURI) {
        return clientResources.socketAddressResolver().resolve(redisURI);
    }

}
