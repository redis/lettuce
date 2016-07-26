package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterScanSupport.reactiveClusterKeyScanCursorMapper;
import static com.lambdaworks.redis.cluster.ClusterScanSupport.reactiveClusterStreamScanCursorMapper;
import static com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode.NodeFlag.MASTER;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.lambdaworks.redis.internal.LettuceLists;
import rx.Observable;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisKeyReactiveCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * An advanced reactive and thread-safe API to a Redis Cluster connection.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class RedisAdvancedClusterReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V>
        implements RedisAdvancedClusterReactiveCommands<K, V> {

    private Random random = new Random();

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection,
            RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public Single<Long> del(K... keys) {
        return del(Arrays.asList(keys));
    }

    @Override
    public Single<Long> del(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.del(keys);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.del(entry.getValue()).toObservable());
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next).last().toSingle();
    }

    @Override
    public Single<Long> unlink(K... keys) {
        return unlink(Arrays.asList(keys));
    }

    @Override
    public Single<Long> unlink(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.unlink(keys);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.unlink(entry.getValue()).toObservable());
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next).last().toSingle();
    }

    @Override
    public Single<Long> exists(K... keys) {
        return exists(Arrays.asList(keys));
    }

    public Single<Long> exists(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.exists(keyList);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.exists(entry.getValue()).toObservable());
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next).toSingle();
    }

    @Override
    public Observable<KeyValue<K, V>> mget(K... keys) {
        return mget(Arrays.asList(keys));
    }

    public Observable<KeyValue<K, V>> mget(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(keyList);
        }

        List<Observable<KeyValue<K, V>>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.mget(entry.getValue()));
        }
        Observable<KeyValue<K, V>> observable = Observable.concat(Observable.from(observables));

        Observable<List<KeyValue<K, V>>> map = observable.toList().map(vs -> {

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

        return map.compose(new FlattenTransform<>());
    }

    @Override
    public Single<Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        return mget(channel, Arrays.asList(keys));
    }

    @Override
    public Single<Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(channel, keyList);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.mget(channel, entry.getValue()).toObservable());
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next).last().toSingle();
    }

    @Override
    public Single<Boolean> msetnx(Map<K, V> map) {

        return pipeliningWithMap(map, kvMap -> super.msetnx(kvMap).toObservable(),
                booleanObservable -> booleanObservable.reduce((accu, next) -> accu || next)).last().toSingle();
    }

    @Override
    public Single<String> mset(Map<K, V> map) {
        return pipeliningWithMap(map, kvMap -> super.mset(kvMap).toObservable(), Observable::last).last().toSingle();
    }

    @Override
    public Observable<K> clusterGetKeysInSlot(int slot, int count) {
        RedisClusterReactiveCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterGetKeysInSlot(slot, count);
        }

        return super.clusterGetKeysInSlot(slot, count);
    }

    @Override
    public Single<Long> clusterCountKeysInSlot(int slot) {
        RedisClusterReactiveCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterCountKeysInSlot(slot);
        }

        return super.clusterCountKeysInSlot(slot);
    }

    @Override
    public Single<String> clientSetname(K name) {
        List<Observable<String>> observables = new ArrayList<>();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {
            StatefulRedisConnection<K, V> byNodeId = getStatefulConnection().getConnection(redisClusterNode.getNodeId());
            if (byNodeId.isOpen()) {
                observables.add(byNodeId.reactive().clientSetname(name).toObservable());
            }

            StatefulRedisConnection<K, V> byHost = getStatefulConnection().getConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());
            if (byHost.isOpen()) {
                observables.add(byHost.reactive().clientSetname(name).toObservable());
            }
        }

        return Observable.merge(observables).last().last().toSingle();
    }

    @Override
    public Single<Long> dbsize() {
        Map<String, Observable<Long>> observables = executeOnMasters((commands) -> commands.dbsize().toObservable());
        return Observable.merge(observables.values()).reduce((accu, next) -> accu + next).toSingle();
    }

    @Override
    public Single<String> flushall() {
        Map<String, Observable<String>> observables = executeOnMasters(
                (kvRedisClusterReactiveCommancommandss) -> kvRedisClusterReactiveCommancommandss.flushall().toObservable());
        return Observable.merge(observables.values()).last().last().toSingle();
    }

    @Override
    public Single<String> flushdb() {
        Map<String, Observable<String>> observables = executeOnMasters((commands) -> commands.flushdb().toObservable());
        return Observable.merge(observables.values()).last().last().toSingle();
    }

    @Override
    public Observable<K> keys(K pattern) {
        Map<String, Observable<K>> observables = executeOnMasters(commands -> commands.keys(pattern));
        return Observable.merge(observables.values());
    }

    @Override
    public Single<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        Map<String, Observable<Long>> observables = executeOnMasters(
                commands -> commands.keys(channel, pattern).toObservable());
        return Observable.merge(observables.values()).reduce((accu, next) -> accu + next).last().toSingle();
    }

    @Override
    public Single<V> randomkey() {

        Partitions partitions = getStatefulConnection().getPartitions();
        int index = random.nextInt(partitions.size());

        RedisClusterReactiveCommands<K, V> connection = getConnection(partitions.getPartition(index).getNodeId());
        return connection.randomkey();
    }

    @Override
    public Single<String> scriptFlush() {
        Map<String, Observable<String>> observables = executeOnNodes((commands) -> commands.scriptFlush().toObservable(),
                redisClusterNode -> true);
        return Observable.merge(observables.values()).last().last().toSingle();
    }

    @Override
    public Single<String> scriptKill() {
        Map<String, Observable<String>> observables = executeOnNodes((commands) -> commands.scriptFlush().toObservable(),
                redisClusterNode -> true);
        return Observable.merge(observables.values()).onErrorReturn(throwable -> "OK").last().toSingle();
    }

    @Override
    public Completable shutdown(boolean save) {
        Map<String, Observable<Boolean>> observables = executeOnNodes(commands -> commands.shutdown(save).toObservable(),
                redisClusterNode -> true);
        return Completable.fromObservable(Observable.merge(observables.values()).onErrorReturn(throwable -> null).last());
    }

    @Override
    public Single<Long> touch(K... keys) {
        return touch(Arrays.asList(keys));
    }

    public Single<Long> touch(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.touch(keyList);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.touch(entry.getValue()).toObservable());
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next).toSingle();
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
        return (StatefulRedisClusterConnection<K, V>) connection;
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
    public Single<KeyScanCursor<K>> scan() {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(), reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Single<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Single<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor, scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Single<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor), reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor),
                reactiveClusterStreamScanCursorMapper());
    }

    private <T extends ScanCursor> Single<T> clusterScan(ScanCursor cursor,
            BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Single<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Single<T>> resultMapper) {

        return clusterScan(getStatefulConnection(), cursor, scanFunction, (ClusterScanSupport.ScanCursorMapper) resultMapper);
    }

    /**
     * Perform a SCAN in the cluster.
     * 
     */
    static <T extends ScanCursor, K, V> Single<T> clusterScan(StatefulRedisClusterConnection<K, V> connection,
            ScanCursor cursor, BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Single<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Single<T>> mapper) {

        List<String> nodeIds = ClusterScanSupport.getNodeIds(connection, cursor);
        String currentNodeId = ClusterScanSupport.getCurrentNodeId(cursor, nodeIds);
        ScanCursor continuationCursor = ClusterScanSupport.getContinuationCursor(cursor);

        Single<T> scanCursor = scanFunction.apply(connection.getConnection(currentNodeId).reactive(), continuationCursor);
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
            return source.flatMap(values -> Observable.from(values));
        }
    }

}
