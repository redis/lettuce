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
package com.lambdaworks.redis;

import java.io.Closeable;

import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Writer for a channel. Writers push commands on to the communication channel and maintain a state for the commands.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisChannelWriter<K, V> extends Closeable {

    /**
     * Write a command on the channel. The command may be changed/wrapped during write and the written instance is returned
     * after the call.
     * 
     * @param command the redis command
     * @param <T> result type
     * @param <C> command type
     * @return the written redis command
     */
    <T, C extends RedisCommand<K, V, T>> C write(C command);

    @Override
    void close();

    /**
     * Reset the writer state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection.
     */
    void reset();

    /**
     * Set the corresponding connection instance in order to notify it about channel active/inactive state.
     * 
     * @param redisChannelHandler the channel handler (external connection object)
     */
    void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler);

    /**
     * Disable or enable auto-flush behavior. Default is {@literal true}. If autoFlushCommands is disabled, multiple commands
     * can be issued without writing them actually to the transport. Commands are buffered until a {@link #flushCommands()} is
     * issued. After calling {@link #flushCommands()} commands are sent to the transport and executed by Redis.
     * 
     * @param autoFlush state of autoFlush.
     */
    void setAutoFlushCommands(boolean autoFlush);

    /**
     * Flush pending commands. This commands forces a flush on the channel and can be used to buffer ("pipeline") commands to
     * achieve batching. No-op if channel is not connected.
     */
    void flushCommands();
}
