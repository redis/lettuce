package com.lambdaworks.redis.dynamic;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactory;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.output.OutputSelector;
import com.lambdaworks.redis.dynamic.parameter.ExecutionSpecificParameters;
import com.lambdaworks.redis.dynamic.parameter.MethodParametersAccessor;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * {@link CommandFactory} based on {@link CommandSegments}.
 *
 * @author Mark Paluch
 */
class CommandSegmentCommandFactory<K, V> implements CommandFactory {

    private final CommandMethod commandMethod;
    private final CommandSegments segments;
    private final CommandOutputFactoryResolver outputResolver;
    private final RedisCodec<K, V> redisCodec;
    private final ParameterBinder parameterBinder = new ParameterBinder();
    private final CommandOutputFactory outputFactory;

    public CommandSegmentCommandFactory(CommandSegments commandSegments, CommandMethod commandMethod,
            RedisCodec<K, V> redisCodec, CommandOutputFactoryResolver outputResolver) {

        this.segments = commandSegments;
        this.commandMethod = commandMethod;
        this.redisCodec = redisCodec;
        this.outputResolver = outputResolver;

        OutputSelector outputSelector = new OutputSelector(commandMethod.getActualReturnType());
        CommandOutputFactory factory = resolveCommandOutputFactory(outputSelector);

        if (factory == null) {
            throw new IllegalArgumentException(String.format("Cannot resolve CommandOutput for result type %s on method %s",
                    commandMethod.getActualReturnType(), commandMethod.getMethod()));
        }

        if (commandMethod.getParameters() instanceof ExecutionSpecificParameters) {

            ExecutionSpecificParameters executionAwareParameters = (ExecutionSpecificParameters) commandMethod.getParameters();

            if (commandMethod.isFutureExecution() && executionAwareParameters.hasTimeoutIndex()) {
                throw new CommandCreationException(commandMethod,
                        "Asynchronous command methods do not support Timeout parameters");
            }
        }

        this.outputFactory = factory;
    }

    protected CommandOutputFactoryResolver getOutputResolver() {
        return outputResolver;
    }

    protected CommandOutputFactory resolveCommandOutputFactory(OutputSelector outputSelector) {
        return outputResolver.resolveCommandOutput(outputSelector);
    }

    @Override
    public RedisCommand<?, ?, ?> createCommand(Object[] parameters) {

        MethodParametersAccessor parametersAccessor = new CodecAwareMethodParametersAccessor(
                new DefaultMethodParametersAccessor(commandMethod.getParameters(), parameters), redisCodec);

        CommandArgs<K, V> args = new CommandArgs<>(redisCodec);

        CommandOutput<K, V, ?> output = outputFactory.create(redisCodec);
        com.lambdaworks.redis.protocol.Command<?, ?, ?> command = new com.lambdaworks.redis.protocol.Command<>(
                this.segments.getCommandType(), output, args);

        parameterBinder.bind(args, segments, parametersAccessor);

        return command;
    }
}
