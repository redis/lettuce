package io.lettuce.core.dynamic.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link RedisCodec}-aware implementation of {@link CommandOutputFactoryResolver}. This implementation inspects
 * {@link RedisCodec} regarding its type and enhances {@link OutputSelector} for {@link CommandOutputFactory} resolution.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public class CodecAwareOutputFactoryResolver implements CommandOutputFactoryResolver {

    private final CommandOutputFactoryResolver delegate;

    private final RedisCodec<?, ?> redisCodec;

    /**
     * Create a new {@link CodecAwareOutputFactoryResolver} given {@link CommandOutputFactoryResolver} and {@link RedisCodec}.
     *
     * @param delegate must not be {@code null}.
     * @param redisCodec must not be {@code null}.
     */
    public CodecAwareOutputFactoryResolver(CommandOutputFactoryResolver delegate, RedisCodec<?, ?> redisCodec) {

        LettuceAssert.notNull(delegate, "CommandOutputFactoryResolver delegate must not be null");
        LettuceAssert.notNull(redisCodec, "RedisCodec must not be null");

        this.delegate = delegate;
        this.redisCodec = redisCodec;
    }

    @Override
    public CommandOutputFactory resolveCommandOutput(OutputSelector outputSelector) {
        return delegate.resolveCommandOutput(new OutputSelector(outputSelector.getOutputType(), redisCodec));
    }

    @Override
    public CommandOutputFactory resolveStreamingCommandOutput(OutputSelector outputSelector) {
        return delegate.resolveStreamingCommandOutput(new OutputSelector(outputSelector.getOutputType(), redisCodec));
    }

}
