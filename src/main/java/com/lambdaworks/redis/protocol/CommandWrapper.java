package com.lambdaworks.redis.protocol;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.output.CommandOutput;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 16.06.15 10:34
 */
public class CommandWrapper<K, V, T> implements RedisCommand<K, V, T>, CompleteableCommand<T> {

    private RedisCommand<K, V, T> command;
    private List<Consumer<? super T>> onComplete = Lists.newArrayList();

    public CommandWrapper(RedisCommand<K, V, T> command) {
        this.command = command;
    }

    @Override
    public CommandOutput<K, V, T> getOutput() {
        return command.getOutput();
    }

    @Override
    public void complete() {
        command.complete();
        for (Consumer<? super T> consumer : onComplete) {
            if (getOutput() != null) {
                consumer.accept(getOutput().get());
            } else {
                consumer.accept(null);
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
        onComplete.add(action);
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
}
