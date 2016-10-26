/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.dynamic.parameter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.lambdaworks.redis.dynamic.domain.Timeout;

/**
 * {@link Parameters}-implementation specific to execution. This implementation considers {@link Timeout} for a command method
 * applying the appropriate synchronization.
 * 
 * @author Mark Paluch
 * @since 5.0
 * @see Timeout
 */
public class ExecutionSpecificParameters extends Parameters<ExecutionSpecificParameters.ExecutionAwareParameter> {

    private static final List<Class<?>> TYPES = Arrays.asList(Timeout.class);

    private final int timeoutIndex;

    /**
     * Create new {@link ExecutionSpecificParameters} given a {@link Method}.
     * 
     * @param method must not be {@literal null}.
     */
    public ExecutionSpecificParameters(Method method) {

        super(method);

        int timeoutIndex = -1;

        List<ExecutionAwareParameter> parameters = getParameters();

        for (int i = 0; i < method.getParameterCount(); i++) {

            Parameter methodParameter = parameters.get(i);

            if (methodParameter.isSpecialParameter()) {
                if (methodParameter.isAssignableTo(Timeout.class)) {
                    timeoutIndex = i;
                }
            }
        }

        this.timeoutIndex = timeoutIndex;
    }

    public int getTimeoutIndex() {
        return timeoutIndex;
    }

    @Override
    protected ExecutionAwareParameter createParameter(Method method, int parameterIndex) {
        return new ExecutionAwareParameter(method, parameterIndex);
    }

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
