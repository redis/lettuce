/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A thread-safe, lazily-populated cache keyed by an opaque {@link Object}. The value for a key is built on its first access and
 * reused for every subsequent access with that key.
 * <p>
 * Each connection holds one {@code Store} to cache the command APIs created through its {@code commands(...)} accessors, so a
 * given factory key yields a single instance per connection.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
public final class Store {

    private final Map<Object, Object> store = new ConcurrentHashMap<>();

    /**
     * Returns the value cached under {@code key}, building it via {@code builder} on first access. The builder runs at most
     * once per key.
     *
     * @param key the cache key
     * @param builder builds the value on a cache miss
     * @param <T> the value type
     * @return the cached or newly built value
     */
    @SuppressWarnings("unchecked")
    public <T> T compute(Object key, Supplier<T> builder) {
        return (T) store.computeIfAbsent(key, k -> builder.get());
    }

}
