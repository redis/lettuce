package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.protocol.ConnectionFacade;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class EmptyRedisChannelWriter implements RedisChannelWriter {
    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void setConnectionFacade(ConnectionFacade connection) {

    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {

    }

    @Override
    public void flushCommands() {

    }
}
