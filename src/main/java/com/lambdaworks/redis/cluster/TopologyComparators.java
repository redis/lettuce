package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Comparators for {@link RedisClusterNode} and {@link RedisURI}.
 *
 * @author Mark Paluch
 */
class TopologyComparators {

    /**
     * Sort partitions by a {@code fixedOrder} and by {@link RedisURI}. Nodes are sorted as provided in {@code fixedOrder}.
     * {@link RedisURI RedisURIs}s not contained in {@code fixedOrder} are ordered after the fixed sorting and sorted wihin the
     * block by comparing {@link RedisURI}.
     *
     * @param clusterNodes the sorting input
     * @param fixedOrder the fixed order part
     * @return List containing {@link RedisClusterNode}s ordered by {@code fixedOrder} and {@link RedisURI}
     */
    public static List<RedisClusterNode> predefinedSort(Iterable<RedisClusterNode> clusterNodes,
            Iterable<RedisURI> fixedOrder) {

        checkArgument(clusterNodes != null, "Cluster nodes must not be null");
        checkArgument(fixedOrder != null, "Fixed order must not be null");

        final List<RedisURI> fixedOrderList = Lists.newArrayList(fixedOrder);
        List<RedisClusterNode> withOrderSpecification = filter(clusterNodes, new Predicate<RedisClusterNode>() {
            @Override
            public boolean apply(@Nullable RedisClusterNode input) {
                return fixedOrderList.contains(input.getUri());
            }
        });

        List<RedisClusterNode> withoutSpecification = filter(clusterNodes, new Predicate<RedisClusterNode>() {
            @Override
            public boolean apply(@Nullable RedisClusterNode input) {
                return !fixedOrderList.contains(input.getUri());
            }
        });

        Collections.sort(withOrderSpecification, new PredefinedRedisClusterNodeComparator(fixedOrderList));
        Collections.sort(withoutSpecification, new Comparator<RedisClusterNode>() {
            @Override
            public int compare(RedisClusterNode o1, RedisClusterNode o2) {
                return RedisURIComparator.INSTANCE.compare(o1.getUri(), o2.getUri());
            }
        });

        withOrderSpecification.addAll(withoutSpecification);

        return withOrderSpecification;
    }

    private static List<RedisClusterNode> filter(Iterable<RedisClusterNode> clusterNodes,
            Predicate<RedisClusterNode> predicate) {

        List<RedisClusterNode> list = Lists.newArrayList();

        for (RedisClusterNode clusterNode : clusterNodes) {

            if (!predicate.apply(clusterNode)) {
                continue;
            }

            list.add(clusterNode);
        }

        return list;
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

        if (!Sets.newHashSet(o1.getSlots()).equals(Sets.newHashSet(o2.getSlots()))) {
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

    static class PredefinedRedisClusterNodeComparator implements Comparator<RedisClusterNode> {
        private final List<RedisURI> fixedOrder;

        public PredefinedRedisClusterNodeComparator(List<RedisURI> fixedOrder) {
            this.fixedOrder = fixedOrder;
        }

        @Override
        public int compare(RedisClusterNode o1, RedisClusterNode o2) {

            int index1 = fixedOrder.indexOf(o1.getUri());
            int index2 = fixedOrder.indexOf(o2.getUri());

            return compare(index1, index2);
        }

        public static int compare(int x, int y) {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

    /**
     * Compare {@link RedisURI} based on their host and port representation.
     */
    enum RedisURIComparator implements Comparator<RedisURI> {

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