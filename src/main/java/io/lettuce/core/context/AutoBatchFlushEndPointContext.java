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
public class AutoBatchFlushEndPointContext {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AutoBatchFlushEndPointContext.class);

    public static class HasOngoingSendLoop {

        /**
         * Used in multi-threaded environment, can be used to synchronize between threads.
         */
        final AtomicInteger safe;

        public HasOngoingSendLoop() {
            safe = new AtomicInteger();
        }

        /**
         * Try enter loop with the memory semantic getVolatile
         *
         * @return true if entered the loop, false if already have a running loop.
         */
        public boolean tryEnter() {
            return safe.get() == 0 && /* rare case if QPS is high */ safe.compareAndSet(0, 1);
        }

        public void exit() {
            safe.set(0);
        }

    }

    AutoBatchFlushEndPointContext() {
    }

    /**
     * Commands that failed to send (probably due to connection errors)
     */
    @Nullable
    Deque<RedisCommand<?, ?, ?>> retryableFailedToSendCommands = null;

    Throwable firstDiscontinueReason = null;

    public Throwable getFirstDiscontinueReason() {
        return firstDiscontinueReason;
    }

    private int flyingCmdNum;

    public final HasOngoingSendLoop hasOngoingSendLoop = new HasOngoingSendLoop();

    public void add(int n) {
        this.flyingCmdNum += n;
    }

    public @Nullable Deque<RedisCommand<?, ?, ?>> getAndClearRetryableFailedToSendCommands() {
        final Deque<RedisCommand<?, ?, ?>> old = this.retryableFailedToSendCommands;
        // don't set to null so give us a chance to expose potential bugs if there is addRetryableFailedToSendCommand()
        // afterwards
        this.retryableFailedToSendCommands = UnmodifiableDeque.emptyDeque();
        return old;
    }

    public void done(int n) {
        this.flyingCmdNum -= n;
    }

    public boolean isDone() {
        if (this.flyingCmdNum < 0) {
            logger.error("[unexpected] flyingCmdNum < 0, flyingCmdNum: {}", this.flyingCmdNum);
            return true;
        }
        return this.flyingCmdNum == 0;
    }

    public boolean hasRetryableFailedToSendCommands() {
        return retryableFailedToSendCommands != null;
    }

    /**
     * @param retryableCommand retryable command
     * @param cause fail reason
     * @return true if this is the first retryable failed command
     */
    public boolean addRetryableFailedToSendCommand(RedisCommand<?, ?, ?> retryableCommand, @Nonnull Throwable cause) {
        if (retryableFailedToSendCommands == null) {
            retryableFailedToSendCommands = new ArrayDeque<>();
            retryableFailedToSendCommands.add(retryableCommand);

            firstDiscontinueReason = cause;
            return true;
        }

        try {
            retryableFailedToSendCommands.add(retryableCommand);
        } catch (Exception e) {
            logger.error("[unexpected] retryableFailedToSendCommands is empty, but we are adding a command: {}",
                    retryableCommand);
        }
        return false;
    }

}
