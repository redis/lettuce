package io.lettuce.core.failover.api;

import java.util.function.Function;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.failover.StatefulRedisMultiDbConnectionImpl;
import io.lettuce.core.failover.StatefulRedisMultiDbPubSubConnectionImpl;
import io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;

public final class MultiDbCommandsFactory {

    private MultiDbCommandsFactory() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<StatefulRedisMultiDbConnection<?, ?>, RedisReactiveCommands<?, ?>> REACTIVE_COMMANDS_PROVIDER = conn -> new RedisReactiveCommandsImpl(
            conn, conn.getCodec(), () -> conn.getOptions().getJsonParser().get());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<StatefulRedisMultiDbPubSubConnection<?, ?>, RedisPubSubReactiveCommands<?, ?>> PUBSUB_REACTIVE_COMMANDS_PROVIDER = conn -> new RedisPubSubReactiveCommandsImpl(
            conn, conn.getCodec());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> RedisReactiveCommands<K, V> reactive(StatefulRedisMultiDbConnection<K, V> conn) {
        return (RedisReactiveCommands<K, V>) ((StatefulRedisMultiDbConnectionImpl) conn)
                .getCachedBySupplier(REACTIVE_COMMANDS_PROVIDER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> RedisPubSubReactiveCommands<K, V> pubsubReactive(StatefulRedisMultiDbPubSubConnection<K, V> conn) {
        return (RedisPubSubReactiveCommands<K, V>) ((StatefulRedisMultiDbPubSubConnectionImpl) conn)
                .getCachedBySupplier(PUBSUB_REACTIVE_COMMANDS_PROVIDER);
    }

}
