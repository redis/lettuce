/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.support.caching;

import java.util.Map;

/**
 * Interface defining access to the client-side cache. The cache must support value retrieval, value update (for Redis Cache
 * read-through so values obtained from Redis get written into the client-side cache) and removal (used for invalidations).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
public interface CacheAccessor<K, V> {

    /**
     * Obtain a {@link CacheAccessor} for a cache object implementing {@link Map}.
     *
     * @param map the cache.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a {@link CacheAccessor} backed by a {@link Map} implementation.
     */
    static <K, V> CacheAccessor<K, V> forMap(Map<K, V> map) {
        return new MapCacheAccessor<>(map);
    }

    /**
     * Return the value to which this cache maps the specified key.
     * <p>
     * Note: This method does not allow for differentiating between a cached {@code null} value and no cache entry found at all.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value to which this cache maps the specified key (which may be {@code null} itself), or also {@code null} if
     *         the cache contains no mapping for this key.
     */
    V get(K key);

    /**
     * Associate the specified value with the specified key in this cache.
     * <p>
     * If the cache previously contained a mapping for this key, the old value is replaced by the specified value.
     * <p>
     * Actual registration may be performed in an asynchronous or deferred fashion, with subsequent lookups possibly not seeing
     * the entry yet. This may for example be the case with transactional cache decorators.
     *
     * @param key the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key.
     */
    void put(K key, V value);

    /**
     * Evict the mapping for this key from this cache if it is present.
     * <p>
     * Actual eviction may be performed in an asynchronous or deferred fashion, with subsequent lookups possibly still seeing
     * the entry.
     *
     * @param key the key whose mapping is to be removed from the cache.
     */
    void evict(K key);

}
