/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.authx;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import io.lettuce.core.internal.LettuceAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
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
 * <p>
 * Subscription replay: a subscriber registered via {@link #subscribeToCredentials(Consumer, Consumer)} is delivered the last
 * known credentials on subscription, if any are available. Transient token-fetch errors are dispatched live to already-
 * registered subscribers via their {@code onError} callback, but are not retained for replay to subscribers added later.
 * </p>
 *
 * @since 6.6
 */
public class TokenBasedRedisCredentialsProvider implements RedisCredentialsProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TokenBasedRedisCredentialsProvider.class);

    private static class SimpleSubscription implements CredentialsSubscription {

        private final TokenBasedRedisCredentialsProvider provider;

        private final Consumer<RedisCredentials> onNext;

        private final Consumer<Throwable> onError;

        private final AtomicReference<RedisCredentials> lastPlayed = new AtomicReference<>();

        private SimpleSubscription(TokenBasedRedisCredentialsProvider provider, Consumer<RedisCredentials> onNext,
                Consumer<Throwable> onError) {
            this.provider = provider;
            this.onNext = onNext;
            this.onError = onError;
        }

        @Override
        public void close() {
            provider.subscriptions.remove(this);
        }

        private void onCredentials(RedisCredentials credentials) {
            RedisCredentials played = lastPlayed.getAndSet(credentials);
            if (played != credentials) {
                onNext.accept(credentials);
            }
        }

        private void replay(RedisCredentials candidate) {
            if (candidate != null && lastPlayed.compareAndSet(null, candidate)) {
                onNext.accept(candidate);
            }
        }

        private void onError(Throwable throwable) {
            onError.accept(throwable);
        }

    }

    private final TokenManager tokenManager;

    private final Executor executor;

    private final List<SimpleSubscription> subscriptions = new CopyOnWriteArrayList<>();

    private final AtomicReference<CompletableFuture<RedisCredentials>> credentialsFutureRef = new AtomicReference<>(
            new CompletableFuture<>());

    private volatile boolean isClosed = false;

    private TokenBasedRedisCredentialsProvider(TokenManager tokenManager, Executor executor) {
        this.tokenManager = tokenManager;
        this.executor = executor;
    }

    private void init() {

        TokenListener listener = new TokenListener() {

            @Override
            public void onTokenRenewed(Token token) {
                if (isClosed) {
                    return;
                }
                String username = token.getUser();
                char[] pass = token.getValue().toCharArray();
                RedisCredentials credentials = RedisCredentials.just(username, pass);

                CompletableFuture<RedisCredentials> previous = credentialsFutureRef
                        .getAndSet(CompletableFuture.completedFuture(credentials));
                if (!previous.isDone()) {
                    executor.execute(() -> previous.complete(credentials));
                }
                subscriptions.forEach(s -> executor.execute(() -> dispatchOnNext(s, credentials)));
            }

            @Override
            public void onError(Exception exception) {
                if (isClosed) {
                    return;
                }
                log.error("Token renew failed!", exception);
                CompletableFuture<RedisCredentials> previous = credentialsFutureRef.get();
                if (!previous.isDone()) {
                    credentialsFutureRef.compareAndSet(previous, new CompletableFuture<>());
                    executor.execute(() -> previous.completeExceptionally(exception));
                    // here we need to react if provider gets closed meanwhile
                    if (isClosed) {
                        credentialsFutureRef.get()
                                .completeExceptionally(new IllegalStateException("Credentials provider closed"));
                    }
                }
                subscriptions.forEach(s -> executor.execute(() -> dispatchOnError(s, exception)));
            }

        };

        try {
            tokenManager.start(listener, false);
        } catch (Exception e) {
            credentialsFutureRef.get().completeExceptionally(e);
            tokenManager.stop();
            isClosed = true;
            throw new RuntimeException("Failed to start TokenManager", e);
        }
    }

    private static void dispatchOnNext(SimpleSubscription subscription, RedisCredentials credentials) {
        try {
            subscription.onCredentials(credentials);
        } catch (Throwable t) {
            log.warn("Subscriber threw while delivering credentials", t);
        }
    }

    private static void dispatchOnError(SimpleSubscription subscription, Throwable throwable) {
        try {
            subscription.onError(throwable);
        } catch (Throwable t) {
            log.warn("Subscriber threw while delivering credentials error notification", t);
        }
    }

    /**
     * Resolve the latest available credentials as a {@link CompletionStage}.
     * <p>
     * This method returns a {@link CompletionStage} that completes with the most recent set of Redis credentials. If no
     * credentials are available at the time of invocation, the returned stage will complete once credentials become available.
     * <p>
     * Consistent with the subscription replay semantics of this provider, prior token-fetch errors are not surfaced through
     * this method to callers that arrive after the error: if the provider's most recent state is a failure, the returned stage
     * is left pending and will complete on the next successful resolution (or fail if that resolution itself fails).
     *
     * @return a {@link CompletionStage} that completes with the latest Redis credentials
     */
    @Override
    public CompletionStage<RedisCredentials> resolveCredentials() {
        CompletableFuture<RedisCredentials> result = new CompletableFuture<>();
        credentialsFutureRef.get().whenComplete((creds, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
            } else {
                result.complete(creds);
            }
        });
        return result;
    }

    @Override
    public CredentialsSubscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
        if (isClosed) {
            throw new IllegalStateException("Credentials provider closed");
        }
        LettuceAssert.notNull(onNext, "onNext consumer must not be null");
        LettuceAssert.notNull(onError, "onError consumer must not be null");

        SimpleSubscription subscription = new SimpleSubscription(this, onNext, onError);
        subscriptions.add(subscription);
        executor.execute(() -> subscription.replay(getReplayCandidate()));
        return subscription;
    }

    private RedisCredentials getReplayCandidate() {
        CompletableFuture<RedisCredentials> latest = credentialsFutureRef.get();
        if (latest.isDone() && !latest.isCompletedExceptionally()) {
            return latest.getNow(null);
        }
        return null;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    /**
     * Stop the credentials provider and clean up resources.
     * <p>
     * This method stops the TokenManager and drops all active subscriptions. It should be called when the credentials provider
     * is no longer needed.
     */
    @Override
    public void close() {
        isClosed = true;
        tokenManager.stop();
        CompletableFuture<RedisCredentials> credentialsFuture = credentialsFutureRef.get();
        if (!credentialsFuture.isDone()) {
            credentialsFuture.completeExceptionally(new IllegalStateException("Credentials provider closed"));
        }
        subscriptions.clear();
    }

    /**
     * Create a provider from a {@link TokenAuthConfig} using the caller-thread executor.
     * <p>
     * Consequence of not supplying an executor:
     * <ul>
     * <li>Subscriber {@code onNext}/{@code onError} callbacks for live token renewals run on the {@link TokenManager}'s renewal
     * thread. A slow or blocking subscriber can delay or miss subsequent renewals.</li>
     * <li>Continuations chained off {@link #resolveCredentials()} run on the renewal thread when the initial future
     * completes.</li>
     * <li>Replay deliveries to a newly subscribing consumer run on the subscribing thread.</li>
     * </ul>
     * Use {@link #create(TokenAuthConfig, Executor)} when subscriber code is non-trivial.
     *
     * @param tokenAuthConfig the token authentication configuration, must not be {@code null}
     * @return a started {@link TokenBasedRedisCredentialsProvider}
     */
    public static TokenBasedRedisCredentialsProvider create(TokenAuthConfig tokenAuthConfig) {
        return create(new TokenManager(tokenAuthConfig.getIdentityProviderConfig().getProvider(),
                tokenAuthConfig.getTokenManagerConfig()));
    }

    /**
     * Create a provider from a {@link TokenAuthConfig} dispatching subscriber callbacks on the supplied {@link Executor}.
     * <p>
     * Consequence of supplying an executor:
     * <ul>
     * <li>Subscriber {@code onNext}/{@code onError} callbacks and the completion of the initial credentials future are
     * dispatched on {@code executor}, isolating the {@link TokenManager}'s renewal thread from arbitrary consumer code.</li>
     * <li>The executor must preserve submission order per subscription to keep live deliveries monotonically ordered. A direct,
     * single-threaded, or otherwise serial executor satisfies this trivially; a multi-threaded executor (e.g.
     * {@link java.util.concurrent.ForkJoinPool#commonPool()}) does not, and out-of-order delivery becomes possible.</li>
     * <li>The executor's lifecycle is owned by the caller; {@link #close()} does not shut it down.</li>
     * </ul>
     *
     * @param tokenAuthConfig the token authentication configuration, must not be {@code null}
     * @param executor the executor used to dispatch credential and error notifications, must not be {@code null}
     * @return a started {@link TokenBasedRedisCredentialsProvider}
     */
    public static TokenBasedRedisCredentialsProvider create(TokenAuthConfig tokenAuthConfig, Executor executor) {
        return create(new TokenManager(tokenAuthConfig.getIdentityProviderConfig().getProvider(),
                tokenAuthConfig.getTokenManagerConfig()), executor);
    }

    /**
     * Create a provider from an existing {@link TokenManager} using the caller-thread executor.
     * <p>
     * Consequence of not supplying an executor:
     * <ul>
     * <li>Subscriber {@code onNext}/{@code onError} callbacks for live token renewals run on the {@link TokenManager}'s renewal
     * thread. A slow or blocking subscriber can delay or miss subsequent renewals.</li>
     * <li>Continuations chained off {@link #resolveCredentials()} run on the renewal thread when the initial future
     * completes.</li>
     * <li>Replay deliveries to a newly subscribing consumer run on the subscribing thread.</li>
     * </ul>
     * Use {@link #create(TokenManager, Executor)} when subscriber code is non-trivial.
     *
     * @param tokenManager the {@link TokenManager} to use, must not be {@code null}
     * @return a started {@link TokenBasedRedisCredentialsProvider}
     */
    public static TokenBasedRedisCredentialsProvider create(TokenManager tokenManager) {
        return create(tokenManager, r -> r.run());
    }

    /**
     * Create a provider from an existing {@link TokenManager} dispatching subscriber callbacks on the supplied
     * {@link Executor}.
     * <p>
     * Consequence of supplying an executor:
     * <ul>
     * <li>Subscriber {@code onNext}/{@code onError} callbacks and the completion of the initial credentials future are
     * dispatched on {@code executor}, isolating the {@link TokenManager}'s renewal thread from arbitrary consumer code.</li>
     * <li>The executor must preserve submission order per subscription to keep live deliveries monotonically ordered. A direct,
     * single-threaded, or otherwise serial executor satisfies this trivially; a multi-threaded executor (e.g.
     * {@link java.util.concurrent.ForkJoinPool#commonPool()}) does not, and out-of-order delivery becomes possible.</li>
     * <li>The executor's lifecycle is owned by the caller; {@link #close()} does not shut it down.</li>
     * </ul>
     *
     * @param tokenManager the {@link TokenManager} to use, must not be {@code null}
     * @param executor the executor used to dispatch credential and error notifications, must not be {@code null}
     * @return a started {@link TokenBasedRedisCredentialsProvider}
     */
    public static TokenBasedRedisCredentialsProvider create(TokenManager tokenManager, Executor executor) {
        LettuceAssert.notNull(tokenManager, "TokenManager must not be null");
        LettuceAssert.notNull(executor, "Executor must not be null");
        TokenBasedRedisCredentialsProvider credentialManager = new TokenBasedRedisCredentialsProvider(tokenManager, executor);
        credentialManager.init();
        return credentialManager;
    }

}
