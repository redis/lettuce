/*
 * Copyright 2017-2022 the original author or authors.
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
package io.lettuce.core.protocol;

import static io.lettuce.core.TimeoutOptions.*;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.ClientResources;

/**
 * Extension to {@link RedisChannelWriter} that expires commands. Command timeout starts at the time the command is written
 * regardless to {@link #setAutoFlushCommands(boolean) flushing mode} (user-controlled batching).
 *
 * @author Mark Paluch
 * @since 5.1
 * @see io.lettuce.core.TimeoutOptions
 */
public class CommandExpiryWriter implements RedisChannelWriter {

    private final RedisChannelWriter delegate;

    private final TimeoutSource source;

    private final TimeUnit timeUnit;

    private final ScheduledExecutorService executorService;

    private final boolean applyConnectionTimeout;

    private volatile long timeout = -1;

    /**
     * Create a new {@link CommandExpiryWriter}.
     *
     * @param delegate must not be {@code null}.
     * @param clientOptions must not be {@code null}.
     * @param clientResources must not be {@code null}.
     */
    public CommandExpiryWriter(RedisChannelWriter delegate, ClientOptions clientOptions, ClientResources clientResources) {

        LettuceAssert.notNull(delegate, "RedisChannelWriter must not be null");
        LettuceAssert.isTrue(isSupported(clientOptions), "Command timeout not enabled");
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");

        TimeoutOptions timeoutOptions = clientOptions.getTimeoutOptions();
        this.delegate = delegate;
        this.source = timeoutOptions.getSource();
        this.applyConnectionTimeout = timeoutOptions.isApplyConnectionTimeout();
        this.timeUnit = source.getTimeUnit();
        this.executorService = clientResources.eventExecutorGroup();
    }

    /**
     * Check whether {@link ClientOptions} is configured to timeout commands.
     *
     * @param clientOptions must not be {@code null}.
     * @return {@code true} if {@link ClientOptions} are configured to timeout commands.
     */
    public static boolean isSupported(ClientOptions clientOptions) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");

        return isSupported(clientOptions.getTimeoutOptions());
    }

    private static boolean isSupported(TimeoutOptions timeoutOptions) {

        LettuceAssert.notNull(timeoutOptions, "TimeoutOptions must not be null");

        return timeoutOptions.isTimeoutCommands();
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connectionFacade) {
        delegate.setConnectionFacade(connectionFacade);
    }

    @Override
    public ClientResources getClientResources() {
        return delegate.getClientResources();
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        delegate.setAutoFlushCommands(autoFlush);
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        potentiallyExpire(command, getExecutorService());
        return delegate.write(command);
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {

        ScheduledExecutorService executorService = getExecutorService();

        for (RedisCommand<K, V, ?> command : redisCommands) {
            potentiallyExpire(command, executorService);
        }

        return delegate.write(redisCommands);
    }

    @Override
    public void flushCommands() {
        delegate.flushCommands();
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
    public void reset() {
        delegate.reset();
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeUnit.convert(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public RedisChannelWriter getDelegate() {
        return delegate;
    }

    private ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    @SuppressWarnings("unchecked")
    private void potentiallyExpire(RedisCommand<?, ?, ?> command, ScheduledExecutorService executors) {

        long timeout = applyConnectionTimeout ? this.timeout : source.getTimeout(command);

        if (timeout <= 0) {
            return;
        }

        ScheduledFuture<?> schedule = executors.schedule(() -> {

            if (!command.isDone()) {
                command.completeExceptionally(
                        ExceptionFactory.createTimeoutException(Duration.ofNanos(timeUnit.toNanos(timeout))));
            }

        }, timeout, timeUnit);

        if (command instanceof CompleteableCommand) {
            ((CompleteableCommand) command).onComplete((o, o2) -> {

                if (!schedule.isDone()) {
                    schedule.cancel(false);
                }
            });
        }
    }

}
