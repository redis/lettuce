/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.annotations.Experimental;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ServiceLoader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages shared {@link HttpClient} instances with reference counting. The client is lazily initialized on first use and
 * automatically closed when the last reference is released.
 * <p>
 * Use {@link #acquire()} to get a client reference and {@link #release(HttpClient)} when done. The client will be automatically
 * closed when the last reference is released.
 * </p>
 * <p>
 * Example usage:
 * </p>
 *
 * <pre class="code">
 * HttpClient client = HttpClientResources.acquire();
 *
 * try {
 *     // Use the client
 *     ByteBuffer response = client.get(new URL("https://example.com/health"));
 * } finally {
 *     // Release the reference
 *     HttpClientResources.release(client);
 * }
 * </pre>
 * <p>
 * This class is thread-safe.
 * </p>
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 * @see HttpClient
 * @see HttpClientProvider
 */
@Experimental
public class HttpClientResources {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpClientResources.class);

    private static final Lock lock = new ReentrantLock();

    private static volatile HttpClient sharedClient;

    private static volatile HttpClientProvider cachedProvider;

    private static long refCount = 0;

    /**
     * Acquires a reference to the shared {@link HttpClient} instance. The client is lazily initialized on first call and uses
     * shared Netty infrastructure. Connections created through this client can have different SSL configurations and timeouts.
     * <p>
     * Each call to {@code acquire()} must be matched with a corresponding call to {@link #release(HttpClient)}. The client will
     * be automatically closed when the last reference is released.
     * </p>
     *
     * @return the shared {@link HttpClient} instance.
     * @see #release(HttpClient)
     */
    public static HttpClient acquire() {

        lock.lock();
        try {
            if (sharedClient == null) {
                logger.debug("Creating shared HTTP client");
                HttpClientProvider provider = getProvider();
                sharedClient = provider.createHttpClient();
                logger.info("Shared HTTP client created using provider: {}", provider.getClass().getName());
            }

            refCount++;
            logger.debug("Acquired HTTP client reference, ref count: {}", refCount);

            return sharedClient;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases a reference to the shared {@link HttpClient} instance. When the last reference is released (reference count
     * reaches zero), the client is automatically closed and all resources are released.
     * <p>
     * This method should be called in a {@code finally} block to ensure the reference is always released, even if an exception
     * occurs.
     * </p>
     *
     * @param client the {@link HttpClient} instance to release (must be the instance returned by {@link #acquire()}).
     */
    public static void release(HttpClient client) {

        if (client == null) {
            return;
        }

        lock.lock();
        try {
            if (client != sharedClient) {
                logger.warn("Attempting to release an HTTP client that is not the shared instance");
                return;
            }

            if (refCount < 1) {
                logger.warn("Attempting to release HTTP client but ref count is {}", refCount);
                return;
            }

            refCount--;
            logger.debug("Released HTTP client reference, ref count: {}", refCount);

            if (refCount == 0) {
                logger.info("Last HTTP client reference released, shutting down client");
                try {
                    sharedClient.close();
                } catch (Exception e) {
                    logger.warn("Error closing HTTP client during release", e);
                } finally {
                    sharedClient = null;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Discovers and returns the best available {@link HttpClientProvider}. Providers are discovered via Java's
     * {@link ServiceLoader} mechanism. The provider with the highest priority that is available will be used.
     *
     * @return the {@link HttpClientProvider}.
     */
    private static HttpClientProvider getProvider() {

        if (cachedProvider != null) {
            return cachedProvider;
        }

        lock.lock();
        try {
            if (cachedProvider != null) {
                return cachedProvider;
            }

            HttpClientProvider selectedProvider = null;
            int highestPriority = Integer.MIN_VALUE;

            ServiceLoader<HttpClientProvider> loader = ServiceLoader.load(HttpClientProvider.class);

            for (HttpClientProvider provider : loader) {
                if (provider.isAvailable() && provider.getPriority() > highestPriority) {
                    selectedProvider = provider;
                    highestPriority = provider.getPriority();
                    logger.debug("Found HTTP client provider: {} with priority {}", provider.getClass().getName(),
                            provider.getPriority());
                }
            }

            if (selectedProvider == null) {
                logger.debug("No custom HTTP client provider found, using default Netty-based provider");
                selectedProvider = new NettyHttpClientProvider();
            }

            cachedProvider = selectedProvider;
            logger.info("Using HTTP client provider: {}", selectedProvider.getClass().getName());

            return cachedProvider;
        } finally {
            lock.unlock();
        }
    }

}
