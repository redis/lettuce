package com.lambdaworks.redis;

import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 15:17
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
     * 
     * @return True if all futures complete in time.
     */
    public static boolean awaitAll(long timeout, TimeUnit unit, Future<?>... futures) {
        boolean complete;

        try {
            long nanos = unit.toNanos(timeout);
            long time = System.nanoTime();

            for (Future<?> f : futures) {
                if (nanos < 0)
                    return false;
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

    public static <K, V, T> T await(Command<K, V, T> cmd, long timeout, TimeUnit unit) {
        if (!cmd.await(timeout, unit)) {
            cmd.cancel(true);
            throw new RedisException("Command timed out");
        }
        CommandOutput<K, V, T> output = cmd.getOutput();
        if (output.hasError())
            throw new RedisException(output.getError());
        return output.get();
    }
}
