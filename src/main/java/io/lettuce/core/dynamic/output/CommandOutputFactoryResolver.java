package io.lettuce.core.dynamic.output;

/**
 * Strategy interface to resolve a {@link CommandOutputFactory} based on a {@link OutputSelector}. Resolution of
 * {@link CommandOutputFactory} is based on {@link io.lettuce.core.dynamic.CommandMethod} result types and can be influenced
 * whether the result type is a key or value result type. Additional type variables (based on the used
 * {@link io.lettuce.core.codec.RedisCodec} are hints to improve output resolution.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see OutputSelector
 */
public interface CommandOutputFactoryResolver {

    /**
     * Resolve a regular {@link CommandOutputFactory} that produces the {@link io.lettuce.core.output.CommandOutput} result
     * component type.
     *
     * @param outputSelector must not be {@code null}.
     * @return the {@link CommandOutputFactory} if resolved, {@code null} otherwise.
     */
    CommandOutputFactory resolveCommandOutput(OutputSelector outputSelector);

    /**
     * Resolve a streaming {@link CommandOutputFactory} that produces the {@link io.lettuce.core.output.StreamingOutput} result
     * component type.
     *
     * @param outputSelector must not be {@code null}.
     * @return the {@link CommandOutputFactory} that implements {@link io.lettuce.core.output.StreamingOutput} if resolved,
     *         {@code null} otherwise.
     */
    CommandOutputFactory resolveStreamingCommandOutput(OutputSelector outputSelector);

}
