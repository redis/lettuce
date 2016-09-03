package com.lambdaworks;

import java.util.Queue;

import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.internal.LettuceFactories;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.protocol.DefaultEndpoint;

import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
public class ConnectionTestUtil {

    /**
     * Extract the {@link Channel} from a {@link StatefulConnection}.
     * 
     * @param connection the connection
     * @return the {@link Channel}
     */
    public static Channel getChannel(StatefulConnection<?, ?> connection) {

        RedisChannelHandler<?, ?> channelHandler = (RedisChannelHandler<?, ?>) connection;
        return (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
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
     * Extract the queue from a from a {@link StatefulConnection}.
     *
     * @param connection the connection
     * @return the queue
     */
    public static Queue<Object> getQueue(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);

        if (channel != null) {
            CommandHandler commandHandler = channel.pipeline().get(CommandHandler.class);
            return (Queue) commandHandler.getQueue();
        }

        return LettuceFactories.newConcurrentQueue();
    }

    /**
     * Extract the command buffer from a from a {@link StatefulConnection}.
     * 
     * @param connection the connection
     * @return the queue
     */
    public static Queue<Object> getCommandBuffer(StatefulConnection<?, ?> connection) {

        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);
        if (channelWriter instanceof DefaultEndpoint) {
            return (Queue) ((DefaultEndpoint) channelWriter).getQueue();
        }

        return LettuceFactories.newConcurrentQueue();
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
