/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.reactive;

import java.util.function.Function;

import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;

/**
 * Provides the family-precise reactive {@link CommandsFactory} for a given connection, intended to be passed straight to
 * {@link io.lettuce.core.api.StatefulConnection#commands(CommandsFactory)}:
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     RedisReactiveCommands<K, V> reactive = connection.commands(CommandsFactoryProvider.from(connection));
 * }
 * </pre>
 *
 * The {@code from(...)} overload is resolved on the connection's static type, so the returned factory — and therefore the
 * command API obtained from {@code commands(...)} — carries the precise per-family type. This is the only class here that
 * references Reactor, keeping {@link io.lettuce.core.api.StatefulConnection} and {@link CommandsFactory} flavor-neutral.
 *
 * @since 7.7
 */
public final class CommandsFactoryProvider {

    private CommandsFactoryProvider() {
    }

    /**
     * @return the reactive factory for a standalone connection.
     */
    public static <K, V> CommandsFactory<StatefulRedisConnection<K, V>, RedisReactiveCommands<K, V>> from(
            StatefulRedisConnection<K, V> connection) {
        return factory(RedisReactiveCommands.class,
                conn -> new RedisReactiveCommandsImpl<>(conn, conn.getCodec(), () -> conn.getOptions().getJsonParser().get()));
    }

    /**
     * @return the reactive factory for a cluster connection.
     */
    public static <K, V> CommandsFactory<StatefulRedisClusterConnection<K, V>, RedisAdvancedClusterReactiveCommands<K, V>> from(
            StatefulRedisClusterConnection<K, V> connection) {
        return factory(RedisAdvancedClusterReactiveCommands.class, conn -> new RedisAdvancedClusterReactiveCommandsImpl<>(conn,
                conn.getCodec(), () -> conn.getOptions().getJsonParser().get()));
    }

    /**
     * @return the reactive factory for a Pub/Sub connection.
     */
    public static <K, V> CommandsFactory<StatefulRedisPubSubConnection<K, V>, RedisPubSubReactiveCommands<K, V>> from(
            StatefulRedisPubSubConnection<K, V> connection) {
        return factory(RedisPubSubReactiveCommands.class, conn -> new RedisPubSubReactiveCommandsImpl<>(conn, conn.getCodec()));
    }

    /**
     * @return the reactive factory for a Cluster Pub/Sub connection.
     */
    public static <K, V> CommandsFactory<StatefulRedisClusterPubSubConnection<K, V>, RedisClusterPubSubReactiveCommands<K, V>> from(
            StatefulRedisClusterPubSubConnection<K, V> connection) {
        return factory(RedisClusterPubSubReactiveCommands.class,
                conn -> new RedisClusterPubSubReactiveCommandsImpl<>(conn, conn.getCodec()));
    }

    /**
     * @return the reactive factory for a Sentinel connection.
     */
    public static <K, V> CommandsFactory<StatefulRedisSentinelConnection<K, V>, RedisSentinelReactiveCommands<K, V>> from(
            StatefulRedisSentinelConnection<K, V> connection) {
        return factory(RedisSentinelReactiveCommands.class, conn -> new RedisSentinelReactiveCommandsImpl<>(conn,
                conn.getCodec(), () -> conn.getOptions().getJsonParser().get()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <C, T> CommandsFactory<C, T> factory(Class<?> type, Function<C, T> builder) {
        return new CommandsFactory<C, T>() {

            @Override
            public T apply(C connection) {
                return builder.apply(connection);
            }

            @Override
            public Class<T> type() {
                return (Class) type;
            }

        };
    }

}
