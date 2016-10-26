/*
 * Copyright 2011-2016 the original author or authors.
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
