package io.lettuce.core.masterreplica;

import java.time.Duration;
import java.util.function.Supplier;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;

import static io.lettuce.core.ClientOptions.DEFAULT_JSON_PARSER;

/**
 * @author Mark Paluch
 * @author Sanghun Lee
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
        super(writer, NoOpPushHandler.INSTANCE, codec, timeout, DEFAULT_JSON_PARSER);
    }

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param parser the JSON parser to use
     */
    StatefulRedisMasterReplicaConnectionImpl(MasterReplicaChannelWriter writer, RedisCodec<K, V> codec, Duration timeout,
            Supplier<JsonParser> parser) {
        super(writer, NoOpPushHandler.INSTANCE, codec, timeout, parser);
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

    /**
     * Build the async command set. Overridden so scan commands receive node-affine routing (see
     * {@link MasterReplicaAsyncCommandsImpl}); sync commands derive from this instance.
     */
    @Override
    protected RedisAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new MasterReplicaAsyncCommandsImpl<>(this, getCodec(), getJsonParser());
    }

    /**
     * Build the reactive command set. Overridden so scan commands receive node-affine routing (see
     * {@link MasterReplicaReactiveCommandsImpl}).
     */
    @Override
    protected RedisReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new MasterReplicaReactiveCommandsImpl<>(this, getCodec(), getJsonParser());
    }

}
