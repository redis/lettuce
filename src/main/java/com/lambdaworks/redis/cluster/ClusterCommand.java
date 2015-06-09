package com.lambdaworks.redis.cluster;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.CommandOutput;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import com.lambdaworks.redis.protocol.RedisCommand;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class ClusterCommand<K, V, T> extends AsyncCommand<K, V, T> implements RedisCommand<K, V, T> {

    private RedisChannelWriter<K, V> retry;
    private int executions;
    private int executionLimit;
    private List<Throwable> exceptions = new ArrayList<Throwable>();

    ClusterCommand(RedisCommand<K, V, T> command, RedisChannelWriter<K, V> retry, int executionLimit) {
        super(command);
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

        super.complete();
    }

    public boolean isMoved() {
        if (getError() != null && getError().startsWith(CommandKeyword.MOVED.name())) {
            return true;
        }
        return false;
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return command.getArgs();
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
    public boolean completeExceptionally(Throwable ex) {
        return command.completeExceptionally(ex);
    }

    @Override
    public ProtocolKeyword getType() {
        return command.getType();
    }
}
