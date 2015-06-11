package com.lambdaworks.redis.cluster;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import io.netty.channel.ChannelHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@ChannelHandler.Sharable
public class RedisAdvancedClusterConnectionImpl<K, V> extends RedisAsyncConnectionImpl<K, V> implements
        RedisAdvancedClusterConnection<K, V> {

    private Partitions partitions;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public RedisAdvancedClusterConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, codec, timeout, unit);
    }

    @Override
    public NodeSelectionAsyncOperations<K, V> nodes(Predicate<RedisClusterNode> predicate) {
        return nodes(predicate, false);
    }

    @Override
    public NodeSelectionAsyncOperations<K, V> nodes(Predicate<RedisClusterNode> predicate, boolean dynamic) {

        NodeSelection<K, V> selection = new StaticNodeSelection<>(this, predicate);
        NodeSelectionInvocationHandler h = new NodeSelectionInvocationHandler(selection);
        return (NodeSelectionAsyncOperations<K, V>) Proxy.newProxyInstance(NodeSelection.class.getClassLoader(),
                new Class[] { NodeSelectionAsyncOperations.class }, h);
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
    public Partitions getPartitions() {
        return partitions;
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }
}
