package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.CommandWrapper;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import com.lambdaworks.redis.protocol.RedisCommand;
import io.netty.buffer.ByteBuf;

/**
 * @author Mark Paluch
 * @since 3.0
 */
class ClusterCommand<K, V, T> extends CommandWrapper<K, V, T> implements RedisCommand<K, V, T> {

    private RedisChannelWriter retry;
    private int redirections;
    private int maxRedirections;
    private boolean completed;

    /**
     *
     * @param command
     * @param retry
     * @param maxRedirections
     */
    ClusterCommand(RedisCommand<K, V, T> command, RedisChannelWriter retry, int maxRedirections) {
        super(command);
        this.retry = retry;
        this.maxRedirections = maxRedirections;
    }

    @Override
    public void complete() {

        if (isMoved() || isAsk()) {

            boolean retryCommand = maxRedirections > redirections;
            redirections++;

            if(retryCommand) {
                try {
                    retry.write(this);
                } catch (Exception e) {
                    completeExceptionally(e);
                }
                return;
            }
        }
        super.complete();
        completed = true;
    }

    public boolean isMoved() {
        if (command.getOutput() != null && command.getOutput().getError() != null
                && command.getOutput().getError().startsWith(CommandKeyword.MOVED.name())) {
            return true;
        }
        return false;
    }

    public boolean isAsk() {
        if (getError() != null && getError().startsWith(CommandKeyword.ASK.name())) {
            return true;
        }
        return false;
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return command.getArgs();
    }

    @Override
    public void encode(ByteBuf buf) {
        command.encode(buf);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        boolean result = command.completeExceptionally(ex);
        completed = true;
        return result;
    }

    @Override
    public ProtocolKeyword getType() {
        return command.getType();
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public boolean isDone() {
        return isCompleted();
    }

    public String getError() {
        if (command.getOutput() != null) {
            return command.getOutput().getError();
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [command=").append(command);
        sb.append(", redirections=").append(redirections);
        sb.append(", maxRedirections=").append(maxRedirections);
        sb.append(']');
        return sb.toString();
    }
}
