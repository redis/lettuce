package io.lettuce.core.cluster.api;

import java.util.function.Function;
import io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import io.lettuce.core.internal.SupplierCaching;

public final class ClusterCommandsFactory {

    private ClusterCommandsFactory() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<StatefulRedisClusterConnection<?, ?>, RedisAdvancedClusterReactiveCommands<?, ?>> REACTIVE_COMMANDS_PROVIDER = conn -> new RedisAdvancedClusterReactiveCommandsImpl(
            conn, conn.getCodec(), conn.getOptions().getJsonParser());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Function<StatefulRedisClusterPubSubConnection<?, ?>, RedisClusterPubSubReactiveCommands<?, ?>> PUBSUB_REACTIVE_COMMANDS_PROVIDER = conn -> new RedisClusterPubSubReactiveCommandsImpl(
            conn, conn.getCodec());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> RedisAdvancedClusterReactiveCommands<K, V> reactive(StatefulRedisClusterConnection<K, V> conn) {
        return (RedisAdvancedClusterReactiveCommands<K, V>) ((SupplierCaching) conn)
                .getCachedBySupplier(REACTIVE_COMMANDS_PROVIDER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> RedisClusterPubSubReactiveCommands<K, V> pubsubReactive(
            StatefulRedisClusterPubSubConnection<K, V> conn) {
        return (RedisClusterPubSubReactiveCommands<K, V>) ((SupplierCaching) conn)
                .getCachedBySupplier(PUBSUB_REACTIVE_COMMANDS_PROVIDER);
    }

}
