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
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.api.async.BaseRedisAsyncCommands;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

/**
 * 
 * Basic asynchronous executed commands.
 * 
 * @author Mark Paluch
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 3.0
 * @deprecated Use {@link BaseRedisAsyncCommands}
 */
@Deprecated
public interface BaseRedisAsyncConnection<K, V> extends Closeable, BaseRedisAsyncCommands<K, V> {

    /**
     * Post a message to a channel.
     * 
     * @param channel the channel type: key
     * @param message the message type: value
     * @return RedisFuture&lt;Long&gt; integer-reply the number of clients that received the message.
     */
    RedisFuture<Long> publish(K channel, V message);

    /**
     * Lists the currently *active channels*.
     * 
     * @return RedisFuture&lt;List&lt;K&gt;&gt; array-reply a list of active channels, optionally matching the specified
     *         pattern.
     */
    RedisFuture<List<K>> pubsubChannels();

    /**
     * Lists the currently *active channels*.
     * 
     * @param channel the key
     * @return RedisFuture&lt;List&lt;K&gt;&gt; array-reply a list of active channels, optionally matching the specified
     *         pattern.
     */
    RedisFuture<List<K>> pubsubChannels(K channel);

    /**
     * Returns the number of subscribers (not counting clients subscribed to patterns) for the specified channels.
     *
     * @param channels channel keys
     * @return array-reply a list of channels and number of subscribers for every channel.
     */
    RedisFuture<Map<K, Long>> pubsubNumsub(K... channels);

    /**
     * Returns the number of subscriptions to patterns.
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply the number of patterns all the clients are subscribed to.
     */
    RedisFuture<Long> pubsubNumpat();

    /**
     * Echo the given string.
     * 
     * @param msg the message type: value
     * @return RedisFuture&lt;V&gt; bulk-string-reply
     */
    RedisFuture<V> echo(V msg);

    /**
     * Return the role of the instance in the context of replication.
     *
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply where the first element is one of master, slave, sentinel and
     *         the additional elements are role-specific.
     */
    RedisFuture<List<Object>> role();

    /**
     * Ping the server.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> ping();

    /**
     * Close the connection.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always OK.
     */
    RedisFuture<String> quit();

    /**
     * Create a SHA1 digest from a Lua script.
     * 
     * @param script script content
     * @return the SHA1 value
     */
    String digest(V script);

    /**
     * Discard all commands issued after MULTI.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always {@code OK}.
     */
    RedisFuture<String> discard();

    /**
     * Execute all commands issued after MULTI.
     * 
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply each element being the reply to each of the commands in the
     *         atomic transaction.
     * 
     *         When using {@code WATCH}, {@code EXEC} can return a
     */
    RedisFuture<List<Object>> exec();

    /**
     * Mark the start of a transaction block.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always {@code OK}.
     */
    RedisFuture<String> multi();

    /**
     * Watch the given keys to determine execution of the MULTI/EXEC block.
     * 
     * @param keys the key
     * @return RedisFuture&lt;String&gt; simple-string-reply always {@code OK}.
     */
    RedisFuture<String> watch(K... keys);

    /**
     * Forget about all watched keys.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always {@code OK}.
     */
    RedisFuture<String> unwatch();

    /**
     * Wait for replication.
     * 
     * @param replicas minimum number of replicas
     * @param timeout timeout in milliseconds
     * @return number of replicas
     */
    RedisFuture<Long> waitForReplication(int replicas, long timeout);

    /**
     * Dispatch a command to the Redis Server. Please note the command output type must fit to the command response.
     *
     * @param type the command, must not be {@literal null}.
     * @param output the command output, must not be {@literal null}.
     * @param <T> response type
     * @return the command response
     */
    <T> RedisFuture<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, T> output);

    /**
     * Dispatch a command to the Redis Server. Please note the command output type must fit to the command response.
     *
     * @param type the command, must not be {@literal null}.
     * @param output the command output, must not be {@literal null}.
     * @param args the command arguments, must not be {@literal null}.
     * @param <T> response type
     * @return the command response
     */
    <T> RedisFuture<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args);

    /**
     * Close the connection. The connection will become not usable anymore as soon as this method was called.
     */
    @Override
    void close();

    /**
     * 
     * @return true if the connection is open (connected and not closed).
     */
    boolean isOpen();

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
