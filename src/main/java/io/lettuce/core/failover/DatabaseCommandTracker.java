package io.lettuce.core.failover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.failover.api.RedisCircuitBreakerException;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

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
        if (channel.pipeline().get(MultiDbOutboundHandler.class) != null) {
            channel.pipeline().remove(MultiDbOutboundHandler.class);
        }
    }

    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        if (circuitBreaker == null) {
            return commandWriter.writeOne(command);
        }
        if (!circuitBreaker.isClosed()) {
            command.completeExceptionally(RedisCircuitBreakerException.INSTANCE);
            return command;
        }

        @SuppressWarnings("unchecked")
        CircuitBreakerAwareCommand<K, V, T> circuitBreakerAwareCommand = CommandWrapper.unwrap(command,
                CircuitBreakerAwareCommand.class);
        if (circuitBreakerAwareCommand == null) {
            command = new CircuitBreakerAwareCommand<>(command, circuitBreaker.getGeneration());
        }

        RedisCommand<K, V, T> result;
        try {
            // Delegate to parent
            result = commandWriter.writeOne(command);
        } catch (Exception e) {
            circuitBreaker.getGeneration().recordResult(e);
            throw e;
        }

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

        Collection<RedisCommand<K, V, ?>> circuitBreakerAwareCommands = new ArrayList<>(commands.size());
        for (RedisCommand<K, V, ?> command : commands) {
            @SuppressWarnings("unchecked")
            CircuitBreakerAwareCommand<K, V, ?> circuitBreakerAwareCommand = CommandWrapper.unwrap(command,
                    CircuitBreakerAwareCommand.class);
            if (circuitBreakerAwareCommand == null) {
                command = new CircuitBreakerAwareCommand<>(command, circuitBreaker.getGeneration());
            }
            circuitBreakerAwareCommands.add(command);
        }

        commands = circuitBreakerAwareCommands;

        Collection<RedisCommand<K, V, ?>> result;
        try {
            // Delegate to parent
            result = commandWriter.writeMany(commands);
        } catch (Exception e) {
            commands.forEach(c -> circuitBreaker.getGeneration().recordResult(e));
            throw e;
        }

        return result;
    }

    static class CircuitBreakerAwareCommand<K, V, T> extends CommandWrapper<K, V, T>
            implements GenericFutureListener<Future<Void>> {

        private final CircuitBreakerGeneration generation;

        public CircuitBreakerAwareCommand(RedisCommand<K, V, T> command, CircuitBreakerGeneration generation) {
            super(command);
            this.generation = generation;
        }

        @Override
        protected void doBeforeComplete() {
            generation.recordResult(null);
        }

        @Override
        protected void doBeforeError(Throwable throwable) {
            generation.recordResult(throwable);
        }

        @Override
        public void operationComplete(Future<Void> future) throws Exception {
            if (!future.isSuccess()) {
                generation.recordResult(future.cause());
            }
        }

    }

}
