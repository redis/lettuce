package io.lettuce.core;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.lettuce.core.internal.LettuceAssert;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for loading {@link RedisCredentials} that are used for authentication. A commonly-used implementation is
 * {@link StaticCredentialsProvider} for a fixed set of credentials.
 * <p>
 * Credentials are requested by the driver after connecting to the server. Therefore, credential retrieval is subject to
 * complete within the connection creation timeout to avoid connection failures.
 *
 * @author Mark Paluch
 * @since 6.2
 * @deprecated since 7.7, use {@link CredentialsProvider} instead; scheduled for removal in Lettuce 8.0.
 */
@Deprecated
@FunctionalInterface
public interface RedisCredentialsProvider {

    /**
     * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
     * {@code RedisCredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation
     * might load credentials from an existing key management system, or load new credentials when credentials are rotated. If
     * an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be raised.
     *
     * @return a {@link Mono} emitting {@link RedisCredentials} that can be used to authorize a Redis connection.
     */
    Mono<RedisCredentials> resolveCredentials();

    /**
     * Resolves the latest available credentials as a {@link CompletionStage}.
     *
     * @return a {@link CompletionStage} that completes with the {@link RedisCredentials} used to authorize a Redis connection.
     * @since 7.7
     */
    @SuppressWarnings("deprecation")
    default CompletionStage<RedisCredentials> resolveCredentialsAsync() {
        return resolveCredentials().toFuture();
    }

    /**
     * Creates a new {@link RedisCredentialsProvider} from a given {@link Supplier}.
     *
     * @param supplier must not be {@code null}.
     * @return the {@link RedisCredentials} using credentials from {@link Supplier}.
     */
    static RedisCredentialsProvider from(Supplier<RedisCredentials> supplier) {

        LettuceAssert.notNull(supplier, "Supplier must not be null");

        return () -> Mono.fromSupplier(supplier);
    }

    /**
     * Some implementations of the {@link RedisCredentialsProvider} may support streaming new credentials, based on some event
     * that originates outside the driver. In this case they should indicate that so the {@link RedisAuthenticationHandler} is
     * able to process these new credentials.
     *
     * @return whether the {@link RedisCredentialsProvider} supports streaming credentials.
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * Returns a {@link Flux} emitting {@link RedisCredentials} that can be used to authorize a Redis connection.
     *
     * For implementations that support streaming credentials (as indicated by {@link #supportsStreaming()} returning
     * {@code true}), this method can emit multiple credentials over time, typically based on external events like token renewal
     * or rotation.
     *
     * For implementations that do not support streaming credentials (where {@link #supportsStreaming()} returns {@code false}),
     * this method throws an {@link UnsupportedOperationException} by default.
     *
     * @return a {@link Flux} emitting {@link RedisCredentials}, or throws an exception if streaming is not supported.
     * @throws UnsupportedOperationException if the provider does not support streaming credentials.
     * @deprecated since 7.7, use {@link #subscribeToCredentials(Consumer, Consumer)} instead; scheduled for removal in Lettuce
     *             8.0.
     */
    @Deprecated
    default Flux<RedisCredentials> credentials() {
        throw new UnsupportedOperationException("Streaming credentials are not supported by this provider.");
    }

    /**
     * Subscribe to credential updates produced by this provider. For providers that support streaming (as indicated by
     * {@link #supportsStreaming()} returning {@code true}), {@code onNext} is invoked whenever new credentials become available
     * (e.g. token renewal or rotation) and {@code onError} is invoked when the provider observes a failure while producing
     * credentials. Delivery stops once the returned {@link Subscription} is {@link Subscription#close() closed}.
     * <p>
     * Providers that do not support streaming throw an {@link UnsupportedOperationException} by default.
     *
     * @param onNext consumer invoked with each new {@link RedisCredentials} value, must not be {@code null}.
     * @param onError consumer invoked with errors observed while producing credentials, must not be {@code null}.
     * @return a {@link Subscription} that stops delivery when closed.
     * @throws UnsupportedOperationException if the provider does not support streaming credentials.
     * @since 7.7
     */
    default Subscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
        LettuceAssert.notNull(onNext, "onNext consumer must not be null");
        LettuceAssert.notNull(onError, "onError consumer must not be null");
        Disposable disposable = credentials().subscribe(onNext, onError);
        return disposable::dispose;
    }

    /**
     * Extension to {@link RedisCredentialsProvider} that resolves credentials immediately without the need to defer the
     * credential resolution.
     */
    @FunctionalInterface
    interface ImmediateRedisCredentialsProvider extends RedisCredentialsProvider {

        @Override
        default Mono<RedisCredentials> resolveCredentials() {
            return Mono.just(resolveCredentialsNow());
        }

        /**
         * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
         * {@code RedisCredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation
         * might load credentials from an existing key management system, or load new credentials when credentials are rotated.
         * If an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be
         * raised.
         *
         * @return the resolved {@link RedisCredentials} that can be used to authorize a Redis connection.
         */
        RedisCredentials resolveCredentialsNow();

    }

}
