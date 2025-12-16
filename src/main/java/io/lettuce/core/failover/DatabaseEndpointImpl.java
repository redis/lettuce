package io.lettuce.core.failover;

import java.util.Collection;
import java.util.List;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.Channel;

/**
 * Database endpoint implementation for multi-database failover with circuit breaker metrics tracking. Extends DefaultEndpoint
 * and tracks command successes and failures.
 *
 * @author Ali Takavci
 * @since 7.4
 */
class DatabaseEndpointImpl extends DefaultEndpoint implements DatabaseEndpoint, DatabaseCommandTracker.CommandWriter {

    private final DatabaseCommandTracker tracker;

    private CircuitBreaker circuitBreaker;

    public DatabaseEndpointImpl(ClientOptions clientOptions, ClientResources clientResources) {
        super(clientOptions, clientResources);
        this.tracker = new DatabaseCommandTracker(this);
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
        tracker.bind(circuitBreaker);
    }

    @Override
    public void notifyChannelActive(Channel channel) {
        super.notifyChannelActive(channel);
        tracker.setChannel(channel);
    }

    @Override
    public void notifyChannelInactive(Channel channel) {
        super.notifyChannelInactive(channel);
        // remove/unbind tracker here
        tracker.resetChannel(channel);
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
        return tracker.write(command);
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {
        return tracker.write(commands);
    }

    @Override
    public List<RedisCommand<?, ?, ?>> drainCommands() {
        return super.drainCommands();
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> writeOne(RedisCommand<K, V, T> command) {
        return super.write(command);
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> writeMany(Collection<? extends RedisCommand<K, V, ?>> commands) {
        return super.write(commands);
    }

}
