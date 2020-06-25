/*
 * Copyright 2011-2020 the original author or authors.
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.lettuce.core.output.CommandOutput;
import io.netty.buffer.ByteBuf;

/**
 * Wrapper for a command.
 *
 * @author Mark Paluch
 */
public class CommandWrapper<K, V, T> implements RedisCommand<K, V, T>, CompleteableCommand<T>, DecoratedCommand<K, V, T> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final AtomicReferenceFieldUpdater<CommandWrapper, Object[]> ONCOMPLETE = AtomicReferenceFieldUpdater
            .newUpdater(CommandWrapper.class, Object[].class, "onComplete");

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final Object[] EMPTY = new Object[0];

    protected final RedisCommand<K, V, T> command;

    // accessed via AtomicReferenceFieldUpdater.
    @SuppressWarnings("unused")
    private volatile Object[] onComplete = EMPTY;

    public CommandWrapper(RedisCommand<K, V, T> command) {
        this.command = command;
    }

    @Override
    public CommandOutput<K, V, T> getOutput() {
        return command.getOutput();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void complete() {

        command.complete();

        Object[] consumers = ONCOMPLETE.get(this);
        if (!expireCallbacks(consumers)) {
            return;
        }

        for (Object callback : consumers) {

            if (callback instanceof Consumer) {
                Consumer consumer = (Consumer) callback;
                if (getOutput() != null) {
                    consumer.accept(getOutput().get());
                } else {
                    consumer.accept(null);
                }
            }

            if (callback instanceof BiConsumer) {
                BiConsumer consumer = (BiConsumer) callback;
                if (getOutput() != null) {
                    consumer.accept(getOutput().get(), null);
                } else {
                    consumer.accept(null, null);
                }
            }
        }
    }

    @Override
    public void cancel() {

        command.cancel();
        notifyBiConsumer(new CancellationException());
    }

    @Override
    public boolean completeExceptionally(Throwable throwable) {

        boolean result = command.completeExceptionally(throwable);
        notifyBiConsumer(throwable);

        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void notifyBiConsumer(Throwable exception) {

        Object[] consumers = ONCOMPLETE.get(this);

        if (!expireCallbacks(consumers)) {
            return;
        }

        for (Object callback : consumers) {

            if (!(callback instanceof BiConsumer)) {
                continue;
            }

            BiConsumer consumer = (BiConsumer) callback;
            if (getOutput() != null) {
                consumer.accept(getOutput().get(), exception);
            } else {
                consumer.accept(null, exception);
            }
        }
    }

    private boolean expireCallbacks(Object[] consumers) {
        return consumers != EMPTY && ONCOMPLETE.compareAndSet(this, consumers, EMPTY);
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return command.getArgs();
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
    public void setOutput(CommandOutput<K, V, T> output) {
        command.setOutput(output);
    }

    @Override
    public void onComplete(Consumer<? super T> action) {
        addOnComplete(action);
    }

    @Override
    public void onComplete(BiConsumer<? super T, Throwable> action) {
        addOnComplete(action);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addOnComplete(Object action) {

        for (;;) {

            Object[] existing = ONCOMPLETE.get(this);
            Object[] updated = new Object[existing.length + 1];
            System.arraycopy(existing, 0, updated, 0, existing.length);
            updated[existing.length] = action;

            if (ONCOMPLETE.compareAndSet(this, existing, updated)) {
                return;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [type=").append(getType());
        sb.append(", output=").append(getOutput());
        sb.append(", commandType=").append(command.getClass().getName());
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean isDone() {
        return command.isDone();
    }

    @Override
    public RedisCommand<K, V, T> getDelegate() {
        return command;
    }

    /**
     * Unwrap a wrapped command.
     *
     * @param wrapped
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V, T> RedisCommand<K, V, T> unwrap(RedisCommand<K, V, T> wrapped) {

        RedisCommand<K, V, T> result = wrapped;
        while (result instanceof DecoratedCommand<?, ?, ?>) {
            result = ((DecoratedCommand<K, V, T>) result).getDelegate();
        }

        return result;
    }

    /**
     * Returns an object that implements the given interface to allow access to non-standard methods, or standard methods not
     * exposed by the proxy.
     *
     * If the receiver implements the interface then the result is the receiver or a proxy for the receiver. If the receiver is
     * a wrapper and the wrapped object implements the interface then the result is the wrapped object or a proxy for the
     * wrapped object. Otherwise return the the result of calling <code>unwrap</code> recursively on the wrapped object or a
     * proxy for that result. If the receiver is not a wrapper and does not implement the interface, then an {@code null} is
     * returned.
     *
     * @param wrapped
     * @param iface A Class defining an interface that the result must implement.
     * @return the unwrapped instance or {@code null}.
     * @since 5.1
     */
    @SuppressWarnings("unchecked")
    public static <R, K, V, T> R unwrap(RedisCommand<K, V, T> wrapped, Class<R> iface) {

        RedisCommand<K, V, T> result = wrapped;

        if (iface.isInstance(wrapped)) {
            return iface.cast(wrapped);
        }

        while (result instanceof DecoratedCommand<?, ?, ?>) {
            result = ((DecoratedCommand<K, V, T>) result).getDelegate();

            if (iface.isInstance(result)) {
                return iface.cast(result);
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof RedisCommand)) {
            return false;
        }

        RedisCommand<?, ?, ?> left = command;
        while (left instanceof DecoratedCommand) {
            left = CommandWrapper.unwrap(left);
        }

        RedisCommand<?, ?, ?> right = (RedisCommand<?, ?, ?>) o;
        while (right instanceof DecoratedCommand) {
            right = CommandWrapper.unwrap(right);
        }

        return left == right;
    }

    @Override
    public int hashCode() {

        RedisCommand<?, ?, ?> toHash = command;
        while (toHash instanceof DecoratedCommand) {
            toHash = CommandWrapper.unwrap(toHash);
        }

        return toHash != null ? toHash.hashCode() : 0;
    }

}
