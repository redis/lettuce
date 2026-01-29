/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.LettuceAssert;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;

/**
 * Netty-based implementation of {@link HttpClient}. This implementation uses Netty's http codecs to implement a asynchronous
 * HTTP client. The client uses a shared event loop group while supporting per-connection SSL configurations and timeouts.
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
class NettyHttpClient implements HttpClient {

    /**
     * Default port for HTTPS.
     */
    private static final int DEFAULT_HTTPS_PORT = 443;

    /**
     * Default port for HTTP.
     */
    private static final int DEFAULT_HTTP_PORT = 80;

    /**
     * Default maximum content length for HTTP responses (1MB).
     */
    public static final int DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024;

    private final EventLoopGroup eventLoopGroup;

    private final boolean ownEventLoopGroup;

    /**
     * Creates a new {@link NettyHttpClient} with a shared event loop group.
     */
    NettyHttpClient() {
        this(null);
    }

    /**
     * Creates a new {@link NettyHttpClient} with the given event loop group.
     *
     * @param eventLoopGroup the event loop group to use, or {@code null} to create a new one.
     */
    NettyHttpClient(EventLoopGroup eventLoopGroup) {
        if (eventLoopGroup != null) {
            this.eventLoopGroup = eventLoopGroup;
            this.ownEventLoopGroup = false;
        } else {
            this.eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
            this.ownEventLoopGroup = true;
        }
    }

    /**
     * Creates a new {@link NettyHttpClient} with a custom event loop group configuration.
     *
     * @param numberOfThreads the number of threads to use in the event loop group.
     * @param threadFactory the thread factory to use for creating threads.
     */
    NettyHttpClient(int numberOfThreads, ThreadFactory threadFactory) {
        LettuceAssert.isTrue(numberOfThreads > 0, "Number of threads must be greater than zero");
        LettuceAssert.notNull(threadFactory, "ThreadFactory must not be null");

        this.eventLoopGroup = new MultiThreadIoEventLoopGroup(numberOfThreads, threadFactory, NioIoHandler.newFactory());
        this.ownEventLoopGroup = true;
    }

    @Override
    public HttpConnection connect(URI uri, ConnectionConfig connectionConfig) throws IOException {
        try {
            return connectAsync(uri, connectionConfig).get();
        } catch (ExecutionException | InterruptedException e) {

            throw new IOException("Failed to establish HTTP connection", Exceptions.unwrap(e));
        }
    }

    @Override
    public CompletableFuture<HttpConnection> connectAsync(URI uri, ConnectionConfig connectionConfig) {

        LettuceAssert.notNull(uri, "URI must not be null");
        LettuceAssert.notNull(connectionConfig, "ConnectionConfig must not be null");

        CompletableFuture<HttpConnection> future = new CompletableFuture<>();

        String scheme = uri.getScheme();
        if (scheme == null) {
            future.completeExceptionally(new IllegalArgumentException("URI scheme must not be null"));
            return future;
        }

        String host = uri.getHost();
        if (host == null) {
            future.completeExceptionally(new IllegalArgumentException("URI host must not be null"));
            return future;
        }

        boolean isHttps = "https".equalsIgnoreCase(scheme);

        int port = uri.getPort();
        if (port == -1) {
            port = isHttps ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
        }

        // Create SSL context if needed
        SslContext sslContext = null;
        if (isHttps && connectionConfig.getSslOptions() != null) {
            try {
                SslContextBuilder builder = connectionConfig.getSslOptions().createSslContextBuilder();
                sslContext = builder.build();
            } catch (Exception e) {
                future.completeExceptionally(new IOException("Failed to create SSL context", e));
                return future;
            }
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionConfig.getConnectionTimeout())
                .handler(new HttpConnectionInitializer(uri, isHttps, sslContext, connectionConfig.getReadTimeout()));

        ChannelFuture connectFuture = bootstrap.connect(host, port);

        connectFuture.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                future.complete(new NettyHttpConnection(channelFuture.channel(), uri));
            } else {
                future.completeExceptionally(channelFuture.cause());
            }
        });

        return future;
    }

    /**
     * Shutdown this client.
     *
     * The shutdown is executed without quiet time and a timeout of 2 {@link TimeUnit#SECONDS}.
     *
     * @see #shutdown(long, long, TimeUnit)
     */
    public void shutdown() {
        shutdown(0, 2, TimeUnit.SECONDS);
    }

    /**
     * Shutdown this client.
     *
     * @param quietPeriod the quiet period to allow the executor gracefully shut down.
     * @param timeout the maximum amount of time to wait until the backing executor is shutdown regardless if a task was
     *        submitted during the quiet period.
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}.
     */
    public void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
        if (ownEventLoopGroup) {
            Future<?> shutdownFuture = eventLoopGroup.shutdownGracefully(quietPeriod, timeout, timeUnit);
            shutdownFuture.awaitUninterruptibly();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Channel initializer for HTTP client connections that supports connection reuse.
     */
    private static class HttpConnectionInitializer extends ChannelInitializer<SocketChannel> {

        private final URI uri;

        private final boolean isHttps;

        private final SslContext sslContext;

        private final int readTimeoutMs;

        HttpConnectionInitializer(URI uri, boolean isHttps, SslContext sslContext, int readTimeoutMs) {
            this.uri = uri;
            this.isHttps = isHttps;
            this.sslContext = sslContext;
            this.readTimeoutMs = readTimeoutMs;
        }

        @Override
        protected void initChannel(SocketChannel ch) {

            ChannelPipeline pipeline = ch.pipeline();

            if (isHttps && sslContext == null) {
                throw new IllegalStateException("SSL context must be provided for HTTPS");
            }

            if (isHttps) {
                int port = uri.getPort() != -1 ? uri.getPort() : DEFAULT_HTTPS_PORT;
                pipeline.addLast(sslContext.newHandler(ch.alloc(), uri.getHost(), port));
            }

            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new HttpObjectAggregator(DEFAULT_MAX_CONTENT_LENGTH));
            pipeline.addLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
        }

    }

    /**
     * Netty-based implementation of {@link HttpConnection} that supports connection reuse with sequential request processing.
     * Only one request is in-flight at a time to avoid HTTP/1.1 pipelining issues (proxy reordering, head-of-line blocking,
     * etc.).
     */
    private static class NettyHttpConnection implements HttpConnection {

        private final Channel channel;

        private final URI baseUri;

        private final SequentialHttpHandler sequentialHandler;

        NettyHttpConnection(Channel channel, URI baseUri) {
            this.channel = channel;
            this.baseUri = baseUri;
            // Add a single persistent handler that manages sequential request/response processing
            this.sequentialHandler = new SequentialHttpHandler(baseUri);
            channel.pipeline().addLast(sequentialHandler);
        }

        @Override
        public HttpClient.Response execute(HttpClient.Request request) throws IOException {
            try {
                return executeAsync(request).get();
            } catch (ExecutionException | InterruptedException e) {

                throw new IOException("HTTP request failed: " + request.getUri(), Exceptions.unwrap(e));
            }
        }

        @Override
        public CompletableFuture<HttpClient.Response> executeAsync(HttpClient.Request request) {
            LettuceAssert.notNull(request, "Request must not be null");

            // Currently only GET is supported
            if (request.getMethod() != HttpClient.Method.GET) {
                CompletableFuture<HttpClient.Response> future = new CompletableFuture<>();
                future.completeExceptionally(new UnsupportedOperationException("Only GET method is currently supported"));
                return future;
            }

            if (!channel.isActive()) {
                CompletableFuture<HttpClient.Response> future = new CompletableFuture<>();
                future.completeExceptionally(new IOException("Connection is not active"));
                return future;
            }

            CompletableFuture<HttpClient.Response> responseFuture = new CompletableFuture<>();

            // Build the Netty HTTP request
            FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, request.getUri());

            setHostHeader(nettyRequest, baseUri);
            nettyRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            // Add custom headers from the request
            Map<String, String> headers = request.getHeaders();
            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> header : headers.entrySet()) {
                    nettyRequest.headers().set(header.getKey(), header.getValue());
                }
            }

            // Submit to sequential handler (thread-safe, runs on event loop)
            sequentialHandler.sendRequest(channel, nettyRequest, responseFuture);

            return responseFuture;
        }

        private void setHostHeader(FullHttpRequest nettyRequest, URI baseUri) {
            String hostHeader;
            int port = baseUri.getPort();

            if (port == -1 || port == getDefaultPort()) {
                hostHeader = baseUri.getHost();
            } else {
                hostHeader = baseUri.getHost() + ":" + port;
            }
            nettyRequest.headers().set(HttpHeaderNames.HOST, hostHeader);
        }

        private int getDefaultPort() {
            boolean isHttps = "https".equalsIgnoreCase(baseUri.getScheme());
            return isHttps ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
        }

        @Override
        public boolean isActive() {
            return channel != null && channel.isActive();
        }

        @Override
        public void close() {
            if (channel != null && channel.isOpen()) {
                channel.close().awaitUninterruptibly();
            }
        }

        @Override
        public CompletableFuture<Void> closeAsync() {
            if (channel != null && channel.isOpen()) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                channel.close().addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        future.complete(null);
                    } else {
                        future.completeExceptionally(f.cause());
                    }
                });
                return future;
            }
            return CompletableFuture.completedFuture(null);
        }

    }

    /**
     * Sequential HTTP handler that ensures only one request is in-flight at a time. This avoids HTTP/1.1 pipelining issues such
     * as proxy reordering, head-of-line blocking, and response mismatching.
     * <p>
     * Requests are queued and sent sequentially: the next request is only sent after the previous response is received. All
     * operations are executed on the channel's event loop to ensure thread safety.
     */
    private static class SequentialHttpHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final URI baseUri;

        private final Queue<PendingRequest> requestQueue = new ArrayDeque<>();

        private boolean requestInFlight = false;

        SequentialHttpHandler(URI baseUri) {
            this.baseUri = baseUri;
        }

        /**
         * Represents a pending HTTP request with its associated future.
         */
        private static class PendingRequest {

            final FullHttpRequest request;

            final CompletableFuture<HttpClient.Response> future;

            PendingRequest(FullHttpRequest request, CompletableFuture<HttpClient.Response> future) {
                this.request = request;
                this.future = future;
            }

        }

        /**
         * Submits a request to be sent. If no request is currently in-flight, sends immediately. Otherwise, queues the request
         * to be sent after the current request completes.
         * <p>
         * This method is thread-safe and can be called from any thread.
         */
        void sendRequest(Channel channel, FullHttpRequest request, CompletableFuture<HttpClient.Response> future) {
            // Execute on event loop to ensure thread safety
            channel.eventLoop().execute(() -> {
                PendingRequest pending = new PendingRequest(request, future);
                requestQueue.offer(pending);
                trySendNext(channel);
            });
        }

        /**
         * Attempts to send the next queued request if no request is currently in-flight. Must be called on the event loop
         * thread.
         */
        private void trySendNext(Channel channel) {
            // Only send if no request is in-flight and queue is not empty
            if (!requestInFlight && !requestQueue.isEmpty()) {
                requestInFlight = true;
                PendingRequest next = requestQueue.peek(); // Don't remove yet - remove on response

                channel.writeAndFlush(next.request).addListener((ChannelFutureListener) writeResult -> {
                    if (!writeResult.isSuccess()) {
                        // Write failed - complete the future and try next
                        requestInFlight = false;
                        PendingRequest failed = requestQueue.poll();
                        if (failed != null && !failed.future.isDone()) {
                            failed.future.completeExceptionally(writeResult.cause());
                        }
                        trySendNext(channel);
                    }
                });
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {

            // Get the current in-flight request
            PendingRequest completed = requestQueue.poll();
            requestInFlight = false;

            if (completed == null) {
                // No pending request - protocol violation
                throw new IllegalStateException("Received HTTP response with no pending request");
            }

            try {
                // Extract response body
                ByteBuf content = response.content();
                ByteBuffer buffer = ByteBuffer.allocate(content.readableBytes());
                content.readBytes(buffer);
                buffer.flip();

                // Extract headers
                Map<String, String> headers = new HashMap<>();
                for (Entry<String, String> header : response.headers()) {
                    headers.put(header.getKey(), header.getValue());
                }

                // Create response object
                HttpClient.Response httpResponse = DefaultHttpResponse.builder().statusCode(response.status().code())
                        .body(buffer).headers(headers).build();

                completed.future.complete(httpResponse);
            } catch (Exception e) {
                completed.future.completeExceptionally(e);
            }

            // Try to send the next queued request
            trySendNext(ctx.channel());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            requestInFlight = false;

            // Fail all pending requests
            PendingRequest pending;
            while ((pending = requestQueue.poll()) != null) {
                if (!pending.future.isDone()) {
                    pending.future.completeExceptionally(cause);
                }
            }

            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            requestInFlight = false;

            // Fail all pending requests with connection closed exception
            PendingRequest pending;
            while ((pending = requestQueue.poll()) != null) {
                if (!pending.future.isDone()) {
                    pending.future.completeExceptionally(new IOException("Connection closed"));
                }
            }

            super.channelInactive(ctx);
        }

    }

}
