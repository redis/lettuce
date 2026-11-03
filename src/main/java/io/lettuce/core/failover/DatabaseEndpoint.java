package io.lettuce.core.failover;

import java.util.Collection;

import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Database endpoint interface for multi-database failover with circuit breaker metrics tracking.
 *
 * @author Ali Takavci
 * @since 7.4
 */
interface DatabaseEndpoint {

    /**
     * Bind a circuit breaker to this endpoint. There is 1-1 relationship between a database endpoint and a circuit breaker.
     * Must be called before any commands are written.
     *
     * @param circuitBreaker the circuit breaker instance
     */
    void bind(CircuitBreaker circuitBreaker);

    /**
     * Drains the command queue.
     *
     * @return the drained commands
     */
    Collection<RedisCommand<?, ?, ?>> drainCommands();

    /**
     * Write a command on the channel. The command may be changed/wrapped during write and the written instance is returned
     * after the call.
     *
     * @param command the Redis command.
     * @param <T> result type
     * @return the written Redis command.
     */
    <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command);

    /**
     * Hand over the command queue to the target endpoint.
     *
     * @param target the target endpoint
     */
    default void handOverCommandQueue(DatabaseEndpoint target) {
        Collection<RedisCommand<?, ?, ?>> commands = this.drainCommands();

        for (RedisCommand<?, ?, ?> queuedCommand : commands) {
            if (queuedCommand == null || queuedCommand.isCancelled()) {
                continue;
            }

            try {
                target.write(queuedCommand);
            } catch (RedisException e) {
                queuedCommand.completeExceptionally(e);
            }
        }
    }

}
