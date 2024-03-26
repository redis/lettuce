package io.lettuce.core.dynamic.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.support.ResolvableType;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Selector {@link CommandOutputFactory} resolution.
 * <p>
 * A {@link OutputSelector} is based on the result {@link ResolvableType} and {@link io.lettuce.core.codec.RedisCodec}. The
 * codec supplies types for generics resolution of {@link io.lettuce.core.output.CommandOutput}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public class OutputSelector {

    private final ResolvableType outputType;

    private final RedisCodec<?, ?> redisCodec;

    /**
     * Creates a new {@link OutputSelector} given {@link ResolvableType} and {@link RedisCodec}.
     *
     * @param outputType must not be {@code null}.
     * @param redisCodec must not be {@code null}.
     */
    public OutputSelector(ResolvableType outputType, RedisCodec<?, ?> redisCodec) {

        LettuceAssert.notNull(outputType, "Output type must not be null!");
        LettuceAssert.notNull(redisCodec, "RedisCodec must not be null!");

        this.outputType = outputType;
        this.redisCodec = redisCodec;
    }

    /**
     * @return the output type.
     */
    public ResolvableType getOutputType() {
        return outputType;
    }

    /**
     *
     * @return the associated codec.
     */
    public RedisCodec<?, ?> getRedisCodec() {
        return redisCodec;
    }

}
