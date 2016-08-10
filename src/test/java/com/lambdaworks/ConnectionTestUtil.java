package com.lambdaworks;

import java.util.Queue;

import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.internal.LettuceFactories;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.protocol.DefaultEndpoint;

import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
public class ConnectionTestUtil {

    /**
     * Extract the {@link Channel} from a stateful connection.
     * 
     * @param connection
     * @return
     */
    public static Channel getChannel(StatefulConnection<?, ?> connection) {
        RedisChannelHandler<?, ?> channelHandler = (RedisChannelHandler<?, ?>) connection;

        return (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
    }

    /**
     * Extract the {@link ConnectionWatchdog} from a stateful connection.
     * 
     * @param connection
     * @return
     */
    public static ConnectionWatchdog getConnectionWatchdog(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);
        return channel.pipeline().get(ConnectionWatchdog.class);
    }

    public static <K, V> StatefulRedisConnectionImpl<K, V> getStatefulConnection(RedisAsyncCommands<K, V> connection) {
        return (StatefulRedisConnectionImpl<K, V>) connection.getStatefulConnection();
    }

    public static <K, V> RedisChannelWriter getChannelWriter(StatefulConnection<K, V> connection) {
        return ((RedisChannelHandler<K, V>) connection).getChannelWriter();
    }

    public static Queue<Object> getQueue(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);

        if (channel != null) {
            CommandHandler commandHandler = channel.pipeline().get(CommandHandler.class);
            return (Queue) commandHandler.getQueue();
        }

        return LettuceFactories.newConcurrentQueue();

    }

    public static Queue<Object> getCommandBuffer(StatefulConnection<?, ?> connection) {

        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);
        if (channelWriter instanceof DefaultEndpoint) {
            return (Queue) ((DefaultEndpoint) channelWriter).getQueue();
        }

        return LettuceFactories.newConcurrentQueue();
    }

    public static String getConnectionState(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);

        if (channel != null) {
            CommandHandler commandHandler = channel.pipeline().get(CommandHandler.class);
            return ReflectionTestUtils.getField(commandHandler, "lifecycleState").toString();
        }

        return "";
    }
}
