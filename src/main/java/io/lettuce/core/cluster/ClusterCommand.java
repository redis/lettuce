package io.lettuce.core.cluster;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
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

    // Keyless routing metadata (no behavior change yet)
    private Iterable<RedisClusterNode> keylessCandidates;

    private boolean keylessBroadcast;

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

    public boolean isKeyless() {
        CommandArgs<K, V> a = getArgs();
        return a == null || a.getFirstEncodedKey() == null;
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

    // --- Keyless routing metadata accessors ---
    public ClusterCommand<K, V, T> withKeylessCandidates(Iterable<RedisClusterNode> candidates) {
        this.keylessCandidates = candidates;
        return this;
    }

    public ClusterCommand<K, V, T> asKeylessBroadcast(Iterable<RedisClusterNode> nodes) {
        this.keylessBroadcast = true;
        this.keylessCandidates = nodes;
        return this;
    }

    public Iterable<RedisClusterNode> getKeylessCandidates() {
        return keylessCandidates;
    }

    public boolean isKeylessBroadcast() {
        return keylessBroadcast;
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
