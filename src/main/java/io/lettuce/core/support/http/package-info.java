/**
 * HTTP client infrastructure for lightweight HTTP operations.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a lightweight, dependency-agnostic HTTP client abstraction designed for health checks, REST API calls,
 * and other HTTP-based operations. The implementation uses Netty's HTTP codecs and supports connection reuse, SSL/TLS, custom
 * timeouts, and asynchronous operations.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 * <li>{@link io.lettuce.core.support.http.HttpClient} - Main interface for HTTP operations with connection-based API</li>
 * <li>{@link io.lettuce.core.support.http.HttpClient.HttpConnection} - Reusable HTTP connection for multiple requests</li>
 * <li>{@link io.lettuce.core.support.http.HttpClient.Request} - HTTP request builder with fluent API</li>
 * <li>{@link io.lettuce.core.support.http.HttpClient.Response} - HTTP response with status, headers, and body</li>
 * <li>{@link io.lettuce.core.support.http.HttpClient.ConnectionConfig} - Per-connection configuration (SSL, timeouts)</li>
 * <li>{@link io.lettuce.core.support.http.HttpClientProvider} - SPI for custom HTTP client implementations</li>
 * <li>{@link io.lettuce.core.support.http.HttpClientResources} - Shared resource management with reference counting</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <p>
 * The HTTP client uses a connection-based API where connections can be reused for multiple requests to the same host:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Acquire a shared HTTP client
 *     HttpClient client = HttpClientResources.acquire();
 *
 *     try {
 *         // Configure connection settings
 *         HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().connectionTimeout(5000).readTimeout(5000)
 *                 .build();
 *
 *         // Establish connection (reusable for multiple requests)
 *         try (HttpClient.HttpConnection connection = client.connect(URI.create("https://api.example.com"), config)) {
 *
 *             // Execute first request
 *             HttpClient.Request request1 = HttpClient.Request.get("/v1/health").build();
 *             HttpClient.Response response1 = connection.execute(request1);
 *
 *             if (response1.getStatusCode() == 200) {
 *                 String body = response1.getResponseBody(StandardCharsets.UTF_8);
 *                 System.out.println("Health check: " + body);
 *             }
 *
 *             // Reuse connection for second request
 *             HttpClient.Request request2 = HttpClient.Request.get("/v1/databases").queryParam("fields", "uid,status")
 *                     .header("Authorization", "Bearer token").build();
 *             HttpClient.Response response2 = connection.execute(request2);
 *
 *             // Process response2...
 *         }
 *     } finally {
 *         // Release the reference (client closed when last reference is released)
 *         HttpClientResources.release(client);
 *     }
 * }
 * </pre>
 *
 * <h2>Asynchronous Operations</h2>
 * <p>
 * Both connection establishment and request execution support asynchronous operations:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     HttpClient client = HttpClientResources.acquire();
 *
 *     try {
 *         HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.defaults();
 *
 *         // Async connection
 *         CompletableFuture&lt;HttpClient.HttpConnection&gt; connectionFuture = client
 *                 .connectAsync(URI.create("https://api.example.com"), config);
 *
 *         connectionFuture.thenCompose(connection -&gt; {
 *             HttpClient.Request request = HttpClient.Request.get("/v1/status").build();
 *             // Async request execution
 *             return connection.executeAsync(request);
 *         }).thenAccept(response -&gt; {
 *             System.out.println("Status: " + response.getStatusCode());
 *         }).exceptionally(ex -&gt; {
 *             ex.printStackTrace();
 *             return null;
 *         });
 *     } finally {
 *         HttpClientResources.release(client);
 *     }
 * }
 * </pre>
 *
 * <h2>SSL/TLS Support</h2>
 * <p>
 * HTTPS connections are supported via {@link io.lettuce.core.SslOptions}. SSL options are configured per-connection:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Configure SSL options
 *     SslOptions sslOptions = SslOptions.builder().truststore(new File("/path/to/truststore.jks"), "password".toCharArray())
 *             .protocols("TLSv1.2", "TLSv1.3").build();
 *
 *     // Create connection config with SSL
 *     HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().sslOptions(sslOptions).connectionTimeout(5000)
 *             .readTimeout(5000).build();
 *
 *     HttpClient client = HttpClientResources.acquire();
 *     try (HttpClient.HttpConnection connection = client.connect(URI.create("https://secure-api.example.com"), config)) {
 *         HttpClient.Request request = HttpClient.Request.get("/secure/endpoint").build();
 *         HttpClient.Response response = connection.execute(request);
 *         // Process response...
 *     } finally {
 *         HttpClientResources.release(client);
 *     }
 * }
 * </pre>
 *
 * <h2>Request Builder API</h2>
 * <p>
 * The {@link io.lettuce.core.support.http.HttpClient.Request} interface provides a fluent builder for constructing HTTP
 * requests:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Simple GET request
 *     HttpClient.Request request = HttpClient.Request.get("/api/users").build();
 *
 *     // GET with query parameters
 *     HttpClient.Request request = HttpClient.Request.get("/api/users").queryParam("page", "1").queryParam("limit", "10")
 *             .build();
 *
 *     // GET with custom headers
 *     HttpClient.Request request = HttpClient.Request.get("/api/users").header("Authorization", "Bearer token")
 *             .header("Accept", "application/json").build();
 *
 *     // Combined: path, query params, and headers
 *     HttpClient.Request request = HttpClient.Request.get("/api/databases").queryParam("fields", "uid,name,status")
 *             .header("Authorization", "Basic " + base64Credentials).build();
 * }
 * </pre>
 *
 * <h2>Default Implementation</h2>
 * <p>
 * The default implementation uses Netty's HTTP client ({@code NettyHttpClient}) with the following features:
 * </p>
 * <ul>
 * <li>Shared event loop group for efficient resource usage</li>
 * <li>Configurable thread pool size via {@code -Dio.lettuce.http.eventLoopThreads} system property</li>
 * <li>HTTP/1.1 with keep-alive for connection reuse</li>
 * <li>Sequential request processing to avoid HTTP/1.1 pipelining issues</li>
 * <li>Per-connection SSL/TLS configuration</li>
 * <li>Configurable connection and read timeouts</li>
 * </ul>
 *
 * <h2>Custom Implementations</h2>
 * <p>
 * Custom HTTP client implementations can be provided via the {@link io.lettuce.core.support.http.HttpClientProvider} SPI. To
 * register a custom provider:
 * </p>
 * <ol>
 * <li>Implement {@link io.lettuce.core.support.http.HttpClientProvider}</li>
 * <li>Create a file {@code META-INF/services/io.lettuce.core.support.http.HttpClientProvider}</li>
 * <li>Add the fully qualified class name of your implementation to the file</li>
 * </ol>
 *
 * <h3>Example Custom Provider</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     public class CustomHttpClientProvider implements HttpClientProvider {
 *
 *         &#64;Override
 *         public HttpClient createHttpClient() {
 *             return new CustomHttpClient();
 *         }
 *
 *         &#64;Override
 *         public boolean isAvailable() {
 *             try {
 *                 Class.forName("com.example.CustomHttpClient");
 *                 return true;
 *             } catch (ClassNotFoundException e) {
 *                 return false;
 *             }
 *         }
 *
 *         &#64;Override
 *         public int getPriority() {
 *             return 10; // Higher priority than default (0)
 *         }
 * 
 *     }
 * }
 * </pre>
 *
 * <h2>Resource Management</h2>
 * <p>
 * {@link io.lettuce.core.support.http.HttpClientResources} manages shared HTTP client instances with reference counting:
 * </p>
 * <ul>
 * <li>Lazy initialization - client created on first {@code acquire()}</li>
 * <li>Reference counting - tracks number of active references</li>
 * <li>Automatic cleanup - client closed when last reference is released</li>
 * <li>Thread-safe - safe for concurrent access from multiple threads</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All components in this package are thread-safe:
 * </p>
 * <ul>
 * <li>{@link io.lettuce.core.support.http.HttpClient} - Thread-safe, can be shared across threads</li>
 * <li>{@link io.lettuce.core.support.http.HttpClient.HttpConnection} - Thread-safe for sequential requests</li>
 * <li>{@link io.lettuce.core.support.http.HttpClientResources} - Thread-safe reference counting</li>
 * </ul>
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
package io.lettuce.core.support.http;
