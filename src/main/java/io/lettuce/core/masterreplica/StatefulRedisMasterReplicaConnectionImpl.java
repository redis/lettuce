package io.lettuce.core.masterreplica;

import java.time.Duration;
import java.util.function.Supplier;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;

import static io.lettuce.core.ClientOptions.DEFAULT_JSON_PARSER;

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
        super(writer, NoOpPushHandler.INSTANCE, codec, timeout, DEFAULT_JSON_PARSER);
        shareHashImportRegistry(writer);
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
        shareHashImportRegistry(writer);
    }

    /**
     * Share this facade's {@link io.lettuce.core.HashImportRegistry} with the physical connections created by the provider so
     * that {@code HIMPORT} fieldsets are re-established on their activation (reconnect).
     */
    private void shareHashImportRegistry(MasterReplicaChannelWriter writer) {
        writer.getUpstreamReplicaConnectionProvider().setHashImportRegistry(getConnectionState().getHashImportRegistry());
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
