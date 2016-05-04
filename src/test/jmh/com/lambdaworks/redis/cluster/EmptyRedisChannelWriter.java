package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class EmptyRedisChannelWriter implements RedisChannelWriter {
    @Override
    public RedisCommand write(RedisCommand command) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler redisChannelHandler) {

    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {

    }

    @Override
    public void flushCommands() {

    }
}
