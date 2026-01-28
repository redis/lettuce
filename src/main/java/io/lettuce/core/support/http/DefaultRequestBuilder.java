/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.support.http;

import io.lettuce.core.internal.LettuceAssert;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of {@link HttpClient.Request.RequestBuilder}.
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
class DefaultRequestBuilder implements HttpClient.Request.RequestBuilder {

    private final HttpClient.Method method;

    private final String path;

    private final Map<String, String> queryParams = new LinkedHashMap<>();

    private final Map<String, String> headers = new LinkedHashMap<>();

    DefaultRequestBuilder(HttpClient.Method method, String path) {
        LettuceAssert.notNull(method, "Method must not be null");
        LettuceAssert.notNull(path, "Path must not be null");
        this.method = method;
        this.path = path;
    }

    /**
     * Creates a new GET request builder with the specified path.
     *
     * @param path the request path, must not be {@code null}.
     * @return a new GET request builder.
     */
    public static DefaultRequestBuilder get(String path) {
        return new DefaultRequestBuilder(HttpClient.Method.GET, path);
    }

    @Override
    public HttpClient.Request.RequestBuilder queryParam(String name, String value) {
        LettuceAssert.notNull(name, "Query parameter name must not be null");
        LettuceAssert.notNull(value, "Query parameter value must not be null");
        queryParams.put(name, value);
        return this;
    }

    @Override
    public HttpClient.Request.RequestBuilder queryParams(Map<String, String> params) {
        LettuceAssert.notNull(params, "Query parameters must not be null");
        queryParams.putAll(params);
        return this;
    }

    @Override
    public HttpClient.Request.RequestBuilder header(String name, String value) {
        LettuceAssert.notNull(name, "Header name must not be null");
        LettuceAssert.notNull(value, "Header value must not be null");
        headers.put(name, value);
        return this;
    }

    @Override
    public HttpClient.Request.RequestBuilder headers(Map<String, String> headers) {
        LettuceAssert.notNull(headers, "Headers must not be null");
        this.headers.putAll(headers);
        return this;
    }

    @Override
    public HttpClient.Request build() {
        return new DefaultRequest(method, path, queryParams, headers);
    }

    /**
     * Default implementation of {@link HttpClient.Request}.
     */
    private static class DefaultRequest implements HttpClient.Request {

        private final HttpClient.Method method;

        private final String path;

        private final Map<String, String> queryParams;

        private final Map<String, String> headers;

        private final String uri;

        DefaultRequest(HttpClient.Method method, String path, Map<String, String> queryParams, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(queryParams));
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
            this.uri = buildUri(path, queryParams);
        }

        @Override
        public HttpClient.Method getMethod() {
            return method;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public Map<String, String> getQueryParams() {
            return queryParams;
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }

        @Override
        public String getUri() {
            return uri;
        }

        private static String buildUri(String path, Map<String, String> queryParams) {
            if (queryParams.isEmpty()) {
                return path;
            }

            StringBuilder uri = new StringBuilder(path);
            uri.append('?');

            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    uri.append('&');
                }
                uri.append(urlEncode(entry.getKey()));
                uri.append('=');
                uri.append(urlEncode(entry.getValue()));
                first = false;
            }

            return uri.toString();
        }

        private static String urlEncode(String value) {
            try {
                return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // UTF-8 is always supported
                throw new IllegalStateException("UTF-8 encoding not supported", e);
            }
        }

    }

}
