package io.lettuce.core;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A {@code RedisFuture} represents the result of an asynchronous computation, extending {@link CompletionStage}. The execution
 * of the notification happens either on finish of the future execution or, if the future is completed already, immediately.
 *
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisFuture<V> extends CompletionStage<V>, Future<V> {

    /**
     * @return error text, if any error occurred.
     */
    String getError();

    /**
     * Wait up to the specified time for the command output to become available.
     *
     * @param timeout Maximum time to wait for a result.
     * @param unit Unit of time for the timeout.
     *
     * @return {@code true} if the output became available.
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

}
