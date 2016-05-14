package com.lambdaworks.redis.cluster.topology;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.internal.LettuceSets;

/**
 * Comparators for {@link RedisClusterNode} and {@link RedisURI}.
 *
 * @author Mark Paluch
 */
public class TopologyComparators {

    /**
     * Sort partitions by RedisURI.
     *
     * @param clusterNodes
     * @return List containing {@link RedisClusterNode}s ordered by {@link RedisURI}
     */
    public static List<RedisClusterNode> sortByUri(Iterable<RedisClusterNode> clusterNodes) {
        List<RedisClusterNode> ordered = LettuceLists.newList(clusterNodes);
        Collections.sort(ordered, (o1, o2) -> RedisUriComparator.INSTANCE.compare(o1.getUri(), o2.getUri()));
        return ordered;
    }

    /**
     * Sort partitions by client count.
     *
     * @param clusterNodes
     * @return List containing {@link RedisClusterNode}s ordered by client count
     */
    public static List<RedisClusterNode> sortByClientCount(Iterable<RedisClusterNode> clusterNodes) {

        List<RedisClusterNode> ordered = LettuceLists.newList(clusterNodes);
        Collections.sort(ordered, ClientCountComparator.INSTANCE);
        return ordered;
    }

    /**
     * Sort partitions by latency.
     *
     * @param clusterNodes
     * @return List containing {@link RedisClusterNode}s ordered by lastency
     */
    public static List<RedisClusterNode> sortByLatency(Iterable<RedisClusterNode> clusterNodes) {

        List<RedisClusterNode> ordered = LettuceLists.newList(clusterNodes);
        Collections.sort(ordered, LatencyComparator.INSTANCE);
        return ordered;
    }

    /**
     * Check if properties changed which are essential for cluster operations.
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return {@literal true} if {@code MASTER} or {@code SLAVE} flags changed or the responsible slots changed.
     */
    public static boolean isChanged(Partitions o1, Partitions o2) {

        if (o1.size() != o2.size()) {
            return true;
        }

        for (RedisClusterNode base : o2) {
            if (!essentiallyEqualsTo(base, o1.getPartitionByNodeId(base.getNodeId()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check for {@code MASTER} or {@code SLAVE} flags and whether the responsible slots changed.
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return {@literal true} if {@code MASTER} or {@code SLAVE} flags changed or the responsible slots changed.
     */
    static boolean essentiallyEqualsTo(RedisClusterNode o1, RedisClusterNode o2) {

        if (o2 == null) {
            return false;
        }

        if (!sameFlags(o1, o2, RedisClusterNode.NodeFlag.MASTER)) {
            return false;
        }

        if (!sameFlags(o1, o2, RedisClusterNode.NodeFlag.SLAVE)) {
            return false;
        }

        if (!LettuceSets.newHashSet(o1.getSlots()).equals(LettuceSets.newHashSet(o2.getSlots()))) {
            return false;
        }

        return true;
    }

    private static boolean sameFlags(RedisClusterNode base, RedisClusterNode other, RedisClusterNode.NodeFlag flag) {
        if (base.getFlags().contains(flag)) {
            if (!other.getFlags().contains(flag)) {
                return false;
            }
        } else {
            if (other.getFlags().contains(flag)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare {@link RedisClusterNodeSnapshot} based on their latency. Lowest comes first. Objects of type
     * {@link RedisClusterNode} cannot be compared and yield to a result of {@literal 0}.
     */
    enum LatencyComparator implements Comparator<RedisClusterNode> {

        INSTANCE;

        @Override
        public int compare(RedisClusterNode o1, RedisClusterNode o2) {
            if (o1 instanceof RedisClusterNodeSnapshot && o2 instanceof RedisClusterNodeSnapshot) {

                RedisClusterNodeSnapshot w1 = (RedisClusterNodeSnapshot) o1;
                RedisClusterNodeSnapshot w2 = (RedisClusterNodeSnapshot) o2;

                if (w1.getLatencyNs() != null && w2.getLatencyNs() != null) {
                    return w1.getLatencyNs().compareTo(w2.getLatencyNs());
                }

                if (w1.getLatencyNs() != null && w2.getLatencyNs() == null) {
                    return -1;
                }

                if (w1.getLatencyNs() == null && w2.getLatencyNs() != null) {
                    return 1;
                }
            }

            return 0;
        }
    }

    /**
     * Compare {@link RedisClusterNodeSnapshot} based on their client count. Lowest comes first. Objects of type
     * {@link RedisClusterNode} cannot be compared and yield to a result of {@literal 0}.
     */
    enum ClientCountComparator implements Comparator<RedisClusterNode> {

        INSTANCE;

        @Override
        public int compare(RedisClusterNode o1, RedisClusterNode o2) {
            if (o1 instanceof RedisClusterNodeSnapshot && o2 instanceof RedisClusterNodeSnapshot) {

                RedisClusterNodeSnapshot w1 = (RedisClusterNodeSnapshot) o1;
                RedisClusterNodeSnapshot w2 = (RedisClusterNodeSnapshot) o2;

                if (w1.getConnectedClients() != null && w2.getConnectedClients() != null) {
                    return w1.getConnectedClients().compareTo(w2.getConnectedClients());
                }

                if (w1.getConnectedClients() == null && w2.getConnectedClients() != null) {
                    return 1;
                }

                if (w1.getConnectedClients() != null && w2.getConnectedClients() == null) {
                    return -1;
                }
            }

            return 0;
        }
    }

    /**
     * Compare {@link RedisURI} based on their host and port representation.
     */
    enum RedisUriComparator implements Comparator<RedisURI> {

        INSTANCE;

        @Override
        public int compare(RedisURI o1, RedisURI o2) {
            String h1 = "";
            String h2 = "";

            if (o1 != null) {
                h1 = o1.getHost() + ":" + o1.getPort();
            }

            if (o2 != null) {
                h2 = o2.getHost() + ":" + o2.getPort();
            }

            return h1.compareToIgnoreCase(h2);
        }
    }
}
