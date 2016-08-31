package com.lambdaworks.redis.sentinel;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.reactive.RedisSentinelReactiveCommands;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;
import io.netty.channel.ChannelHandler;

/**
 * @author Mark Paluch
 */
@ChannelHandler.Sharable
public class StatefulRedisSentinelConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements
        StatefulRedisSentinelConnection<K, V> {

    protected final RedisCodec<K, V> codec;
    protected final RedisSentinelCommands<K, V> sync;
    protected final RedisSentinelAsyncCommands<K, V> async;
    protected final RedisSentinelReactiveCommands<K, V> reactive;

    public StatefulRedisSentinelConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, timeout, unit);

        this.codec = codec;
        this.async = new RedisSentinelAsyncCommandsImpl<>(this, codec);
        this.sync = syncHandler(async, RedisSentinelCommands.class);
        this.reactive = new RedisSentinelReactiveCommandsImpl<>(this, codec);
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {
        return super.dispatch(cmd);
    }

    @Override
    public RedisSentinelCommands<K, V> sync() {
        return sync;
    }

    @Override
    public RedisSentinelAsyncCommands<K, V> async() {
        return async;
    }

    @Override
    public RedisSentinelReactiveCommands<K, V> reactive() {
        return reactive;
    }
}
