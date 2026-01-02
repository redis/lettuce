package io.lettuce.core.failover;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.channel.Channel;

/**
 * Tracks command results and records them to the circuit breaker.
 *
 * @author Ali Takavci
 * @since 7.4
 */
class DatabaseCommandTracker {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCommandTracker.class);

    interface CommandWriter {

        <K, V, T> RedisCommand<K, V, T> writeOne(RedisCommand<K, V, T> command);

        <K, V> Collection<RedisCommand<K, V, ?>> writeMany(Collection<? extends RedisCommand<K, V, ?>> commands);

    }

    private final CommandWriter commandWriter;

    private CircuitBreaker circuitBreaker;

    private AtomicReference<Channel> channelRef = new AtomicReference<>();

    public DatabaseCommandTracker(CommandWriter commandWriter) {
        this.commandWriter = commandWriter;
    }

    public void bind(CircuitBreaker cb) {
        circuitBreaker = cb;
        registerHandlerToPipeline();
    }

    private void registerHandlerToPipeline() {
        try {
            Channel targetChannel = channelRef.get();
            if (targetChannel == null || circuitBreaker == null) {
                return;
            }

            if (channelRef.compareAndSet(targetChannel, null)) {
                String commandHandlerName = targetChannel.pipeline().context(CommandHandler.class).name();
                targetChannel.pipeline().addAfter(commandHandlerName, MultiDbOutboundHandler.HANDLER_NAME,
                        new MultiDbOutboundHandler(circuitBreaker));
            }
        } catch (Exception e) {
            log.error("Failed to register MultiDbOutboundHandler to pipeline", e);
            throw e;
        }
    }

    public void setChannel(Channel channel) {
        channelRef.set(channel);
        registerHandlerToPipeline();
    }

    public void resetChannel(Channel channel) {
        // remove/unbind tracker here
        channel.pipeline().remove(MultiDbOutboundHandler.class);
    }

    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        if (circuitBreaker == null) {
            return commandWriter.writeOne(command);
        }
        if (!circuitBreaker.isClosed()) {
            command.completeExceptionally(RedisCircuitBreakerException.INSTANCE);
            return command;
        }

        RedisCommand<K, V, T> result;
        try {
            // Delegate to parent
            result = commandWriter.writeOne(command);
        } catch (Exception e) {
            circuitBreaker.getGeneration().recordResult(e);
            throw e;
        }

        CircuitBreakerGeneration generation = circuitBreaker.getGeneration();
        // Attach completion callback to track success/failure
        attachRecorder(generation, result);
        return result;
    }

    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {
        if (circuitBreaker == null) {
            return commandWriter.writeMany(commands);
        }
        if (!circuitBreaker.isClosed()) {
            commands.forEach(c -> c.completeExceptionally(RedisCircuitBreakerException.INSTANCE));
            return (Collection) commands;
        }
        Collection<RedisCommand<K, V, ?>> result;
        try {
            // Delegate to parent
            result = commandWriter.writeMany(commands);
        } catch (Exception e) {
            // TODO: here not sure we should record exception for each command or just once for the batch
            circuitBreaker.getGeneration().recordResult(e);
            throw e;
        }

        // Attach completion callbacks to track success/failure for each command
        CircuitBreakerGeneration generation = circuitBreaker.getGeneration();
        for (RedisCommand<K, V, ?> command : result) {
            attachRecorder(generation, command);
        }
        return result;
    }

    private <K, V> void attachRecorder(CircuitBreakerGeneration generation, RedisCommand<K, V, ?> command) {
        if (command instanceof CompleteableCommand) {
            @SuppressWarnings("unchecked")
            CompleteableCommand<Object> completeable = (CompleteableCommand<Object>) command;
            completeable.onComplete((o, e) -> {
                // Only record timeout exceptions
                // Other exceptions are tracked inside the pipeline vioa MultiDbOutboundHandler
                if (e instanceof RedisCommandTimeoutException) {
                    generation.recordResult(e);
                }
            });
        }
    }

}
