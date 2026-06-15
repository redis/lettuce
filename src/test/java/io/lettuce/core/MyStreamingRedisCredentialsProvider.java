package io.lettuce.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A provider for streaming credentials that can be used to authorize a Redis connection
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
public class MyStreamingRedisCredentialsProvider implements RedisCredentialsProvider {

    private final Object lock = new Object();

    private final List<Listener> listeners = new ArrayList<>();

    private final AtomicReference<CompletableFuture<RedisCredentials>> credentialsFutureRef = new AtomicReference<>(
            new CompletableFuture<>());

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public CompletionStage<RedisCredentials> resolveCredentials() {
        return credentialsFutureRef.get();
    }

    @Override
    public CredentialsSubscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
        Listener listener = new Listener(onNext, onError);
        RedisCredentials toReplay;
        synchronized (lock) {
            listeners.add(listener);
            CompletableFuture<RedisCredentials> latest = credentialsFutureRef.get();
            toReplay = (latest.isDone() && !latest.isCompletedExceptionally()) ? latest.getNow(null) : null;
        }
        if (toReplay != null) {
            onNext.accept(toReplay);
        }
        return () -> {
            synchronized (lock) {
                listeners.remove(listener);
            }
        };
    }

    public void shutdown() {
        synchronized (lock) {
            listeners.clear();
        }
    }

    public void emitCredentials(RedisCredentials credentials) {
        List<Listener> snapshot;
        synchronized (lock) {
            CompletableFuture<RedisCredentials> previous = credentialsFutureRef
                    .getAndSet(CompletableFuture.completedFuture(credentials));
            if (!previous.isDone()) {
                previous.complete(credentials);
            }
            snapshot = new ArrayList<>(listeners);
        }
        for (Listener l : snapshot) {
            l.onNext.accept(credentials);
        }
    }

    public void emitCredentials(String username, char[] password) {
        emitCredentials(new StaticRedisCredentials(username, password));
    }

    public void tryEmitError(RuntimeException testError) {
        List<Listener> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(listeners);
        }
        for (Listener l : snapshot) {
            l.onError.accept(testError);
        }
    }

    private static class Listener {

        final Consumer<RedisCredentials> onNext;

        final Consumer<Throwable> onError;

        Listener(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
            this.onNext = onNext;
            this.onError = onError;
        }

    }

}
