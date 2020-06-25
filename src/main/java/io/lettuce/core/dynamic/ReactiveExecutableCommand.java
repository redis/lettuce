/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
