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
     * Wait until futures are complete or the supplied timeout is reached. Commands are not canceled (in contrast to
     * {@link #await(RedisFuture, long, TimeUnit)}) when the timeout expires.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param futures Futures to wait for.
     * @return {@literal true} if all futures complete in time, otherwise {@literal false}
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
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RedisCommandExecutionException) {
                throw new RedisCommandExecutionException(e.getCause().getMessage(), e.getCause());
            }
            throw new RedisException(e.getCause());
        } catch (Exception e) {
            throw new RedisCommandInterruptedException(e);
        }

        return complete;
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are canceled if the timeout is reached but
     * the command is not finished.
     *
     * @param cmd Command to wait for.
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param <T> Result type.
     * 
     * @return Result of the command.
     */
    public static <T> T awaitOrCancel(RedisFuture<T> cmd, long timeout, TimeUnit unit) {
        return await(cmd, timeout, unit);
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are canceled if the timeout is reached but
     * the command is not finished.
     * 
     * @param cmd Command to wait for.
     * @param timeout Maximum time to wait for futures to complete.
     * @param <T> Result type.
     * @deprecated The method name does not reflect what the method is doing, therefore it is deprecated. Use
     *             {@link #awaitOrCancel(RedisFuture, long, TimeUnit)} instead. The semantics did not change and
     *             {@link #awaitOrCancel(RedisFuture, long, TimeUnit)} simply calls this method.
     * @return True if all futures complete in time.
     */
    public static <T> T await(RedisFuture<T> cmd, long timeout, TimeUnit unit) {
        return await(timeout, unit, cmd);
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param cmd Command to wait for.
     * @param <T> Result type.
     * @return Result of the command.
     */
    @Deprecated
    public static <T> T await(long timeout, TimeUnit unit, RedisFuture<T> cmd) {
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
            if (e.getCause() instanceof RedisCommandExecutionException) {
                throw new RedisCommandExecutionException(e.getCause().getMessage(), e.getCause());
            }
            throw new RedisException(e.getCause());
        }
    }
}
