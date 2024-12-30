package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;
import reactor.core.publisher.Mono;

/**
 * A reactive and thread-safe API for a Redis Sentinel connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V>
        implements RedisReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V> {

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on.
     * @param codec the codec for command encoding.
     * @param parser the implementation of the {@link JsonParser} to use
     */
    public RedisReactiveCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec,
            Mono<JsonParser> parser) {
        super(connection, codec, parser);
    }

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on.
     * @param codec the codec for command encoding.
     */
    public RedisReactiveCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return (StatefulRedisConnection<K, V>) super.getConnection();
    }

}
