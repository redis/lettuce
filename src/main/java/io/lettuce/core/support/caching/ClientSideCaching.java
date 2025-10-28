package io.lettuce.core.support.caching;

import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility to provide server-side assistance for client-side caches. This is a {@link CacheFrontend} that represents a two-level
 * cache backed by a client-side and a Redis cache.
 *
 * For example:
 *
 * <pre class="code">
 *
 * Map<String, String> clientCache = new ConcurrentHashMap<>();
 *
 * StatefulRedisConnection&lt;String, String&gt; connection = redisClient.connect();
 *
 * CacheFrontend&lt;String, String&gt; frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
 *         TrackingArgs.Builder.enabled());
 *
 * String value = frontend.get(key);
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Yoobin Yoon
 * @since 6.0
 */
public class ClientSideCaching<K, V> implements CacheFrontend<K, V> {

    private final CacheAccessor<K, V> cacheAccessor;

    private final RedisCache<K, V> redisCache;

    private final List<Consumer<K>> invalidationListeners = new CopyOnWriteArrayList<>();

    private final ConcurrentHashMap<K, ReentrantLock> keyLocks = new ConcurrentHashMap<>();

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
     * @param connection the Redis connection to use. The connection will be associated with {@link CacheFrontend} and must be
     *        closed through {@link CacheFrontend#close()}.
     * @param tracking the tracking parameters.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link CacheFrontend} for value retrieval.
     */
    public static <K, V> CacheFrontend<K, V> enable(CacheAccessor<K, V> cacheAccessor, StatefulRedisConnection<K, V> connection,
            TrackingArgs tracking) {

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
     * @param connection the Redis connection to use. The connection will be associated with {@link CacheFrontend} and must be
     *        closed through {@link CacheFrontend#close()}.
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
        keyLocks.remove(key);

        for (java.util.function.Consumer<K> invalidationListener : invalidationListeners) {
            invalidationListener.accept(key);
        }
    }

    @Override
    public void close() {
        keyLocks.clear();
        redisCache.close();
    }

    public void addInvalidationListener(java.util.function.Consumer<K> invalidationListener) {
        invalidationListeners.add(invalidationListener);
    }

    /**
     * Execute the supplied function while holding the lock for the given key.
     *
     * @param key the key to lock
     * @param supplier the function to execute under the lock
     * @return the result of the supplied function
     */
    private <T> T withKeyLock(K key, Supplier<T> supplier) {
        ReentrantLock keyLock = keyLocks.computeIfAbsent(key, k -> new ReentrantLock());
        keyLock.lock();
        try {
            return supplier.get();
        } finally {
            keyLock.unlock();
        }
    }

    @Override
    public V get(K key) {

        V value = cacheAccessor.get(key);

        if (value == null) {
            value = withKeyLock(key, () -> {
                V cachedValue = cacheAccessor.get(key);

                if (cachedValue == null) {
                    V redisValue = redisCache.get(key);

                    if (redisValue != null) {
                        cacheAccessor.put(key, redisValue);
                    }

                    return redisValue;
                }

                return cachedValue;
            });
        }

        return value;
    }

    @Override
    public V get(K key, Callable<V> valueLoader) {

        V value = cacheAccessor.get(key);

        if (value == null) {
            value = withKeyLock(key, () -> {
                V cachedValue = cacheAccessor.get(key);

                if (cachedValue == null) {
                    V redisValue = redisCache.get(key);

                    if (redisValue == null) {
                        try {
                            V loadedValue = valueLoader.call();

                            if (loadedValue == null) {
                                throw new ValueRetrievalException(
                                        String.format("Value loader %s returned a null value for key %s", valueLoader, key));
                            }

                            redisCache.put(key, loadedValue);
                            redisCache.get(key);
                            cacheAccessor.put(key, loadedValue);

                            return loadedValue;
                        } catch (Exception e) {
                            throw new ValueRetrievalException(
                                    String.format("Value loader %s failed with an exception for key %s", valueLoader, key), e);
                        }
                    } else {
                        cacheAccessor.put(key, redisValue);
                        return redisValue;
                    }
                }

                return cachedValue;
            });
        }

        return value;
    }

}
