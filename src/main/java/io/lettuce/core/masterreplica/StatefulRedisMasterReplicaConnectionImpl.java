package io.lettuce.core.masterreplica;

import java.time.Duration;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
class StatefulRedisMasterReplicaConnectionImpl<K, V> extends StatefulRedisConnectionImpl<K, V>
        implements StatefulRedisMasterReplicaConnection<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     */
    StatefulRedisMasterReplicaConnectionImpl(MasterReplicaChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {
        super(writer, NoOpPushHandler.INSTANCE, codec, timeout);
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
    public MasterReplicaChannelWriter getChannelWriter() {
        return (MasterReplicaChannelWriter) super.getChannelWriter();
    }

}
