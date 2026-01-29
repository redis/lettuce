package io.lettuce.core.support.caching;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

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
 * @since 6.0
 */
public class ClientSideCaching<K, V> implements CacheFrontend<K, V> {

    private final CacheAccessor<K, V> cacheAccessor;

    private final RedisCache<K, V> redisCache;

    private final List<Consumer<K>> invalidationListeners = new CopyOnWriteArrayList<>();

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
        return enable(cacheAccessor, connection, tracking, true);
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
     * @param closeConnection {@code true} to close the connection when the {@link CacheFrontend} is closed; {@code false} to
     *        keep the connection open.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link CacheFrontend} for value retrieval.
     * @since 6.5
     */
    public static <K, V> CacheFrontend<K, V> enable(CacheAccessor<K, V> cacheAccessor, StatefulRedisConnection<K, V> connection,
            TrackingArgs tracking, boolean closeConnection) {

        connection.sync().clientTracking(tracking);

        return create(cacheAccessor, connection, closeConnection);
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
        return create(cacheAccessor, connection, true);
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
     * @param closeConnection {@code true} to close the connection when the {@link CacheFrontend} is closed; {@code false} to
     *        keep the connection open.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link CacheFrontend} for value retrieval.
     * @since 6.5
     */
    public static <K, V> CacheFrontend<K, V> create(CacheAccessor<K, V> cacheAccessor, StatefulRedisConnection<K, V> connection,
            boolean closeConnection) {

        StatefulRedisConnectionImpl<K, V> connectionImpl = (StatefulRedisConnectionImpl) connection;
        RedisCodec<K, V> codec = connectionImpl.getCodec();
        RedisCache<K, V> redisCache = new DefaultRedisCache<>(connection, codec, closeConnection);

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

}
