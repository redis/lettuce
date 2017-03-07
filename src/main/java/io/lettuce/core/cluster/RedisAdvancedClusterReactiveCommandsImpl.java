/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.core.cluster.ClusterScanSupport.reactiveClusterKeyScanCursorMapper;
import static io.lettuce.core.cluster.ClusterScanSupport.reactiveClusterStreamScanCursorMapper;
import static io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag.MASTER;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;

/**
 * An advanced reactive and thread-safe API to a Redis Cluster connection.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class RedisAdvancedClusterReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V>
        implements RedisAdvancedClusterReactiveCommands<K, V> {

    private final Random random = new Random();
    private final RedisCodec<K, V> codec;

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection,
            RedisCodec<K, V> codec) {
        super(connection, codec);
        this.codec = codec;
    }

    @Override
    public Mono<Long> del(K... keys) {
        return del(Arrays.asList(keys));
    }

    @Override
    public Mono<Long> del(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.del(keys);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.del(entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<Long> unlink(K... keys) {
        return unlink(Arrays.asList(keys));
    }

    @Override
    public Mono<Long> unlink(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.unlink(keys);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.unlink(entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<Long> exists(K... keys) {
        return exists(Arrays.asList(keys));
    }

    public Mono<Long> exists(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.exists(keyList);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.exists(entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public Flux<KeyValue<K, V>> mget(K... keys) {
        return mget(Arrays.asList(keys));
    }

    @SuppressWarnings("unchecked")
    public Flux<KeyValue<K, V>> mget(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(keyList);
        }

        List<Publisher<KeyValue<K, V>>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.mget(entry.getValue()));
        }

        Flux<KeyValue<K, V>> fluxes = Flux.concat(publishers);

        Mono<List<KeyValue<K, V>>> map = fluxes.collectList().map(vs -> {

            KeyValue<K, V>[] values = new KeyValue[vs.size()];
            int offset = 0;
            for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

                for (int i = 0; i < keyList.size(); i++) {

                    int index = entry.getValue().indexOf(keyList.get(i));
                    if (index == -1) {
                        continue;
                    }

                    values[i] = vs.get(offset + index);
                }

                offset += entry.getValue().size();
            }

            List<KeyValue<K, V>> objects = new ArrayList<>(Arrays.asList(values));
            return objects;
        });

        return map.flatMap(Flux::fromIterable);
    }

    @Override
    public Mono<Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        return mget(channel, Arrays.asList(keys));
    }

    @Override
    public Mono<Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(channel, keyList);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.mget(channel, entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<Boolean> msetnx(Map<K, V> map) {

        return pipeliningWithMap(map, kvMap -> RedisAdvancedClusterReactiveCommandsImpl.super.msetnx(kvMap).flux(),
                booleanFlux -> booleanFlux).reduce((accu, next) -> accu && next);
    }

    @Override
    public Mono<String> mset(Map<K, V> map) {
        return pipeliningWithMap(map, kvMap -> RedisAdvancedClusterReactiveCommandsImpl.super.mset(kvMap).flux(),
                booleanFlux -> booleanFlux).last();
    }

    @Override
    public Flux<K> clusterGetKeysInSlot(int slot, int count) {
        RedisClusterReactiveCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterGetKeysInSlot(slot, count);
        }

        return super.clusterGetKeysInSlot(slot, count);
    }

    @Override
    public Mono<Long> clusterCountKeysInSlot(int slot) {
        RedisClusterReactiveCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterCountKeysInSlot(slot);
        }

        return super.clusterCountKeysInSlot(slot);
    }

    @Override
    public Mono<String> clientSetname(K name) {
        List<Publisher<String>> publishers = new ArrayList<>();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {
            StatefulRedisConnection<K, V> byNodeId = getStatefulConnection().getConnection(redisClusterNode.getNodeId());
            if (byNodeId.isOpen()) {
                publishers.add(byNodeId.reactive().clientSetname(name));
            }

            StatefulRedisConnection<K, V> byHost = getStatefulConnection().getConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());
            if (byHost.isOpen()) {
                publishers.add(byHost.reactive().clientSetname(name));
            }
        }

        return Flux.merge(publishers).last();
    }

    @Override
    public Mono<Long> dbsize() {
        Map<String, Flux<Long>> publishers = executeOnMasters((commands) -> commands.dbsize().flux());
        return Flux.merge(publishers.values()).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<String> flushall() {
        Map<String, Flux<String>> publishers = executeOnMasters(
                (kvRedisClusterReactiveCommancommandss) -> kvRedisClusterReactiveCommancommandss.flushall().flux());
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<String> flushdb() {
        Map<String, Flux<String>> publishers = executeOnMasters((commands) -> commands.flushdb().flux());
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Flux<K> keys(K pattern) {
        Map<String, Flux<K>> publishers = executeOnMasters(commands -> commands.keys(pattern));
        return Flux.merge(publishers.values());
    }

    @Override
    public Mono<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        Map<String, Flux<Long>> publishers = executeOnMasters(commands -> commands.keys(channel, pattern).flux());
        return Flux.merge(publishers.values()).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<V> randomkey() {

        Partitions partitions = getStatefulConnection().getPartitions();
        int index = random.nextInt(partitions.size());

        RedisClusterReactiveCommands<K, V> connection = getConnection(partitions.getPartition(index).getNodeId());
        return connection.randomkey();
    }

    @Override
    public Mono<String> scriptFlush() {
        Map<String, Flux<String>> publishers = executeOnNodes((commands) -> commands.scriptFlush().flux(),
                redisClusterNode -> true);
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<String> scriptKill() {
        Map<String, Flux<String>> publishers = executeOnNodes((commands) -> commands.scriptFlush().flux(),
                redisClusterNode -> true);
        return Flux.merge(publishers.values()).onErrorReturn("OK").last();
    }

    @Override
    public Mono<Void> shutdown(boolean save) {
        Map<String, Flux<Void>> publishers = executeOnNodes(commands -> commands.shutdown(save).flux(),
                redisClusterNode -> true);
        return Flux.merge(publishers.values()).then();
    }

    @Override
    public Mono<Long> touch(K... keys) {
        return touch(Arrays.asList(keys));
    }

    public Mono<Long> touch(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.touch(keyList);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.touch(entry.getValue()).flux());
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    /**
     * Run a command on all available masters,
     *
     * @param function function producing the command
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, Flux<T>> executeOnMasters(Function<RedisClusterReactiveCommands<K, V>, Flux<T>> function) {
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
    protected <T> Map<String, Flux<T>> executeOnNodes(Function<RedisClusterReactiveCommands<K, V>, Flux<T>> function,
            Function<RedisClusterNode, Boolean> filter) {
        Map<String, Flux<T>> executions = new HashMap<>();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {

            if (!filter.apply(redisClusterNode)) {
                continue;
            }

            RedisURI uri = redisClusterNode.getUri();
            StatefulRedisConnection<K, V> connection = getStatefulConnection().getConnection(uri.getHost(), uri.getPort());
            if (connection.isOpen()) {
                executions.put(redisClusterNode.getNodeId(), function.apply(connection.reactive()));
            }
        }
        return executions;
    }

    private RedisClusterReactiveCommands<K, V> findConnectionBySlot(int slot) {
        RedisClusterNode node = getStatefulConnection().getPartitions().getPartitionBySlot(slot);
        if (node != null) {
            return getConnection(node.getUri().getHost(), node.getUri().getPort());
        }

        return null;
    }

    @Override
    public StatefulRedisClusterConnection<K, V> getStatefulConnection() {
        return (StatefulRedisClusterConnection<K, V>) super.getConnection();
    }

    @Override
    public RedisClusterReactiveCommands<K, V> getConnection(String nodeId) {
        return getStatefulConnection().getConnection(nodeId).reactive();
    }

    @Override
    public RedisClusterReactiveCommands<K, V> getConnection(String host, int port) {
        return getStatefulConnection().getConnection(host, port).reactive();
    }

    @Override
    public Mono<KeyScanCursor<K>> scan() {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(), reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor, scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor), reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor),
                reactiveClusterStreamScanCursorMapper());
    }

    private <T extends ScanCursor> Mono<T> clusterScan(ScanCursor cursor,
            BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Mono<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Mono<T>> resultMapper) {

        return clusterScan(getStatefulConnection(), cursor, scanFunction, (ClusterScanSupport.ScanCursorMapper) resultMapper);
    }

    /**
     * Perform a SCAN in the cluster.
     *
     */
    static <T extends ScanCursor, K, V> Mono<T> clusterScan(StatefulRedisClusterConnection<K, V> connection, ScanCursor cursor,
            BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Mono<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Mono<T>> mapper) {

        List<String> nodeIds = ClusterScanSupport.getNodeIds(connection, cursor);
        String currentNodeId = ClusterScanSupport.getCurrentNodeId(cursor, nodeIds);
        ScanCursor continuationCursor = ClusterScanSupport.getContinuationCursor(cursor);

        Mono<T> scanCursor = scanFunction.apply(connection.getConnection(currentNodeId).reactive(), continuationCursor);
        return mapper.map(nodeIds, currentNodeId, scanCursor);
    }

    private <T> Flux<T> pipeliningWithMap(Map<K, V> map, Function<Map<K, V>, Flux<T>> function,
            Function<Flux<T>, Flux<T>> resultFunction) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, map.keySet());

        if (partitioned.size() < 2) {
            return function.apply(map);
        }

        List<Flux<T>> publishers = partitioned.values().stream().map(ks -> {
            Map<K, V> op = new HashMap<>();
            ks.forEach(k -> op.put(k, map.get(k)));
            return function.apply(op);
        }).collect(Collectors.toList());

        return resultFunction.apply(Flux.merge(publishers));
    }

}
