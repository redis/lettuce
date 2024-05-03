package io.lettuce.core.dynamic;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Strategy interface to create {@link RedisCommand}s.
 * <p>
 * Implementing classes are required to construct {@link RedisCommand}s given an array of parameters for command execution.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
interface CommandFactory {

    /**
     * Create a new {@link RedisCommand} given {@code parameters}.
     *
     * @param parameters must not be {@code null}.
     * @return the {@link RedisCommand}.
     */
    RedisCommand<Object, Object, Object> createCommand(Object[] parameters);

}
