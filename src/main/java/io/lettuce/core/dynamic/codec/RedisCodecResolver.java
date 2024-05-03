package io.lettuce.core.dynamic.codec;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.CommandMethod;

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
     * @param commandMethod must not be {@code null}.
     * @return the resolved {@link RedisCodec} or {@code null} if not resolvable.
     */
    RedisCodec<?, ?> resolve(CommandMethod commandMethod);

}
