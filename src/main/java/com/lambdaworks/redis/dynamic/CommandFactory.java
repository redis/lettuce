package com.lambdaworks.redis.dynamic;

import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Strategy interface to create {@link RedisCommand}s.
 *
 * <p>
 * Implementing classes are required to construct {@link RedisCommand}s given an array of parameters for command execution.
 *
 * @author Mark Paluch
 * @since 5.0
 */
interface CommandFactory {

    /**
     * Create a new {@link RedisCommand} given {@code parameters}.
     * 
     * @param parameters must not be {@literal null}.
     * @return the {@link RedisCommand}.
     */
    RedisCommand<?, ?, ?> createCommand(Object[] parameters);
}
