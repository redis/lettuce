/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.SslOptions;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.AsyncCloseable;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for performing HTTP requests in a dependency-agnostic manner. This interface provides basic HTTP client
 * functionality for health checks and other lightweight HTTP operations.
 * <p>
 * Implementations can be provided via {@link HttpClientProvider} SPI or by using the default Netty-based implementation. The
 * client uses a shared Netty infrastructure (event loops) and supports connection reuse. Once a channel is initialized with
 * specific SSL options and timeouts, it can be reused for multiple HTTP requests to the same host until the connection is
 * closed, times out, or encounters an error.
 * </p>
 * <p>
 * Usage example:
 *
 * <pre>
 * HttpClient client = HttpClientResources.shared();
 * ConnectionConfig config = ConnectionConfig.builder().sslOptions(sslOptions).connectionTimeout(1000).readTimeout(1000)
 *         .build();
 *
 * try (HttpConnection conn = client.connect(URI.create("https://example.com"), config)) {
 *     Request request1 = Request.get("/api/endpoint1").build();
 *     Response response1 = conn.execute(request1);
 *     if (response1.getStatusCode() == 200) {
 *         String body = response1.getResponseBody(StandardCharsets.UTF_8);
 *         // Process response
 *     }
 *
 *     Request request2 = Request.get("/api/endpoint2").queryParam("filter", "active").build();
 *     Response response2 = conn.execute(request2);
 *     // Multiple requests reuse the same connection
 * }
 * </pre>
 * </p>
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 * @see HttpClientProvider
 * @see HttpClientResources
 */
@Experimental
public interface HttpClient extends Closeable {

    /**
     * Establishes an HTTP connection to the specified URI with the given configuration. The returned connection can be reused
     * for multiple HTTP requests to the same host. The connection uses HTTP/1.1 keep-alive to maintain the underlying channel.
     * <p>
     * The SSL options and timeouts are configured once during connection establishment and apply to all requests made through
     * this connection.
     * </p>
     *
     * @param uri the URI to connect to (scheme, host, and port are used), must not be {@code null}.
     * @param connectionConfig the connection-specific configuration (SSL, timeouts), must not be {@code null}.
     * @return an {@link HttpConnection} that can be used to perform multiple requests.
     * @throws IOException if an I/O error occurs during connection establishment.
     */
    HttpConnection connect(URI uri, ConnectionConfig connectionConfig) throws IOException;

    /**
     * Asynchronously establishes an HTTP connection to the specified URI with the given configuration.
     *
     * @param uri the URI to connect to (scheme, host, and port are used), must not be {@code null}.
     * @param connectionConfig the connection-specific configuration (SSL, timeouts), must not be {@code null}.
     * @return a {@link CompletableFuture} that will be completed with an {@link HttpConnection}.
     */
    CompletableFuture<HttpConnection> connectAsync(URI uri, ConnectionConfig connectionConfig);

    /**
     * Closes this HTTP client and releases any resources.
     */
    @Override
    void close();

    /**
     * Represents an HTTP connection that can be reused for multiple requests. The connection is configured with specific SSL
     * options and timeouts during establishment and maintains these settings for all requests.
     * <p>
     * Connections should be closed when no longer needed to release resources.
     * </p>
     */
    interface HttpConnection extends Closeable, AsyncCloseable {

        /**
         * Executes an HTTP request using this connection.
         *
         * @param request the HTTP request to execute, must not be {@code null}.
         * @return the HTTP response including status code, headers, and body.
         * @throws IOException if an I/O error occurs or the connection is closed.
         */
        Response execute(Request request) throws IOException;

        /**
         * Asynchronously executes an HTTP request using this connection.
         *
         * @param request the HTTP request to execute, must not be {@code null}.
         * @return a {@link CompletableFuture} that will be completed with the HTTP response.
         */
        CompletableFuture<Response> executeAsync(Request request);

        /**
         * Checks if this connection is still active and can be used for requests.
         *
         * @return {@code true} if the connection is active, {@code false} otherwise.
         */
        boolean isActive();

        /**
         * Closes this connection and releases any resources.
         */
        @Override
        void close();

        /**
         * Asynchronously closes this connection and releases any resources.
         *
         * @return a {@link CompletableFuture} that will be completed when the connection is closed.
         */
        @Override
        CompletableFuture<Void> closeAsync();

    }

    /**
     * Represents an HTTP response with status code, headers, and body.
     */
    interface Response {

        /**
         * Returns the HTTP status code for the response.
         *
         * @return the status code (e.g., 200, 404, 500).
         */
        int getStatusCode();

        /**
         * Returns the response body as a {@link ByteBuffer}. The buffer is ready to be read (position is 0).
         * <p>
         * Note: This method can be called multiple times and will return the same buffer each time. The buffer should not be
         * modified.
         * </p>
         *
         * @return the response body as a {@link ByteBuffer}.
         */
        ByteBuffer getResponseBodyAsByteBuffer();

        /**
         * Returns the entire response body as a String using the specified charset.
         *
         * @param charset the charset to use when decoding the response body.
         * @return the response body as a String.
         */
        String getResponseBody(Charset charset);

        /**
         * Returns the first value of the specified response header.
         *
         * @param name the header name (case-insensitive).
         * @return the first header value, or {@code null} if the header is not present.
         */
        String getHeader(CharSequence name);

    }

    /**
     * Per-connection configuration for HTTP connections. SSL options and timeouts are set once during connection establishment
     * and apply to all requests made through that connection.
     */
    interface ConnectionConfig {

        /**
         * @return the connection timeout in milliseconds.
         */
        int getConnectionTimeout();

        /**
         * @return the read timeout in milliseconds.
         */
        int getReadTimeout();

        /**
         * @return the SSL options for this connection, may be {@code null} for non-HTTPS or default SSL.
         */
        SslOptions getSslOptions();

        /**
         * Creates a new {@link Builder} for {@link ConnectionConfig}.
         *
         * @return a new {@link Builder}.
         */
        static Builder builder() {
            return new DefaultConnectionConfig.Builder();
        }

        /**
         * Creates a default {@link ConnectionConfig} with standard timeout values.
         *
         * @return a default {@link ConnectionConfig}.
         */
        static ConnectionConfig defaults() {
            return DefaultConnectionConfig.DEFAULTS;
        }

        /**
         * Builder for {@link ConnectionConfig}.
         */
        interface Builder {

            /**
             * Sets the connection timeout.
             *
             * @param timeoutMs the timeout in milliseconds (default: 5000).
             * @return {@code this}.
             */
            Builder connectionTimeout(int timeoutMs);

            /**
             * Sets the read timeout.
             *
             * @param timeoutMs the timeout in milliseconds (default: 5000).
             * @return {@code this}.
             */
            Builder readTimeout(int timeoutMs);

            /**
             * Sets the SSL options for HTTPS connections.
             *
             * @param sslOptions the SSL options.
             * @return {@code this}.
             */
            Builder sslOptions(SslOptions sslOptions);

            /**
             * Builds the {@link ConnectionConfig}.
             *
             * @return the {@link ConnectionConfig}.
             */
            ConnectionConfig build();

        }

    }

    /**
     * HTTP request method.
     */
    enum Method {

        /**
         * HTTP GET method.
         */
        GET

    }

    /**
     * Represents an HTTP request with method, path, query parameters, headers, and optional body. This interface provides a
     * fluent builder API for constructing requests.
     *
     * <h3>Example Usage:</h3>
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     // Simple GET request
     *     Request request = Request.get("/v1/bdbs").build();
     *
     *     // GET with query parameters and headers
     *     Request request = Request.get("/v1/bdbs").queryParam("fields", "uid,endpoints").header("Authorization", "Bearer token")
     *             .build();
     *
     *     // Future: POST with body
     *     // Request request = Request.post("/v1/bdbs")
     *     // .header("Content-Type", "application/json")
     *     // .body("{\"name\":\"mydb\"}")
     *     // .build();
     * }
     * </pre>
     */
    interface Request {

        /**
         * Gets the HTTP method.
         *
         * @return the HTTP method.
         */
        Method getMethod();

        /**
         * Gets the request path (e.g., "/v1/bdbs").
         *
         * @return the request path.
         */
        String getPath();

        /**
         * Gets the query parameters as a map.
         *
         * @return an unmodifiable map of query parameters, never {@code null}.
         */
        java.util.Map<String, String> getQueryParams();

        /**
         * Gets the request headers as a map.
         *
         * @return an unmodifiable map of headers, never {@code null}.
         */
        java.util.Map<String, String> getHeaders();

        /**
         * Gets the full request URI including path and query string.
         *
         * @return the full URI (e.g., "/v1/bdbs?fields=uid,endpoints").
         */
        String getUri();

        /**
         * Creates a new GET request builder.
         *
         * @param path the request path, must not be {@code null}.
         * @return a new request builder.
         */
        static RequestBuilder get(String path) {
            return DefaultRequestBuilder.get(path);
        }

        /**
         * Builder for constructing HTTP requests.
         */
        interface RequestBuilder {

            /**
             * Adds a query parameter to the request.
             *
             * @param name the parameter name, must not be {@code null}.
             * @param value the parameter value, must not be {@code null}.
             * @return {@code this}.
             */
            RequestBuilder queryParam(String name, String value);

            /**
             * Adds multiple query parameters to the request.
             *
             * @param params the parameters to add, must not be {@code null}.
             * @return {@code this}.
             */
            RequestBuilder queryParams(java.util.Map<String, String> params);

            /**
             * Adds a header to the request.
             *
             * @param name the header name, must not be {@code null}.
             * @param value the header value, must not be {@code null}.
             * @return {@code this}.
             */
            RequestBuilder header(String name, String value);

            /**
             * Adds multiple headers to the request.
             *
             * @param headers the headers to add, must not be {@code null}.
             * @return {@code this}.
             */
            RequestBuilder headers(java.util.Map<String, String> headers);

            /**
             * Builds the request.
             *
             * @return the constructed request.
             */
            Request build();

        }

    }

}
