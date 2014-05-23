package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.Command;
import io.netty.channel.ChannelHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 16:27
 */
@ChannelHandler.Sharable
public class RedisSentinelAsyncConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements
        RedisSentinelAsyncConnection<K, V> {

    private SentinelCommandBuilder<K, V> commandBuilder;

    public RedisSentinelAsyncConnectionImpl(RedisCodec<K, V> codec, BlockingQueue<Command<K, V, ?>> queue, long timeout,
            TimeUnit unit) {
        super(queue, timeout, unit);
        commandBuilder = new SentinelCommandBuilder<K, V>(codec);

    }

    @Override
    public Future<SocketAddress> getMasterAddrByName(K key) {

        Command<K, V, List<V>> cmd = commandBuilder.getMasterAddrByKey(key);
        final Future<List<V>> future = dispatch(cmd);

        Future<SocketAddress> result = Futures.lazyTransform(future, new Function<List<V>, SocketAddress>() {
            @Override
            public SocketAddress apply(List<V> input) {
                checkArgument(input.size() == 2, "List must contain exact 2 entries (Hostname, Port)");
                String hostname = (String) input.get(0);
                String port = (String) input.get(1);
                return new InetSocketAddress(hostname, Integer.parseInt(port));
            }
        });

        return result;
    }

    @Override
    public Future<Map<K, V>> master(K key) {

        return dispatch(commandBuilder.master(key));
    }

    @Override
    public Future<Map<K, V>> slaves(K key) {

        return dispatch(commandBuilder.slaves(key));
    }

    @Override
    public Future<Long> reset(K key) {

        return dispatch(commandBuilder.reset(key));
    }

    @Override
    public Future<Long> failover(K key) {

        return dispatch(commandBuilder.failover(key));
    }

    @Override
    public Future<String> monitor(K key, String ip, int port, int quorum) {

        return dispatch(commandBuilder.monitor(key, ip, port, quorum));
    }

    @Override
    public Future<String> set(K key, String option, V value) {

        return dispatch(commandBuilder.set(key, option, value));
    }

    @Override
    public Future<String> remove(K key) {
        return dispatch(commandBuilder.remove(key));
    }

    @Override
    public Future<String> ping() {
        return dispatch(commandBuilder.ping());
    }
}
