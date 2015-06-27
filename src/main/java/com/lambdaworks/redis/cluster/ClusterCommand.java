package com.lambdaworks.redis.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.AbstractFuture;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.CommandOutput;
import com.lambdaworks.redis.protocol.RedisCommand;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class ClusterCommand<K, V, T> extends AbstractFuture<T> implements RedisCommand<K, V, T> {

    private RedisCommand<K, V, T> command;
    private RedisChannelWriter<K, V> retry;
    private int executions;
    private int executionLimit;

    ClusterCommand(RedisCommand<K, V, T> command, RedisChannelWriter<K, V> retry, int executionLimit) {
        this.command = command;
        this.retry = retry;
        this.executionLimit = executionLimit;
    }

    @Override
    public CommandOutput<K, V, T> getOutput() {
        return command.getOutput();
    }

    @Override
    public void complete() {
        executions++;

        if (executions < executionLimit && isMoved()) {
            retry.write(this);
            return;
        }

        command.complete();
    }

    public boolean isMoved() {
        if (getError() != null && getError().startsWith(CommandKeyword.MOVED.name())) {
            return true;
        }
        return false;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        command.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return command.cancel(mayInterruptIfRunning);
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
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return command.get(timeout, unit);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return command.get();
    }

    @Override
    public String getError() {
        return command.getError();
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return command.getArgs();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) {
        return command.await(timeout, unit);
    }

    public int getExecutions() {
        return executions;
    }

    public int getExecutionLimit() {
        return executionLimit;
    }

    @Override
    public void encode(ByteBuf buf) {
        command.encode(buf);
    }

    @Override
    public boolean setException(Throwable exception) {
        return command.setException(exception);
    }
}
