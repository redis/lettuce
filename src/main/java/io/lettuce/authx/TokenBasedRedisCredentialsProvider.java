/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.authx;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A {@link RedisCredentialsProvider} implementation that supports token-based authentication for Redis.
 * <p>
 * This provider uses a {@link TokenManager} to manage and renew tokens, ensuring that the Redis client can authenticate with
 * Redis using a dynamically updated token. This is particularly useful in scenarios where Redis access is controlled via
 * token-based authentication, such as when Redis is integrated with an identity provider like EntraID.
 * </p>
 * <p>
 * The provider supports streaming of credentials and automatically emits new credentials whenever a token is renewed. It must
 * be used with {@link io.lettuce.core.ClientOptions.ReauthenticateBehavior#ON_NEW_CREDENTIALS} to automatically re-authenticate
 * connections whenever new tokens are emitted by the provider.
 * </p>
 * <p>
 * The lifecycle of this provider is externally managed. It should be closed when there are no longer any connections using it,
 * to stop the token management process and release resources.
 * </p>
 *
 * @since 6.6
 */
public class TokenBasedRedisCredentialsProvider implements RedisCredentialsProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TokenBasedRedisCredentialsProvider.class);

    private static class SimpleSubscription implements CredentialsSubscription {

        private final TokenBasedRedisCredentialsProvider provider;

        private final Consumer<RedisCredentials> consumer;

        private SimpleSubscription(TokenBasedRedisCredentialsProvider provider, Consumer<RedisCredentials> consumer) {
            this.provider = provider;
            this.consumer = consumer;
        }

        @Override
        public void close() {
            provider.subscriptions.remove(this);
        }

        private void onCredentials(RedisCredentials credentials) {
            consumer.accept(credentials);
        }

    }

    private final TokenManager tokenManager;

    private final List<SimpleSubscription> subscriptions = new CopyOnWriteArrayList<>();

    private volatile CompletableFuture<RedisCredentials> credentialsFuture = new CompletableFuture<>();

    private TokenBasedRedisCredentialsProvider(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    private void init() {

        TokenListener listener = new TokenListener() {

            @Override
            public void onTokenRenewed(Token token) {
                String username = token.getUser();
                char[] pass = token.getValue().toCharArray();
                RedisCredentials credentials = RedisCredentials.just(username, pass);

                if (!credentialsFuture.isDone()) {
                    credentialsFuture.complete(credentials);
                }
                credentialsFuture = CompletableFuture.completedFuture(credentials);
                subscriptions.forEach(s -> s.onCredentials(credentials));
            }

            @Override
            public void onError(Exception exception) {
                log.error("Token renew failed!", exception);
            }

        };

        try {
            tokenManager.start(listener, false);
        } catch (Exception e) {
            credentialsFuture.completeExceptionally(e);
            tokenManager.stop();
            throw new RuntimeException("Failed to start TokenManager", e);
        }
    }

    /**
     * Resolve the latest available credentials as a {@link CompletionStage}.
     * <p>
     * This method returns a {@link CompletionStage} that completes with the most recent set of Redis credentials. If no
     * credentials are available at the time of invocation, the returned stage will complete once credentials become available.
     *
     * @return a {@link CompletionStage} that completes with the latest Redis credentials
     */
    @Override
    public CompletionStage<RedisCredentials> resolveCredentials() {
        return credentialsFuture;
    }

    @Override
    public CredentialsSubscription subscribeToCredentials(Consumer<RedisCredentials> consumer) {
        SimpleSubscription subscription = new SimpleSubscription(this, consumer);
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    /**
     * Stop the credentials provider and clean up resources.
     * <p>
     * This method stops the TokenManager and completes the credentials sink, ensuring that all resources are properly released.
     * It should be called when the credentials provider is no longer needed.
     */
    @Override
    public void close() {
        subscriptions.clear();
        tokenManager.stop();
    }

    public static TokenBasedRedisCredentialsProvider create(TokenAuthConfig tokenAuthConfig) {
        return create(new TokenManager(tokenAuthConfig.getIdentityProviderConfig().getProvider(),
                tokenAuthConfig.getTokenManagerConfig()));
    }

    public static TokenBasedRedisCredentialsProvider create(TokenManager tokenManager) {
        TokenBasedRedisCredentialsProvider credentialManager = new TokenBasedRedisCredentialsProvider(tokenManager);
        credentialManager.init();
        return credentialManager;
    }

}
