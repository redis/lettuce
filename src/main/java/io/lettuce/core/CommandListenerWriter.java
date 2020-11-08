/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.event.command.CommandStartedEvent;
import io.lettuce.core.event.command.CommandSucceededEvent;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Writer for command listeners.
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.1
 */
@SuppressWarnings("unchecked")
public class CommandListenerWriter implements RedisChannelWriter {

    private final RedisChannelWriter delegate;

    private final CommandListener listener;

    private final Clock clock = Clock.systemDefaultZone();

    public CommandListenerWriter(RedisChannelWriter delegate, List<CommandListener> listeners) {
        this.delegate = delegate;
        this.listener = new CommandListenerMulticaster(new ArrayList<>(listeners));
    }

    /**
     * Check whether the list of {@link CommandListener} is not empty.
     *
     * @param commandListeners must not be {@code null}.
     * @return {@code true} if the list of {@link CommandListener} is not empty.
     */
    public static boolean isSupported(List<CommandListener> commandListeners) {

        LettuceAssert.notNull(commandListeners, "CommandListeners must not be null");

        return !commandListeners.isEmpty();
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        long now = clock.millis();
        CommandStartedEvent startedEvent = new CommandStartedEvent((RedisCommand<Object, Object, Object>) command, now);
        listener.commandStarted(startedEvent);

        return delegate.write(new RedisCommandListenerCommand<>(command, clock, startedEvent.getContext(), now, listener));
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {

        List<RedisCommandListenerCommand<K, V, ?>> listenedCommands = new ArrayList<>();
        long now = clock.millis();

        for (RedisCommand<K, V, ?> redisCommand : redisCommands) {

            CommandStartedEvent startedEvent = new CommandStartedEvent((RedisCommand<Object, Object, Object>) redisCommand,
                    now);
            listener.commandStarted(startedEvent);
            RedisCommandListenerCommand<K, V, ?> command = new RedisCommandListenerCommand<>(redisCommand, clock,
                    startedEvent.getContext(), now, listener);
            listenedCommands.add(command);
        }

        return delegate.write(listenedCommands);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return delegate.closeAsync();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reset() {
        delegate.reset();
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connection) {
        delegate.setConnectionFacade(connection);
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        delegate.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        delegate.flushCommands();
    }

    @Override
    public ClientResources getClientResources() {
        return delegate.getClientResources();
    }

    public RedisChannelWriter getDelegate() {
        return this.delegate;
    }

    private static class RedisCommandListenerCommand<K, V, T> extends CommandWrapper<K, V, T> {

        private final Clock clock;

        private final Map<String, Object> context;

        private final long startedAt;

        private final CommandListener listener;

        public RedisCommandListenerCommand(RedisCommand<K, V, T> command, Clock clock, Map<String, Object> context,
                long startedAt, CommandListener listener) {
            super(command);

            this.clock = clock;
            this.context = context;
            this.startedAt = startedAt;
            this.listener = listener;
        }

        @Override
        public void complete() {
            super.complete();

            if (getOutput().hasError()) {

                CommandFailedEvent failedEvent = new CommandFailedEvent((RedisCommand<Object, Object, Object>) command, context,
                        ExceptionFactory.createExecutionException(getOutput().getError()));
                listener.commandFailed(failedEvent);
            } else {
                long now = clock.millis();
                CommandSucceededEvent succeededEvent = new CommandSucceededEvent((RedisCommand<Object, Object, Object>) command,
                        context, startedAt, now);
                listener.commandSucceeded(succeededEvent);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
        }

        @Override
        public boolean completeExceptionally(Throwable throwable) {

            boolean state = super.completeExceptionally(throwable);

            CommandFailedEvent failedEvent = new CommandFailedEvent((RedisCommand<Object, Object, Object>) command, context,
                    throwable);
            listener.commandFailed(failedEvent);

            return state;
        }

    }

    /**
     * Wraps multiple command listeners into one multicaster.
     *
     * @author Mikhael Sokolov
     * @since 6.1
     */
    public static class CommandListenerMulticaster implements CommandListener {

        private final List<CommandListener> listeners;

        public CommandListenerMulticaster(List<CommandListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void commandStarted(CommandStartedEvent event) {
            for (CommandListener listener : listeners) {
                listener.commandStarted(event);
            }
        }

        @Override
        public void commandSucceeded(CommandSucceededEvent event) {
            for (CommandListener listener : listeners) {
                listener.commandSucceeded(event);
            }
        }

        @Override
        public void commandFailed(CommandFailedEvent event) {
            for (CommandListener listener : listeners) {
                listener.commandFailed(event);
            }
        }

    }

}
