package com.lambdaworks.redis.cluster;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import rx.Observable;
import rx.internal.operators.OperatorConcat;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.AbstractRedisReactiveCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * An advanced reactive and thread-safe API to a Redis Cluster connection.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public class RedisAdvancedClusterReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V> implements
        RedisAdvancedClusterReactiveCommands<K, V> {

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

        List<Observable<Long>> observables = Lists.newArrayList();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            observables.add(super.del(entry.getValue()));
        }

        return Observable.merge(observables).reduce((accu, next) -> accu + next);
    }

    @Override
    public Observable<V> mget(K... keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, Arrays.asList(keys));

        if (partitioned.size() < 2) {
            return super.mget(keys);
        }

        List<Observable<V>> observables = Lists.newArrayList();

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

            List<V> objects = (List<V>) Lists.newArrayList(Arrays.asList(values));
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

        List<Observable<Long>> observables = Lists.newArrayList();

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

    static class FlattenTransform<T> implements Observable.Transformer<Iterable<T>, T> {

        @Override
        public Observable<T> call(Observable<Iterable<T>> source) {
            return source.flatMap(values -> Observable.from(values));
        }
    }

    private <T> Observable<T> pipeliningWithMap(Map<K, V> map, Function<Map<K, V>, Observable<T>> function,
            Function<Observable<T>, Observable<T>> resultFunction) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, map.keySet());

        if (partitioned.size() < 2) {
            return function.apply(map);
        }

        List<Observable<T>> observables = partitioned.values().stream().map(ks -> {
            Map<K, V> op = Maps.newHashMap();
            ks.forEach(k -> op.put(k, map.get(k)));
            return function.apply(op);
        }).collect(Collectors.toList());

        return resultFunction.apply(Observable.merge(observables));
    }
}
