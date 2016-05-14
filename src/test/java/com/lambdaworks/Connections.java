package com.lambdaworks;

import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.RedisChannelHandler;
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
}
