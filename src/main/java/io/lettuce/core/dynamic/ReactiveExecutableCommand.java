package io.lettuce.core.dynamic;

import io.lettuce.core.AbstractRedisReactiveCommands;

/**
 * An {@link ExecutableCommand} that is executed using reactive infrastructure.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class ReactiveExecutableCommand implements ExecutableCommand {

    private final CommandMethod commandMethod;

    private final ReactiveCommandSegmentCommandFactory commandFactory;

    private final AbstractRedisReactiveCommands<Object, Object> redisReactiveCommands;

    ReactiveExecutableCommand(CommandMethod commandMethod, ReactiveCommandSegmentCommandFactory commandFactory,
            AbstractRedisReactiveCommands<Object, Object> redisReactiveCommands) {

        this.commandMethod = commandMethod;
        this.commandFactory = commandFactory;
        this.redisReactiveCommands = redisReactiveCommands;
    }

    @Override
    public Object execute(Object[] parameters) {
        return dispatch(parameters);
    }

    protected Object dispatch(Object[] arguments) {

        if (ReactiveTypes.isSingleValueType(commandMethod.getReturnType().getRawClass())) {
            return redisReactiveCommands.createMono(() -> commandFactory.createCommand(arguments));
        }

        if (commandFactory.isStreamingExecution()) {
            return redisReactiveCommands.createDissolvingFlux(() -> commandFactory.createCommand(arguments));
        }

        return redisReactiveCommands.createFlux(() -> commandFactory.createCommand(arguments));
    }

    @Override
    public CommandMethod getCommandMethod() {
        return commandMethod;
    }

}
