package com.lambdaworks.redis.sentinel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.function.Supplier;

import rx.Observable;

import com.lambdaworks.redis.ReactiveCommandDispatcher;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.rx.RedisSentinelReactiveCommands;

/**
 * A reactive and thread-safe API for a Redis Sentinel connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisSentinelReactiveCommandsImpl<K, V> implements RedisSentinelReactiveCommands<K, V> {

    private final SentinelCommandBuilder<K, V> commandBuilder;
    private final StatefulConnection<K, V> connection;

    public RedisSentinelReactiveCommandsImpl(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        commandBuilder = new SentinelCommandBuilder<K, V>(codec);
    }

    @Override
    public Observable<SocketAddress> getMasterAddrByName(K key) {

        Observable<V> observable = createDissolvingObservable(() -> commandBuilder.getMasterAddrByKey(key));
        return observable.buffer(2).map(list -> {
            if (list.isEmpty()) {
                return null;
            }

            LettuceAssert.isTrue(list.size() == 2, "List must contain exact 2 entries (Hostname, Port)");
            String hostname = (String) list.get(0);
            String port = (String) list.get(1);
            return new InetSocketAddress(hostname, Integer.parseInt(port));
        });
    }

    @Override
    public Observable<Map<K, V>> masters() {
        return createDissolvingObservable(() -> commandBuilder.masters());
    }

    @Override
    public Observable<Map<K, V>> master(K key) {
        return createObservable(() -> commandBuilder.master(key));
    }

    @Override
    public Observable<Map<K, V>> slaves(K key) {
        return createDissolvingObservable(() -> commandBuilder.slaves(key));
    }

    @Override
    public Observable<Long> reset(K key) {
        return createObservable(() -> commandBuilder.reset(key));
    }

    @Override
    public Observable<String> failover(K key) {
        return createObservable(() -> commandBuilder.failover(key));
    }

    @Override
    public Observable<String> monitor(K key, String ip, int port, int quorum) {
        return createObservable(() -> commandBuilder.monitor(key, ip, port, quorum));
    }

    @Override
    public Observable<String> set(K key, String option, V value) {
        return createObservable(() -> commandBuilder.set(key, option, value));
    }

    @Override
    public Observable<String> remove(K key) {
        return createObservable(() -> commandBuilder.remove(key));
    }

    @Override
    public Observable<String> ping() {
        return createObservable(() -> commandBuilder.ping());
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public StatefulRedisSentinelConnection<K, V> getStatefulConnection() {
        return (StatefulRedisSentinelConnection<K, V>) connection;
    }

    public <T> Observable<T> createObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return Observable.create(new ReactiveCommandDispatcher<K, V, T>(commandSupplier, connection, false));
    }

    @SuppressWarnings("unchecked")
    public <T, R> R createDissolvingObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return (R) Observable.create(new ReactiveCommandDispatcher<>(commandSupplier, connection, true));
    }
}
