package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonParserRegistry;

/**
 * An asynchronous and thread-safe API for a Redis connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisAsyncCommandsImpl<K, V> extends AbstractRedisAsyncCommands<K, V>
        implements RedisAsyncCommands<K, V>, RedisClusterAsyncCommands<K, V> {

    private final RedisCodec<K,V> codec;

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on
     * @param codec the codec for command encoding
     *
     */
    public RedisAsyncCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
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
