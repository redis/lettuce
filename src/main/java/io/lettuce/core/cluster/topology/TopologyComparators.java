/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.cluster.topology;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.internal.LettuceSets;

/**
 * Comparators for {@link RedisClusterNode} and {@link RedisURI}.
 *
 * @author Mark Paluch
 */
public class TopologyComparators {

    /**
     * Sort partitions by a {@code fixedOrder} and by {@link RedisURI}. Nodes are sorted as provided in {@code fixedOrder}.
     * {@link RedisURI RedisURIs}s not contained in {@code fixedOrder} are ordered after the fixed sorting and sorted wihin the
     * block by comparing {@link RedisURI}.
     *
     * @param clusterNodes the sorting input
     * @param fixedOrder the fixed order part
     * @return List containing {@link RedisClusterNode}s ordered by {@code fixedOrder} and {@link RedisURI}
     * @see #sortByUri(Iterable)
     */
    public static List<RedisClusterNode> predefinedSort(Iterable<RedisClusterNode> clusterNodes,
            Iterable<RedisURI> fixedOrder) {

        LettuceAssert.notNull(clusterNodes, "Cluster nodes must not be null");
        LettuceAssert.notNull(fixedOrder, "Fixed order must not be null");

        List<RedisURI> fixedOrderList = LettuceLists.newList(fixedOrder);
        List<RedisClusterNode> withOrderSpecification = LettuceLists.newList(clusterNodes)//
                .stream()//
                .filter(redisClusterNode -> fixedOrderList.contains(redisClusterNode.getUri()))//
                .collect(Collectors.toList());

        List<RedisClusterNode> withoutSpecification = LettuceLists.newList(clusterNodes)//
                .stream()//
                .filter(redisClusterNode -> !fixedOrderList.contains(redisClusterNode.getUri()))//
                .collect(Collectors.toList());

        Collections.sort(withOrderSpecification, new PredefinedRedisClusterNodeComparator(fixedOrderList));
        Collections.sort(withoutSpecification, (o1, o2) -> RedisURIComparator.INSTANCE.compare(o1.getUri(), o2.getUri()));

        withOrderSpecification.addAll(withoutSpecification);

        return withOrderSpecification;
    }

    /**
     * Sort partitions by RedisURI.
     *
     * @param clusterNodes
     * @return List containing {@link RedisClusterNode}s ordered by {@link RedisURI}
     */
    public static List<RedisClusterNode> sortByUri(Iterable<RedisClusterNode> clusterNodes) {

        LettuceAssert.notNull(clusterNodes, "Cluster nodes must not be null");

        List<RedisClusterNode> ordered = LettuceLists.newList(clusterNodes);
        Collections.sort(ordered, (o1, o2) -> RedisURIComparator.INSTANCE.compare(o1.getUri(), o2.getUri()));
        return ordered;
    }

    /**
     * Sort partitions by client count.
     *
     * @param clusterNodes
     * @return List containing {@link RedisClusterNode}s ordered by client count
     */
    public static List<RedisClusterNode> sortByClientCount(Iterable<RedisClusterNode> clusterNodes) {

        LettuceAssert.notNull(clusterNodes, "Cluster nodes must not be null");

        List<RedisClusterNode> ordered = LettuceLists.newList(clusterNodes);
        Collections.sort(ordered, ClientCountComparator.INSTANCE);
        return ordered;
    }

    /**
     * Sort partitions by latency.
     *
     * @param clusterNodes
     * @return List containing {@link RedisClusterNode}s ordered by latency
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

    static class PredefinedRedisClusterNodeComparator implements Comparator<RedisClusterNode> {
        private final List<RedisURI> fixedOrder;

        public PredefinedRedisClusterNodeComparator(List<RedisURI> fixedOrder) {
            this.fixedOrder = fixedOrder;
        }

        @Override
        public int compare(RedisClusterNode o1, RedisClusterNode o2) {

            int index1 = fixedOrder.indexOf(o1.getUri());
            int index2 = fixedOrder.indexOf(o2.getUri());

            return Integer.compare(index1, index2);
        }
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
