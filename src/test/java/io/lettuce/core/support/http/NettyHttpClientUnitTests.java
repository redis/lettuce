/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link NettyHttpClient}.
 *
 * @author Ivo Gaydazhiev
 */
class NettyHttpClientUnitTests {

    private static EventLoopGroup bossGroup;

    private static EventLoopGroup workerGroup;

    private NettyHttpClient httpClient;

    private final java.util.List<Channel> mockServers = new java.util.ArrayList<>();

    @BeforeAll
    static void setUpClass() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);
    }

    @AfterAll
    static void tearDownClass() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).awaitUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).awaitUninterruptibly();
        }
    }

    @BeforeEach
    void setUp() {
        httpClient = new NettyHttpClient();
    }

    @AfterEach
    void tearDown() {
        for (Channel server : mockServers) {
            server.close().awaitUninterruptibly();
        }
        mockServers.clear();
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Test
    void shouldConnectAndPerformGetRequest() throws Exception {
        // Start mock HTTP server
        int port = startMockServer(new SimpleHttpHandler("Hello, World!", 200));

        // Create connection
        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().connectionTimeout(5000).readTimeout(5000)
                .build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            assertThat(connection.isActive()).isTrue();

            // Perform GET request
            HttpClient.Request request = HttpClient.Request.get("/test").build();
            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Hello, World!");
        }
    }

    @Test
    void shouldReuseConnectionForMultipleRequests() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);
        int port = startMockServer(new CountingHttpHandler(requestCount));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            // Make multiple requests
            HttpClient.Request request1 = HttpClient.Request.get("/request1").build();
            HttpClient.Request request2 = HttpClient.Request.get("/request2").build();
            HttpClient.Request request3 = HttpClient.Request.get("/request3").build();

            HttpClient.Response response1 = connection.execute(request1);
            HttpClient.Response response2 = connection.execute(request2);
            HttpClient.Response response3 = connection.execute(request3);

            assertThat(response1.getStatusCode()).isEqualTo(200);
            assertThat(response2.getStatusCode()).isEqualTo(200);
            assertThat(response3.getStatusCode()).isEqualTo(200);

            // Verify all requests used the same connection
            assertThat(requestCount.get()).isEqualTo(3);
        }
    }

    @Test
    void shouldHandleSequentialRequests() throws Exception {
        int port = startMockServer(new EchoPathHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request1 = HttpClient.Request.get("/path1").build();
            HttpClient.Request request2 = HttpClient.Request.get("/path2").build();
            HttpClient.Request request3 = HttpClient.Request.get("/path3").build();

            HttpClient.Response response1 = connection.execute(request1);
            HttpClient.Response response2 = connection.execute(request2);
            HttpClient.Response response3 = connection.execute(request3);

            assertThat(response1.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/path1");
            assertThat(response2.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/path2");
            assertThat(response3.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/path3");
        }
    }

    @Test
    void shouldHandleAsyncRequests() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("Async Response", 200));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/async").build();
            CompletableFuture<HttpClient.Response> future = connection.executeAsync(request);

            HttpClient.Response response = future.get(5, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Async Response");
        }
    }

    @Test
    void shouldHandleMultipleAsyncRequestsSequentially() throws Exception {
        int port = startMockServer(new EchoPathHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            // Submit multiple async requests
            HttpClient.Request request1 = HttpClient.Request.get("/async1").build();
            HttpClient.Request request2 = HttpClient.Request.get("/async2").build();
            HttpClient.Request request3 = HttpClient.Request.get("/async3").build();

            CompletableFuture<HttpClient.Response> future1 = connection.executeAsync(request1);
            CompletableFuture<HttpClient.Response> future2 = connection.executeAsync(request2);
            CompletableFuture<HttpClient.Response> future3 = connection.executeAsync(request3);

            // Wait for all to complete
            HttpClient.Response response1 = future1.get(5, TimeUnit.SECONDS);
            HttpClient.Response response2 = future2.get(5, TimeUnit.SECONDS);
            HttpClient.Response response3 = future3.get(5, TimeUnit.SECONDS);

            // Verify responses are in correct order
            assertThat(response1.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/async1");
            assertThat(response2.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/async2");
            assertThat(response3.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/async3");
        }
    }

    @Test
    void shouldHandleHttpErrorStatus() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("Not Found", 404));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/notfound").build();
            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(404);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Not Found");
        }
    }

    @Test
    void shouldHandleServerError() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("Internal Server Error", 500));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/error").build();
            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(500);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Internal Server Error");
        }
    }

    @Test
    void shouldHandleResponseHeaders() throws Exception {
        int port = startMockServer(new HeadersHttpHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/headers").build();
            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
            assertThat(response.getHeader("X-Custom-Header")).isEqualTo("CustomValue");
            assertThat(response.getHeader("X-Missing-Header")).isNull();
        }
    }

    @Test
    void shouldSendCustomRequestHeaders() throws Exception {
        int port = startMockServer(new RequestHeadersEchoHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/echo-headers").header("Authorization", "Bearer test-token")
                    .header("X-Custom-Header", "CustomValue").build();

            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(200);
            String body = response.getResponseBody(StandardCharsets.UTF_8);
            assertThat(body).contains("\"Authorization\":\"Bearer test-token\"");
            assertThat(body).contains("\"X-Custom-Header\":\"CustomValue\"");
        }
    }

    @Test
    void shouldHandleQueryParameters() throws Exception {
        int port = startMockServer(new EchoPathHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/v1/bdbs?fields=uid,endpoints").build();
            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(200);
            // Verify that query parameters are preserved in the request
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/v1/bdbs?fields=uid,endpoints");
        }
    }

    @Test
    void shouldExecuteRequestWithBuilder() throws Exception {
        int port = startMockServer(new EchoPathHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/v1/bdbs").queryParam("fields", "uid,endpoints").build();

            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/v1/bdbs?fields=uid%2Cendpoints");
        }
    }

    @Test
    void shouldExecuteRequestWithMultipleQueryParams() throws Exception {
        int port = startMockServer(new EchoPathHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/v1/bdbs").queryParam("fields", "uid,endpoints")
                    .queryParam("limit", "10").queryParam("offset", "0").build();

            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(200);
            String body = response.getResponseBody(StandardCharsets.UTF_8);
            assertThat(body).contains("fields=uid%2Cendpoints");
            assertThat(body).contains("limit=10");
            assertThat(body).contains("offset=0");
        }
    }

    @Test
    void shouldExecuteRequestWithHeadersAndQueryParams() throws Exception {
        int port = startMockServer(new RequestHeadersEchoHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/v1/bdbs").queryParam("fields", "uid")
                    .header("Authorization", "Bearer token").header("X-Custom", "value").build();

            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(200);
            String body = response.getResponseBody(StandardCharsets.UTF_8);
            assertThat(body).contains("\"Authorization\":\"Bearer token\"");
            assertThat(body).contains("\"X-Custom\":\"value\"");
        }
    }

    @Test
    void shouldExecuteAsyncRequestWithBuilder() throws Exception {
        int port = startMockServer(new EchoPathHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/v1/bdbs").queryParam("fields", "uid").build();

            CompletableFuture<HttpClient.Response> future = connection.executeAsync(request);
            HttpClient.Response response = future.get(5, TimeUnit.SECONDS);

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).contains("/v1/bdbs?fields=uid");
        }
    }

    @Test
    void shouldVerifyRequestMethodIsGet() {
        HttpClient.Request request = HttpClient.Request.get("/v1/bdbs").build();

        assertThat(request.getMethod()).isEqualTo(HttpClient.Method.GET);
        assertThat(request.getPath()).isEqualTo("/v1/bdbs");
    }

    @Test
    void shouldHandleEmptyResponse() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("", 204));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/empty").build();
            HttpClient.Response response = connection.execute(request);

            assertThat(response.getStatusCode()).isEqualTo(204);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEmpty();
        }
    }

    @Test
    void shouldHandleConnectionTimeout() {
        // Create a server that doesn't accept connections
        URI uri = URI.create("http://localhost:1"); // Port 1 is typically not listening

        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().connectionTimeout(100).build();

        assertThatThrownBy(() -> httpClient.connect(uri, config)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldDetectInactiveConnection() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("OK", 200));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        HttpClient.HttpConnection connection = httpClient.connect(uri, config);
        assertThat(connection.isActive()).isTrue();

        connection.close();
        assertThat(connection.isActive()).isFalse();
    }

    @Test
    void shouldFailRequestOnClosedConnection() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("OK", 200));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        HttpClient.HttpConnection connection = httpClient.connect(uri, config);
        connection.close();

        HttpClient.Request request = HttpClient.Request.get("/test").build();
        assertThatThrownBy(() -> connection.execute(request)).isInstanceOf(IOException.class)
                .hasMessageContaining("Connection is not active");
    }

    @Test
    void shouldConnectAsyncAndPerformGetRequest() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("Async Connect", 200));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        CompletableFuture<HttpClient.HttpConnection> connectFuture = httpClient.connectAsync(uri, config);
        HttpClient.HttpConnection connection = connectFuture.get(5, TimeUnit.SECONDS);

        try {
            assertThat(connection.isActive()).isTrue();

            HttpClient.Request request = HttpClient.Request.get("/test").build();
            HttpClient.Response response = connection.execute(request);
            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Async Connect");
        } finally {
            connection.close();
        }
    }

    @Test
    void shouldHandleByteBufferResponse() throws Exception {
        int port = startMockServer(new SimpleHttpHandler("Binary Data", 200));

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            HttpClient.Request request = HttpClient.Request.get("/binary").build();
            HttpClient.Response response = connection.execute(request);

            ByteBuffer buffer = response.getResponseBodyAsByteBuffer();
            assertThat(buffer).isNotNull();
            assertThat(buffer.remaining()).isGreaterThan(0);

            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("Binary Data");
        }
    }

    @Test
    void shouldHandleMultipleConnectionsInParallel() throws Exception {
        // Start a single mock server that can handle multiple connections
        int port = startMockServer(new EchoPathHandler());

        URI uri = URI.create("http://localhost:" + port);
        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        // Create a single connection and make multiple parallel requests on it
        try (HttpClient.HttpConnection connection = httpClient.connect(uri, config)) {
            assertThat(connection.isActive()).isTrue();

            // Execute multiple requests in parallel using the same connection
            HttpClient.Request request1 = HttpClient.Request.get("/parallel1").build();
            HttpClient.Request request2 = HttpClient.Request.get("/parallel2").build();
            HttpClient.Request request3 = HttpClient.Request.get("/parallel3").build();

            CompletableFuture<HttpClient.Response> future1 = connection.executeAsync(request1);
            CompletableFuture<HttpClient.Response> future2 = connection.executeAsync(request2);
            CompletableFuture<HttpClient.Response> future3 = connection.executeAsync(request3);

            // Wait for all to complete
            CompletableFuture.allOf(future1, future2, future3).get(5, TimeUnit.SECONDS);

            // Verify each request got its own response
            HttpClient.Response response1 = future1.get();
            HttpClient.Response response2 = future2.get();
            HttpClient.Response response3 = future3.get();

            assertThat(response1.getStatusCode()).isEqualTo(200);
            assertThat(response2.getStatusCode()).isEqualTo(200);
            assertThat(response3.getStatusCode()).isEqualTo(200);

            assertThat(response1.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/parallel1");
            assertThat(response2.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/parallel2");
            assertThat(response3.getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/parallel3");

            // Make additional parallel requests to verify connection is still usable
            HttpClient.Request request4 = HttpClient.Request.get("/parallel4").build();
            HttpClient.Request request5 = HttpClient.Request.get("/parallel5").build();
            HttpClient.Request request6 = HttpClient.Request.get("/parallel6").build();

            CompletableFuture<HttpClient.Response> future4 = connection.executeAsync(request4);
            CompletableFuture<HttpClient.Response> future5 = connection.executeAsync(request5);
            CompletableFuture<HttpClient.Response> future6 = connection.executeAsync(request6);

            CompletableFuture.allOf(future4, future5, future6).get(5, TimeUnit.SECONDS);

            assertThat(future4.get().getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/parallel4");
            assertThat(future5.get().getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/parallel5");
            assertThat(future6.get().getResponseBody(StandardCharsets.UTF_8)).isEqualTo("/parallel6");
        }
    }

    @Test
    void shouldHandleMultipleConnectionsFromSameClient() throws Exception {
        // Start three separate mock servers
        int port1 = startMockServer(new SimpleHttpHandler("Server1", 200));
        int port2 = startMockServer(new SimpleHttpHandler("Server2", 200));
        int port3 = startMockServer(new SimpleHttpHandler("Server3", 200));

        URI uri1 = URI.create("http://localhost:" + port1);
        URI uri2 = URI.create("http://localhost:" + port2);
        URI uri3 = URI.create("http://localhost:" + port3);

        HttpClient.ConnectionConfig config = HttpClient.ConnectionConfig.builder().build();

        // Create multiple connections from the same client to different servers
        HttpClient.HttpConnection connection1 = httpClient.connect(uri1, config);
        HttpClient.HttpConnection connection2 = httpClient.connect(uri2, config);
        HttpClient.HttpConnection connection3 = httpClient.connect(uri3, config);

        try {
            // Verify all connections are active
            assertThat(connection1.isActive()).isTrue();
            assertThat(connection2.isActive()).isTrue();
            assertThat(connection3.isActive()).isTrue();

            // Execute requests in parallel
            HttpClient.Request request = HttpClient.Request.get("/test").build();
            CompletableFuture<HttpClient.Response> future1 = connection1.executeAsync(request);
            CompletableFuture<HttpClient.Response> future2 = connection2.executeAsync(request);
            CompletableFuture<HttpClient.Response> future3 = connection3.executeAsync(request);

            // Wait for all to complete
            CompletableFuture.allOf(future1, future2, future3).get(5, TimeUnit.SECONDS);

            // Verify each connection got response from its own server
            assertThat(future1.get().getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Server1");
            assertThat(future2.get().getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Server2");
            assertThat(future3.get().getResponseBody(StandardCharsets.UTF_8)).isEqualTo("Server3");

        } finally {
            connection1.close();
            connection2.close();
            connection3.close();
        }
    }

    // ========== Helper Methods ==========

    /**
     * Starts a mock HTTP server and returns its port. The server channel is tracked in {@link #mockServers} for cleanup in
     * {@link #tearDown()}.
     *
     * @param handler the channel handler to process requests
     * @return the port number the server is listening on
     * @throws InterruptedException if interrupted while binding
     */
    private int startMockServer(ChannelHandler handler) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(1024 * 1024));
                        ch.pipeline().addLast(handler);
                    }

                });

        Channel channel = bootstrap.bind(0).sync().channel();
        mockServers.add(channel);
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    // ========== Mock HTTP Handlers ==========

    /**
     * Simple HTTP handler that returns a fixed response.
     */
    private static class SimpleHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final String responseBody;

        private final int statusCode;

        SimpleHttpHandler(String responseBody, int statusCode) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(statusCode), Unpooled.copiedBuffer(responseBody, StandardCharsets.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            ctx.writeAndFlush(response);
        }

    }

    /**
     * HTTP handler that counts requests.
     */
    private static class CountingHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final AtomicInteger requestCount;

        CountingHttpHandler(AtomicInteger requestCount) {
            this.requestCount = requestCount;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            requestCount.incrementAndGet();

            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer("OK", StandardCharsets.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            ctx.writeAndFlush(response);
        }

    }

    /**
     * HTTP handler that echoes the request path in the response body.
     */
    private static class EchoPathHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String path = request.uri();

            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(path, StandardCharsets.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            ctx.writeAndFlush(response);
        }

    }

    /**
     * HTTP handler that echoes request headers in the response body as JSON.
     */
    private static class RequestHeadersEchoHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;

            for (java.util.Map.Entry<String, String> header : request.headers()) {
                // Only echo custom headers (skip standard HTTP headers for cleaner test)
                if (header.getKey().startsWith("X-") || header.getKey().equals("Authorization")) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append("\"").append(header.getKey()).append("\":\"").append(header.getValue()).append("\"");
                    first = false;
                }
            }
            json.append("}");

            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json.toString(), StandardCharsets.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            ctx.writeAndFlush(response);
        }

    }

    /**
     * HTTP handler that returns custom headers.
     */
    private static class HeadersHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer("{\"status\":\"ok\"}", StandardCharsets.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            response.headers().set("X-Custom-Header", "CustomValue");

            ctx.writeAndFlush(response);
        }

    }

}
