package com.lambdaworks.redis.sentinel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisSentinelAsyncConnection;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;

/**
 * An asynchronous and thread-safe API for a Redis Sentinel connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisSentinelAsyncCommandsImpl<K, V> implements RedisSentinelAsyncCommands<K, V>,
        RedisSentinelAsyncConnection<K, V> {

    private final SentinelCommandBuilder<K, V> commandBuilder;
    private final StatefulConnection<K, V> connection;

    public RedisSentinelAsyncCommandsImpl(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        commandBuilder = new SentinelCommandBuilder<K, V>(codec);
    }

    @Override
    public RedisFuture<SocketAddress> getMasterAddrByName(K key) {

        Command<K, V, List<V>> cmd = commandBuilder.getMasterAddrByKey(key);
        CompletionStage<List<V>> future = dispatch(cmd);
        AtomicReference<SocketAddress> ref = new AtomicReference<>();
        AsyncCommand<K, V, SocketAddress> convert = new AsyncCommand<K, V, SocketAddress>((RedisCommand) cmd) {
            @Override
            protected void completeResult() {
                complete(ref.get());
            }
        };

        future.whenComplete((list, t) -> {

            if (t != null) {
                convert.completeExceptionally(t);
                return;
            }

            if (!list.isEmpty()) {
                LettuceAssert.isTrue(list.size() == 2, "List must contain exact 2 entries (Hostname, Port)");
                String hostname = (String) list.get(0);
                String port = (String) list.get(1);
                ref.set(new InetSocketAddress(hostname, Integer.parseInt(port)));
            }

            convert.complete();

        });

        return convert;
    }

    @Override
    public RedisFuture<List<Map<K, V>>> masters() {

        return dispatch(commandBuilder.masters());
    }

    @Override
    public RedisFuture<Map<K, V>> master(K key) {

        return dispatch(commandBuilder.master(key));
    }

    @Override
    public RedisFuture<List<Map<K, V>>> slaves(K key) {

        return dispatch(commandBuilder.slaves(key));
    }

    @Override
    public RedisFuture<Long> reset(K key) {

        return dispatch(commandBuilder.reset(key));
    }

    @Override
    public RedisFuture<String> failover(K key) {

        return dispatch(commandBuilder.failover(key));
    }

    @Override
    public RedisFuture<String> monitor(K key, String ip, int port, int quorum) {

        return dispatch(commandBuilder.monitor(key, ip, port, quorum));
    }

    @Override
    public RedisFuture<String> set(K key, String option, V value) {

        return dispatch(commandBuilder.set(key, option, value));
    }

    @Override
    public RedisFuture<String> remove(K key) {
        return dispatch(commandBuilder.remove(key));
    }

    @Override
    public RedisFuture<String> ping() {
        return dispatch(commandBuilder.ping());
    }

    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        return connection.dispatch(new AsyncCommand<>(cmd));
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
}
