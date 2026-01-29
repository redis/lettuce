/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.annotations.Experimental;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.ThreadFactory;

/**
 * Default {@link HttpClientProvider} that creates Netty-based HTTP clients.
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
@Experimental
public class NettyHttpClientProvider implements HttpClientProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyHttpClientProvider.class);

    private static final int MIN_HTTP_IO_THREADS = 2;

    private static final int DEFAULT_HTTP_IO_THREADS;

    static {
        int threads = Math.max(1, SystemPropertyUtil.getInt("io.lettuce.http.eventLoopThreads",
                Math.max(MIN_HTTP_IO_THREADS, Runtime.getRuntime().availableProcessors())));

        DEFAULT_HTTP_IO_THREADS = threads;

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.lettuce.http.eventLoopThreads: {}", threads);
        }
    }

    @Override
    public HttpClient createHttpClient() {
        ThreadFactory threadFactory = new DefaultThreadFactory("lettuce-http", true);
        return new NettyHttpClient(DEFAULT_HTTP_IO_THREADS, threadFactory);
    }

    @Override
    public boolean isAvailable() {
        try {
            // Check for netty-codec-http dependency which is required for NettyHttpClient
            Class.forName("io.netty.handler.codec.http.HttpClientCodec");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public int getPriority() {
        return 0; // Default priority
    }

}
