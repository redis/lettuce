package com.lambdaworks.redis;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.lambdaworks.redis.protocol.CommandOutput;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class LettuceFutures {

    private LettuceFutures() {

    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are not canceled (in contrast to
     * {@link #await(RedisCommand, long, TimeUnit)}) when the timeout expires.
     * 
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param futures Futures to wait for.
     * 
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
     * @param <K> Key type.
     * @param <V> Value type.
     * @param <T> Result type.
     * 
     * @return Result of the command.
     */
    public static <K, V, T> T awaitOrCancel(RedisCommand<K, V, T> cmd, long timeout, TimeUnit unit) {
        return await(cmd, timeout, unit);
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are canceled if the timeout is reached but
     * the command is not finished.
     * 
     * @param cmd Command to wait for.
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param <K> Key type.
     * @param <V> Value type.
     * @param <T> Result type.
     * @deprecated The method name does not reflect what the method is doing, therefore it is deprecated. Use
     *             {@link #awaitOrCancel(RedisCommand, long, TimeUnit)} instead. The semantics did not change and
     *             {@link #awaitOrCancel(RedisCommand, long, TimeUnit)} simply calls this method.
     * 
     * @return Result of the command.
     */
    @Deprecated
    public static <K, V, T> T await(RedisCommand<K, V, T> cmd, long timeout, TimeUnit unit) {
        if (!cmd.await(timeout, unit)) {
            cmd.cancel(true);
            throw new RedisCommandTimeoutException();
        }
        CommandOutput<K, V, T> output = cmd.getOutput();
        if (output.hasError()) {
            throw new RedisCommandExecutionException(output.getError());
        }
        return output.get();
    }
}
