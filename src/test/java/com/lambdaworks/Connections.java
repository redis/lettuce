/*
 * Copyright 2011-2018 the original author or authors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Queue;

import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;

import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
public class Connections {

    public static <K, V> RedisChannelHandler<K, V> getRedisChannelHandler(RedisConnection<K, V> sync) {

        InvocationHandler invocationHandler = Proxy.getInvocationHandler(sync);
        return (RedisChannelHandler<K, V>) ReflectionTestUtils.getField(invocationHandler, "connection");
    }

    public static <T> T getHandler(Class<T> handlerType, RedisChannelHandler<?, ?> channelHandler) {
        Channel channel = getChannel(channelHandler);
        return (T) channel.pipeline().get((Class) handlerType);
    }

    public static Channel getChannel(RedisChannelHandler<?, ?> channelHandler) {
        return (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
    }

    public static Queue<?> getDisconnectedBuffer(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<?>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "disconnectedBuffer");
    }

    public static Queue<?> getCommandBuffer(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<?>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "commandBuffer");
    }

    public static Queue<Object> getStack(StatefulRedisConnection<?, ?> connection) {
        return getStack((RedisChannelHandler) connection);
    }

    public static Queue<Object> getStack(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<Object>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "stack");
    }

    public static String getConnectionState(RedisChannelHandler<?, ?> channelHandler) {
        return ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "lifecycleState").toString();
    }

    public static Channel getChannel(StatefulConnection<?, ?> connection) {

        RedisChannelHandler<?, ?> channelHandler = (RedisChannelHandler<?, ?>) connection;
        return (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
    }

    public static ConnectionWatchdog getConnectionWatchdog(StatefulConnection<?, ?> connection) {

        Channel channel = getChannel(connection);
        return channel.pipeline().get(ConnectionWatchdog.class);
    }

    public static <K, V> StatefulRedisConnectionImpl<K, V> getStatefulConnection(RedisAsyncCommands<K, V> connection) {
        return (StatefulRedisConnectionImpl<K, V>) connection.getStatefulConnection();
    }
}
