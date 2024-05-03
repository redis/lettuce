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
     * @param method must not be {@code null}.
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
     * @return {@code true} if the method defines a {@link CommandBatching} parameter.
     */
    public boolean hasCommandBatchingIndex() {
        return commandBatchingIndex != -1;
    }

    /**
     * @return {@code true} if the method defines a {@link Timeout} parameter.
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
