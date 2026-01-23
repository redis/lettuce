/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.annotations.Experimental;

/**
 * SPI (Service Provider Interface) for providing custom {@link HttpClient} implementations. Implementations of this interface
 * can be registered via Java's {@link java.util.ServiceLoader} mechanism to provide alternative HTTP client implementations.
 * <p>
 * To register a custom provider, create a file named {@code META-INF/services/io.lettuce.core.http.HttpClientProvider}
 * containing the fully qualified class name of your implementation.
 * </p>
 * <p>
 * The HTTP client uses a shared infrastructure (event loops) and supports per-connection configuration. Connections are
 * established with specific SSL options and timeouts that apply to all requests made through that connection.
 * </p>
 * <p>
 * Example implementation:
 * </p>
 *
 * <pre class="code">
 * public class MyHttpClientProvider implements HttpClientProvider {
 *
 *     &#64;Override
 *     public HttpClient createHttpClient() {
 *         return new MyHttpClient();
 *     }
 *
 *     &#64;Override
 *     public boolean isAvailable() {
 *         try {
 *             Class.forName("com.example.MyHttpClient");
 *             return true;
 *         } catch (ClassNotFoundException e) {
 *             return false;
 *         }
 *     }
 *
 * }
 * </pre>
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 * @see HttpClient
 * @see HttpClientResources
 */
@Experimental
public interface HttpClientProvider {

    /**
     * Creates a new {@link HttpClient} instance. The client uses shared infrastructure and supports per-connection
     * configuration.
     *
     * @return a new {@link HttpClient} instance.
     */
    HttpClient createHttpClient();

    /**
     * Returns {@code true} if this provider is available and can create HTTP clients. This method should check if all required
     * dependencies are present on the classpath.
     *
     * @return {@code true} if this provider is available.
     */
    boolean isAvailable();

    /**
     * Returns the priority of this provider. Higher priority providers are preferred over lower priority ones. The default
     * Netty-based provider has priority 0.
     *
     * @return the priority of this provider.
     */
    default int getPriority() {
        return 0;
    }

}
