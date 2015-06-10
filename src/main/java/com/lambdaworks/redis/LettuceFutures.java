package com.lambdaworks.redis;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class LettuceFutures {

    private LettuceFutures() {

    }

    /**
     * Wait until futures are complete or the supplied timeout is reached.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param futures Futures to wait for.
     * @return True if all futures complete in time.
     */
    public static boolean awaitAll(long timeout, TimeUnit unit, Future<?>... futures) {
        boolean complete;

        try {
            long nanos = unit.toNanos(timeout);
            long time = System.nanoTime();

            for (Future<?> f : futures) {
                if (nanos < 0) {
                    return false;
                }
                f.get(nanos, TimeUnit.NANOSECONDS);
                long now = System.nanoTime();
                nanos -= now - time;
                time = now;
            }

            complete = true;
        } catch (TimeoutException e) {
            complete = false;
        } catch (Exception e) {
            throw new RedisCommandInterruptedException(e);
        }

        return complete;
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached.
     *
     * @param cmd Command to wait for.
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param unit Unit of time for the timeout.
     * @param <T> Result type.
     * @return True if all futures complete in time.
     */
    public static <T> T await(RedisFuture<T> cmd, long timeout, TimeUnit unit) {
        try {
            if (!cmd.await(timeout, unit)) {
                cmd.cancel(true);
                throw new RedisCommandTimeoutException();
            }

            return cmd.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RedisException) {
                try {
                    throw (RedisException) cause.getClass().getConstructor(String.class, Throwable.class)
                            .newInstance(cause.getMessage(), cause);
                } catch (RedisException e1) {
                    throw e1;
                } catch (Exception e1) {
                    throw new RedisException(e1);
                }
            }
            throw new RedisException(cause);
        }
    }
}
