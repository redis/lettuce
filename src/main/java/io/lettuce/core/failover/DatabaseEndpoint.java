package io.lettuce.core.failover;

import java.util.Collection;

import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Database endpoint interface for multi-database failover with circuit breaker metrics tracking.
 *
 * @author Ali Takavci
 * @since 7.1
 */
interface DatabaseEndpoint {

    Collection<RedisCommand<?, ?, ?>> drainCommands();

    <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command);

    /**
     * Set the circuit breaker for this endpoint. Must be called before any commands are written.
     *
     * @param circuitBreaker the circuit breaker instance
     */
    void setCircuitBreaker(CircuitBreaker circuitBreaker);

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
