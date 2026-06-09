package io.lettuce.core.sentinel.api;

import java.util.function.Function;
import io.lettuce.core.internal.SupplierCaching;
import io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;

public final class SentinelCommandsFactory {

    private SentinelCommandsFactory() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<StatefulRedisSentinelConnection<?, ?>, RedisSentinelReactiveCommands<?, ?>> REACTIVE_COMMANDS_PROVIDER = conn -> new RedisSentinelReactiveCommandsImpl(
            conn, conn.getCodec(), () -> conn.getOptions().getJsonParser().get());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> RedisSentinelReactiveCommands<K, V> reactive(StatefulRedisSentinelConnection<K, V> conn) {
        return (RedisSentinelReactiveCommands<K, V>) ((SupplierCaching) conn).getCachedBySupplier(REACTIVE_COMMANDS_PROVIDER);
    }

}
