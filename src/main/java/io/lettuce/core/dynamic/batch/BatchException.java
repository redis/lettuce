package io.lettuce.core.dynamic.batch;

import java.util.Collections;
import java.util.List;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Batch exception to collect multiple errors from batched command execution.
 * <p>
 * Commands that fail during the batch cause a {@link BatchException} while non-failed commands remain executed successfully.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see BatchExecutor
 * @see BatchSize
 * @see CommandBatching
 */
@SuppressWarnings("serial")
public class BatchException extends RedisCommandExecutionException {

    private final List<RedisCommand<?, ?, ?>> failedCommands;

    /**
     * Create a new {@link BatchException}.
     *
     * @param failedCommands {@link List} of failed {@link RedisCommand}s.
     */
    public BatchException(List<RedisCommand<?, ?, ?>> failedCommands) {
        super("Error during batch command execution");
        this.failedCommands = Collections.unmodifiableList(failedCommands);
    }

    /**
     * @return the failed commands.
     */
    public List<RedisCommand<?, ?, ?>> getFailedCommands() {
        return failedCommands;
    }

}
