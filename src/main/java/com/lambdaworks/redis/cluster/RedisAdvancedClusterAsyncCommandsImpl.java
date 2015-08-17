package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode.NodeFlag.MASTER;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.AbstractRedisAsyncCommands;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.async.RedisScriptingAsyncCommands;
import com.lambdaworks.redis.api.async.RedisServerAsyncCommands;
import com.lambdaworks.redis.cluster.api.NodeSelection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.AsyncNodeSelection;
import com.lambdaworks.redis.cluster.api.async.NodeSelectionAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.IntegerOutput;
import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * An advanced asynchronous and thread-safe API for a Redis Cluster connection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public class RedisAdvancedClusterAsyncCommandsImpl<K, V> extends AbstractRedisAsyncCommands<K, V> implements
        RedisAdvancedClusterAsyncConnection<K, V>, RedisAdvancedClusterAsyncCommands<K, V> {

    private Random random = new Random();

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
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.del(keys);
        }

        Map<Integer, RedisFuture<Long>> executions = Maps.newHashMap();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> del = super.del(entry.getValue());
            executions.put(entry.getKey(), del);
        }

        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<List<V>> mget(K... keys) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.mget(keys);
        }

        Map<K, Integer> slots = SlotHash.getSlots(partitioned);
        Map<Integer, RedisFuture<List<V>>> executions = Maps.newHashMap();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<List<V>> mget = super.mget(entry.getValue());
            executions.put(entry.getKey(), mget);
        }

        // restore order of key
        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            List<V> result = Lists.newArrayList();
            for (K opKey : keys) {
                int slot = slots.get(opKey);

                int position = partitioned.get(slot).indexOf(opKey);
                RedisFuture<List<V>> listRedisFuture = executions.get(slot);
                result.add(MultiNodeExecution.execute(() -> listRedisFuture.get().get(position)));
            }

            return result;
        });
    }

    @Override
    public RedisFuture<Long> mget(ValueStreamingChannel<V> channel, K... keys) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.mget(channel, keys);
        }

        Map<Integer, RedisFuture<Long>> executions = Maps.newHashMap();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> del = super.mget(channel, entry.getValue());
            executions.put(entry.getKey(), del);
        }

        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<String> mset(Map<K, V> map) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, map.keySet());

        if (partitioned.size() < 2) {
            return super.mset(map);
        }

        Map<Integer, RedisFuture<String>> executions = Maps.newHashMap();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

            Map<K, V> op = Maps.newHashMap();
            entry.getValue().forEach(k -> op.put(k, map.get(k)));

            RedisFuture<String> mset = super.mset(op);
            executions.put(entry.getKey(), mset);
        }

        return MultiNodeExecution.firstOfAsync(executions);
    }

    @Override
    public RedisFuture<Boolean> msetnx(Map<K, V> map) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, map.keySet());

        if (partitioned.size() < 2) {
            return super.msetnx(map);
        }

        Map<Integer, RedisFuture<Boolean>> executions = Maps.newHashMap();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

            Map<K, V> op = Maps.newHashMap();
            entry.getValue().forEach(k -> op.put(k, map.get(k)));

            RedisFuture<Boolean> msetnx = super.msetnx(op);
            executions.put(entry.getKey(), msetnx);
        }

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            for (RedisFuture<Boolean> listRedisFuture : executions.values()) {
                Boolean b = MultiNodeExecution.execute(() -> listRedisFuture.get());
                if (b != null && b) {
                    return true;
                }
            }

            return false;
        });
    }

    @Override
    public RedisFuture<String> clientSetname(K name) {
        Map<String, RedisFuture<String>> executions = Maps.newHashMap();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {
            RedisClusterAsyncCommands<K, V> byNodeId = getConnection(redisClusterNode.getNodeId());
            if (byNodeId.isOpen()) {
                executions.put("NodeId: " + redisClusterNode.getNodeId(), byNodeId.clientSetname(name));
            }

            RedisURI uri = redisClusterNode.getUri();
            RedisClusterAsyncCommands<K, V> byHost = getConnection(uri.getHost(), uri.getPort());
            if (byHost.isOpen()) {
                executions.put("HostAndPort: " + redisClusterNode.getNodeId(), byHost.clientSetname(name));
            }
        }

        return MultiNodeExecution.firstOfAsync(executions);
    }

    @Override
    public RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count) {
        RedisClusterAsyncCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterGetKeysInSlot(slot, count);
        }

        return super.clusterGetKeysInSlot(slot, count);
    }

    @Override
    public RedisFuture<Long> clusterCountKeysInSlot(int slot) {
        RedisClusterAsyncCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterCountKeysInSlot(slot);
        }

        return super.clusterCountKeysInSlot(slot);
    }

    @Override
    public RedisFuture<Long> dbsize() {
        Map<String, RedisFuture<Long>> executions = executeOnMasters(RedisServerAsyncCommands::dbsize);
        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<String> flushall() {
        Map<String, RedisFuture<String>> executions = executeOnMasters(RedisServerAsyncCommands::flushall);
        return MultiNodeExecution.firstOfAsync(executions);
    }

    @Override
    public RedisFuture<String> flushdb() {
        Map<String, RedisFuture<String>> executions = executeOnMasters(RedisServerAsyncCommands::flushdb);
        return MultiNodeExecution.firstOfAsync(executions);
    }

    @Override
    public RedisFuture<String> scriptFlush() {
        Map<String, RedisFuture<String>> executions = executeOnNodes(RedisScriptingAsyncCommands::scriptFlush,
                redisClusterNode -> true);
        return MultiNodeExecution.firstOfAsync(executions);
    }

    @Override
    public RedisFuture<String> scriptKill() {
        Map<String, RedisFuture<String>> executions = executeOnNodes(RedisScriptingAsyncCommands::scriptFlush,
                redisClusterNode -> true);
        return MultiNodeExecution.alwaysOkOfAsync(executions);
    }

    @Override
    public RedisFuture<V> randomkey() {

        Partitions partitions = getStatefulConnection().getPartitions();
        int index = random.nextInt(partitions.size());

        RedisClusterAsyncCommands<K, V> connection = getConnection(partitions.getPartition(index).getNodeId());
        return connection.randomkey();
    }

    @Override
    public RedisFuture<List<K>> keys(K pattern) {
        Map<String, RedisFuture<List<K>>> executions = executeOnMasters(commands -> commands.keys(pattern));

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            List<K> result = Lists.newArrayList();
            for (RedisFuture<List<K>> future : executions.values()) {
                result.addAll(MultiNodeExecution.execute(() -> future.get()));
            }
            return result;
        });
    }

    @Override
    public RedisFuture<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        Map<String, RedisFuture<Long>> executions = executeOnMasters(commands -> commands.keys(channel, pattern));
        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public void shutdown(boolean save) {

        executeOnNodes(commands -> {
            commands.shutdown(save);

            Command<K, V, Long> command = new Command<>(CommandType.SHUTDOWN, new IntegerOutput<>(codec), null);
            AsyncCommand<K, V, Long> async = new AsyncCommand<K, V, Long>(command);
            async.complete();
            return async;
        }, redisClusterNode -> true);
    }

    /**
     * Run a command on all available masters,
     * 
     * @param function function producing the command
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, RedisFuture<T>> executeOnMasters(
            Function<RedisClusterAsyncCommands<K, V>, RedisFuture<T>> function) {
        return executeOnNodes(function, redisClusterNode -> redisClusterNode.is(MASTER));
    }

    /**
     * Run a command on all available nodes that match {@code filter}.
     * 
     * @param function function producing the command
     * @param filter filter function for the node selection
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, RedisFuture<T>> executeOnNodes(
            Function<RedisClusterAsyncCommands<K, V>, RedisFuture<T>> function, Function<RedisClusterNode, Boolean> filter) {
        Map<String, RedisFuture<T>> executions = Maps.newHashMap();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {

            if (!filter.apply(redisClusterNode)) {
                continue;
            }

            RedisURI uri = redisClusterNode.getUri();
            RedisClusterAsyncCommands<K, V> connection = getConnection(uri.getHost(), uri.getPort());
            if (connection.isOpen()) {
                executions.put(redisClusterNode.getNodeId(), function.apply(connection));
            }
        }
        return executions;
    }

    private RedisClusterAsyncCommands<K, V> findConnectionBySlot(int slot) {
        RedisClusterNode node = getStatefulConnection().getPartitions().getPartitionBySlot(slot);
        if (node != null) {
            return getConnection(node.getUri().getHost(), node.getUri().getPort());
        }

        return null;
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
        return (AsyncNodeSelection<K, V>) Proxy.newProxyInstance(NodeSelection.class.getClassLoader(), new Class<?>[] {
                NodeSelectionAsyncCommands.class, AsyncNodeSelection.class }, h);
    }
}
