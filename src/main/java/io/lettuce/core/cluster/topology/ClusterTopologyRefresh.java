package io.lettuce.core.cluster.topology;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.resource.ClientResources;

/**
 * Utility to refresh the cluster topology view based on {@link Partitions}.
 *
 * @author Mark Paluch
 */
public interface ClusterTopologyRefresh {

    /**
     * Create a new {@link ClusterTopologyRefresh} instance.
     *
     * @param nodeConnectionFactory the connection factory to open connections to specific cluster nodes
     * @param clientResources shared client resources
     * @return a new {@link ClusterTopologyRefresh} instance.
     */
    static ClusterTopologyRefresh create(NodeConnectionFactory nodeConnectionFactory, ClientResources clientResources) {
        return new DefaultClusterTopologyRefresh(nodeConnectionFactory, clientResources);
    }

    /**
     * Load topology views from a collection of {@link RedisURI}s and return the view per {@link RedisURI}. Partitions contain
     * an ordered list of {@link RedisClusterNode}s. The sort key is latency. Nodes with lower latency come first.
     *
     * @param seed collection of {@link RedisURI}s
     * @param connectTimeout connect timeout
     * @param discovery {@code true} to discover additional nodes
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    CompletionStage<Map<RedisURI, Partitions>> loadViews(Iterable<RedisURI> seed, Duration connectTimeout, boolean discovery);

    /**
     * Load topology views from a collection of {@link RedisURI}s and return the view per {@link RedisURI}. Partitions contain
     * an ordered list of {@link RedisClusterNode}s. The sort key is latency. Nodes with lower latency come first.
     *
     * @param seed collection of {@link RedisURI}s
     * @param connectTimeout connect timeout
     * @param discovery {@code true} to discover additional nodes
     * @param maxTopologyRefreshSources maximum number of additionally queried (discovered) nodes. Use {@link Integer#MAX_VALUE}
     *        to query all discovered nodes.
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    CompletionStage<Map<RedisURI, Partitions>> loadViews(Iterable<RedisURI> seed, Duration connectTimeout, boolean discovery,
            int maxTopologyRefreshSources);

}
