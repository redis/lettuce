package com.lambdaworks.redis.cluster;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.CommandOutput;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

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

    private RedisClusterClient client;

    public ClusterTopologyRefresh(RedisClusterClient client) {
        this.client = client;
    }

    /**
     * Load partition views from a collection of {@link RedisURI}s and return the view per {@link RedisURI}. Partitions contain
     * an ordered list of {@link RedisClusterNode}s. The sort key is the latency. Nodes with lower latency come first.
     * 
     * @param seed collection of {@link RedisURI}s
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    public Map<RedisURI, Partitions> loadViews(Iterable<RedisURI> seed) {

        Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections = getConnections(seed);
        Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews = requestViews(connections);
        Map<RedisURI, Partitions> nodeSpecificViews = getNodeSpecificViews(rawViews);
        close(connections);

        return nodeSpecificViews;
    }

    protected Map<RedisURI, Partitions> getNodeSpecificViews(Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews) {
        Map<RedisURI, Partitions> nodeSpecificViews = Maps.newTreeMap(RedisUriComparator.INSTANCE);
        long timeout = client.getFirstUri().getUnit().toNanos(client.getFirstUri().getTimeout());
        long waitTime = 0;
        Map<String, Long> latencies = Maps.newHashMap();

        for (Map.Entry<RedisURI, TimedAsyncCommand<String, String, String>> entry : rawViews.entrySet()) {
            long timeoutLeft = timeout - waitTime;

            if (timeoutLeft <= 0) {
                break;
            }

            long startWait = System.nanoTime();
            RedisFuture<String> future = entry.getValue();

            try {

                if (!future.await(timeoutLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
                waitTime += System.nanoTime() - startWait;

                String raw = future.get();
                if (future.getError() != null) {
                    throw new ExecutionException(new RedisCommandExecutionException(future.getError()));
                }

                Partitions partitions = ClusterPartitionParser.parse(raw);
                List<RedisClusterNode> badNodes = Lists.newArrayList();

                for (RedisClusterNode partition : partitions) {
                    if (partition.getFlags().contains(RedisClusterNode.NodeFlag.NOADDR)) {
                        badNodes.add(partition);
                    }
                    if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                        partition.setUri(entry.getKey());

                        // record latency for later partition ordering
                        latencies.put(partition.getNodeId(), entry.getValue().duration());
                    }
                }
                if(!badNodes.isEmpty()){
                    partitions.removeAll(badNodes);
                }
                nodeSpecificViews.put(entry.getKey(), partitions);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisCommandInterruptedException(e);
            } catch (ExecutionException e) {
                logger.warn("Cannot retrieve partition view from " + entry.getKey(), e);
            }
        }

        LatencyComparator comparator = new LatencyComparator(latencies);

        for (Partitions redisClusterNodes : nodeSpecificViews.values()) {
            Collections.sort(redisClusterNodes.getPartitions(), comparator);
        }

        return nodeSpecificViews;
    }

    /*
     * Async request of views.
     */
    @SuppressWarnings("unchecked")
    private Map<RedisURI, TimedAsyncCommand<String, String, String>> requestViews(
            Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections) {
        Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews = Maps.newTreeMap(RedisUriComparator.INSTANCE);
        for (Map.Entry<RedisURI, RedisAsyncConnectionImpl<String, String>> entry : connections.entrySet()) {

            TimedAsyncCommand<String, String, String> timed = createClusterNodesCommand();

            entry.getValue().dispatch(timed);
            rawViews.put(entry.getKey(), timed);
        }
        return rawViews;
    }

    protected TimedAsyncCommand<String, String, String> createClusterNodesCommand() {
        CommandArgs<String, String> args = new CommandArgs<String, String>(CODEC).add(CommandKeyword.NODES);
        return new TimedAsyncCommand<String, String, String>(CommandType.CLUSTER, new StatusOutput<String, String>(CODEC), args);
    }

    protected void close(Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections) {
        for (RedisAsyncConnectionImpl<String, String> connection : connections.values()) {
            connection.close();
        }
    }

    /*
     * Open connections where an address can be resolved.
     */
    protected Map<RedisURI, RedisAsyncConnectionImpl<String, String>> getConnections(Iterable<RedisURI> seed) {
        Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections = Maps.newTreeMap(RedisUriComparator.INSTANCE);

        for (RedisURI redisURI : seed) {
            if (redisURI.getHost() == null) {
                continue;
            }

            try {
                SocketAddress socketAddress = SocketAddressResolver.resolve(redisURI, client.getResources().dnsResolver());
                RedisAsyncConnectionImpl<String, String> connection = client.connectAsyncImpl(socketAddress);
                if (redisURI.getPassword() != null) {
                    String password = new String(redisURI.getPassword());
                    if (!"".equals(password.trim())) {
                        connection.auth(password);
                    }
                }
                connections.put(redisURI, connection);
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
     * Timed command that records the time at which the command was encoded and completed.
     * 
     * @param <K> Key type
     * @param <V> Value type
     * @param <T> Result type
     */
    static class TimedAsyncCommand<K, V, T> extends Command<K, V, T> {

        long encodedAtNs = -1;
        long completedAtNs = -1;

        public TimedAsyncCommand(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
            super(type, output, args);
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

    static class LatencyComparator implements Comparator<RedisClusterNode> {

        private final Map<String, Long> latencies;

        public LatencyComparator(Map<String, Long> latencies) {
            this.latencies = latencies;
        }

        @Override
        public int compare(RedisClusterNode o1, RedisClusterNode o2) {

            Long latency1 = latencies.get(o1.getNodeId());
            Long latency2 = latencies.get(o2.getNodeId());

            if (latency1 != null && latency2 != null) {
                return latency1.compareTo(latency2);
            }

            if (latency1 != null && latency2 == null) {
                return -1;
            }

            if (latency1 == null && latency2 != null) {
                return 1;
            }

            return 0;
        }

    }

}
