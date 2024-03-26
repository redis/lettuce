package io.lettuce.core.dynamic.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.CommandOutput;

/**
 * Strategy interface to create {@link CommandOutput} given {@link RedisCodec}.
 *
 * <p>
 * Implementing classes usually produce the same {@link CommandOutput} type.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
public interface CommandOutputFactory {

    /**
     * Create and initialize a new {@link CommandOutput} given {@link RedisCodec}.
     *
     * @param codec must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the new {@link CommandOutput}.
     */
    <K, V> CommandOutput<K, V, ?> create(RedisCodec<K, V> codec);

}
