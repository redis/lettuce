package io.lettuce.core.dynamic;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.CodecAwareMethodParametersAccessor.TypeContext;
import io.lettuce.core.dynamic.output.CommandOutputFactory;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;
import io.lettuce.core.dynamic.output.OutputSelector;
import io.lettuce.core.dynamic.parameter.ExecutionSpecificParameters;
import io.lettuce.core.dynamic.parameter.MethodParametersAccessor;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.RedisCommand;

/**
 * {@link CommandFactory} based on {@link CommandSegments}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class CommandSegmentCommandFactory implements CommandFactory {

    private final CommandMethod commandMethod;

    private final CommandSegments segments;

    private final CommandOutputFactoryResolver outputResolver;

    private final RedisCodec<Object, Object> redisCodec;

    private final ParameterBinder parameterBinder = new ParameterBinder();

    private final CommandOutputFactory outputFactory;

    private final TypeContext typeContext;

    public CommandSegmentCommandFactory(CommandSegments commandSegments, CommandMethod commandMethod,
            RedisCodec<?, ?> redisCodec, CommandOutputFactoryResolver outputResolver) {

        this.segments = commandSegments;
        this.commandMethod = commandMethod;
        this.redisCodec = (RedisCodec) redisCodec;
        this.outputResolver = outputResolver;
        this.typeContext = new TypeContext(redisCodec);

        OutputSelector outputSelector = new OutputSelector(commandMethod.getActualReturnType(), redisCodec);
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
    public RedisCommand<Object, Object, Object> createCommand(Object[] parameters) {

        MethodParametersAccessor parametersAccessor = new CodecAwareMethodParametersAccessor(
                new DefaultMethodParametersAccessor(commandMethod.getParameters(), parameters), typeContext);

        CommandArgs<Object, Object> args = new CommandArgs<>(redisCodec);

        CommandOutput<Object, Object, ?> output = outputFactory.create(redisCodec);
        Command<Object, Object, ?> command = new Command<>(this.segments.getCommandType(), output, args);

        parameterBinder.bind(args, redisCodec, segments, parametersAccessor);

        return (Command) command;
    }

}
