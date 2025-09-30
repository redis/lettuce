package io.lettuce.test;

import java.util.Queue;

import io.lettuce.test.ReflectionTestUtils;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
public class ConnectionTestUtil {

    /**
     * Extract the {@link Channel} from a {@link StatefulConnection}. Handles MaintenanceAwareExpiryWriter delegation
     * automatically.
     *
     * @param connection the connection
     * @return the {@link Channel}
     */
    public static Channel getChannel(StatefulConnection<?, ?> connection) {

        RedisChannelHandler<?, ?> channelHandler = (RedisChannelHandler<?, ?>) connection;
        RedisChannelWriter writer = channelHandler.getChannelWriter();

        // Handle MaintenanceAwareExpiryWriter which wraps the real channel writer
        if (writer.getClass().getSimpleName().equals("MaintenanceAwareExpiryWriter")) {
            // Get the delegate field from MaintenanceAwareExpiryWriter
            RedisChannelWriter delegate = ReflectionTestUtils.getField(writer, "delegate");
            return (Channel) ReflectionTestUtils.getField(delegate, "channel");
        } else {
            // Use the standard approach for regular writers
            return (Channel) ReflectionTestUtils.getField(writer, "channel");
        }
    }

    /**
     * Extract the {@link ConnectionWatchdog} from a {@link StatefulConnection}.
     *
     * @param connection the connection
     * @return the {@link ConnectionWatchdog}
     */
    public static ConnectionWatchdog getConnectionWatchdog(StatefulConnection<?, ?> connection) {

        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);
        if (channelWriter instanceof DefaultEndpoint) {
            return (ConnectionWatchdog) ReflectionTestUtils.getField(channelWriter, "connectionWatchdog");
        }

        return null;
    }

    /**
     * Extract the {@link RedisChannelWriter} from a {@link StatefulConnection}.
     *
     * @param connection the connection
     * @return the {@link RedisChannelWriter}
     */
    public static RedisChannelWriter getChannelWriter(StatefulConnection<?, ?> connection) {
        return ((RedisChannelHandler<?, ?>) connection).getChannelWriter();
    }

    /**
     * Extract the stack from a from a {@link StatefulConnection}.
     *
     * @param connection the connection
     * @return the stack
     */
    public static Queue<Object> getStack(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);

        if (channel != null) {
            CommandHandler commandHandler = channel.pipeline().get(CommandHandler.class);
            return (Queue) commandHandler.getStack();
        }

        throw new IllegalArgumentException("Cannot obtain stack from " + connection);
    }

    /**
     * Extract the disconnected buffer from a from a {@link StatefulConnection}.
     *
     * @param connection the connection
     * @return the queue
     */
    public static Queue<Object> getDisconnectedBuffer(StatefulConnection<?, ?> connection) {

        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);
        if (channelWriter instanceof DefaultEndpoint) {
            return getDisconnectedBuffer((DefaultEndpoint) channelWriter);
        }

        throw new IllegalArgumentException("Cannot disconnected command buffer from " + connection);
    }

    /**
     * Extract the disconnected buffer from a {@link DefaultEndpoint}.
     *
     * @param endpoint the endpoint
     * @return the queue
     */
    public static Queue<Object> getDisconnectedBuffer(DefaultEndpoint endpoint) {
        return (Queue) ReflectionTestUtils.getField(endpoint, "disconnectedBuffer");
    }

    /**
     * Extract the active command queue size a {@link DefaultEndpoint}.
     *
     * @param endpoint the endpoint
     * @return the queue
     */
    public static int getQueueSize(DefaultEndpoint endpoint) {
        return (Integer) ReflectionTestUtils.getField(endpoint, "queueSize");
    }

    /**
     * Extract the command buffer from a {@link StatefulConnection}.
     *
     * @param connection the connection
     * @return the command buffer
     */
    public static Queue<Object> getCommandBuffer(StatefulConnection<?, ?> connection) {

        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);
        if (channelWriter instanceof DefaultEndpoint) {
            return (Queue) ReflectionTestUtils.getField(channelWriter, "commandBuffer");
        }

        throw new IllegalArgumentException("Cannot obtain command buffer from " + channelWriter);
    }

    /**
     * Extract the connection state from a from a {@link StatefulConnection}.
     *
     * @param connection the connection
     * @return the connection state as {@link String}
     */
    public static String getConnectionState(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);

        if (channel != null) {
            CommandHandler commandHandler = channel.pipeline().get(CommandHandler.class);
            return ReflectionTestUtils.getField(commandHandler, "lifecycleState").toString();
        }

        return "";
    }

}
