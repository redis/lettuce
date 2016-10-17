package com.lambdaworks.redis.dynamic.output;

/**
 * Strategy interface to resolve a {@link CommandOutputFactory} based on a {@link OutputSelector}. Resolution of
 * {@link CommandOutputFactory} is based on {@link com.lambdaworks.redis.dynamic.CommandMethod} result types and can be
 * influenced whether the result type is a key or value result type. Additional type variables (based on the used
 * {@link com.lambdaworks.redis.codec.RedisCodec} are hints to improve output resolution.
 * 
 * @author Mark Paluch
 * @since 5.0
 * @see OutputSelector
 */
public interface CommandOutputFactoryResolver {

    /**
     * Resolve a regular {@link CommandOutputFactory} that produces the {@link com.lambdaworks.redis.output.CommandOutput}
     * result component type.
     * 
     * @param outputSelector must not be {@literal null}.
     * @return the {@link CommandOutputFactory} if resolved, {@literal null} otherwise.
     */
    CommandOutputFactory resolveCommandOutput(OutputSelector outputSelector);

    /**
     * Resolve a streaming {@link CommandOutputFactory} that produces the {@link com.lambdaworks.redis.output.StreamingOutput}
     * result component type.
     *
     * @param outputSelector must not be {@literal null}.
     * @return the {@link CommandOutputFactory} that implements {@link com.lambdaworks.redis.output.StreamingOutput} if
     *         resolved, {@literal null} otherwise.
     */
    CommandOutputFactory resolveStreamingCommandOutput(OutputSelector outputSelector);
}
