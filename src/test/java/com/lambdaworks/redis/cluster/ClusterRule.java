package com.lambdaworks.redis.cluster;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ClusterRule implements TestRule {

    private RedisClusterClient clusterClient;
    private int[] ports;
    private Map<Integer, RedisAsyncCommands<String, String>> connectionCache = Maps.newHashMap();

    public ClusterRule(RedisClusterClient clusterClient, int... ports) {
        this.clusterClient = clusterClient;
        this.ports = ports;

        for (int port : ports) {
            RedisAsyncCommands<String, String> connection = clusterClient.connectToNode(
                    new InetSocketAddress("localhost", port)).async();
            connectionCache.put(port, connection);
        }
    }

    @Override
    public Statement apply(final Statement base, Description description) {

        final Statement beforeCluster = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                flushdb();
            }
        };

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                beforeCluster.evaluate();

                base.evaluate();

            }
        };
    }

    /**
     * 
     * @return true if the cluster state is {@code ok} and there are no failing nodes
     */
    public boolean isStable() {

        for (RedisAsyncCommands<String, String> commands : connectionCache.values()) {
            try {
                RedisCommands<String, String> sync = commands.getStatefulConnection().sync();
                String info = sync.clusterInfo();
                if (info != null && info.contains("cluster_state:ok")) {

                    String s = sync.clusterNodes();
                    Partitions parse = ClusterPartitionParser.parse(s);

                    for (RedisClusterNode redisClusterNode : parse) {
                        if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.FAIL)
                                || redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.EVENTUAL_FAIL)
                                || redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.HANDSHAKE)) {
                            return false;
                        }
                    }

                } else {
                    return false;
                }
            } catch (Exception e) {
                // nothing to do
            }
        }

        return true;
    }

    /**
     * Flush data on all nodes, ignore failures.
     */
    public void flushdb() {
        onAllConnections(c -> c.flushdb(), true);
    }

    /**
     * Cluster reset on all nodes.
     */
    public void clusterReset() {
        onAllConnections(c -> c.clusterReset(true));
        onAllConnections(RedisClusterAsyncCommands::clusterFlushslots);
    }

    /**
     * Meet on all nodes.
     * 
     * @param host
     * @param port
     */
    public void meet(String host, int port) {
        onAllConnections(c -> c.clusterMeet(host, port));
    }

    public RedisClusterClient getClusterClient() {
        return clusterClient;
    }

    @SuppressWarnings("rawtypes")
    private <T> void onAllConnections(Function<RedisClusterAsyncCommands<?, ?>, Future<T>> function) {
        onAllConnections(function, false);
    }

    @SuppressWarnings("rawtypes")
    private <T> void onAllConnections(Function<RedisClusterAsyncCommands<?, ?>, Future<T>> function,
            boolean ignoreExecutionException) {

        List<Future<?>> futures = Lists.newArrayList();
        for (RedisClusterAsyncCommands<?, ?> connection : connectionCache.values()) {
            futures.add(function.apply(connection));
        }

        try {
            await((List) futures, ignoreExecutionException);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }


    private void await(List<Future<?>> futures, boolean ignoreExecutionException) throws InterruptedException,
            java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                if (!ignoreExecutionException) {
                    throw e;
                }
            }
        }
    }
}
