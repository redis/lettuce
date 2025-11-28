package io.lettuce.core.failover;

import java.util.Collection;
import java.util.List;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Database endpoint implementation for multi-database failover with circuit breaker metrics tracking. Extends DefaultEndpoint
 * and tracks command successes and failures.
 *
 * @author Ali Takavci
 * @since 7.1
 */
class DatabaseEndpointImpl extends DefaultEndpoint implements DatabaseEndpoint {

    private CircuitBreaker circuitBreaker;

    public DatabaseEndpointImpl(ClientOptions clientOptions, ClientResources clientResources) {
        super(clientOptions, clientResources);
    }

    /**
     * Initialize this endpoint. Must be called before any commands are written.
     *
     * @param circuitBreaker the circuit breaker instance
     */
    @Override
    public void init(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Get the circuit breaker for this endpoint.
     *
     * @return the circuit breaker instance
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        if (circuitBreaker == null) {
            return super.write(command);
        }
        if (!circuitBreaker.isClosed()) {
            command.completeExceptionally(RedisCircuitBreakerException.INSTANCE);
            return command;
        }
        // Delegate to parent
        RedisCommand<K, V, T> result = super.write(command);

        // Attach completion callback to track success/failure
        if (result instanceof CompleteableCommand) {
            CircuitBreakerGeneration generation = circuitBreaker.getGeneration();
            @SuppressWarnings("unchecked")
            CompleteableCommand<T> completeable = (CompleteableCommand<T>) result;
            completeable.onComplete(generation::recordResult);
        }
        return result;
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {
        if (circuitBreaker == null) {
            return super.write(commands);
        }
        if (!circuitBreaker.isClosed()) {
            commands.forEach(c -> c.completeExceptionally(RedisCircuitBreakerException.INSTANCE));
            return (Collection) commands;
        }
        // Delegate to parent
        Collection<RedisCommand<K, V, ?>> result = super.write(commands);

        // Attach completion callbacks to track success/failure for each command
        CircuitBreakerGeneration generation = circuitBreaker.getGeneration();
        for (RedisCommand<K, V, ?> command : result) {
            if (command instanceof CompleteableCommand) {
                @SuppressWarnings("unchecked")
                CompleteableCommand<Object> completeable = (CompleteableCommand<Object>) command;
                completeable.onComplete(generation::recordResult);
            }
        }
        return result;
    }

    @Override
    public List<RedisCommand<?, ?, ?>> drainCommands() {
        return super.drainCommands();
    }

}
