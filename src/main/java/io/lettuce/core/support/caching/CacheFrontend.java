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

import java.io.Closeable;
import java.util.concurrent.Callable;

import io.lettuce.core.RedisException;

/**
 * Interface defining a cache frontend for common cache retrieval operations that using Redis server-side caching assistance.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
public interface CacheFrontend<K, V> extends Closeable {

    /**
     * Return the value to which this cache maps the specified key.
     * <p>
     * Note: This method does not allow for differentiating between a cached {@code null} value and no cache entry found at all.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value to which this cache maps the specified key (which may be {@code null} itself), or also {@code null} if
     *         the cache contains no mapping for this key.
     * @see CacheAccessor#get(Object)
     * @see RedisCache#get(Object)
     */
    V get(K key);

    /**
     * Return the value to which this cache maps the specified key, obtaining that value from {@code valueLoader} if necessary.
     * This method provides a simple substitute for the conventional "if cached, return; otherwise create, cache and return"
     * pattern.
     *
     * If the {@code valueLoader} throws an exception, it is wrapped in a {@link ValueRetrievalException}
     *
     * @param key the key whose associated value is to be returned
     * @param valueLoader the value loader that is used to obtain the value if the client-side cache and Redis cache are not
     *        associated with a value.
     * @return the value to which this cache maps the specified key.
     * @throws ValueRetrievalException if the {@code valueLoader} throws an exception or returns a {@code null} value.
     */
    V get(K key, Callable<V> valueLoader);

    /**
     * Closes this cache frontend and releases any system resources associated with it. If the frontend is already closed then
     * invoking this method has no effect.
     */
    @Override
    void close();

    /**
     * Wrapper exception to be thrown from {@link CacheFrontend#get(Object, Callable)} in case of the value loader callback
     * failing with an exception.
     */
    @SuppressWarnings("serial")
    class ValueRetrievalException extends RedisException {

        /**
         * Create a {@code ValueRetrievalException} with the specified detail message.
         *
         * @param msg the detail message.
         */
        public ValueRetrievalException(String msg) {
            super(msg);
        }

        /**
         * Create a {@code ValueRetrievalException} with the specified detail message and nested exception.
         *
         * @param msg the detail message.
         * @param cause the nested exception.
         */
        public ValueRetrievalException(String msg, Throwable cause) {
            super(msg, cause);
        }

    }

}
