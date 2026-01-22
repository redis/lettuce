/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

/**
 * Default implementation of {@link HttpClient.Response}.
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
@Experimental
class DefaultHttpResponse implements HttpClient.Response {

    private final int statusCode;

    private final ByteBuffer body;

    private final Map<String, String> headers;

    /**
     * Creates a new HTTP response.
     *
     * @param statusCode the HTTP status code.
     * @param body the response body as a {@link ByteBuffer}.
     * @param headers the response headers (header names are case-insensitive).
     */
    DefaultHttpResponse(int statusCode, ByteBuffer body, Map<String, String> headers) {
        LettuceAssert.notNull(body, "Body must not be null");
        LettuceAssert.notNull(headers, "Headers must not be null");

        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public ByteBuffer getResponseBodyAsByteBuffer() {
        // Return a duplicate to allow multiple reads without affecting the original position
        return body.duplicate();
    }

    @Override
    public String getResponseBody(Charset charset) {
        LettuceAssert.notNull(charset, "Charset must not be null");

        ByteBuffer duplicate = body.duplicate();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return new String(bytes, charset);
    }

    @Override
    public String getHeader(CharSequence name) {
        LettuceAssert.notNull(name, "Header name must not be null");

        // Case-insensitive lookup
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name.toString())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Creates a builder for {@link DefaultHttpResponse}.
     *
     * @return a new builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DefaultHttpResponse}.
     */
    static class Builder {

        private int statusCode;

        private ByteBuffer body;

        private Map<String, String> headers = Collections.emptyMap();

        Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        Builder body(ByteBuffer body) {
            this.body = body;
            return this;
        }

        Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        DefaultHttpResponse build() {
            return new DefaultHttpResponse(statusCode, body, headers);
        }

    }

}
