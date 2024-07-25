package io.lettuce.core.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.lettuce.core.datastructure.queue.unmodifiabledeque.UnmodifiableDeque;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author chenxiaofan
 */
public class BatchFlushEndPointContext {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BatchFlushEndPointContext.class);

    public static class HasOngoingSendLoop {

        /**
         * Used in multi-threaded environment, can be used to synchronize between threads.
         */
        final AtomicInteger safe;

        /**
         * Used in single thread.
         */
        boolean unsafe;

        public HasOngoingSendLoop() {
            safe = new AtomicInteger();
            unsafe = false;
        }

        /**
         * Try enter loop with the memory semantic getVolatile
         *
         * @return true if entered the loop, false if already have a running loop.
         */
        public boolean tryEnterSafeGetVolatile() {
            return safe.get() == 0 && /* rare case if QPS is high */ safe.compareAndSet(0, 1);
        }

        public void exitSafe() {
            safe.set(0);
        }

        /**
         * This method is not thread safe, can only be used from single thread.
         *
         * @return true if the value was updated
         */
        public boolean tryEnterUnsafe() {
            if (unsafe) {
                return false;
            }
            unsafe = true;
            return true;
        }

        public void exitUnsafe() {
            unsafe = false;
        }

    }

    BatchFlushEndPointContext() {
    }

    /**
     * Tasks that failed to send (probably due to connection errors)
     */
    @Nullable
    Deque<RedisCommand<?, ?, ?>> retryableFailedToSendTasks = null;

    Throwable firstDiscontinueReason = null;

    public Throwable getFirstDiscontinueReason() {
        return firstDiscontinueReason;
    }

    private int flyingTaskNum;

    @SuppressWarnings("unused")
    public int getFlyingTaskNum() {
        return flyingTaskNum;
    }

    private int total = 0;

    public int getTotal() {
        return total;
    }

    private final HasOngoingSendLoop hasOngoingSendLoop = new HasOngoingSendLoop();

    public HasOngoingSendLoop getHasOngoingSendLoop() {
        return hasOngoingSendLoop;
    }

    public void add(int n) {
        this.total += n;
        this.flyingTaskNum += n;
    }

    public @Nullable Deque<RedisCommand<?, ?, ?>> getAndClearRetryableFailedToSendTasks() {
        final Deque<RedisCommand<?, ?, ?>> old = this.retryableFailedToSendTasks;
        // don't set to null so give us a chance to expose potential bugs if there is addRetryableFailedToSendTask() afterwards
        this.retryableFailedToSendTasks = UnmodifiableDeque.emptyDeque();
        return old;
    }

    public void done(int n) {
        this.flyingTaskNum -= n;
    }

    public boolean isDone() {
        if (this.flyingTaskNum < 0) {
            logger.error("[unexpected] flyingTaskNum < 0, flyingTaskNum: {}, total: {}", this.flyingTaskNum, this.total);
            return true;
        }
        return this.flyingTaskNum == 0;
    }

    public boolean hasRetryableFailedToSendTasks() {
        return retryableFailedToSendTasks != null;
    }

    /**
     * @param retryableTask retryable task
     * @param cause fail reason
     * @return true if this is the first retryable failed task
     */
    public boolean addRetryableFailedToSendTask(RedisCommand<?, ?, ?> retryableTask, @Nonnull Throwable cause) {
        if (retryableFailedToSendTasks == null) {
            retryableFailedToSendTasks = new ArrayDeque<>();
            retryableFailedToSendTasks.add(retryableTask);

            firstDiscontinueReason = cause;
            return true;
        }

        retryableFailedToSendTasks.add(retryableTask);
        return false;
    }

}
