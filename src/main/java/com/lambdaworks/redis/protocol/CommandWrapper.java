/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.protocol;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import com.lambdaworks.redis.output.CommandOutput;

import io.netty.buffer.ByteBuf;

/**
 * Wrapper for a command.
 *
 * @author Mark Paluch
 */
public class CommandWrapper<K, V, T> implements RedisCommand<K, V, T>, CompleteableCommand<T>, DecoratedCommand<K, V, T> {

    private final static AtomicReferenceFieldUpdater<CommandWrapper, Consumer[]> ONCOMPLETE = AtomicReferenceFieldUpdater
            .newUpdater(CommandWrapper.class, Consumer[].class, "onComplete");
    private final static Consumer<?>[] EMPTY = new Consumer[0];

    protected final RedisCommand<K, V, T> command;

    // accessed via AtomicReferenceFieldUpdater.
    private volatile Consumer<?>[] onComplete = EMPTY;

    public CommandWrapper(RedisCommand<K, V, T> command) {
        this.command = command;
    }

    @Override
    public CommandOutput<K, V, T> getOutput() {
        return command.getOutput();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void complete() {

        command.complete();

        Consumer[] consumers = ONCOMPLETE.get(this);
        if (consumers != EMPTY && ONCOMPLETE.compareAndSet(this, consumers, EMPTY)) {

            for (Consumer<? super T> consumer : consumers) {
                if (getOutput() != null) {
                    consumer.accept(getOutput().get());
                } else {
                    consumer.accept(null);
                }
            }
        }
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
    public void setOutput(CommandOutput<K, V, T> output) {
        command.setOutput(output);
    }

    @Override
    public void onComplete(Consumer<? super T> action) {

        for (;;) {

            Consumer[] existing = ONCOMPLETE.get(this);
            Consumer[] updated = new Consumer[existing.length + 1];
            System.arraycopy(existing, 0, updated, 0, existing.length);
            updated[existing.length] = action;

            if (ONCOMPLETE.compareAndSet(this, existing, updated)) {
                return;
            }
        }
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
     * @param <K>
     * @param <V>
     * @param <T>
     * @return
     */
    public static <K, V, T> RedisCommand<K, V, T> unwrap(RedisCommand<K, V, T> wrapped) {

        RedisCommand<K, V, T> result = wrapped;
        while (result instanceof DecoratedCommand<?, ?, ?>) {
            result = ((DecoratedCommand<K, V, T>) result).getDelegate();
        }

        return result;
    }
}
