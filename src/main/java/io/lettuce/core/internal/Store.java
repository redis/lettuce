/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A generic, thread-safe per-connection cache ("stamping"), keyed by an opaque key supplied by the {@code CommandsFactory}.
 * Held by each connection and consulted by its {@code commands(...)} accessors so a connection hands out a single instance per
 * key.
 * <p>
 * The key is intentionally {@link Object}: today it is the command-API type, but a multi-codec factory could use a composite
 * key (e.g. type + codec) so several views of the same type coexist on one connection — without any change here.
 *
 * @since 7.7
 */
public final class Store {

    private final Map<Object, Object> store = new ConcurrentHashMap<>();

    /**
     * Return the value cached under {@code key}, building it via {@code builder} on first access. The builder runs at most once
     * per key.
     *
     * @param key the cache key.
     * @param builder builds the value on a cache miss.
     * @param <T> value type.
     * @return the cached or newly built value.
     */
    @SuppressWarnings("unchecked")
    public <T> T compute(Object key, Supplier<T> builder) {
        return (T) store.computeIfAbsent(key, k -> builder.get());
    }

}
