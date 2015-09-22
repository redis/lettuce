package com.lambdaworks.redis.cluster;

import java.util.Collection;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.channel.ChannelHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Command handler for node connections within the Redis Cluster context. This handler can requeue commands if it is
 * disconnected and closed but has commands in the queue. If the handler was connected it would retry commands using the
 * {@literal MOVED} or {@literal ASK} redirection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@ChannelHandler.Sharable
class ClusterNodeCommandHandler<K, V> extends CommandHandler<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterNodeCommandHandler.class);
    private static final Set<LifecycleState> CHANNEL_OPEN_STATES = ImmutableSet.of(LifecycleState.ACTIVATING,
            LifecycleState.ACTIVE, LifecycleState.CONNECTED);

    private final RedisChannelWriter<K, V> clusterChannelWriter;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     * 
     * @param clientOptions client options for this connection
     * @param clientResources client resources for this connection
     * @param queue The command queue
     * @param clusterChannelWriter top-most channel writer.
     */
    public ClusterNodeCommandHandler(ClientOptions clientOptions, ClientResources clientResources,
            Queue<RedisCommand<K, V, ?>> queue, RedisChannelWriter<K, V> clusterChannelWriter) {
        super(clientOptions, clientResources, queue);
        this.clusterChannelWriter = clusterChannelWriter;
    }

    /**
     * Prepare the closing of the channel.
     */
    public void prepareClose() {
        if (channel != null) {
            ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);
            if (connectionWatchdog != null) {
                connectionWatchdog.setReconnectSuspended(true);
            }
        }
    }

    /**
     * Move queued and buffered commands from the inactive connection to the master command writer. This is done only if the
     * current connection is disconnected and auto-reconnect is enabled (command-retries). If the connection would be open, we
     * could get into a race that the commands we're moving are right now in processing. Alive connections can handle redirects
     * and retries on their own.
     */
    @Override
    public void close() {

        logger.debug("{} close()", logPrefix());

        if (clusterChannelWriter != null) {
            if (isAutoReconnect() && !CHANNEL_OPEN_STATES.contains(getState())) {
                Collection<RedisCommand<K, V, ?>> commands = shiftCommands(queue);
                for (RedisCommand<K, V, ?> queuedCommand : commands) {
                    try {
                        clusterChannelWriter.write(queuedCommand);
                    } catch (RedisException e) {
                        queuedCommand.setException(e);
                        queuedCommand.complete();
                    }
                }
            }

            Collection<RedisCommand<K, V, ?>> commands = shiftCommands(commandBuffer);
            for (RedisCommand<K, V, ?> queuedCommand : commands) {
                try {
                    clusterChannelWriter.write(queuedCommand);
                } catch (RedisException e) {
                    queuedCommand.setException(e);
                    queuedCommand.complete();
                }
            }
        }

        super.close();
    }

    /**
     * Retrieve commands within a lock to prevent concurrent modification
     */
    private Collection<RedisCommand<K, V, ?>> shiftCommands(Collection<RedisCommand<K, V, ?>> source) {
        try {
            writeLock.lock();
            try {
                return Lists.newArrayList(source);
            } finally {
                source.clear();
            }
        } finally {
            writeLock.unlock();
        }
    }

    public boolean isAutoReconnect() {
        return clientOptions.isAutoReconnect();
    }
}
