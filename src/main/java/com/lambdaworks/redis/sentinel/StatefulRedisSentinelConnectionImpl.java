package com.lambdaworks.redis.sentinel;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import io.netty.channel.ChannelHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@ChannelHandler.Sharable
public class StatefulRedisSentinelConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements
        StatefulRedisSentinelConnection<K, V> {

    protected RedisSentinelAsyncConnection<K, V> async;
    protected RedisCodec<K, V> codec;

    public StatefulRedisSentinelConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, timeout, unit);
        this.codec = codec;
    }

    @Override
    public RedisSentinelAsyncConnection<K, V> async() {
        if (async == null) {
            async = new RedisSentinelAsyncConnectionImpl<>(this, codec);
        }
        return async;
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {
        return super.dispatch(cmd);
    }

}
