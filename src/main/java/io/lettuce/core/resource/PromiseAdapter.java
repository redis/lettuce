package io.lettuce.core.resource;

import io.netty.util.concurrent.*;

/**
 * Utility class to support netty's future handling.
 *
 * @author Mark Paluch
 * @since 3.4
 */
class PromiseAdapter {

    /**
     * Create a promise that emits a {@code Boolean} value on completion of the {@code future}
     *
     * @param future the future.
     * @return Promise emitting a {@code Boolean} value. {@code true} if the {@code future} completed successfully, otherwise
     *         the cause wil be transported.
     */
    static Promise<Boolean> toBooleanPromise(Future<?> future) {

        DefaultPromise<Boolean> result = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        if (future.isDone() || future.isCancelled()) {
            if (future.isSuccess()) {
                result.setSuccess(true);
            } else {
                result.setFailure(future.cause());
            }
            return result;
        }

        future.addListener((GenericFutureListener<Future<Object>>) f -> {

            if (f.isSuccess()) {
                result.setSuccess(true);
            } else {
                result.setFailure(f.cause());
            }
        });
        return result;
    }

}
