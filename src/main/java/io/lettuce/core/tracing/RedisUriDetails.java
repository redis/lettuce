/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.tracing;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.CredentialsProvider;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.LettuceStrings;

/**
 * Connection details derived from a {@link RedisURI} to be attached to traces. Instances are created once per connection.
 *
 * @since 8.0
 */
final class RedisUriDetails {

    private final String database;

    private final String username;

    private final String url;

    private RedisUriDetails(String database, String username, String url) {
        this.database = database;
        this.username = username;
        this.url = url;
    }

    /**
     * Create {@link RedisUriDetails} from a {@link RedisURI}.
     *
     * @param redisURI the {@link RedisURI}, can be {@code null}.
     * @return the {@link RedisUriDetails} or {@code null} if {@code redisURI} is {@code null}.
     */
    static RedisUriDetails from(RedisURI redisURI) {

        if (redisURI == null) {
            return null;
        }

        return new RedisUriDetails(Integer.toString(redisURI.getDatabase()), getUsername(redisURI), getUrl(redisURI));
    }

    /**
     * @return the database number.
     */
    String getDatabase() {
        return database;
    }

    /**
     * @return the username or {@code null} if the username is not available without waiting for the credentials provider.
     */
    String getUsername() {
        return username;
    }

    /**
     * @return the URI without credentials or {@code null} if the URI cannot be rendered.
     */
    String getUrl() {
        return url;
    }

    private static String getUsername(RedisURI redisURI) {

        CredentialsProvider credentialsProvider = redisURI.getCredentialsProviderAsync();

        RedisCredentials credentials;

        try {
            if (credentialsProvider instanceof RedisCredentialsProvider.ImmediateRedisCredentialsProvider) {
                credentials = ((RedisCredentialsProvider.ImmediateRedisCredentialsProvider) credentialsProvider)
                        .resolveCredentialsNow();
            } else if (credentialsProvider instanceof CredentialsProvider.ImmediateCredentialsProvider) {
                credentials = ((CredentialsProvider.ImmediateCredentialsProvider) credentialsProvider).resolveCredentialsNow();
            } else if (credentialsProvider == null || credentialsProvider instanceof RedisCredentialsProvider) {
                // resolving a reactive provider subscribes to its Mono, which can block the event loop
                return null;
            } else {
                // only use credentials that are available right away, don't wait for asynchronous providers
                CompletableFuture<RedisCredentials> future = credentialsProvider.resolveCredentialsAsync()
                        .toCompletableFuture();

                if (!future.isDone() || future.isCompletedExceptionally()) {
                    return null;
                }

                credentials = future.getNow(null);
            }
        } catch (RuntimeException e) {
            return null;
        }

        if (credentials == null || !credentials.hasUsername() || LettuceStrings.isEmpty(credentials.getUsername())) {
            return null;
        }

        return credentials.getUsername();
    }

    private static String getUrl(RedisURI redisURI) {

        String uri;
        try {
            uri = redisURI.toURI().toString();
        } catch (RuntimeException e) {
            return null;
        }

        // toURI() renders a credentials provider placeholder as user info, remove the user info part
        int authorityStart = uri.indexOf("://");
        if (authorityStart == -1) {
            return uri;
        }
        authorityStart += 3;

        int authorityEnd = uri.length();
        for (char delimiter : new char[] { '/', '?' }) {
            int index = uri.indexOf(delimiter, authorityStart);
            if (index != -1 && index < authorityEnd) {
                authorityEnd = index;
            }
        }

        int userInfoEnd = uri.lastIndexOf('@', authorityEnd - 1);
        if (userInfoEnd < authorityStart) {
            return uri;
        }

        return uri.substring(0, authorityStart) + uri.substring(userInfoEnd + 1);
    }

}
