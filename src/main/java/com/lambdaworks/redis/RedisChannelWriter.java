package com.lambdaworks.redis;

import java.io.Closeable;

import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Writer for a channel. Writers push commands on to the communication channel and maintain a state for the commands.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public interface RedisChannelWriter<K, V> extends Closeable {

    /**
     * Write a command on the channel.
     * 
     * @param command
     * @return RedisCommand<K, V, T>
     */
    <T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command);

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
     * @param redisChannelHandler
     */
    void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler);
}
