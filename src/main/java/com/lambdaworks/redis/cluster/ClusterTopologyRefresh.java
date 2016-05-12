package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.RedisClusterClient.applyUriConnectionSettings;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.internal.LettuceSets;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

import com.lambdaworks.redis.resource.SocketAddressResolver;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility to refresh the cluster topology view based on {@link Partitions}.
 * 
 * @author Mark Paluch
 */
class ClusterTopologyRefresh {

    private static final Utf8StringCodec CODEC = new Utf8StringCodec();
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterTopologyRefresh.class);
    private final RedisClusterClient client;

    public ClusterTopologyRefresh(RedisClusterClient client) {
        this.client = client;
    }

    /**
     * Check if properties changed which are essential for cluster operations.
     * 
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return {@literal true} if {@code MASTER} or {@code SLAVE} flags changed or the responsible slots changed.
     */
    static boolean isChanged(Partitions o1, Partitions o2) {

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
     * Sort partitions by RedisURI.
     * 
     * @param clusterNodes
     * @return List containing {@link RedisClusterNode}s ordered by {@link RedisURI}
     */
    static List<RedisClusterNode> sortByUri(Iterable<RedisClusterNode> clusterNodes) {
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
    static List<RedisClusterNode> sortByClientCount(Iterable<RedisClusterNode> clusterNodes) {
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
    static List<RedisClusterNode> sortByLatency(Iterable<RedisClusterNode> clusterNodes) {
        List<RedisClusterNode> ordered = LettuceLists.newList(clusterNodes);
        Collections.sort(ordered, LatencyComparator.INSTANCE);
        return ordered;
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
     * Load partition views from a collection of {@link RedisURI}s and return the view per {@link RedisURI}. Partitions contain
     * an ordered list of {@link RedisClusterNode}s. The sort key is the latency. Nodes with lower latency come first.
     * 
     * @param seed collection of {@link RedisURI}s
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    public Map<RedisURI, Partitions> loadViews(Iterable<RedisURI> seed) {

        Map<RedisURI, StatefulRedisConnection<String, String>> connections = getConnections(seed);
        Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews = requestViews(connections);
        Map<RedisURI, AsyncCommand<String, String, String>> rawClients = requestClients(connections);

        try {
            Map<RedisURI, Partitions> nodeSpecificViews = getNodeSpecificViews(rawViews, rawClients);

            Set<RedisURI> allKnownUris = nodeSpecificViews.values().stream().flatMap(Collection::stream)
                    .map(RedisClusterNode::getUri).collect(Collectors.toSet());

            Set<RedisURI> discoveredNodes = difference(allKnownUris, connections.keySet());
            if (!discoveredNodes.isEmpty()) {
                RedisURI firstUri = seed.iterator().next();
                discoveredNodes.stream().forEach(redisURI -> applyUriConnectionSettings(firstUri, redisURI));

                Map<RedisURI, StatefulRedisConnection<String, String>> discoveredNodesConnections = getConnections(
                        discoveredNodes);
                connections.putAll(discoveredNodesConnections);
                rawViews.putAll(requestViews(discoveredNodesConnections));
                rawClients.putAll(requestClients(discoveredNodesConnections));
                nodeSpecificViews = getNodeSpecificViews(rawViews, rawClients);
            }

            return nodeSpecificViews;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } finally {
            close(connections);
        }

    }

    protected Map<RedisURI, Partitions> getNodeSpecificViews(Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews,
            Map<RedisURI, AsyncCommand<String, String, String>> rawClients) throws InterruptedException {
        Map<RedisURI, Partitions> nodeSpecificViews = new TreeMap<>(RedisUriComparator.INSTANCE);
        List<RedisClusterNodeSnapshot> allNodes = new ArrayList<>();

        long timeout = client.getFirstUri().getUnit().toNanos(client.getFirstUri().getTimeout());

        Map<String, Long> latencies = new HashMap<>();
        Map<String, Integer> clientCountByNodeId = new HashMap<>();
        Map<RedisURI, Integer> clientCountByRedisUri = new HashMap<>();

        long waitTime = await(rawViews, timeout);
        await(rawClients, timeout - waitTime);

        for (Map.Entry<RedisURI, AsyncCommand<String, String, String>> entry : rawClients.entrySet()) {
            RedisFuture<String> future = entry.getValue();
            try {
                if (!future.isDone() || future.isCancelled()) {
                    continue;
                }
                clientCountByRedisUri.put(entry.getKey(), getClients(future.get()));
            } catch (ExecutionException e) {
                logger.warn("Cannot retrieve CLIENT LIST from " + entry.getKey() + ", error: " + e.toString());
            }
        }

        for (Map.Entry<RedisURI, TimedAsyncCommand<String, String, String>> entry : rawViews.entrySet()) {
            RedisFuture<String> future = entry.getValue();
            if (!future.isDone() || future.isCancelled()) {
                break;
            }
            try {

                String raw = future.get();
                Partitions partitions = ClusterPartitionParser.parse(raw);
                List<RedisClusterNodeSnapshot> nodeWithStats = partitions.stream().map(n -> new RedisClusterNodeSnapshot(n))
                        .collect(Collectors.toList());
                List<RedisClusterNodeSnapshot> badNodes = new ArrayList<>();

                for (RedisClusterNodeSnapshot partition : nodeWithStats) {
                    if (partition.getFlags().contains(RedisClusterNode.NodeFlag.NOADDR)) {
                        badNodes.add(partition);
                    }
                    if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                        partition.setUri(entry.getKey());

                        // record latency for later partition ordering
                        latencies.put(partition.getNodeId(), entry.getValue().duration());
                        clientCountByNodeId.put(partition.getNodeId(), clientCountByRedisUri.get(entry.getKey()));
                    }
                }

                if (!badNodes.isEmpty()) {
                    nodeWithStats.removeAll(badNodes);
                }

                allNodes.addAll(nodeWithStats);
                partitions.clear();
                partitions.addAll(nodeWithStats);
                nodeSpecificViews.put(entry.getKey(), partitions);
            } catch (ExecutionException e) {
                logger.warn("Cannot retrieve partition view from " + entry.getKey() + ", error: " + e.toString());
            }
        }

        for (RedisClusterNodeSnapshot node : allNodes) {
            node.setConnectedClients(clientCountByNodeId.get(node.getNodeId()));
            node.setLatencyNs(latencies.get(node.getNodeId()));
        }

        for (Partitions redisClusterNodes : nodeSpecificViews.values()) {
            Collections.sort(redisClusterNodes.getPartitions(), LatencyComparator.INSTANCE);
        }

        return nodeSpecificViews;
    }

    private long await(Map<RedisURI, ? extends RedisFuture<?>> rawViews, long timeout) throws InterruptedException {
        long waitTime = 0;

        for (Map.Entry<RedisURI, ? extends RedisFuture<?>> entry : rawViews.entrySet()) {
            long timeoutLeft = timeout - waitTime;

            if (timeoutLeft <= 0) {
                break;
            }

            long startWait = System.nanoTime();
            RedisFuture<?> future = entry.getValue();

            if (!future.await(timeoutLeft, TimeUnit.NANOSECONDS)) {
                break;
            }

            waitTime += System.nanoTime() - startWait;
        }
        return waitTime;
    }

    private int getClients(String rawClientsOutput) {
        return rawClientsOutput.trim().split("\\n").length;
    }

    /*
     * Async request of CLUSTER NODES.
     */
    @SuppressWarnings("unchecked")
    private Map<RedisURI, TimedAsyncCommand<String, String, String>> requestViews(
            Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews = new TreeMap<>(RedisUriComparator.INSTANCE);
        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : connections.entrySet()) {

            TimedAsyncCommand<String, String, String> timed = createClusterNodesCommand();

            entry.getValue().dispatch(timed);
            rawViews.put(entry.getKey(), timed);
        }
        return rawViews;
    }

    /*
     * Async request of CLIENT LIST.
     */
    @SuppressWarnings("unchecked")
    private Map<RedisURI, AsyncCommand<String, String, String>> requestClients(
            Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        Map<RedisURI, AsyncCommand<String, String, String>> rawViews = new HashMap<>();
        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : connections.entrySet()) {

            CommandArgs<String, String> args = new CommandArgs<>(CODEC).add(CommandKeyword.LIST);
            Command<String, String, String> command = new Command<>(CommandType.CLIENT, new StatusOutput<>(CODEC), args);
            AsyncCommand<String, String, String> async = new AsyncCommand<>(command);

            entry.getValue().dispatch(async);
            rawViews.put(entry.getKey(), async);
        }
        return rawViews;
    }

    protected TimedAsyncCommand<String, String, String> createClusterNodesCommand() {
        CommandArgs<String, String> args = new CommandArgs<>(CODEC).add(CommandKeyword.NODES);
        Command<String, String, String> command = new Command<>(CommandType.CLUSTER, new StatusOutput<>(CODEC), args);
        return new TimedAsyncCommand<>(command);
    }

    private void close(Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        for (StatefulRedisConnection<String, String> connection : connections.values()) {
            connection.close();
        }
    }

    /*
     * Open connections where an address can be resolved.
     */
    private Map<RedisURI, StatefulRedisConnection<String, String>> getConnections(Iterable<RedisURI> seed) {
        Map<RedisURI, StatefulRedisConnection<String, String>> connections = new TreeMap<>(RedisUriComparator.INSTANCE);

        for (RedisURI redisURI : seed) {
            if (redisURI.getHost() == null) {
                continue;
            }

            try {
                SocketAddress socketAddress = SocketAddressResolver.resolve(redisURI, client.getResources().dnsResolver());
                StatefulRedisConnection<String, String> connection = client.connectToNode(socketAddress);
                if (redisURI.getPassword() != null && redisURI.getPassword().length != 0) {
                    connection.sync().auth(new String(redisURI.getPassword()));
                }
                connection.async().clientSetname("lettuce#ClusterTopologyRefresh");
                connections.put(redisURI, connection);
            } catch (RedisConnectionException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                } else {
                    logger.warn(e.getMessage());
                }
            } catch (RuntimeException e) {
                logger.warn("Cannot connect to " + redisURI, e);
            }
        }
        return connections;
    }

    /**
     * Resolve a {@link RedisURI} from a map of cluster views by {@link Partitions} as key
     * 
     * @param map the map
     * @param partitions the key
     * @return a {@link RedisURI} or null
     */
    protected RedisURI getViewedBy(Map<RedisURI, Partitions> map, Partitions partitions) {

        for (Map.Entry<RedisURI, Partitions> entry : map.entrySet()) {
            if (entry.getValue() == partitions) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Compare {@link RedisURI} based on their host and port representation.
     */
    static class RedisUriComparator implements Comparator<RedisURI> {

        public final static RedisUriComparator INSTANCE = new RedisUriComparator();

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

    /**
     * Compare {@link RedisClusterNodeSnapshot} based on their client count. Lowest comes first. Objects of type
     * {@link RedisClusterNode} cannot be compared and yield to a result of {@literal 0}.
     */
    static class ClientCountComparator implements Comparator<RedisClusterNode> {

        public final static ClientCountComparator INSTANCE = new ClientCountComparator();

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
     * Compare {@link RedisClusterNodeSnapshot} based on their latency. Lowest comes first. Objects of type
     * {@link RedisClusterNode} cannot be compared and yield to a result of {@literal 0}.
     */
    static class LatencyComparator implements Comparator<RedisClusterNode> {

        public final static LatencyComparator INSTANCE = new LatencyComparator();

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

    private static <E> Set<E> difference(Set<E> set1, Set<E> set2) {

        Set<E> result = new HashSet<>();

        for (E e : set1) {
            if (!set2.contains(e)) {
                result.add(e);
            }
        }

        for (E e : set2) {
            if (!set1.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Timed command that records the time at which the command was encoded and completed.
     * 
     * @param <K> Key type
     * @param <V> Value type
     * @param <T> Result type
     */
    static class TimedAsyncCommand<K, V, T> extends AsyncCommand<K, V, T> {

        long encodedAtNs = -1;
        long completedAtNs = -1;

        public TimedAsyncCommand(RedisCommand<K, V, T> command) {
            super(command);
        }

        @Override
        public void encode(ByteBuf buf) {
            completedAtNs = -1;
            encodedAtNs = -1;

            super.encode(buf);
            encodedAtNs = System.nanoTime();
        }

        @Override
        public void complete() {
            completedAtNs = System.nanoTime();
            super.complete();
        }

        public long duration() {
            if (completedAtNs == -1 || encodedAtNs == -1) {
                return -1;
            }
            return completedAtNs - encodedAtNs;
        }
    }

    static class RedisClusterNodeSnapshot extends RedisClusterNode {

        private Long latencyNs;
        private Integer connectedClients;

        RedisClusterNodeSnapshot() {
        }

        RedisClusterNodeSnapshot(RedisClusterNode redisClusterNode) {
            super(redisClusterNode);
        }

        Long getLatencyNs() {
            return latencyNs;
        }

        void setLatencyNs(Long latencyNs) {
            this.latencyNs = latencyNs;
        }

        Integer getConnectedClients() {
            return connectedClients;
        }

        void setConnectedClients(Integer connectedClients) {
            this.connectedClients = connectedClients;
        }
    }
}
