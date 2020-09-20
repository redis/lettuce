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

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.events.CommandFailedEvent;
import io.lettuce.core.models.events.CommandStartedEvent;
import io.lettuce.core.models.events.CommandSucceededEvent;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Writer for command listeners.
 *
 * @author Mikhael Sokolov
 */
public class CommandListenerWriter implements RedisChannelWriter {

    private final CommandListener listener;
    private final RedisChannelWriter delegate;

    public CommandListenerWriter(RedisChannelWriter delegate, CommandListener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    /**
     * Check whether {@link ClientResources} is configured to listen commands.
     *
     * @param clientResources must not be {@code null}.
     * @return {@code true} if {@link ClientResources} are configured to listen commands.
     */
    public static boolean isSupported(ClientResources clientResources) {
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");

        return !clientResources.commandListeners().isEmpty();
    }


    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        long now = System.currentTimeMillis();
        CommandStartedEvent<K, V, T> startedEvent = new CommandStartedEvent<>(command, now);
        listener.commandStarted(startedEvent);

        return delegate.write(new RedisCommandListenerCommand<>(command, startedEvent.getContext(), now, listener));
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {
        List<RedisCommandListenerCommand<K, V, ?>> listenedCommands = new ArrayList<>();
        for (RedisCommand<K, V, ?> redisCommand : redisCommands) {
            long now = System.currentTimeMillis();
            CommandStartedEvent<K, V, ?> startedEvent = new CommandStartedEvent<>(redisCommand, now);
            listener.commandStarted(startedEvent);
            RedisCommandListenerCommand<K, V, ?> command = new RedisCommandListenerCommand<>(redisCommand, startedEvent.getContext(), now, listener);
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


    private static class RedisCommandListenerCommand<K, V, T> implements RedisCommand<K, V, T>, DecoratedCommand<K, V, T> {

        private final RedisCommand<K, V, T> command;
        private final Map<String, ?> context;
        private final long startedAt;
        private final CommandListener listener;

        public RedisCommandListenerCommand(RedisCommand<K, V, T> command, Map<String, ?> context, long startedAt, CommandListener listener) {
            this.command = command;
            this.context = context;
            this.startedAt = startedAt;
            this.listener = listener;
        }

        @Override
        public RedisCommand<K, V, T> getDelegate() {
            return command;
        }

        @Override
        public CommandOutput<K, V, T> getOutput() {
            return command.getOutput();
        }

        @Override
        public void complete() {
            if (getOutput().hasError()) {
                CommandFailedEvent<K, V, T> failedEvent = new CommandFailedEvent<>(command, context, new RedisCommandExecutionException(getOutput().getError()));
                listener.commandFailed(failedEvent);
            } else {
                long now = System.currentTimeMillis();
                CommandSucceededEvent<K, V, T> succeededEvent = new CommandSucceededEvent<>(command, context, startedAt, now);
                listener.commandSucceeded(succeededEvent);
            }
            command.complete();
        }

        @Override
        public void cancel() {
            command.cancel();
        }

        @Override
        public CommandArgs<K, V> getArgs() {
            return command.getArgs();
        }

        @Override
        public boolean completeExceptionally(Throwable throwable) {
            CommandFailedEvent<K, V, T> failedEvent = new CommandFailedEvent<>(command, context, throwable);
            listener.commandFailed(failedEvent);

            return command.completeExceptionally(throwable);
        }

        @Override
        public ProtocolKeyword getType() {
            return command.getType();
        }

        @Override
        public void encode(ByteBuf buf) {
            command.encode(buf);
        }

        @Override
        public boolean isCancelled() {
            return command.isCancelled();
        }

        @Override
        public boolean isDone() {
            return command.isDone();
        }

        @Override
        public void setOutput(CommandOutput<K, V, T> output) {
            command.setOutput(output);
        }
    }
}
