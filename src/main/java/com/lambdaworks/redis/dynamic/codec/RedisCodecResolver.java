package com.lambdaworks.redis.dynamic.codec;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.CommandMethod;

/**
 * Strategy interface to resolve a {@link RedisCodec} for a {@link CommandMethod}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface RedisCodecResolver {

    /**
     * Resolve a {@link RedisCodec} for the given {@link CommandMethod}.
     * 
     * @param commandMethod must not be {@literal null}.
     * @return the resolved {@link RedisCodec} or {@literal null} if not resolvable.
     */
    RedisCodec<?, ?> resolve(CommandMethod commandMethod);
}
