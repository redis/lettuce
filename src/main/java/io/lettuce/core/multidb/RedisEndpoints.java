package io.lettuce.core.multidb;

import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.LettuceAssert;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe container for managing multiple Redis endpoints with an active endpoint selection.
 * <p>
 * This class maintains a set of available Redis URIs and tracks which one is currently active. All operations are thread-safe
 * using a {@link ReadWriteLock} to allow concurrent reads while ensuring exclusive writes.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * 
 * {
 *     &#64;code
 *     RedisEndpoints endpoints = RedisEndpoints.create(RedisURI.create("redis://localhost:6379"));
 * 
 *     // Add additional endpoints
 *     endpoints.add(RedisURI.create("redis://localhost:6380"));
 *     endpoints.add(RedisURI.create("redis://localhost:6381"));
 * 
 *     // Get active endpoint
 *     RedisURI active = endpoints.getActive();
 * 
 *     // Switch to different endpoint
 *     endpoints.setActive(RedisURI.create("redis://localhost:6380"));
 * 
 *     // Remove endpoint
 *     endpoints.remove(RedisURI.create("redis://localhost:6381"));
 * }
 * </pre>
 *
 * @author Ivo Gaydazhiev
 * @since 7.0
 */
public class RedisEndpoints {

    private final Set<RedisURI> endpoints;

    private volatile RedisURI activeEndpoint;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();

    private final Lock writeLock = lock.writeLock();

    /**
     * Creates a new {@link RedisEndpoints} instance with no endpoints.
     */
    public RedisEndpoints() {
        this.endpoints = new CopyOnWriteArraySet<>();
        this.activeEndpoint = null;
    }

    /**
     * Creates a new {@link RedisEndpoints} instance with a single initial endpoint set as active.
     *
     * @param initialEndpoint the initial endpoint, must not be {@code null}
     * @return a new {@link RedisEndpoints} instance
     */
    public static RedisEndpoints create(RedisURI initialEndpoint) {
        LettuceAssert.notNull(initialEndpoint, "Initial endpoint must not be null");
        RedisEndpoints redisEndpoints = new RedisEndpoints();
        redisEndpoints.add(initialEndpoint);
        redisEndpoints.setActive(initialEndpoint);
        return redisEndpoints;
    }

    /**
     * Creates a new {@link RedisEndpoints} instance with multiple initial endpoints. The first endpoint in the collection will
     * be set as active.
     *
     * @param initialEndpoints collection of initial endpoints, must not be {@code null} or empty
     * @return a new {@link RedisEndpoints} instance
     */
    public static RedisEndpoints create(Collection<RedisURI> initialEndpoints) {
        LettuceAssert.notNull(initialEndpoints, "Initial endpoints must not be null");
        LettuceAssert.isTrue(!initialEndpoints.isEmpty(), "Initial endpoints must not be empty");

        RedisEndpoints redisEndpoints = new RedisEndpoints();
        initialEndpoints.forEach(redisEndpoints::add);
        redisEndpoints.setActive(initialEndpoints.iterator().next());
        return redisEndpoints;
    }

    /**
     * Adds a new endpoint to the available endpoints.
     *
     * @param endpoint the endpoint to add, must not be {@code null}
     * @return {@code true} if the endpoint was added, {@code false} if it already exists
     */
    public boolean add(RedisURI endpoint) {
        LettuceAssert.notNull(endpoint, "Endpoint must not be null");

        writeLock.lock();
        try {
            return endpoints.add(endpoint);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Removes an endpoint from the available endpoints.
     * <p>
     * If the removed endpoint is currently active, the active endpoint will be set to {@code null}.
     * </p>
     *
     * @param endpoint the endpoint to remove, must not be {@code null}
     * @return {@code true} if the endpoint was removed, {@code false} if it didn't exist
     * @throws RedisException if attempting to remove the last endpoint
     */
    public boolean remove(RedisURI endpoint) {
        LettuceAssert.notNull(endpoint, "Endpoint must not be null");

        writeLock.lock();
        try {
            if (endpoints.size() == 1 && endpoints.contains(endpoint)) {
                throw new RedisException("Cannot remove the last endpoint");
            }

            boolean removed = endpoints.remove(endpoint);

            if (removed && endpoint.equals(activeEndpoint)) {
                activeEndpoint = null;
            }

            return removed;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Sets the active endpoint.
     * <p>
     * The endpoint must already exist in the available endpoints.
     * </p>
     *
     * @param endpoint the endpoint to set as active, must not be {@code null}
     * @throws RedisException if the endpoint is not in the available endpoints
     */
    public void setActive(RedisURI endpoint) {
        LettuceAssert.notNull(endpoint, "Endpoint must not be null");

        writeLock.lock();
        try {
            if (!endpoints.contains(endpoint)) {
                throw new RedisException(
                        String.format("Endpoint %s is not available. Available endpoints: %s", endpoint, endpoints));
            }
            this.activeEndpoint = endpoint;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Gets the currently active endpoint.
     *
     * @return the active endpoint
     * @throws RedisException if no active endpoint is set
     */
    public RedisURI getActive() {
        readLock.lock();
        try {
            if (activeEndpoint == null) {
                throw new RedisException("No active endpoint is set");
            }
            return activeEndpoint;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Checks if an endpoint is currently active.
     *
     * @param endpoint the endpoint to check, must not be {@code null}
     * @return {@code true} if the endpoint is active, {@code false} otherwise
     */
    public boolean isActive(RedisURI endpoint) {
        LettuceAssert.notNull(endpoint, "Endpoint must not be null");

        readLock.lock();
        try {
            return endpoint.equals(activeEndpoint);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Checks if an endpoint exists in the available endpoints.
     *
     * @param endpoint the endpoint to check, must not be {@code null}
     * @return {@code true} if the endpoint exists, {@code false} otherwise
     */
    public boolean contains(RedisURI endpoint) {
        LettuceAssert.notNull(endpoint, "Endpoint must not be null");

        readLock.lock();
        try {
            return endpoints.contains(endpoint);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Gets all available endpoints.
     *
     * @return an unmodifiable set of all available endpoints
     */
    public Set<RedisURI> getEndpoints() {
        readLock.lock();
        try {
            return Collections.unmodifiableSet(new LinkedHashSet<>(endpoints));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Gets the number of available endpoints.
     *
     * @return the number of endpoints
     */
    public int size() {
        readLock.lock();
        try {
            return endpoints.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Checks if there are any available endpoints.
     *
     * @return {@code true} if there are no endpoints, {@code false} otherwise
     */
    public boolean isEmpty() {
        readLock.lock();
        try {
            return endpoints.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Checks if an active endpoint is set.
     *
     * @return {@code true} if an active endpoint is set, {@code false} otherwise
     */
    public boolean hasActive() {
        readLock.lock();
        try {
            return activeEndpoint != null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String toString() {
        readLock.lock();
        try {
            return "RedisEndpoints{" + "endpoints=" + endpoints + ", activeEndpoint=" + activeEndpoint + '}';
        } finally {
            readLock.unlock();
        }
    }

}
