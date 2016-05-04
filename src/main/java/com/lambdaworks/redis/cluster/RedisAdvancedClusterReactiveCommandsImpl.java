package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterScanSupport.reactiveClusterKeyScanCursorMapper;
import static com.lambdaworks.redis.cluster.ClusterScanSupport.reactiveClusterStreamScanCursorMapper;
import static com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode.NodeFlag.MASTER;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import rx.Observable;
import rx.internal.operators.OperatorConcat;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.rx.RedisKeyReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisScriptingReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisServerReactiveCommands;
import com.lambdaworks.redis.api.rx.Success;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * An advanced reactive and thread-safe API to a Redis Cluster connection.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class RedisAdvancedClusterReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V> implements
        RedisAdvancedClusterReactiveCommands<K, V> {

    private Random random = new Random();

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
    public Observable<Long> del(K... keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

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
    public Observable<Long> unlink(K... keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.unlink(keys);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.unlink(entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<V> mget(K... keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.mget(keys);
        }

        List<Observable<V>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.mget(entry.getValue()));
        }
        Observable<V> observable = Observable.from(observables).lift(OperatorConcat.instance());

        Observable<List<V>> map = observable.toList().map(vs -> {

            Object[] values = new Object[vs.size()];
            int offset = 0;
            for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {

                for (int i = 0; i < keys.length; i++) {

                    int index = entry.getValue().indexOf(keys[i]);
                    if (index == -1) {
                        continue;
                    }

                    values[i] = vs.get(offset + index);
                }

                offset += entry.getValue().size();
            }

            List<V> objects = (List<V>) new ArrayList<>(Arrays.asList(values));
            return objects;
        });

        return map.compose(new FlattenTransform<>());
    }

    @Override
    public Observable<Long> mget(ValueStreamingChannel<V> channel, K... keys) {
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.mget(channel, keys);
        }

        List<Observable<Long>> observables = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.mget(channel, entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<Boolean> msetnx(Map<K, V> map) {

        return pipeliningWithMap(map, kvMap -> super.msetnx(kvMap),
                booleanObservable -> booleanObservable.reduce((accu, next) -> accu || next));
    }

    @Override
    public Observable<String> mset(Map<K, V> map) {
        return pipeliningWithMap(map, kvMap -> super.mset(kvMap), Observable::last);
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
    public Observable<Long> clusterCountKeysInSlot(int slot) {
        RedisClusterReactiveCommands<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterCountKeysInSlot(slot);
        }

        return super.clusterCountKeysInSlot(slot);
    }

    @Override
    public Observable<String> clientSetname(K name) {
        List<Observable<String>> observables = new ArrayList<>();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {
            RedisClusterReactiveCommands<K, V> byNodeId = getConnection(redisClusterNode.getNodeId());
            if (byNodeId.isOpen()) {
                observables.add(byNodeId.clientSetname(name));
            }

            RedisClusterReactiveCommands<K, V> byHost = getConnection(redisClusterNode.getUri().getHost(), redisClusterNode
                    .getUri().getPort());
            if (byHost.isOpen()) {
                observables.add(byHost.clientSetname(name));
            }
        }

        return Observable.merge(observables).last();
    }

    @Override
    public Observable<Long> dbsize() {
        Map<String, Observable<Long>> observables = executeOnMasters(RedisServerReactiveCommands::dbsize);
        return Observable.merge(observables.values()).reduce((accu, next) -> accu + next);
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
    public Observable<V> randomkey() {

        Partitions partitions = getStatefulConnection().getPartitions();
        int index = random.nextInt(partitions.size());

        RedisClusterReactiveCommands<K, V> connection = getConnection(partitions.getPartition(index).getNodeId());
        return connection.randomkey();
    }

    @Override
    public Observable<String> scriptFlush() {
        Map<String, Observable<String>> observables = executeOnNodes(RedisScriptingReactiveCommands::scriptFlush,
                redisClusterNode -> true);
        return Observable.merge(observables.values()).last();
    }

    @Override
    public Observable<String> scriptKill() {
        Map<String, Observable<String>> observables = executeOnNodes(RedisScriptingReactiveCommands::scriptFlush,
                redisClusterNode -> true);
        return Observable.merge(observables.values()).onErrorReturn(throwable -> "OK").last();
    }

    @Override
    public Observable<Success> shutdown(boolean save) {
        Map<String, Observable<Success>> observables = executeOnNodes(commands -> commands.shutdown(save),
                redisClusterNode -> true);
        return Observable.merge(observables.values()).onErrorReturn(throwable -> null).last();
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
            RedisClusterReactiveCommands<K, V> connection = getConnection(uri.getHost(), uri.getPort());
            if (connection.isOpen()) {
                executions.put(redisClusterNode.getNodeId(), function.apply(connection));
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
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor), reactiveClusterKeyScanCursorMapper());
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

    private <T extends ScanCursor> Observable<T> clusterScan(ScanCursor cursor,
            BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Observable<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Observable<T>> resultMapper) {

        return clusterScan(getStatefulConnection(), cursor, scanFunction, (ClusterScanSupport.ScanCursorMapper) resultMapper);
    }

    /**
     * Perform a SCAN in the cluster.
     * 
     */
    static <T extends ScanCursor, K, V> Observable<T> clusterScan(StatefulRedisClusterConnection<K, V> connection,
            ScanCursor cursor, BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Observable<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Observable<T>> mapper) {

        List<String> nodeIds = ClusterScanSupport.getNodeIds(connection, cursor);
        String currentNodeId = ClusterScanSupport.getCurrentNodeId(cursor, nodeIds);
        ScanCursor continuationCursor = ClusterScanSupport.getContinuationCursor(cursor);

        Observable<T> scanCursor = scanFunction.apply(connection.getConnection(currentNodeId).reactive(), continuationCursor);
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
