package io.lettuce.core.api;

import java.util.function.Function;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.internal.SupplierCaching;
import io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;

public final class RedisCommandsFactory {

    private RedisCommandsFactory() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<StatefulRedisConnection<?, ?>, RedisReactiveCommands<?, ?>> REACTIVE_COMMANDS_PROVIDER = conn -> new RedisReactiveCommandsImpl(
            conn, conn.getCodec(), () -> conn.getOptions().getJsonParser().get());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<StatefulRedisPubSubConnection<?, ?>, RedisPubSubReactiveCommands<?, ?>> PUBSUB_REACTIVE_COMMANDS_PROVIDER = conn -> new RedisPubSubReactiveCommandsImpl(
            conn, conn.getCodec());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> RedisReactiveCommands<K, V> reactive(StatefulRedisConnection<K, V> conn) {
        return (RedisReactiveCommands<K, V>) ((SupplierCaching) conn).getCachedBySupplier(REACTIVE_COMMANDS_PROVIDER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> RedisPubSubReactiveCommands<K, V> pubsubReactive(StatefulRedisPubSubConnection<K, V> conn) {
        return (RedisPubSubReactiveCommands<K, V>) ((SupplierCaching) conn)
                .getCachedBySupplier(PUBSUB_REACTIVE_COMMANDS_PROVIDER);
    }

}
