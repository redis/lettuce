package com.lambdaworks.redis.cluster;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class ClusterTopologyRefresh {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterTopologyRefresh.class);
    private RedisClusterClient client;

    public ClusterTopologyRefresh(RedisClusterClient client) {
        this.client = client;
    }

    public boolean isChanged(Partitions active, Partitions mostRecent) {

        if (active.size() != mostRecent.size()) {
            return true;
        }

        for (RedisClusterNode base : mostRecent) {

            if (!essentiallyEqualsTo(base, active.getPartitionByNodeId(base.getNodeId()))) {
                return true;
            }
        }

        return false;
    }

    protected boolean essentiallyEqualsTo(RedisClusterNode base, RedisClusterNode other) {

        if (other == null) {
            return false;
        }

        if (!sameFlags(base, other, RedisClusterNode.NodeFlag.MASTER)) {
            return false;
        }

        if (!sameFlags(base, other, RedisClusterNode.NodeFlag.SLAVE)) {
            return false;
        }

        if (!Sets.newHashSet(base.getSlots()).equals(Sets.newHashSet(other.getSlots()))) {
            return false;
        }

        return true;
    }

    protected boolean sameFlags(RedisClusterNode base, RedisClusterNode other, RedisClusterNode.NodeFlag flag) {
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

    public Map<RedisURI, Partitions> loadViews(Collection<RedisURI> seed) {

        Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections = getConnections(seed);
        Map<RedisURI, RedisFuture<String>> rawViews = requestViews(connections);
        Map<RedisURI, Partitions> nodeSpecificViews = getNodeSpecificViews(rawViews);
        close(connections);

        return nodeSpecificViews;
    }

    protected Map<RedisURI, Partitions> getNodeSpecificViews(Map<RedisURI, RedisFuture<String>> rawViews) {
        Map<RedisURI, Partitions> nodeSpecificViews = Maps.newHashMap();
        long timeout = client.getFirstUri().getUnit().toNanos(client.getFirstUri().getTimeout());
        long waitTime = 0;
        for (Map.Entry<RedisURI, RedisFuture<String>> entry : rawViews.entrySet()) {
            long timeoutLeft = timeout - waitTime;

            if (timeoutLeft <= 0) {
                break;
            }

            long startWait = System.nanoTime();
            RedisFuture<String> future = entry.getValue();
            if (!future.await(timeoutLeft, TimeUnit.NANOSECONDS)) {
                break;
            }
            waitTime += System.nanoTime() - startWait;

            try {
                String raw = future.get();
                Partitions partitions = ClusterPartitionParser.parse(raw);

                for (RedisClusterNode partition : partitions) {
                    if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                        partition.setUri(entry.getKey());
                    }
                }

                nodeSpecificViews.put(entry.getKey(), partitions);
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RedisCommandInterruptedException(e);
            } catch (ExecutionException e) {
                logger.warn("Cannot retrieve partition view from " + entry.getKey(), e);
            }
        }
        return nodeSpecificViews;
    }

    protected Map<RedisURI, RedisFuture<String>> requestViews(
            Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections) {
        Map<RedisURI, RedisFuture<String>> rawViews = Maps.newHashMap();
        for (Map.Entry<RedisURI, RedisAsyncConnectionImpl<String, String>> entry : connections.entrySet()) {
            rawViews.put(entry.getKey(), entry.getValue().clusterNodes());
        }
        return rawViews;
    }

    protected void close(Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections) {
        for (RedisAsyncConnectionImpl<String, String> connection : connections.values()) {
            connection.close();
        }
    }

    protected Map<RedisURI, RedisAsyncConnectionImpl<String, String>> getConnections(Collection<RedisURI> seed) {
        Map<RedisURI, RedisAsyncConnectionImpl<String, String>> connections = Maps.newHashMap();

        for (RedisURI redisURI : seed) {
            if (redisURI.getResolvedAddress() == null) {
                continue;
            }

            try {
                RedisAsyncConnectionImpl<String, String> connection = client.connectAsyncImpl(redisURI.getResolvedAddress());
                connections.put(redisURI, connection);
            } catch (RuntimeException e) {
                logger.warn("Cannot connect to " + redisURI, e);
            }
        }
        return connections;
    }

    public RedisURI getViewedBy(Map<RedisURI, Partitions> map, Partitions partitions) {

        for (Map.Entry<RedisURI, Partitions> entry : map.entrySet()) {
            if (entry.getValue() == partitions) {
                return entry.getKey();
            }
        }

        return null;
    }
}
