package com.lambdaworks;

import java.util.Queue;

import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;

import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
public class Connections {

    public static Channel getChannel(StatefulConnection<?, ?> connection) {
        RedisChannelHandler<?, ?> channelHandler = (RedisChannelHandler<?, ?>) connection;

        Channel channel = (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
        return channel;
    }

    public static ConnectionWatchdog getConnectionWatchdog(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);
        return channel.pipeline().get(ConnectionWatchdog.class);
    }

    public static <K, V> StatefulRedisConnectionImpl<K, V> getStatefulConnection(RedisAsyncCommands<K, V> connection) {
        return (StatefulRedisConnectionImpl<K, V>) connection.getStatefulConnection();
    }

    public static <K, V> RedisChannelWriter<K, V> getChannelWriter(StatefulConnection<K, V> connection) {
        return ((RedisChannelHandler<K, V>) connection).getChannelWriter();
    }


    public static Queue<Object> getQueue(StatefulConnection<?, ?> connection) {
        return (Queue<Object>) ReflectionTestUtils.getField(Connections.getChannelWriter(connection), "queue");
    }

    public static Queue<Object> getCommandBuffer(StatefulConnection<?, ?> connection) {
        return (Queue<Object>) ReflectionTestUtils.getField(Connections.getChannelWriter(connection), "commandBuffer");
    }

    public static String getConnectionState(StatefulConnection<?, ?> connection) {
        return ReflectionTestUtils.getField(Connections.getChannelWriter(connection), "lifecycleState").toString();
    }
}
