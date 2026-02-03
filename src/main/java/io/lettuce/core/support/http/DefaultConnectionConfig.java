/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.SslOptions;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Default implementation of {@link HttpClient.ConnectionConfig}.
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
class DefaultConnectionConfig implements HttpClient.ConnectionConfig {

    static final DefaultConnectionConfig DEFAULTS = new DefaultConnectionConfig(new Builder());

    private final int connectionTimeout;

    private final int readTimeout;

    private final SslOptions sslOptions;

    private DefaultConnectionConfig(Builder builder) {
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.sslOptions = builder.sslOptions;
    }

    @Override
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public SslOptions getSslOptions() {
        return sslOptions;
    }

    /**
     * Builder for {@link DefaultConnectionConfig}.
     */
    static class Builder implements HttpClient.ConnectionConfig.Builder {

        private int connectionTimeout = 5000; // 5 seconds default

        private int readTimeout = 5000; // 5 seconds default

        private SslOptions sslOptions;

        @Override
        public Builder connectionTimeout(int timeoutMs) {
            LettuceAssert.isTrue(timeoutMs >= 0, "Connection timeout must be greater than or equal to 0");
            this.connectionTimeout = timeoutMs;
            return this;
        }

        @Override
        public Builder readTimeout(int timeoutMs) {
            LettuceAssert.isTrue(timeoutMs >= 0, "Read timeout must be greater than or equal to 0");
            this.readTimeout = timeoutMs;
            return this;
        }

        @Override
        public Builder sslOptions(SslOptions sslOptions) {
            LettuceAssert.notNull(sslOptions, "SslOptions must not be null");
            this.sslOptions = sslOptions;
            return this;
        }

        @Override
        public HttpClient.ConnectionConfig build() {
            return new DefaultConnectionConfig(this);
        }

    }

}
