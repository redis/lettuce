/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce;

import java.util.Queue;

import org.springframework.test.util.ReflectionTestUtils;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.internal.LettuceFactories;
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
