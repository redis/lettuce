/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.annotations.Experimental;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ServiceLoader;

/**
 * Manages a shared {@link HttpClient} instance with lazy initialization.
 * <p>
 * Use {@link #get()} to obtain the shared client instance. The client is intended to be a long-lived singleton that is never
 * closed during application lifetime.
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

    private HttpClientResources() {

    }

    private static HttpClientProvider discoverProvider() {
        HttpClientProvider selectedProvider = null;
        int highestPriority = Integer.MIN_VALUE;

        ServiceLoader<HttpClientProvider> loader = ServiceLoader.load(HttpClientProvider.class);

        for (HttpClientProvider p : loader) {
            if (p.isAvailable() && p.getPriority() > highestPriority) {
                selectedProvider = p;
                highestPriority = p.getPriority();
                logger.debug("Found HTTP client provider: {} with priority {}", p.getClass().getName(), p.getPriority());
            }
        }

        if (selectedProvider == null) {
            throw new IllegalStateException(
                    "No HTTP client provider available. To use default HTTP client provider, add dependency io.netty:netty-codec-http. For custom provider, implement and register an SPI implementation of HttpClientProvider.");
        }

        logger.debug("Using HTTP client provider: {}", selectedProvider.getClass().getName());
        return selectedProvider;
    }

    /**
     * Holder class for lazy initialization of the shared {@link HttpClient}.
     */
    private static class ClientHolder {

        static final HttpClient INSTANCE = createClient();

        private static HttpClient createClient() {
            logger.debug("Creating shared HTTP client");
            HttpClientProvider provider = discoverProvider();
            HttpClient client = provider.createHttpClient();
            logger.debug("Shared HTTP client created using provider: {}", provider.getClass().getName());
            return client;
        }

    }

    /**
     * Returns the shared {@link HttpClient} instance. The client is lazily initialized on first call The client uses shared
     * Netty infrastructure. Connections created through this client can have different SSL configurations and timeouts.
     *
     * @return the shared {@link HttpClient} instance.
     */
    public static HttpClient get() {
        return ClientHolder.INSTANCE;
    }

}
