package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonParserRegistry;

/**
 * A reactive and thread-safe API for a Redis Sentinel connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V>
        implements RedisReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V> {

    private final RedisCodec<K, V> codec;

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on.
     * @param codec the codec for command encoding.
     *
     */
    public RedisReactiveCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.codec = codec;
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return (StatefulRedisConnection<K, V>) super.getConnection();
    }

    @Override
    public JsonParser<K, V> getJsonParser() {
        return JsonParserRegistry.getJsonParser(this.codec);
    }

    @Override
    public void setJsonParser(JsonParser<K, V> jsonParser) {
        throw new UnsupportedOperationException("Setting a custom JsonParser is not supported");
    }

}
