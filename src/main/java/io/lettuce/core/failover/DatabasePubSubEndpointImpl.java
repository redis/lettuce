package io.lettuce.core.failover;

import java.util.Collection;
import java.util.List;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.resource.ClientResources;

/**
 * Database PubSub endpoint implementation for multi-database failover with circuit breaker metrics tracking. Extends
 * PubSubEndpoint and tracks command successes and failures.
 *
 * @author Ali Takavci
 * @since 7.1
 */
class DatabasePubSubEndpointImpl<K, V> extends PubSubEndpoint<K, V> implements DatabaseEndpoint {

    private CircuitBreaker circuitBreaker;

    public DatabasePubSubEndpointImpl(ClientOptions clientOptions, ClientResources clientResources) {
        super(clientOptions, clientResources);
    }

    /**
     * Bind a circuit breaker to this endpoint. There is 1-1 relationship between a database endpoint and a circuit breaker.
     * Must be called before any commands are written.
     *
     * @param circuitBreaker the circuit breaker instance
     */
    @Override
    public void bind(CircuitBreaker circuitBreaker) {
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
    public <K1, V1, T> RedisCommand<K1, V1, T> write(RedisCommand<K1, V1, T> command) {
        if (circuitBreaker == null) {
            return super.write(command);
        }
        if (!circuitBreaker.isClosed()) {
            command.completeExceptionally(RedisCircuitBreakerException.INSTANCE);
            return command;
        }
        // Delegate to parent
        RedisCommand<K1, V1, T> result = super.write(command);

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
    public <K1, V1> Collection<RedisCommand<K1, V1, ?>> write(Collection<? extends RedisCommand<K1, V1, ?>> commands) {
        if (circuitBreaker == null) {
            return super.write(commands);
        }
        if (!circuitBreaker.isClosed()) {
            commands.forEach(c -> c.completeExceptionally(RedisCircuitBreakerException.INSTANCE));
            return (Collection) commands;
        }
        // Delegate to parent
        Collection<RedisCommand<K1, V1, ?>> result = super.write(commands);

        // Attach completion callbacks to track success/failure for each command
        CircuitBreakerGeneration generation = circuitBreaker.getGeneration();
        for (RedisCommand<K1, V1, ?> command : result) {
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
