/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
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

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.async.RedisServerAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
public class ClusterRule implements TestRule {

    private RedisClusterClient clusterClient;
    private int[] ports;
    private Map<Integer, RedisAsyncCommands<String, String>> connectionCache = new HashMap<>();

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
            public void evaluate() {
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
                e.printStackTrace();
                // nothing to do
            }
        }

        return true;
    }

    /**
     * Flush data on all nodes, ignore failures.
     */
    private void flushdb() {
        onAllConnections(c -> c.flushdb(), true);
    }

    /**
     * Cluster reset on all nodes.
     */
    public void clusterReset() {
        onAllConnections(RedisServerAsyncCommands::flushall, true);
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

        List<Future<?>> futures = new ArrayList<>();
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
