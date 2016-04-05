package com.lambdaworks.redis.sentinel;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.rx.RedisSentinelReactiveCommands;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;
import io.netty.channel.ChannelHandler;

/**
 * @author Mark Paluch
 */
@ChannelHandler.Sharable
public class StatefulRedisSentinelConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements
        StatefulRedisSentinelConnection<K, V> {

    protected RedisCodec<K, V> codec;
    protected RedisSentinelCommands<K, V> sync;
    protected RedisSentinelAsyncCommands<K, V> async;
    protected RedisSentinelReactiveCommands<K, V> reactive;

    public StatefulRedisSentinelConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, timeout, unit);
        this.codec = codec;
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {
        return super.dispatch(cmd);
    }

    public RedisSentinelCommands<K, V> sync() {
        if (sync == null) {
            sync = syncHandler(async(), RedisSentinelCommands.class);
        }
        return sync;
    }

    @Override
    public RedisSentinelAsyncCommands<K, V> async() {
        if (async == null) {
            async = new RedisSentinelAsyncCommandsImpl<>(this, codec);
        }
        return async;
    }

    @Override
    public RedisSentinelReactiveCommands<K, V> reactive() {
        if (reactive == null) {
            reactive = new RedisSentinelReactiveCommandsImpl<>(this, codec);
        }
        return reactive;
    }
}
