/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for server-side assisted cache invalidation.
 *
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("ACL")
public class ClientsideCachingIntegrationTests extends TestSupport {

    private final RedisClient redisClient;

    @Inject
    public ClientsideCachingIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @BeforeEach
    void setUp() {

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            connection.sync().flushdb();
        }
    }

    @Test
    void clientCachingResp2() {

        ClientOptions resp2 = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();

        redisClient.setOptions(resp2);

        StatefulRedisConnection<String, String> data = redisClient.connect();
        RedisCommands<String, String> commands = data.sync();
        StatefulRedisPubSubConnection<String, String> pubSub = redisClient.connectPubSub();

        List<String> invalidations = new CopyOnWriteArrayList<>();

        commands.clientTracking(TrackingArgs.Builder.enabled().redirect(pubSub.sync().clientId()));

        pubSub.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                if (channel.equals("__redis__:invalidate")) {
                    invalidations.add(message);
                }
            }
        });

        pubSub.sync().subscribe("__redis__:invalidate");

        commands.get("key1");
        commands.get("key2");

        assertThat(invalidations).isEmpty();

        Map<String, String> keys = new HashMap<>();
        keys.put("key1", "value1");
        keys.put("key2", "value2");

        commands.mset(keys);

        Wait.untilEquals(2, invalidations::size).waitOrTimeout();

        assertThat(invalidations).contains("key1", "key2");

        data.close();
        pubSub.close();
    }

    @Test
    void clientCachingResp3() {

        ClientOptions resp2 = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();

        redisClient.setOptions(resp2);

        StatefulRedisConnection<String, String> data = redisClient.connect();
        RedisCommands<String, String> commands = data.sync();

        List<String> invalidations = new CopyOnWriteArrayList<>();

        commands.clientTracking(TrackingArgs.Builder.enabled());

        data.addListener(message -> {

            if (message.getType().equals("invalidate")) {
                invalidations.addAll((List) message.getContent(StringCodec.UTF8::decodeKey).get(1));
            }
        });

        commands.get("key1");
        commands.get("key2");

        assertThat(invalidations).isEmpty();

        Map<String, String> keys = new HashMap<>();
        keys.put("key1", "value1");
        keys.put("key2", "value2");

        commands.mset(keys);

        Wait.untilEquals(2, invalidations::size).waitOrTimeout();

        assertThat(invalidations).contains("key1", "key2");

        data.close();
    }

    @Test
    void serverAssistedCachingShouldFetchValueFromRedis() {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> otherParty = redisClient.connect();
        RedisCommands<String, String> commands = otherParty.sync();

        commands.set(key, value);

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled().noloop());

        assertThat(clientCache).isEmpty();
        String shouldExist = frontend.get(key);
        assertThat(shouldExist).isNotNull();
        assertThat(clientCache).hasSize(1);

        otherParty.close();
        frontend.close();
    }

    @Test
    void serverAssistedCachingShouldExpireValueFromRedis() throws InterruptedException {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> otherParty = redisClient.connect();
        RedisCommands<String, String> commands = otherParty.sync();

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled());

        // make sure value exists in Redis
        // client-side cache is empty
        commands.set(key, value);

        // Read-through into Redis
        String cachedValue = frontend.get(key);
        assertThat(cachedValue).isNotNull();

        // client-side cache holds the same value
        assertThat(clientCache).hasSize(1);

        // now, the key expires
        commands.pexpire(key, 1);

        // a while later
        Thread.sleep(200);

        // the expiration reflects in the client-side cache
        assertThat(clientCache).isEmpty();

        assertThat(frontend.get(key)).isNull();

        otherParty.close();
        frontend.close();
    }

    @Test
    void serverAssistedCachingShouldUseValueLoader() throws InterruptedException {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> otherParty = redisClient.connect();
        RedisCommands<String, String> commands = otherParty.sync();

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled().noloop());

        String shouldLoad = frontend.get(key, () -> "myvalue");
        assertThat(shouldLoad).isEqualTo("myvalue");
        assertThat(clientCache).hasSize(1);
        assertThat(commands.get(key)).isEqualTo("myvalue");

        commands.set(key, value);
        Thread.sleep(100);
        assertThat(clientCache).isEmpty();

        otherParty.close();
        frontend.close();
    }

    /**
     * Interface defining a cache frontend for common cache retrieval operations that using Redis server-side caching
     * assistance.
     *
     * @param <K>
     * @param <V>
     */
    interface CacheFrontend<K, V> extends Closeable {

        /**
         * Return the value to which this cache maps the specified key.
         * <p>
         * Note: This method does not allow for differentiating between a cached {@literal null} value and no cache entry found
         * at all.
         *
         * @param key the key whose associated value is to be returned.
         * @return the value to which this cache maps the specified key (which may be {@literal null} itself), or also
         *         {@literal null} if the cache contains no mapping for this key.
         * @see CacheAccessor#get(Object)
         * @see RedisCache#get(Object)
         */
        V get(K key);

        /**
         * Return the value to which this cache maps the specified key, obtaining that value from {@code valueLoader} if
         * necessary. This method provides a simple substitute for the conventional "if cached, return; otherwise create, cache
         * and return" pattern.
         *
         * If the {@code valueLoader} throws an exception, it is wrapped in a {@link ValueRetrievalException}
         *
         * @param key the key whose associated value is to be returned
         * @param valueLoader the value loader that is used to obtain the value if the client-side cache and Redis cache are not
         *        associated with a value.
         * @return the value to which this cache maps the specified key.
         * @throws ValueRetrievalException if the {@code valueLoader} throws an exception or returns a {@literal null} value.
         */
        V get(K key, Callable<V> valueLoader);

        /**
         * Closes this cache frontend and releases any system resources associated with it. If the frontend is already closed
         * then invoking this method has no effect.
         */
        @Override
        void close();
    }

    /**
     * Interface defining access to the client-side cache. The cache must support value retrieval, value update (for Redis Cache
     * read-through so values obtained from Redis get written into the client-side cache) and removal (used for invalidations).
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    interface CacheAccessor<K, V> {

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
         * Note: This method does not allow for differentiating between a cached {@literal null} value and no cache entry found
         * at all.
         *
         * @param key the key whose associated value is to be returned.
         * @return the value to which this cache maps the specified key (which may be {@literal null} itself), or also
         *         {@literal null} if the cache contains no mapping for this key.
         */
        V get(K key);

        /**
         * Associate the specified value with the specified key in this cache.
         * <p>
         * If the cache previously contained a mapping for this key, the old value is replaced by the specified value.
         * <p>
         * Actual registration may be performed in an asynchronous or deferred fashion, with subsequent lookups possibly not
         * seeing the entry yet. This may for example be the case with transactional cache decorators.
         *
         * @param key the key with which the specified value is to be associated.
         * @param value the value to be associated with the specified key.
         */
        void put(K key, V value);

        /**
         * Evict the mapping for this key from this cache if it is present.
         * <p>
         * Actual eviction may be performed in an asynchronous or deferred fashion, with subsequent lookups possibly still
         * seeing the entry.
         *
         * @param key the key whose mapping is to be removed from the cache.
         */
        void evict(K key);

    }

    /**
     * {@link CacheAccessor} implementation for {@link Map}-based cache implementations.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    static class MapCacheAccessor<K, V> implements CacheAccessor<K, V> {

        private final Map<K, V> map;

        MapCacheAccessor(Map<K, V> map) {
            this.map = map;
        }

        @Override
        public V get(K key) {
            return map.get(key);
        }

        @Override
        public void put(K key, V value) {
            map.put(key, value);
        }

        @Override
        public void evict(K key) {
            map.remove(key);
        }
    }

    /**
     * Interface defining common Redis Cache operations.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    interface RedisCache<K, V> {

        /**
         * Retrieve a {@code value} from Redis for the given cache {@code key}.
         *
         * @param key the key whose associated value is to be returned.
         * @return the value to which this Redis cache value maps the specified key (which may be {@literal null} itself), or
         *         also {@literal null} if the Redis cache contains no mapping for this key.
         */
        V get(K key);

        /**
         * Associate the specified value with the specified key in this Redis cache.
         *
         * @param key the key with which the specified value is to be associated.
         * @param value the value to be associated with the specified key.
         */
        void put(K key, V value);

        /**
         * Register a invalidation {@code listener} that is notified if a key in this Redis cache expires or gets modified.
         *
         * @param listener the listener to notify.
         */
        void addInvalidationListener(java.util.function.Consumer<? super K> listener);

        /**
         * Closes this Redis cache and releases any connections associated with it. If the cache is already closed then invoking
         * this method has no effect.
         */
        void close();
    }

    /**
     * Utility to provide server-side assistance for client-side caches. This is a {@link CacheFrontend} that represents a
     * two-level cache backed by a client-side and a Redis cache.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     */
    static class ClientSideCaching<K, V> implements CacheFrontend<K, V> {

        private final CacheAccessor<K, V> cacheAccessor;
        private final RedisCache<K, V> redisCache;
        private final List<java.util.function.Consumer<K>> invalidationListeners = new CopyOnWriteArrayList<>();

        private ClientSideCaching(CacheAccessor<K, V> cacheAccessor, RedisCache<K, V> redisCache) {
            this.cacheAccessor = cacheAccessor;
            this.redisCache = redisCache;
        }

        /**
         * Enable server-assisted Client side caching for the given {@link CacheAccessor} and {@link StatefulRedisConnection}.
         * <p>
         * Note that the {@link CacheFrontend} is associated with a Redis connection. Make sure to {@link CacheFrontend#close()
         * close} the frontend object to release the Redis connection after use.
         *
         * @param cacheAccessor the accessor used to interact with the client-side cache.
         * @param connection the Redis connection to use. The connection will be associated with {@link CacheFrontend} and must
         *        be closed through {@link CacheFrontend#close()}.
         * @param tracking the tracking parameters.
         * @param <K> Key type.
         * @param <V> Value type.
         * @return the {@link CacheFrontend} for value retrieval.
         */
        public static <K, V> CacheFrontend<K, V> enable(CacheAccessor<K, V> cacheAccessor,
                StatefulRedisConnection<K, V> connection, TrackingArgs tracking) {

            connection.sync().clientTracking(tracking);

            return create(cacheAccessor, connection);
        }

        /**
         * Create a server-assisted Client side caching for the given {@link CacheAccessor} and {@link StatefulRedisConnection}.
         * This method expects that client key tracking is already configured.
         * <p>
         * Note that the {@link CacheFrontend} is associated with a Redis connection. Make sure to {@link CacheFrontend#close()
         * close} the frontend object to release the Redis connection after use.
         *
         * @param cacheAccessor the accessor used to interact with the client-side cache.
         * @param connection the Redis connection to use. The connection will be associated with {@link CacheFrontend} and must
         *        be closed through {@link CacheFrontend#close()}.
         * @param <K> Key type.
         * @param <V> Value type.
         * @return the {@link CacheFrontend} for value retrieval.
         */
        public static <K, V> CacheFrontend<K, V> create(CacheAccessor<K, V> cacheAccessor,
                StatefulRedisConnection<K, V> connection) {

            StatefulRedisConnectionImpl<K, V> connectionImpl = (StatefulRedisConnectionImpl) connection;
            RedisCodec<K, V> codec = connectionImpl.getCodec();
            RedisCache<K, V> redisCache = new DefaultRedisCache<>(connection, codec);

            return create(cacheAccessor, redisCache);
        }

        private static <K, V> CacheFrontend<K, V> create(CacheAccessor<K, V> cacheAccessor, RedisCache<K, V> redisCache) {

            ClientSideCaching<K, V> caching = new ClientSideCaching<>(cacheAccessor, redisCache);

            redisCache.addInvalidationListener(caching::notifyInvalidate);
            caching.addInvalidationListener(cacheAccessor::evict);

            return caching;
        }

        private void notifyInvalidate(K key) {

            for (java.util.function.Consumer<K> invalidationListener : invalidationListeners) {
                invalidationListener.accept(key);
            }
        }

        @Override
        public void close() {
            redisCache.close();
        }

        public void addInvalidationListener(java.util.function.Consumer<K> invalidationListener) {
            invalidationListeners.add(invalidationListener);
        }

        @Override
        public V get(K key) {

            V value = cacheAccessor.get(key);

            if (value == null) {
                value = redisCache.get(key);

                if (value != null) {
                    cacheAccessor.put(key, value);
                }
            }

            return value;
        }

        @Override
        public V get(K key, Callable<V> valueLoader) {

            V value = cacheAccessor.get(key);

            if (value == null) {
                value = redisCache.get(key);

                if (value == null) {

                    try {
                        value = valueLoader.call();
                    } catch (Exception e) {
                        throw new ValueRetrievalException(
                                String.format("Value loader %s failed with an exception for key %s", valueLoader, key), e);
                    }

                    if (value == null) {
                        throw new ValueRetrievalException(
                                String.format("Value loader %s returned a null value for key %s", valueLoader, key));
                    }
                    redisCache.put(key, value);

                    // register interest in key
                    redisCache.get(key);
                }

                cacheAccessor.put(key, value);
            }

            return value;
        }

        /**
         * Default {@link RedisCache} implementation using {@code GET} and {@code SET} operations to map cache values to
         * top-level keys.
         *
         * @param <K> Key type.
         * @param <V> Value type.
         */
        static class DefaultRedisCache<K, V> implements RedisCache<K, V> {

            private final StatefulRedisConnection<K, V> connection;
            private final RedisCodec<K, V> codec;

            public DefaultRedisCache(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
                this.connection = connection;
                this.codec = codec;
            }

            @Override
            public V get(K key) {
                return connection.sync().get(key);
            }

            @Override
            public void put(K key, V value) {
                connection.sync().set(key, value);
            }

            @Override
            public void addInvalidationListener(java.util.function.Consumer<? super K> listener) {

                connection.addListener(message -> {
                    if (message.getType().equals("invalidate")) {

                        List<Object> content = message.getContent(codec::decodeKey);
                        List<K> keys = (List<K>) content.get(1);
                        keys.forEach(listener);
                    }
                });
            }

            @Override
            public void close() {
                connection.close();
            }
        }
    }

    /**
     * Wrapper exception to be thrown from {@link CacheFrontend#get(Object, Callable)} in case of the value loader callback
     * failing with an exception.
     */
    @SuppressWarnings("serial")
    static class ValueRetrievalException extends RedisException {

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
