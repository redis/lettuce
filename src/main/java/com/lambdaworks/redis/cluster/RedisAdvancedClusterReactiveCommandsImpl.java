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
package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterScanSupport.reactiveClusterKeyScanCursorMapper;
import static com.lambdaworks.redis.cluster.ClusterScanSupport.reactiveClusterStreamScanCursorMapper;
import static com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode.NodeFlag.MASTER;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Single;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisKeyReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisScriptingReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisServerReactiveCommands;
import com.lambdaworks.redis.api.rx.Success;
import com.lambdaworks.redis.cluster.ClusterConnectionProvider.Intent;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * An advanced reactive and thread-safe API to a Redis Cluster connection.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class RedisAdvancedClusterReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V> implements
        RedisAdvancedClusterReactiveCommands<K, V> {

    private static Function<RedisClusterNode, Boolean> ALL_NODE_FILTER = node -> true;

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public Observable<String> clientSetname(K name) {

        List<Observable<String>> observables = new ArrayList<>();

        observables.add(super.clientSetname(name));

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {
            Single<RedisClusterReactiveCommands<K, V>> byNodeId = getConnectionReactive(redisClusterNode.getNodeId());

            observables.add(byNodeId.flatMapObservable(conn -> {

                if (conn.isOpen()) {
                    return conn.clientSetname(name);
                }
                return Observable.empty();
            }));

            Single<RedisClusterReactiveCommands<K, V>> byHost = getConnectionReactive(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());

            observables.add(byHost.flatMapObservable(conn -> {

                if (conn.isOpen()) {
                    return conn.clientSetname(name);
                }
                return Observable.empty();
            }));
        }

        return Observable.merge(observables).last();
    }

    @Override
    public Observable<Long> clusterCountKeysInSlot(int slot) {

        Single<RedisClusterReactiveCommands<K, V>> connectionBySlot = findConnectionBySlotReactive(slot);

        return connectionBySlot.flatMapObservable(cmd -> cmd.clusterCountKeysInSlot(slot));
    }

    @Override
    public Observable<K> clusterGetKeysInSlot(int slot, int count) {

        Single<RedisClusterReactiveCommands<K, V>> connectionBySlot = findConnectionBySlotReactive(slot);

        return connectionBySlot.flatMapObservable(conn -> conn.clusterGetKeysInSlot(slot, count));
    }

    @Override
    public Observable<Long> dbsize() {
        Map<String, Observable<Long>> observables = executeOnMasters(RedisServerReactiveCommands::dbsize);
        return Observable.merge(observables.values()).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<Long> del(K... keys) {
        return del(Arrays.asList(keys));
    }

    @Override
    public Observable<Long> del(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.del(keys);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.del(entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<Long> exists(K... keys) {
        return exists(Arrays.asList(keys));
    }

    public Observable<Long> exists(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.exists(keys);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.exists(entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<String> flushall() {
        Map<String, Observable<String>> observables = executeOnMasters(RedisServerReactiveCommands::flushall);
        return Observable.merge(observables.values()).last();
    }

    @Override
    public Observable<String> flushdb() {
        Map<String, Observable<String>> observables = executeOnMasters(RedisServerReactiveCommands::flushdb);
        return Observable.merge(observables.values()).last();
    }

    @Override
    public Observable<V> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadius_ro(key, longitude, latitude, distance, unit);
        }

        return super.georadius(key, longitude, latitude, distance, unit);
    }

    @Override
    public Observable<GeoWithin<V>> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadius_ro(key, longitude, latitude, distance, unit, geoArgs);
        }

        return super.georadius(key, longitude, latitude, distance, unit, geoArgs);
    }

    @Override
    public Observable<V> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadiusbymember_ro(key, member, distance, unit);
        }

        return super.georadiusbymember(key, member, distance, unit);
    }

    @Override
    public Observable<GeoWithin<V>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit, GeoArgs geoArgs) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadiusbymember_ro(key, member, distance, unit, geoArgs);
        }

        return super.georadiusbymember(key, member, distance, unit, geoArgs);
    }

    @Override
    public Observable<K> keys(K pattern) {
        Map<String, Observable<K>> observables = executeOnMasters(commands -> commands.keys(pattern));
        return Observable.merge(observables.values());
    }

    @Override
    public Observable<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        Map<String, Observable<Long>> observables = executeOnMasters(commands -> commands.keys(channel, pattern));
        return Observable.merge(observables.values()).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<V> mget(K... keys) {
        return mget(Arrays.asList(keys));
    }

    @SuppressWarnings("unchecked")
    public Observable<V> mget(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(keyList);
        }

        List<Observable<V>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.mget(entry.getValue()));
        }
        Observable<V> observable = Observable.concat(Observable.from(observables));

        Observable<List<V>> map = observable.toList().map(vs -> {

            Object[] values = new Object[vs.size()];
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

            return (List<V>) new ArrayList<>(Arrays.asList(values));
        });

        return map.compose(new FlattenTransform<>());
    }

    @Override
    public Observable<Long> mget(ValueStreamingChannel<V> channel, K... keys) {
        return mget(channel, Arrays.asList(keys));
    }

    public Observable<Long> mget(ValueStreamingChannel<V> channel, Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(channel, keyList);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.mget(channel, entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<Boolean> msetnx(Map<K, V> map) {

        return pipeliningWithMap(map, super::msetnx,
                booleanObservable -> booleanObservable.reduce((accu, next) -> accu && next));
    }

    @Override
    public Observable<String> mset(Map<K, V> map) {
        return pipeliningWithMap(map, super::mset, Observable::last);
    }

    @Override
    public Observable<V> randomkey() {

        Partitions partitions = getStatefulConnection().getPartitions();
        int index = ThreadLocalRandom.current().nextInt(partitions.size());

        Single<RedisClusterReactiveCommands<K, V>> connection = getConnectionReactive(partitions.getPartition(index)
                .getNodeId());
        return connection.flatMapObservable(RedisKeyReactiveCommands::randomkey);
    }

    @Override
    public Observable<String> scriptFlush() {
        Map<String, Observable<String>> observables = executeOnNodes(RedisScriptingReactiveCommands::scriptFlush,
                ALL_NODE_FILTER);
        return Observable.merge(observables.values()).last();
    }

    @Override
    public Observable<String> scriptKill() {
        Map<String, Observable<String>> observables = executeOnNodes(RedisScriptingReactiveCommands::scriptFlush,
                ALL_NODE_FILTER);
        return Observable.merge(observables.values()).onErrorReturn(throwable -> "OK").last();
    }

    @Override
    public Observable<String> scriptLoad(V script) {
        Map<String, Observable<String>> observables = executeOnNodes(cmd -> cmd.scriptLoad(script), ALL_NODE_FILTER);
        return Observable.merge(observables.values()).last();
    }

    @Override
    public Observable<Success> shutdown(boolean save) {
        Map<String, Observable<Success>> observables = executeOnNodes(commands -> commands.shutdown(save), ALL_NODE_FILTER);
        return Observable.merge(observables.values()).onErrorReturn(throwable -> null).last();
    }

    @Override
    public Observable<Long> touch(K... keys) {
        return touch(Arrays.asList(keys));
    }

    public Observable<Long> touch(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.touch(keyList);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.touch(entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<Long> unlink(K... keys) {
        return unlink(Arrays.asList(keys));
    }

    @Override
    public Observable<Long> unlink(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.unlink(keys);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.unlink(entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    /**
     * Run a command on all available masters,
     *
     * @param function function producing the command
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, Observable<T>> executeOnMasters(
            Function<RedisClusterReactiveCommands<K, V>, Observable<T>> function) {
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
    protected <T> Map<String, Observable<T>> executeOnNodes(
            Function<RedisClusterReactiveCommands<K, V>, Observable<T>> function, Function<RedisClusterNode, Boolean> filter) {

        Map<String, Observable<T>> executions = new HashMap<>();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {

            if (!filter.apply(redisClusterNode)) {
                continue;
            }

            RedisURI uri = redisClusterNode.getUri();
            Single<RedisClusterReactiveCommands<K, V>> connection = getConnectionReactive(uri.getHost(), uri.getPort());

            executions.put(redisClusterNode.getNodeId(), connection.flatMapObservable(function::apply));
        }
        return executions;
    }

    private Single<RedisClusterReactiveCommands<K, V>> findConnectionBySlotReactive(int slot) {

        RedisClusterNode node = getStatefulConnection().getPartitions().getPartitionBySlot(slot);
        if (node != null) {
            return getConnectionReactive(node.getUri().getHost(), node.getUri().getPort());
        }

        return Single.error(new RedisException("No partition for slot " + slot));
    }

    @Override
    public StatefulRedisClusterConnectionImpl<K, V> getStatefulConnection() {
        return (StatefulRedisClusterConnectionImpl<K, V>) connection;
    }

    @Override
    public RedisClusterReactiveCommands<K, V> getConnection(String nodeId) {
        return getStatefulConnection().getConnection(nodeId).reactive();
    }

    private Single<RedisClusterReactiveCommands<K, V>> getConnectionReactive(String nodeId) {
        return getSingle(getConnectionProvider().<K, V> getConnectionAsync(Intent.WRITE, nodeId)).map(
                StatefulRedisConnection::reactive);
    }

    @Override
    public RedisClusterReactiveCommands<K, V> getConnection(String host, int port) {
        return getStatefulConnection().getConnection(host, port).reactive();
    }

    private Single<RedisClusterReactiveCommands<K, V>> getConnectionReactive(String host, int port) {
        return getSingle(getConnectionProvider().<K, V> getConnectionAsync(Intent.WRITE, host, port)).map(
                StatefulRedisConnection::reactive);
    }

    private AsyncClusterConnectionProvider getConnectionProvider() {
        return (AsyncClusterConnectionProvider) getStatefulConnection().getClusterDistributionChannelWriter()
                .getClusterConnectionProvider();
    }

    @Override
    public Observable<KeyScanCursor<K>> scan() {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(), reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Observable<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Observable<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor, scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Observable<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return clusterScan(scanCursor, RedisKeyReactiveCommands::scan, reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor),
                reactiveClusterStreamScanCursorMapper());
    }

    @SuppressWarnings("unchecked")
    private <T extends ScanCursor> Observable<T> clusterScan(ScanCursor cursor,
            BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Observable<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Observable<T>> resultMapper) {

        return clusterScan(getStatefulConnection(), cursor, scanFunction, (ClusterScanSupport.ScanCursorMapper) resultMapper);
    }

    /**
     * Perform a SCAN in the cluster.
     *
     */
    private static <T extends ScanCursor, K, V> Observable<T> clusterScan(StatefulRedisClusterConnectionImpl<K, V> connection,
            ScanCursor cursor, BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Observable<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Observable<T>> mapper) {

        List<String> nodeIds = ClusterScanSupport.getNodeIds(connection, cursor);
        String currentNodeId = ClusterScanSupport.getCurrentNodeId(cursor, nodeIds);
        ScanCursor continuationCursor = ClusterScanSupport.getContinuationCursor(cursor);

        AsyncClusterConnectionProvider connectionProvider = (AsyncClusterConnectionProvider) connection
                .getClusterDistributionChannelWriter().getClusterConnectionProvider();

        Observable<T> scanCursor = getSingle(connectionProvider.<K, V> getConnectionAsync(Intent.WRITE, currentNodeId))
                .flatMapObservable(conn -> scanFunction.apply(conn.reactive(), continuationCursor));
        return mapper.map(nodeIds, currentNodeId, scanCursor);
    }

    private <T> Observable<T> pipeliningWithMap(Map<K, V> map, Function<Map<K, V>, Observable<T>> function,
            Function<Observable<T>, Observable<T>> resultFunction) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, map.keySet());

        if (partitioned.size() < 2) {
            return function.apply(map);
        }

        List<Observable<T>> observables = partitioned.values().stream().map(ks -> {
            Map<K, V> op = new HashMap<>();
            ks.forEach(k -> op.put(k, map.get(k)));
            return function.apply(op);
        }).collect(Collectors.toList());

        return resultFunction.apply(Observable.merge(observables));
    }

    static class FlattenTransform<T> implements Observable.Transformer<Iterable<T>, T> {

        @Override
        public Observable<T> call(Observable<Iterable<T>> source) {
            return source.flatMap(Observable::from);
        }
    }

    private static <T> Single<T> getSingle(CompletableFuture<T> future) {

        return Single.create(singleSubscriber -> {

            future.whenComplete((connection, throwable) -> {

                if (throwable != null) {
                    singleSubscriber.onError(throwable);
                } else {
                    singleSubscriber.onSuccess(connection);
                }
            });
        });
    }
}
