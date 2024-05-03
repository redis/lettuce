package io.lettuce.core.dynamic.batch;

/**
 * Batch executor interface to enforce command queue flushing using {@link BatchSize}.
 * <p>
 * Commands remain in a batch queue until the batch size is reached or the queue is {@link BatchExecutor#flush() flushed}. If
 * the batch size is not reached, commands remain not executed.
 * <p>
 * Commands that fail during the batch cause a {@link BatchException} while non-failed commands remain executed successfully.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see BatchSize
 */
public interface BatchExecutor {

    /**
     * Flush the command queue resulting in the queued commands being executed.
     *
     * @throws BatchException if at least one command failed.
     */
    void flush() throws BatchException;

}
