package com.lambdaworks.redis.dynamic.parameter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.lambdaworks.redis.dynamic.domain.FlushMode;
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

    private static final List<Class<?>> TYPES = Arrays.asList(FlushMode.class, Timeout.class);

    private final int flushModeIndex;
    private final int timeoutIndex;

    /**
     * Create new {@link ExecutionSpecificParameters} given a {@link Method}.
     * 
     * @param method must not be {@literal null}.
     */
    public ExecutionSpecificParameters(Method method) {

        super(method);

        int flushModeIndex = -1;
        int timeoutIndex = -1;

        List<ExecutionAwareParameter> parameters = getParameters();

        for (int i = 0; i < method.getParameterCount(); i++) {

            Parameter methodParameter = parameters.get(i);

            if (methodParameter.isSpecialParameter()) {
                if (methodParameter.isAssignableTo(Timeout.class)) {
                    timeoutIndex = i;
                }

                if (methodParameter.isAssignableTo(FlushMode.class)) {
                    flushModeIndex = i;
                }
            }
        }

        this.flushModeIndex = flushModeIndex;
        this.timeoutIndex = timeoutIndex;
    }

    public int getFlushModeIndex() {
        return flushModeIndex;
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
