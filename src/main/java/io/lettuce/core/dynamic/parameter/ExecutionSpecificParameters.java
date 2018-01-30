/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.dynamic.parameter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import io.lettuce.core.dynamic.batch.CommandBatching;
import io.lettuce.core.dynamic.domain.Timeout;

/**
 * {@link Parameters}-implementation specific to execution. This implementation considers {@link Timeout} for a command method
 * applying the appropriate synchronization and {@link CommandBatching} to batch commands.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see Timeout
 * @see CommandBatching
 */
public class ExecutionSpecificParameters extends Parameters<ExecutionSpecificParameters.ExecutionAwareParameter> {

    private static final List<Class<?>> TYPES = Arrays.asList(Timeout.class, CommandBatching.class);

    private final int timeoutIndex;
    private final int commandBatchingIndex;

    /**
     * Create new {@link ExecutionSpecificParameters} given a {@link Method}.
     *
     * @param method must not be {@literal null}.
     */
    public ExecutionSpecificParameters(Method method) {

        super(method);

        int timeoutIndex = -1;
        int commandBatchingIndex = -1;

        List<ExecutionAwareParameter> parameters = getParameters();

        for (int i = 0; i < method.getParameterCount(); i++) {

            Parameter methodParameter = parameters.get(i);

            if (methodParameter.isSpecialParameter()) {
                if (methodParameter.isAssignableTo(Timeout.class)) {
                    timeoutIndex = i;
                }

                if (methodParameter.isAssignableTo(CommandBatching.class)) {
                    commandBatchingIndex = i;
                }
            }
        }

        this.timeoutIndex = timeoutIndex;
        this.commandBatchingIndex = commandBatchingIndex;
    }

    /**
     * @return the timeout argument index if present, or {@literal -1} if the command method declares a {@link Timeout}
     *         parameter.
     */
    public int getTimeoutIndex() {
        return timeoutIndex;
    }

    /**
     * @return the command batching argument index if present, or {@literal -1} if the command method declares a
     *         {@link CommandBatching} parameter.
     */
    public int getCommandBatchingIndex() {
        return commandBatchingIndex;
    }

    @Override
    protected ExecutionAwareParameter createParameter(Method method, int parameterIndex) {
        return new ExecutionAwareParameter(method, parameterIndex);
    }

    /**
     * @return {@literal true} if the method defines a {@link CommandBatching} parameter.
     */
    public boolean hasCommandBatchingIndex() {
        return commandBatchingIndex != -1;
    }

    /**
     * @return {@literal true} if the method defines a {@link Timeout} parameter.
     */
    public boolean hasTimeoutIndex() {
        return getTimeoutIndex() != -1;
    }

    public static class ExecutionAwareParameter extends Parameter {

        public ExecutionAwareParameter(Method method, int parameterIndex) {
            super(method, parameterIndex);
        }

        @Override
        public boolean isSpecialParameter() {
            return super.isSpecialParameter() || TYPES.contains(getParameterType());
        }
    }
}
