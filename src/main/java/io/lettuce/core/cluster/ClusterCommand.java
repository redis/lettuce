package io.lettuce.core.cluster;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.protocol.*;
import io.netty.buffer.ByteBuf;

/**
 * @author Mark Paluch
 * @since 3.0
 */
class ClusterCommand<K, V, T> extends CommandWrapper<K, V, T> implements RedisCommand<K, V, T> {

    private int redirections;

    private final int maxRedirections;

    private final RedisChannelWriter retry;

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

            if (retryCommand) {
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

        if (getError() != null && getError().startsWith(CommandKeyword.MOVED.name())) {
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
        completed = true;
        return super.completeExceptionally(ex);
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

    @Override
    public void markEncodingError() {
        command.markEncodingError();
    }

    @Override
    public boolean hasEncodingError() {
        return command.hasEncodingError();
    }

}
