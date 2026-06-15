package io.lettuce.core;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Interface for loading {@link RedisCredentials} that are used for authentication. A commonly-used implementation is
 * {@link StaticCredentialsProvider} for a fixed set of credentials.
 * <p>
 * Credentials are requested by the driver after connecting to the server. Therefore, credential retrieval is subject to
 * complete within the connection creation timeout to avoid connection failures.
 *
 * @author Mark Paluch
 * @since 6.2
 */
@FunctionalInterface
public interface RedisCredentialsProvider {

    /**
     * Handle to a subscription created by {@link #subscribeToCredentials(Consumer, Consumer)}. Closing the subscription stops
     * the provider from delivering further credential updates to the registered consumers.
     */
    interface CredentialsSubscription extends Closeable {

        @Override
        void close();

    }

    /**
     * Returns {@link RedisCredentials} that can be used to authorize a Redis connection. Each implementation of
     * {@code RedisCredentialsProvider} can choose its own strategy for loading credentials. For example, an implementation
     * might load credentials from an existing key management system, or load new credentials when credentials are rotated. If
     * an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be raised.
     *
     * @return a {@link CompletionStage} emitting {@link RedisCredentials} that can be used to authorize a Redis connection.
     */
    CompletionStage<RedisCredentials> resolveCredentials();

    /**
     * Creates a new {@link RedisCredentialsProvider} from a given {@link Supplier}.
     *
     * @param supplier must not be {@code null}.
     * @return the {@link RedisCredentials} using credentials from {@link Supplier}.
     */
    static RedisCredentialsProvider from(Supplier<RedisCredentials> supplier) {

        LettuceAssert.notNull(supplier, "Supplier must not be null");

        return () -> {
            try {
                return CompletableFuture.completedFuture(supplier.get());
            } catch (Exception e) {
                return Futures.failed(e);
            }
        };
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
     * Subscribe to credential updates produced by this provider.
     * <p>
     * For implementations that support streaming credentials (as indicated by {@link #supportsStreaming()} returning
     * {@code true}), the {@code onNext} consumer is invoked whenever new credentials become available, typically as a result of
     * external events such as token renewal or rotation. The {@code onError} consumer is invoked when the provider observes a
     * failure while obtaining new credentials.
     * <p>
     * Replay semantics on subscription are defined by the implementation and callers should consult the concrete provider's
     * documentation. Typical aspects an implementation may choose to specify include:
     * <ul>
     * <li>whether the most recently known credentials are delivered to {@code onNext} immediately on subscription;</li>
     * <li>whether prior errors observed before subscription are delivered to {@code onError}, or are silently dropped in favour
     * of waiting for the next credentials or next error event;</li>
     * <li>the threading context on which {@code onNext} and {@code onError} are invoked, and any ordering guarantees between
     * replay deliveries and live updates.</li>
     * </ul>
     * Subscribers must not assume any specific replay behaviour from this interface alone.
     * <p>
     * Implementations that do not support streaming credentials (where {@link #supportsStreaming()} returns {@code false})
     * throw an {@link UnsupportedOperationException} by default.
     *
     * @param onNext consumer invoked with each new {@link RedisCredentials} value, must not be {@code null}.
     * @param onError consumer invoked with errors observed while producing credentials, must not be {@code null}.
     * @return a {@link CredentialsSubscription} that can be used to stop receiving updates.
     * @throws UnsupportedOperationException if the provider does not support streaming credentials.
     */
    default CredentialsSubscription subscribeToCredentials(Consumer<RedisCredentials> onNext, Consumer<Throwable> onError) {
        throw new UnsupportedOperationException("Streaming credentials are not supported by this provider.");
    }

    /**
     * Extension to {@link RedisCredentialsProvider} that resolves credentials immediately without the need to defer the
     * credential resolution.
     */
    @FunctionalInterface
    interface ImmediateRedisCredentialsProvider extends RedisCredentialsProvider {

        @Override
        default CompletionStage<RedisCredentials> resolveCredentials() {
            try {
                return CompletableFuture.completedFuture(resolveCredentialsNow());
            } catch (Exception e) {
                return Futures.failed(e);
            }
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
