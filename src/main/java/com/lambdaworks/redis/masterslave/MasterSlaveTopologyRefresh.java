package com.lambdaworks.redis.masterslave;

import static com.lambdaworks.redis.masterslave.MasterSlaveUtils.findNodeByUri;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility to refresh the Master-Slave topology view based on {@link RedisNodeDescription}.
 * 
 * @author Mark Paluch
 */
class MasterSlaveTopologyRefresh {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MasterSlaveTopologyRefresh.class);

    private final RedisClient client;
    private final TopologyProvider topologyProvider;

    public MasterSlaveTopologyRefresh(RedisClient client, TopologyProvider topologyProvider) {
        this.client = client;
        this.topologyProvider = topologyProvider;
    }

    /**
     * Load master slave nodes. Result contains an ordered list of {@link RedisNodeDescription}s. The sort key is the latency.
     * Nodes with lower latency come first.
     * 
     * @param seed collection of {@link RedisURI}s
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    public List<RedisNodeDescription> getNodes(RedisURI seed) {

        List<RedisNodeDescription> nodes = topologyProvider.getNodes();

        addPasswordIfNeeded(nodes, seed);

        Map<RedisURI, StatefulRedisConnection<String, String>> connections = getConnections(nodes);
        Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews = requestPing(connections);
        List<RedisNodeDescription> result = getNodeSpecificViews(rawViews, nodes, seed);
        close(connections);

        return result;
    }

    private void addPasswordIfNeeded(List<RedisNodeDescription> nodes, RedisURI seed) {

        if (seed.getPassword() != null && seed.getPassword().length != 0) {
            for (RedisNodeDescription node : nodes) {
                node.getUri().setPassword(new String(seed.getPassword()));
            }
        }
    }

    protected List<RedisNodeDescription> getNodeSpecificViews(
            Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews, List<RedisNodeDescription> nodes, RedisURI seed) {
        List<RedisNodeDescription> result = new ArrayList<>();

        long timeout = seed.getUnit().toNanos(seed.getTimeout());
        long waitTime = 0;
        Map<RedisNodeDescription, Long> latencies = new HashMap<>();

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

                future.get();

                RedisNodeDescription redisNodeDescription = findNodeByUri(nodes, entry.getKey());
                latencies.put(redisNodeDescription, entry.getValue().duration());
                result.add(redisNodeDescription);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisCommandInterruptedException(e);
            } catch (ExecutionException e) {
                logger.warn("Cannot retrieve partition view from " + entry.getKey(), e);
            }
        }

        LatencyComparator comparator = new LatencyComparator(latencies);

        Collections.sort(result, comparator);

        return result;
    }

    /*
     * Async request of views.
     */
    @SuppressWarnings("unchecked")
    private Map<RedisURI, TimedAsyncCommand<String, String, String>> requestPing(
            Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews = new TreeMap<>(RedisUriComparator.INSTANCE);
        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : connections.entrySet()) {

            TimedAsyncCommand<String, String, String> timed = createPingCommand();

            entry.getValue().dispatch(timed);
            rawViews.put(entry.getKey(), timed);
        }
        return rawViews;
    }

    protected TimedAsyncCommand<String, String, String> createPingCommand() {
        CommandArgs<String, String> args = new CommandArgs<>(MasterSlaveUtils.CODEC).add(CommandKeyword.NODES);
        Command<String, String, String> command = new Command<>(CommandType.PING, new StatusOutput<>(MasterSlaveUtils.CODEC),
                args);
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
    private Map<RedisURI, StatefulRedisConnection<String, String>> getConnections(Iterable<RedisNodeDescription> nodes) {
        Map<RedisURI, StatefulRedisConnection<String, String>> connections = new TreeMap<>(RedisUriComparator.INSTANCE);

        for (RedisNodeDescription node : nodes) {

            try {
                StatefulRedisConnection<String, String> connection = client.connect(node.getUri());
                connections.put(node.getUri(), connection);
            } catch (RuntimeException e) {
                logger.warn("Cannot connect to " + node.getUri(), e);
            }
        }
        return connections;
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

    static class LatencyComparator implements Comparator<RedisNodeDescription> {

        private final Map<RedisNodeDescription, Long> latencies;

        public LatencyComparator(Map<RedisNodeDescription, Long> latencies) {
            this.latencies = latencies;
        }

        @Override
        public int compare(RedisNodeDescription o1, RedisNodeDescription o2) {

            Long latency1 = latencies.get(o1);
            Long latency2 = latencies.get(o2);

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
