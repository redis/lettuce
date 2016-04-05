package com.lambdaworks.redis.masterslave;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
class StatefulRedisMasterSlaveConnectionImpl<K, V> extends StatefulRedisConnectionImpl<K, V> implements
        StatefulRedisMasterSlaveConnection<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public StatefulRedisMasterSlaveConnectionImpl(MasterSlaveChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, codec, timeout, unit);
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {
        getChannelWriter().setReadFrom(readFrom);
    }

    @Override
    public ReadFrom getReadFrom() {
        return getChannelWriter().getReadFrom();
    }

    @Override
    public MasterSlaveChannelWriter<K, V> getChannelWriter() {
        return (MasterSlaveChannelWriter<K, V>) super.getChannelWriter();
    }
}
