/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.internal;

import java.util.function.Supplier;

import io.lettuce.core.api.StatefulConnection;

/**
 * Internal capability: a connection that can cache command API instances ("stamping"), keyed by their type. Used by the
 * {@code XxxCommands.from(connection)} factories to create-and-cache the command API the first time it is requested and return
 * the same instance on subsequent calls.
 * <p>
 * This is intentionally <em>not</em> part of the public {@code StatefulConnection} contract — it is an implementation detail
 * that the factory discovers via {@code instanceof}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.7
 */
public interface CommandsCache<K, V> {

    /**
     * Return the cached instance for {@code type}, creating and caching it via {@code factory} on first access. The factory is
     * invoked at most once per type.
     *
     * @param type the command API type, used as cache key.
     * @param factory creates the instance on cache miss.
     * @param <T> command API type.
     * @return the cached or newly created instance.
     */
    <T> T computeCommands(Class<T> type, Supplier<T> factory);

    /**
     * Create-and-cache the command API on the connection when it supports caching, otherwise create a fresh instance. Used by
     * the {@code XxxCommands.from(connection)} factories.
     *
     * @param connection the connection.
     * @param type the command API type (cache key).
     * @param factory creates the instance on cache miss.
     * @param <K> Key type.
     * @param <V> Value type.
     * @param <T> command API type.
     * @return the cached or newly created instance.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <K, V, T> T stamp(StatefulConnection<K, V> connection, Class<?> type, Supplier<T> factory) {
        if (connection instanceof CommandsCache) {
            return (T) ((CommandsCache) connection).computeCommands(type, factory);
        }
        return factory.get();
    }

}
