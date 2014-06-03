package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.internal.RedisChannelWriter;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Channel writer for cluster operation. This writer looks up the right partition by hash/slot for the operation.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:46
 */
public class ClusterDistributionChannelWriter<K, V> implements RedisChannelWriter<K, V> {

    private RedisChannelWriter<K, V> defaultWriter;
    private ClusterConnectionProvider clusterConnectionProvider;
    private boolean closed = false;
    private int executionLimit = 5;

    public ClusterDistributionChannelWriter(RedisChannelWriter<K, V> defaultWriter,
            ClusterConnectionProvider clusterConnectionProvider) {
        this.defaultWriter = defaultWriter;
        this.clusterConnectionProvider = clusterConnectionProvider;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        RedisCommand<K, V, T> commandToSend = command;
        CommandArgs<K, V> args = command.getArgs();

        if (command instanceof Command) {
            Command<K, V, T> singleCommand = (Command<K, V, T>) command;
            if (!singleCommand.isMulti()) {
                commandToSend = new ClusterCommand<K, V, T>(singleCommand, this, executionLimit);
            }
        }

        RedisChannelWriter<K, V> channelWriter = null;

        if (commandToSend instanceof ClusterCommand) {
            ClusterCommand<K, V, T> clusterCommand = (ClusterCommand<K, V, T>) commandToSend;
            if (!clusterCommand.isDone() && clusterCommand.isMoved()) {
                HostAndPort moveTarget = getMoveTarget(clusterCommand.getError());

                RedisAsyncConnectionImpl<K, V> connection = clusterConnectionProvider.getConnection(
                        ClusterConnectionProvider.Intent.WRITE, moveTarget.getHostText(), moveTarget.getPort());
                channelWriter = connection.getChannelWriter();
            }

        }

        if (channelWriter == null && args != null && !args.getKeys().isEmpty()) {

            int hash = getHash(args.getEncodedKey(0));
            RedisAsyncConnectionImpl<K, V> connection = clusterConnectionProvider.getConnection(
                    ClusterConnectionProvider.Intent.WRITE, hash);

            channelWriter = connection.getChannelWriter();

        }

        if (channelWriter instanceof ClusterDistributionChannelWriter) {
            ClusterDistributionChannelWriter<K, V> writer = (ClusterDistributionChannelWriter<K, V>) channelWriter;
            channelWriter = writer.defaultWriter;
        }

        commandToSend.getOutput().setError((String) null);
        if (channelWriter != this && channelWriter != defaultWriter) {
            return channelWriter.write(commandToSend);
        }

        return defaultWriter.write(commandToSend);
    }

    private HostAndPort getMoveTarget(String errorMessage) {

        checkArgument(LettuceStrings.isNotEmpty(errorMessage), "errorMessage must not be empty");
        checkArgument(errorMessage.startsWith(CommandKeyword.MOVED.name()), "errorMessage must start with "
                + CommandKeyword.MOVED);

        List<String> movedMessageParts = Splitter.on(' ').splitToList(errorMessage);
        checkArgument(movedMessageParts.size() >= 3, "errorMessage must consist of 3 tokens (" + movedMessageParts + ")");

        return HostAndPort.fromString(movedMessageParts.get(2));
    }

    protected int getHash(byte[] encodedKey) {
        return SlotHash.getSlot(encodedKey);
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        if (defaultWriter != null) {
            defaultWriter.close();
            defaultWriter = null;
        }

        if (clusterConnectionProvider != null) {
            clusterConnectionProvider.close();
            clusterConnectionProvider = null;
        }

    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        defaultWriter.setRedisChannelHandler(redisChannelHandler);
    }

    public ClusterConnectionProvider getClusterConnectionProvider() {
        return clusterConnectionProvider;
    }
}
