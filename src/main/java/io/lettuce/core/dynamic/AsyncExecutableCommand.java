/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.core.dynamic.parameter.ExecutionSpecificParameters;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.RedisCommand;

/**
 * An {@link ExecutableCommand} that is executed asynchronously or synchronously.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class AsyncExecutableCommand implements ExecutableCommand {

    private final CommandMethod commandMethod;
    private final CommandFactory commandFactory;
    private final StatefulConnection<Object, Object> connection;

    AsyncExecutableCommand(CommandMethod commandMethod, CommandFactory commandFactory,
            StatefulConnection<Object, Object> connection) {

        this.commandMethod = commandMethod;
        this.commandFactory = commandFactory;
        this.connection = connection;
    }

    @Override
    public Object execute(Object[] parameters) throws ExecutionException, InterruptedException {

        RedisCommand<Object, Object, Object> command = commandFactory.createCommand(parameters);

        return dispatchCommand(parameters, command);
    }

    protected Object dispatchCommand(Object[] arguments, RedisCommand<Object, Object, Object> command)
            throws InterruptedException, java.util.concurrent.ExecutionException {

        AsyncCommand<Object, Object, Object> asyncCommand = new AsyncCommand<>(command);

        if (commandMethod.isFutureExecution()) {
            return connection.dispatch(asyncCommand);
        }

        connection.dispatch(asyncCommand);

        Duration timeout = connection.getTimeout();

        if (commandMethod.getParameters() instanceof ExecutionSpecificParameters) {
            ExecutionSpecificParameters executionSpecificParameters = (ExecutionSpecificParameters) commandMethod
                    .getParameters();

            if (executionSpecificParameters.hasTimeoutIndex()) {
                Timeout timeoutArg = (Timeout) arguments[executionSpecificParameters.getTimeoutIndex()];
                if (timeoutArg != null) {
                    timeout = timeoutArg.getTimeout();
                }
            }
        }

        LettuceFutures.awaitAll(timeout, asyncCommand);

        return asyncCommand.get();
    }

    @Override
    public CommandMethod getCommandMethod() {
        return commandMethod;
    }
}
