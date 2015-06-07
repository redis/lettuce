package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.lambdaworks.redis.api.async.RedisSentinelAsyncConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandOutput;
import io.netty.channel.ChannelHandler;

/**
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@ChannelHandler.Sharable
class RedisSentinelAsyncConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements RedisSentinelAsyncConnection<K, V> {

    private final SentinelCommandBuilder<K, V> commandBuilder;

    public RedisSentinelAsyncConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        super(writer, timeout, unit);
        commandBuilder = new SentinelCommandBuilder<K, V>(codec);
    }

    @Override
    public RedisFuture<SocketAddress> getMasterAddrByName(K key) {

        Command<K, V, List<V>> cmd = commandBuilder.getMasterAddrByKey(key);
        RedisFuture<List<V>> future = dispatch(cmd);
        AtomicReference<SocketAddress> ref = new AtomicReference<>();
        Command<K, V, SocketAddress> convert = new Command<>(cmd.getType(), new CommandOutput<K, V, SocketAddress>(null, null) {
            @Override
            public SocketAddress get() {
                return ref.get();
            }
        }, cmd.getArgs());

        future.whenComplete((list, t) -> {

            if (t != null) {
                convert.completeExceptionally(t);
                return;
            }

            if (!list.isEmpty()) {
                checkArgument(list.size() == 2, "List must contain exact 2 entries (Hostname, Port)");
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
}
