package com.lambdaworks.redis.dynamic.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.CommandOutput;

/**
 * Strategy interface to create {@link CommandOutput} given {@link RedisCodec}.
 *
 * <p>
 * Implementing classes usually produce the same {@link CommandOutput} type.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface CommandOutputFactory {

    /**
     * Create and initialize a new {@link CommandOutput} given {@link RedisCodec}.
     * 
     * @param codec must not be {@literal null}.
     * @return the new {@link CommandOutput}.
     */
    <K, V> CommandOutput<K, V, ?> create(RedisCodec<K, V> codec);
}
