/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.CommandOutput;
import io.netty.buffer.ByteBuf;

/**
 * An asynchronous redis command and its result. All successfully executed commands will eventually return a
 * {@link CommandOutput} object.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Command output type.
 *
 * @author Mark Paluch
 */
public class AsyncCommand<K, V, T> extends CompletableFuture<T>
        implements RedisCommand<K, V, T>, RedisFuture<T>, CompleteableCommand<T>, DecoratedCommand<K, V, T> {

    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AsyncCommand> COUNT_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(AsyncCommand.class, "count");

    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<AsyncCommand> QUEUED_AT_UPDATER = AtomicLongFieldUpdater
            .newUpdater(AsyncCommand.class, "queuedAtNanos");

    private final RedisCommand<K, V, T> command;

    // access via COUNT_UPDATER
    @SuppressWarnings({ "unused" })
    private volatile int count = 1;

    // First accepted submission, preserved when a command is replayed after reconnect.
    private volatile long queuedAtNanos;

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AsyncCommand, FirstSend> FIRST_SENT_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(AsyncCommand.class, FirstSend.class, "firstSend");

    private volatile FirstSend firstSend;

    /**
     * The timestamp and connection identity must be published together: a replay can run on another channel.
     */
    static final class FirstSend {

        @SuppressWarnings("rawtypes")
        private static final AtomicLongFieldUpdater<FirstSend> DECODED_AT_UPDATER = AtomicLongFieldUpdater
                .newUpdater(FirstSend.class, "decodedAtNanos");

        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<FirstSend> SAMPLE_CLAIMED_UPDATER = AtomicIntegerFieldUpdater
                .newUpdater(FirstSend.class, "sampleClaimed");

        private volatile long decodedAtNanos;

        private volatile int sampleClaimed;

        final long timestampNanos;

        final SocketAddress local;

        final SocketAddress remote;

        FirstSend(long timestampNanos, SocketAddress local, SocketAddress remote) {
            this.timestampNanos = timestampNanos;
            this.local = local;
            this.remote = remote;
        }

        void markDecoded(long timestampNanos) {
            long previous = decodedAtNanos;
            // Redirected commands may decode concurrently; retain the final decode timestamp.
            while (previous == 0 || timestampNanos - previous > 0) {
                if (DECODED_AT_UPDATER.compareAndSet(this, previous, timestampNanos)) {
                    return;
                }
                previous = decodedAtNanos;
            }
        }

        long latestDecodedAt() {
            return decodedAtNanos;
        }

        boolean claimQueueSample() {
            return SAMPLE_CLAIMED_UPDATER.compareAndSet(this, 0, 1);
        }

    }

    /**
     * @param command the command, must not be {@code null}.
     */
    public AsyncCommand(RedisCommand<K, V, T> command) {
        this(command, 1);
    }

    /**
     * @param command the command, must not be {@code null}.
     */
    protected AsyncCommand(RedisCommand<K, V, T> command, int count) {
        LettuceAssert.notNull(command, "RedisCommand must not be null");
        this.command = command;
        this.count = count;
    }

    /** Record submission once, including when the command is nested in other decorators. */
    static void markEnqueued(RedisCommand<?, ?, ?> command, long timestampNanos) {
        RedisCommand<?, ?, ?> carrier = findTimestampCarrier(command);
        if (carrier instanceof AsyncCommand) {
            QUEUED_AT_UPDATER.compareAndSet((AsyncCommand<?, ?, ?>) carrier, 0, timestampNanos);
        } else if (carrier instanceof Command) {
            ((Command<?, ?, ?>) carrier).markEnqueued(timestampNanos);
        }
    }

    /** Returns zero when the command has not been instrumented. */
    static long queuedAt(RedisCommand<?, ?, ?> command) {
        RedisCommand<?, ?, ?> carrier = findTimestampCarrier(command);
        if (carrier instanceof AsyncCommand) {
            return ((AsyncCommand<?, ?, ?>) carrier).queuedAtNanos;
        }
        return carrier instanceof Command ? ((Command<?, ?, ?>) carrier).queuedAt() : 0;
    }

    static void markSent(RedisCommand<?, ?, ?> command, long timestampNanos) {
        markSent(command, timestampNanos, null, null);
    }

    static void markSent(RedisCommand<?, ?, ?> command, long timestampNanos, SocketAddress local,
            SocketAddress remote) {
        RedisCommand<?, ?, ?> carrier = findTimestampCarrier(command);
        FirstSend firstSend = new FirstSend(timestampNanos, local, remote);
        if (carrier instanceof AsyncCommand) {
            FIRST_SENT_UPDATER.compareAndSet((AsyncCommand<?, ?, ?>) carrier, null, firstSend);
        } else if (carrier instanceof Command) {
            ((Command<?, ?, ?>) carrier).markSent(firstSend);
        }
    }

    static FirstSend firstSend(RedisCommand<?, ?, ?> command) {
        RedisCommand<?, ?, ?> carrier = findTimestampCarrier(command);
        if (carrier instanceof AsyncCommand) {
            return ((AsyncCommand<?, ?, ?>) carrier).firstSend;
        }
        return carrier instanceof Command ? ((Command<?, ?, ?>) carrier).firstSend() : null;
    }

    static long firstSent(RedisCommand<?, ?, ?> command) {
        FirstSend firstSend = firstSend(command);
        return firstSend != null ? firstSend.timestampNanos : 0;
    }

    static void markDecoded(RedisCommand<?, ?, ?> command, long timestampNanos) {
        FirstSend firstSend = firstSend(command);
        if (firstSend != null) {
            firstSend.markDecoded(timestampNanos);
        }
    }

    /** Check future cancellation after completion without mistaking reactive cleanup for cancellation of a result. */
    static boolean hasCancelledFuture(RedisCommand<?, ?, ?> command) {
        while (command != null) {
            if (command instanceof AsyncCommand && command.isCancelled()) {
                return true;
            }
            if (!(command instanceof DecoratedCommand)) {
                break;
            }
            RedisCommand<?, ?, ?> delegate = ((DecoratedCommand<?, ?, ?>) command).getDelegate();
            if (delegate == command) {
                break;
            }
            command = delegate;
        }
        return false;
    }

    private static RedisCommand<?, ?, ?> findTimestampCarrier(RedisCommand<?, ?, ?> command) {
        while (command != null) {
            if (command instanceof AsyncCommand || command instanceof Command) {
                return command;
            }
            if (!(command instanceof DecoratedCommand)) {
                break;
            }
            RedisCommand<?, ?, ?> delegate = ((DecoratedCommand<?, ?, ?>) command).getDelegate();
            if (delegate == command) {
                break;
            }
            command = delegate;
        }
        return null;
    }

    /**
     * Wait up to the specified time for the command output to become available.
     *
     * @param timeout Maximum time to wait for a result.
     * @param unit Unit of time for the timeout.
     *
     * @return true if the output became available.
     */
    @Override
    public boolean await(long timeout, TimeUnit unit) {
        try {
            get(timeout, unit);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } catch (ExecutionException e) {
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Get the object that holds this command's output.
     *
     * @return The command output object.
     */
    @Override
    public CommandOutput<K, V, T> getOutput() {
        return command.getOutput();
    }

    /**
     * Mark this command complete and notify all waiting threads.
     */
    @Override
    public void complete() {
        if (COUNT_UPDATER.decrementAndGet(this) == 0) {
            completeResult();
            command.complete();
        }
    }

    protected void completeResult() {
        if (command.getOutput() == null) {
            complete(null);
        } else if (command.getOutput().hasError()) {
            doCompleteExceptionally(ExceptionFactory.createExecutionException(command.getOutput().getError()));
        } else {
            complete(command.getOutput().get());
        }
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        boolean result = false;

        int ref = COUNT_UPDATER.get(this);
        if (ref > 0 && COUNT_UPDATER.compareAndSet(this, ref, 0)) {
            result = doCompleteExceptionally(ex);
        }
        return result;
    }

    private boolean doCompleteExceptionally(Throwable ex) {
        command.completeExceptionally(ex);
        return super.completeExceptionally(ex);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        try {
            command.cancel();
            return super.cancel(mayInterruptIfRunning);
        } finally {
            COUNT_UPDATER.set(this, 0);
        }
    }

    @Override
    public String getError() {
        return command.getOutput().getError();
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return command.getArgs();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [type=").append(getType());
        sb.append(", output=").append(getOutput());
        sb.append(", commandType=").append(command.getClass().getName());
        sb.append(']');
        return sb.toString();
    }

    @Override
    public ProtocolKeyword getType() {
        return command.getType();
    }

    @Override
    public void cancel() {
        cancel(true);
    }

    @Override
    public void encode(ByteBuf buf) {
        command.encode(buf);
    }

    @Override
    public void setOutput(CommandOutput<K, V, T> output) {
        command.setOutput(output);
    }

    @Override
    public void onComplete(Consumer<? super T> action) {
        thenAccept(action);
    }

    @Override
    public void onComplete(BiConsumer<? super T, Throwable> action) {
        whenComplete(action);
    }

    @Override
    public RedisCommand<K, V, T> getDelegate() {
        return command;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof RedisCommand)) {
            return false;
        }

        RedisCommand<?, ?, ?> left = CommandWrapper.unwrap(command);
        RedisCommand<?, ?, ?> right = CommandWrapper.unwrap((RedisCommand<?, ?, ?>) o);

        return left == right;
    }

    @Override
    public int hashCode() {

        RedisCommand<?, ?, ?> toHash = CommandWrapper.unwrap(command);

        return toHash != null ? toHash.hashCode() : 0;
    }

}
