package com.lambdaworks.redis.sentinel;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.FutureSyncInvocationHandler;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.api.sync.RedisSentinelCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
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

    protected RedisSentinelAsyncCommands<K, V> async;
    protected RedisCodec<K, V> codec;
    protected RedisSentinelCommands<K, V> sync;

    public StatefulRedisSentinelConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, timeout, unit);
        this.codec = codec;
    }

    @Override
    public RedisSentinelAsyncCommands<K, V> async() {
        if (async == null) {
            async = new RedisSentinelAsyncCommandsImpl<>(this, codec);
        }
        return async;
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {
        return super.dispatch(cmd);
    }

    public RedisSentinelCommands<K, V> sync() {
        if (sync == null) {
            sync = (RedisSentinelCommands) syncHandler(RedisSentinelCommands.class);
        }
        return sync;
    }

    protected Object syncHandler(Class... interfaces) {
        FutureSyncInvocationHandler<K, V> h = new FutureSyncInvocationHandler<>(this, async());
        return Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(), interfaces, h);
    }

}
