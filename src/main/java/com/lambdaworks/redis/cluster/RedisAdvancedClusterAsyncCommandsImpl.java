package com.lambdaworks.redis.cluster;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.AbstractRedisAsyncCommands;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.NodeSelection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.AsyncNodeSelection;
import com.lambdaworks.redis.cluster.api.async.NodeSelectionAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Advanced asynchronous Cluster connection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public class RedisAdvancedClusterAsyncCommandsImpl<K, V> extends AbstractRedisAsyncCommands<K, V> implements
        RedisAdvancedClusterAsyncConnection<K, V>, RedisAdvancedClusterAsyncCommands<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterAsyncCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public RedisFuture<Long> del(K... keys) {

        Map<Integer, List<K>> partitioned = Maps.newHashMap();
        Map<Integer, RedisFuture<Long>> executions = Maps.newHashMap();
        partition(partitioned, Arrays.asList(keys), Maps.newHashMap());

        if (partitioned.size() < 2) {
            return super.del(keys);
        }

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> del = super.del(entry.getValue());
            executions.put(entry.getKey(), del);
        }

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            AtomicLong result = new AtomicLong();
            for (RedisFuture<Long> longRedisFuture : executions.values()) {
                Long value = execute(() -> longRedisFuture.get());

                if (value != null) {
                    result.getAndAdd(value);
                }
            }

            return result.get();
        });
    }

    protected void partition(Map<Integer, List<K>> partitioned, Iterable<K> keys, Map<K, Integer> slots) {
        for (K key : keys) {
            int slot = SlotHash.getSlot(codec.encodeKey(key));
            slots.put(key, slot);
            List<K> list = commandPartition(partitioned, slot);
            list.add(key);
        }
    }

    @Override
    public RedisFuture<List<V>> mget(K... keys) {

        Map<Integer, List<K>> partitioned = Maps.newHashMap();
        Map<K, Integer> slots = Maps.newHashMap();
        Map<Integer, RedisFuture<List<V>>> executions = Maps.newHashMap();
        partition(partitioned, Arrays.asList(keys), slots);

        if (partitioned.size() < 2) {
            return super.mget(keys);
        }

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<List<V>> mget = super.mget(entry.getValue());
            executions.put(entry.getKey(), mget);
        }

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            List<V> result = Lists.newArrayList();
            for (K opKey : keys) {
                int slot = slots.get(opKey);

                int position = partitioned.get(slot).indexOf(opKey);
                RedisFuture<List<V>> listRedisFuture = executions.get(slot);
                result.add(execute(() -> listRedisFuture.get().get(position)));
            }

            return result;
        });
    }

    @Override
    public RedisFuture<String> mset(Map<K, V> map) {

        Map<Integer, List<K>> partitioned = Maps.newHashMap();
        Map<K, Integer> slots = Maps.newHashMap();
        Map<Integer, RedisFuture<String>> executions = Maps.newHashMap();
        partition(partitioned, map.keySet(), slots);

        if (partitioned.size() < 2) {
            return super.mset(map);
        }

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

            Map<K, V> op = Maps.newHashMap();
            entry.getValue().forEach(k -> op.put(k, map.get(k)));

            RedisFuture<String> mset = super.mset(op);
            executions.put(entry.getKey(), mset);
        }

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            for (RedisFuture<String> listRedisFuture : executions.values()) {
                return execute(() -> listRedisFuture.get());
            }

            return null;
        });

    }

    @Override
    public RedisFuture<Boolean> msetnx(Map<K, V> map) {

        Map<Integer, List<K>> partitioned = Maps.newHashMap();
        Map<K, Integer> slots = Maps.newHashMap();
        Map<Integer, RedisFuture<Boolean>> executions = Maps.newHashMap();
        partition(partitioned, map.keySet(), slots);

        if (partitioned.size() < 2) {
            return super.msetnx(map);
        }

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

            Map<K, V> op = Maps.newHashMap();
            entry.getValue().forEach(k -> op.put(k, map.get(k)));

            RedisFuture<Boolean> msetnx = super.msetnx(op);
            executions.put(entry.getKey(), msetnx);
        }

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            for (RedisFuture<Boolean> listRedisFuture : executions.values()) {
                Boolean b = execute(() -> listRedisFuture.get());
                if (b != null && b) {
                    return true;
                }
            }

            return false;
        });

    }

    private <T> T execute(Callable<T> function) {
        try {
            return function.call();
        } catch (Exception e) {
            throw new RedisException(e);
        }

    }

    private List<K> commandPartition(Map<Integer, List<K>> partitioned, int slot) {
        if (!partitioned.containsKey(slot)) {
            partitioned.put(slot, Lists.newArrayList());
        }
        return partitioned.get(slot);
    }

    @Override
    public RedisClusterAsyncCommands<K, V> getConnection(String nodeId) {
        return getStatefulConnection().getConnection(nodeId).async();
    }

    @Override
    public RedisClusterAsyncCommands<K, V> getConnection(String host, int port) {
        return getStatefulConnection().getConnection(host, port).async();
    }

    @Override
    public StatefulRedisClusterConnection<K, V> getStatefulConnection() {
        return (StatefulRedisClusterConnection<K, V>) connection;
    }

    @Override
    public AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate) {
        return nodes(predicate, false);
    }

    @Override
    public AsyncNodeSelection<K, V> readonly(Predicate<RedisClusterNode> predicate) {
        return nodes(predicate, ClusterConnectionProvider.Intent.READ, false);
    }

    @Override
    public AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate, boolean dynamic) {
        return nodes(predicate, ClusterConnectionProvider.Intent.WRITE, dynamic);
    }

    @SuppressWarnings("unchecked")
    protected AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate, ClusterConnectionProvider.Intent intent,
            boolean dynamic) {

        NodeSelection<RedisAsyncCommands<K, V>, ?> selection;

        if (dynamic) {
            selection = new DynamicAsyncNodeSelection<>((StatefulRedisClusterConnection<K, V>) getConnection(), predicate,
                    intent);
        } else {
            selection = new StaticAsyncNodeSelection<>((StatefulRedisClusterConnection<K, V>) getConnection(), predicate,
                    intent);
        }

        NodeSelectionInvocationHandler h = new NodeSelectionInvocationHandler((AbstractNodeSelection<?, ?, ?, ?>) selection,
                false);
        return (AsyncNodeSelection<K, V>) Proxy.newProxyInstance(NodeSelection.class.getClassLoader(), new Class[] {
                NodeSelectionAsyncCommands.class, AsyncNodeSelection.class }, h);
    }

}
