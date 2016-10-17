package com.lambdaworks.redis.dynamic;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactory;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.output.OutputSelector;
import com.lambdaworks.redis.dynamic.parameter.ExecutionSpecificParameters;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;

/**
 * {@link CommandSegmentCommandFactory} for Reactive Command execution.
 * 
 * @author Mark Paluch
 */
class ReactiveCommandSegmentCommandFactory<K, V> extends CommandSegmentCommandFactory<K, V> {

    private boolean streamingExecution;

    public ReactiveCommandSegmentCommandFactory(CommandSegments commandSegments, CommandMethod commandMethod,
            RedisCodec<K, V> redisCodec, CommandOutputFactoryResolver outputResolver) {

        super(commandSegments, commandMethod, redisCodec, outputResolver);

        if (commandMethod.getParameters() instanceof ExecutionSpecificParameters) {

            ExecutionSpecificParameters executionAwareParameters = (ExecutionSpecificParameters) commandMethod.getParameters();

            if (executionAwareParameters.hasTimeoutIndex()) {
                throw new CommandCreationException(commandMethod, "Reactive command methods do not support Timeout parameters");
            }
        }
    }

    @Override
    protected CommandOutputFactory resolveCommandOutputFactory(OutputSelector outputSelector) {

        CommandOutputFactory factory = getOutputResolver().resolveStreamingCommandOutput(outputSelector);

        if (factory != null) {
            streamingExecution = true;
            return factory;
        }

        return super.resolveCommandOutputFactory(outputSelector);
    }

    /**
     * @return {@literal true} if the resolved {@link com.lambdaworks.redis.output.CommandOutput} should use streaming.
     */
    public boolean isStreamingExecution() {
        return streamingExecution;
    }
}
