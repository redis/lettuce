/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.core.cluster.ClusterScanSupport.*;
import static io.lettuce.core.cluster.NodeSelectionInvocationHandler.ExecutionModel.*;
import static io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import io.lettuce.core.api.async.RedisScriptingAsyncCommands;
import io.lettuce.core.api.async.RedisServerAsyncCommands;
import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.AsyncNodeSelection;
import io.lettuce.core.cluster.api.async.NodeSelectionAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ConnectionIntent;

/**
 * An advanced asynchronous and thread-safe API for a Redis Cluster connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Jon Chambers
 * @author Tihomir Mateev
 * @since 3.3
 */
@SuppressWarnings("unchecked")
public class RedisAdvancedClusterAsyncCommandsImpl<K, V> extends AbstractRedisAsyncCommands<K, V>
        implements RedisAdvancedClusterAsyncCommands<K, V> {

    private final RedisCodec<K, V> codec;

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     * @param parser the implementation of the {@link JsonParser} to use
     * @deprecated since 5.1, use
     *             {@link #RedisAdvancedClusterAsyncCommandsImpl(StatefulRedisClusterConnection, RedisCodec, Supplier)}.
     */
    @Deprecated
    public RedisAdvancedClusterAsyncCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
        this.codec = codec;
    }

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     * @deprecated since 5.1, use
     *             {@link #RedisAdvancedClusterAsyncCommandsImpl(StatefulRedisClusterConnection, RedisCodec, Supplier)}.
     */
    @Deprecated
    public RedisAdvancedClusterAsyncCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.codec = codec;
    }

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     * @param parser the implementation of the {@link JsonParser} to use
     */
    public RedisAdvancedClusterAsyncCommandsImpl(StatefulRedisClusterConnection<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
        this.codec = codec;
    }

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterAsyncCommandsImpl(StatefulRedisClusterConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.codec = codec;
    }

    @Override
    public RedisFuture<String> clientSetname(K name) {

        Map<String, CompletionStage<String>> executions = new HashMap<>();

        CompletableFuture<String> ok = CompletableFuture.completedFuture("OK");

        executions.put("Default", super.clientSetname(name).toCompletableFuture());

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {

            RedisURI uri = redisClusterNode.getUri();

            CompletableFuture<StatefulRedisConnection<K, V>> byNodeId = getStatefulConnection(redisClusterNode.getNodeId());

            executions.put("NodeId: " + redisClusterNode.getNodeId(), byNodeId.thenCompose(c -> {

                if (c.isOpen()) {
                    return c.async().clientSetname(name);
                }
                return ok;
            }));

            CompletableFuture<StatefulRedisConnection<K, V>> byHost = getStatefulConnection(uri.getHost(), uri.getPort());

            executions.put("HostAndPort: " + redisClusterNode.getNodeId(), byHost.thenCompose(c -> {

                if (c.isOpen()) {
                    return c.async().clientSetname(name);
                }
                return ok;
            }));
        }

        return MultiNodeExecution.firstOfAsync(executions);
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
    public RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count) {

        RedisClusterAsyncCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterGetKeysInSlot(slot, count);
        }

        return super.clusterGetKeysInSlot(slot, count);
    }

    @Override
    public RedisFuture<Long> dbsize() {
        return MultiNodeExecution.aggregateAsync(executeOnUpstream(RedisServerAsyncCommands::dbsize));
    }

    @Override
    public RedisFuture<Long> del(K... keys) {
        return del(Arrays.asList(keys));
    }

    @Override
    public RedisFuture<Long> del(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.del(keys);
        }

        Map<Integer, RedisFuture<Long>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> del = super.del(entry.getValue());
            executions.put(entry.getKey(), del);
        }

        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<Long> exists(K... keys) {
        return exists(Arrays.asList(keys));
    }

    public RedisFuture<Long> exists(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.exists(keys);
        }

        Map<Integer, RedisFuture<Long>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> exists = super.exists(entry.getValue());
            executions.put(entry.getKey(), exists);
        }

        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<String> flushall() {
        return MultiNodeExecution.firstOfAsync(executeOnUpstream(RedisServerAsyncCommands::flushall));
    }

    @Override
    public RedisFuture<String> flushall(FlushMode flushMode) {
        return MultiNodeExecution.firstOfAsync(
                executeOnUpstream(kvRedisClusterAsyncCommands -> kvRedisClusterAsyncCommands.flushall(flushMode)));
    }

    @Override
    public RedisFuture<String> flushallAsync() {
        return MultiNodeExecution.firstOfAsync(executeOnUpstream(RedisServerAsyncCommands::flushallAsync));
    }

    @Override
    public RedisFuture<String> flushdb() {
        return MultiNodeExecution.firstOfAsync(executeOnUpstream(RedisServerAsyncCommands::flushdb));
    }

    @Override
    public RedisFuture<String> flushdb(FlushMode flushMode) {
        return MultiNodeExecution
                .firstOfAsync(executeOnUpstream(kvRedisClusterAsyncCommands -> kvRedisClusterAsyncCommands.flushdb(flushMode)));
    }

    @Override
    public RedisFuture<List<K>> keys(K pattern) {

        Map<String, CompletableFuture<List<K>>> executions = executeOnUpstream(commands -> commands.keys(pattern));

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            List<K> result = new ArrayList<>();
            for (CompletableFuture<List<K>> future : executions.values()) {
                result.addAll(MultiNodeExecution.execute(future::get));
            }
            return result;
        });
    }

    @Override
    public RedisFuture<Long> keys(KeyStreamingChannel<K> channel, K pattern) {

        Map<String, CompletableFuture<Long>> executions = executeOnUpstream(commands -> commands.keys(channel, pattern));
        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<List<JsonValue>> jsonMGet(JsonPath jsonPath, K... keys) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.jsonMGet(jsonPath, keys);
        }

        // For a given partition, maps the key to its index within the List<K> in partitioned for faster lookups below
        Map<Integer, Map<K, Integer>> keysToIndexes = mapKeyToIndex(partitioned);
        Map<K, Integer> slots = SlotHash.getSlots(partitioned);
        Map<Integer, RedisFuture<List<JsonValue>>> executions = new HashMap<>(partitioned.size());

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            K[] partitionKeys = entry.getValue().toArray((K[]) new Object[entry.getValue().size()]);
            RedisFuture<List<JsonValue>> jsonMget = super.jsonMGet(jsonPath, partitionKeys);
            executions.put(entry.getKey(), jsonMget);
        }

        // restore order of key
        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            List<JsonValue> result = new ArrayList<>(slots.size());
            for (K opKey : keys) {
                int slot = slots.get(opKey);

                int position = keysToIndexes.get(slot).get(opKey);
                RedisFuture<List<JsonValue>> listRedisFuture = executions.get(slot);
                result.add(MultiNodeExecution.execute(() -> listRedisFuture.get().get(position)));
            }

            return result;
        });
    }

    private Map<Integer, Map<K, Integer>> mapKeyToIndex(Map<Integer, List<K>> partitioned) {
        Map<Integer, Map<K, Integer>> result = new HashMap<>(partitioned.size());
        for (Integer partition : partitioned.keySet()) {
            List<K> keysForPartition = partitioned.get(partition);
            Map<K, Integer> keysToIndexes = new HashMap<>(keysForPartition.size());
            for (int i = 0; i < keysForPartition.size(); i++) {
                keysToIndexes.put(keysForPartition.get(i), i);
            }
            result.put(partition, keysToIndexes);
        }

        return result;
    }

    @Override
    public RedisFuture<List<KeyValue<K, V>>> mget(K... keys) {
        return mget(Arrays.asList(keys));
    }

    @Override
    public RedisFuture<List<KeyValue<K, V>>> mget(Iterable<K> keys) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.mget(keys);
        }

        // For a given partition, maps the key to its index within the List<K> in partitioned for faster lookups below
        Map<Integer, Map<K, Integer>> partitionedKeysToIndexes = mapKeyToIndex(partitioned);
        Map<K, Integer> slots = SlotHash.getSlots(partitioned);
        Map<Integer, RedisFuture<List<KeyValue<K, V>>>> executions = new HashMap<>(partitioned.size());

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<List<KeyValue<K, V>>> mget = super.mget(entry.getValue());
            executions.put(entry.getKey(), mget);
        }

        // restore order of key
        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            List<KeyValue<K, V>> result = new ArrayList<>(slots.size());
            for (K opKey : keys) {
                int slot = slots.get(opKey);

                int position = partitionedKeysToIndexes.get(slot).get(opKey);
                RedisFuture<List<KeyValue<K, V>>> listRedisFuture = executions.get(slot);
                result.add(MultiNodeExecution.execute(() -> listRedisFuture.get().get(position)));
            }

            return result;
        });
    }

    @Override
    public RedisFuture<Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        return mget(channel, Arrays.asList(keys));
    }

    @Override
    public RedisFuture<Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.mget(channel, keys);
        }

        Map<Integer, RedisFuture<Long>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> del = super.mget(channel, entry.getValue());
            executions.put(entry.getKey(), del);
        }

        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<String> jsonMSet(List<JsonMsetArgs<K, V>> arguments) {
        List<K> keys = arguments.stream().map(JsonMsetArgs::getKey).collect(Collectors.toList());
        Map<K, List<JsonMsetArgs<K, V>>> argsPerKey = arguments.stream().collect(Collectors.groupingBy(JsonMsetArgs::getKey));
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.jsonMSet(arguments);
        }

        Map<Integer, RedisFuture<String>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

            List<JsonMsetArgs<K, V>> op = new ArrayList<>();
            entry.getValue().forEach(k -> op.addAll(argsPerKey.get(k)));

            RedisFuture<String> mset = super.jsonMSet(op);
            executions.put(entry.getKey(), mset);
        }

        return MultiNodeExecution.firstOfAsync(executions);
    }

    @Override
    public RedisFuture<String> mset(Map<K, V> map) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, map.keySet());

        if (partitioned.size() < 2) {
            return super.mset(map);
        }

        Map<Integer, RedisFuture<String>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

            Map<K, V> op = new HashMap<>();
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

        Map<Integer, RedisFuture<Boolean>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

            Map<K, V> op = new HashMap<>();
            entry.getValue().forEach(k -> op.put(k, map.get(k)));

            RedisFuture<Boolean> msetnx = super.msetnx(op);
            executions.put(entry.getKey(), msetnx);
        }

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {

            for (RedisFuture<Boolean> listRedisFuture : executions.values()) {
                Boolean b = MultiNodeExecution.execute(() -> listRedisFuture.get());
                if (b == null || !b) {
                    return false;
                }
            }

            return !executions.isEmpty();
        });
    }

    @Override
    public RedisFuture<K> randomkey() {

        Partitions partitions = getStatefulConnection().getPartitions();

        if (partitions.isEmpty()) {
            return super.randomkey();
        }

        int index = ThreadLocalRandom.current().nextInt(partitions.size());
        RedisClusterNode partition = partitions.getPartition(index);

        CompletableFuture<K> future = getConnectionAsync(partition.getUri().getHost(), partition.getUri().getPort())
                .thenCompose(RedisKeyAsyncCommands::randomkey);

        return new PipelinedRedisFuture<>(future);
    }

    @Override
    public RedisFuture<String> scriptFlush() {

        Map<String, CompletableFuture<String>> executions = executeOnNodes(RedisScriptingAsyncCommands::scriptFlush,
                redisClusterNode -> true);
        return MultiNodeExecution.firstOfAsync(executions);
    }

    @Override
    public RedisFuture<String> scriptKill() {

        Map<String, CompletableFuture<String>> executions = executeOnNodes(RedisScriptingAsyncCommands::scriptKill,
                redisClusterNode -> true);
        return MultiNodeExecution.alwaysOkOfAsync(executions);
    }

    @Override
    public RedisFuture<String> scriptLoad(byte[] script) {

        Map<String, CompletableFuture<String>> executions = executeOnNodes(cmd -> cmd.scriptLoad(script),
                redisClusterNode -> true);
        return MultiNodeExecution.lastOfAsync(executions);
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

    @Override
    public RedisFuture<Long> touch(K... keys) {
        return touch(Arrays.asList(keys));
    }

    public RedisFuture<Long> touch(Iterable<K> keys) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.touch(keys);
        }

        Map<Integer, RedisFuture<Long>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> touch = super.touch(entry.getValue());
            executions.put(entry.getKey(), touch);
        }

        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisFuture<Long> unlink(K... keys) {
        return unlink(Arrays.asList(keys));
    }

    @Override
    public RedisFuture<Long> unlink(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.unlink(keys);
        }

        Map<Integer, RedisFuture<Long>> executions = new HashMap<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            RedisFuture<Long> unlink = super.unlink(entry.getValue());
            executions.put(entry.getKey(), unlink);
        }

        return MultiNodeExecution.aggregateAsync(executions);
    }

    @Override
    public RedisClusterAsyncCommands<K, V> getConnection(String nodeId) {
        return getStatefulConnection().getConnection(nodeId).async();
    }

    @Override
    public RedisClusterAsyncCommands<K, V> getConnection(String host, int port) {
        return getStatefulConnection().getConnection(host, port).async();
    }

    private CompletableFuture<StatefulRedisConnection<K, V>> getStatefulConnection(String nodeId) {
        return getConnectionProvider().getConnectionAsync(ConnectionIntent.WRITE, nodeId);
    }

    private CompletableFuture<StatefulRedisConnection<K, V>> getStatefulConnection(String host, int port) {
        return getConnectionProvider().getConnectionAsync(ConnectionIntent.WRITE, host, port);
    }

    private CompletableFuture<RedisClusterAsyncCommands<K, V>> getConnectionAsync(String host, int port) {
        return getConnectionProvider().<K, V> getConnectionAsync(ConnectionIntent.WRITE, host, port)
                .thenApply(StatefulRedisConnection::async);
    }

    @Override
    public StatefulRedisClusterConnection<K, V> getStatefulConnection() {
        return (StatefulRedisClusterConnection<K, V>) super.getConnection();
    }

    @Override
    public AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate) {
        return nodes(predicate, false);
    }

    @Override
    public AsyncNodeSelection<K, V> readonly(Predicate<RedisClusterNode> predicate) {
        return nodes(predicate, ConnectionIntent.READ, false);
    }

    @Override
    public AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate, boolean dynamic) {
        return nodes(predicate, ConnectionIntent.WRITE, dynamic);
    }

    @SuppressWarnings("unchecked")
    protected AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate, ConnectionIntent connectionIntent,
            boolean dynamic) {

        NodeSelectionSupport<RedisAsyncCommands<K, V>, ?> selection;

        StatefulRedisClusterConnectionImpl<K, V> impl = (StatefulRedisClusterConnectionImpl<K, V>) getConnection();
        if (dynamic) {
            selection = new DynamicNodeSelection<RedisAsyncCommands<K, V>, Object, K, V>(
                    impl.getClusterDistributionChannelWriter(), predicate, connectionIntent, StatefulRedisConnection::async);
        } else {
            selection = new StaticNodeSelection<RedisAsyncCommands<K, V>, Object, K, V>(
                    impl.getClusterDistributionChannelWriter(), predicate, connectionIntent, StatefulRedisConnection::async);
        }

        NodeSelectionInvocationHandler h = new NodeSelectionInvocationHandler((AbstractNodeSelection<?, ?, ?, ?>) selection,
                RedisClusterAsyncCommands.class, ASYNC);
        return (AsyncNodeSelection<K, V>) Proxy.newProxyInstance(NodeSelectionSupport.class.getClassLoader(),
                new Class<?>[] { NodeSelectionAsyncCommands.class, AsyncNodeSelection.class }, h);
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan() {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(), asyncClusterKeyScanCursorMapper());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(scanArgs),
                asyncClusterKeyScanCursorMapper());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor, scanArgs),
                asyncClusterKeyScanCursorMapper());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return clusterScan(scanCursor, RedisKeyAsyncCommands::scan, asyncClusterKeyScanCursorMapper());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel),
                asyncClusterStreamScanCursorMapper());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel, scanArgs),
                asyncClusterStreamScanCursorMapper());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor, scanArgs),
                asyncClusterStreamScanCursorMapper());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor),
                asyncClusterStreamScanCursorMapper());
    }

    private <T extends ScanCursor> RedisFuture<T> clusterScan(ScanCursor cursor,
            BiFunction<RedisKeyAsyncCommands<K, V>, ScanCursor, RedisFuture<T>> scanFunction,
            ScanCursorMapper<RedisFuture<T>> resultMapper) {

        return clusterScan(getStatefulConnection(), cursor, scanFunction, resultMapper);
    }

    /**
     * Run a command on all available masters,
     *
     * @param function function producing the command
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, CompletableFuture<T>> executeOnUpstream(
            Function<RedisClusterAsyncCommands<K, V>, RedisFuture<T>> function) {
        return executeOnNodes(function, redisClusterNode -> redisClusterNode.is(UPSTREAM));
    }

    /**
     * Run a command on all available nodes that match {@code filter}.
     *
     * @param function function producing the command
     * @param filter filter function for the node selection
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, CompletableFuture<T>> executeOnNodes(
            Function<RedisClusterAsyncCommands<K, V>, RedisFuture<T>> function, Function<RedisClusterNode, Boolean> filter) {
        Map<String, CompletableFuture<T>> executions = new HashMap<>();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {

            if (!filter.apply(redisClusterNode)) {
                continue;
            }

            RedisURI uri = redisClusterNode.getUri();
            CompletableFuture<RedisClusterAsyncCommands<K, V>> connection = getConnectionAsync(uri.getHost(), uri.getPort());

            executions.put(redisClusterNode.getNodeId(), connection.thenCompose(function::apply));

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

    private AsyncClusterConnectionProvider getConnectionProvider() {

        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) getStatefulConnection().getChannelWriter();
        return (AsyncClusterConnectionProvider) writer.getClusterConnectionProvider();
    }

    /**
     * Perform a SCAN in the cluster.
     *
     */
    static <T extends ScanCursor, K, V> RedisFuture<T> clusterScan(StatefulRedisClusterConnection<K, V> connection,
            ScanCursor cursor, BiFunction<RedisKeyAsyncCommands<K, V>, ScanCursor, RedisFuture<T>> scanFunction,
            ScanCursorMapper<RedisFuture<T>> mapper) {

        List<String> nodeIds = ClusterScanSupport.getNodeIds(connection, cursor);
        String currentNodeId = ClusterScanSupport.getCurrentNodeId(cursor, nodeIds);
        ScanCursor continuationCursor = ClusterScanSupport.getContinuationCursor(cursor);

        RedisFuture<T> scanCursor = scanFunction.apply(connection.getConnection(currentNodeId).async(), continuationCursor);
        return mapper.map(nodeIds, currentNodeId, scanCursor);
    }

}
