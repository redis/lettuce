package io.lettuce.core;

import java.io.Serializable;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Options for command timeouts. These options configure how and whether commands time out once they were dispatched. Command
 * timeout begins:
 * <ul>
 * <li>When the command is sent successfully to the transport</li>
 * <li>Queued while the connection was inactive</li>
 * </ul>
 *
 * The timeout is canceled upon command completion/cancellation. Timeouts are not tied to a specific API and expire commands
 * regardless of the synchronization method provided by the API that was used to enqueue the command.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class AutoBatchFlushOptions implements Serializable {

    public static final boolean DEFAULT_ENABLE_AUTO_BATCH_FLUSH = false;

    public static final int DEFAULT_WRITE_SPIN_COUNT = 16;

    public static final int DEFAULT_BATCH_SIZE = 32;

    public static final boolean DEFAULT_USE_MPSC_QUEUE = true;

    private final boolean enableAutoBatchFlush;

    private final int writeSpinCount;

    private final int batchSize;

    private final boolean useMpscQueue;

    public AutoBatchFlushOptions(AutoBatchFlushOptions.Builder builder) {
        this.enableAutoBatchFlush = builder.enableAutoBatchFlush;
        this.writeSpinCount = builder.writeSpinCount;
        this.batchSize = builder.batchSize;
        this.useMpscQueue = builder.useMpscQueue;
    }

    /**
     * Returns a new {@link AutoBatchFlushOptions.Builder} to construct {@link AutoBatchFlushOptions}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new instance of {@link AutoBatchFlushOptions} with default settings.
     */
    public static AutoBatchFlushOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link AutoBatchFlushOptions}.
     */
    public static class Builder {

        private boolean enableAutoBatchFlush = DEFAULT_ENABLE_AUTO_BATCH_FLUSH;

        private int writeSpinCount = DEFAULT_WRITE_SPIN_COUNT;

        private int batchSize = DEFAULT_BATCH_SIZE;

        private boolean useMpscQueue = DEFAULT_USE_MPSC_QUEUE;

        /**
         * Enable auto batch flush.
         *
         * @param enableAutoBatchFlush {@code true} to enable auto batch flush.
         * @return {@code this}
         */
        public Builder enableAutoBatchFlush(boolean enableAutoBatchFlush) {
            this.enableAutoBatchFlush = enableAutoBatchFlush;
            return this;
        }

        /**
         * how many times to spin batchPoll() from the task queue
         *
         * @param writeSpinCount the write spin count
         * @return {@code this}
         */
        public Builder writeSpinCount(int writeSpinCount) {
            LettuceAssert.isPositive(writeSpinCount, "Batch size must be greater than 0");

            this.writeSpinCount = writeSpinCount;
            return this;
        }

        /**
         * how many commands to batch in a single flush
         *
         * @param batchSize the batch size
         * @return {@code this}
         */
        public Builder batchSize(int batchSize) {
            LettuceAssert.isPositive(batchSize, "Batch size must be greater than 0");

            this.batchSize = batchSize;
            return this;
        }

        /**
         * @param useMpscQueue use MPSC queue. If {@code false}, a {@link java.util.concurrent.ConcurrentLinkedQueue} is used,
         *        which has lower performance but is safer to consume across multiple threads, the option may be removed in the
         *        future if the mpsc queue is proven to be safe.
         * @return {@code this}
         */
        public Builder useMpscQueue(boolean useMpscQueue) {
            this.useMpscQueue = useMpscQueue;
            return this;
        }

        /**
         * Create a new instance of {@link AutoBatchFlushOptions}.
         *
         * @return new instance of {@link AutoBatchFlushOptions}
         */
        public AutoBatchFlushOptions build() {
            return new AutoBatchFlushOptions(this);
        }

    }

    /**
     * @return {@code true} if auto batch flush is enabled.
     */
    public boolean isAutoBatchFlushEnabled() {
        return enableAutoBatchFlush;
    }

    /**
     * @return the write spin count
     */
    public int getWriteSpinCount() {
        return writeSpinCount;
    }

    /**
     * @return the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @return {@code true} if the queue is a MPSC queue
     */
    public boolean usesMpscQueue() {
        return useMpscQueue;
    }

}
