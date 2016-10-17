package com.lambdaworks.redis.dynamic.segment;

import com.lambdaworks.redis.dynamic.parameter.MethodParametersAccessor;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Value object representing a segment within a Redis Command.
 * <p>
 * A command segment is an ASCII string denoting a command, a named or an index-parameter reference.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public abstract class CommandSegment {

    /**
     * Create a constant {@link CommandSegment}.
     * 
     * @param content must not be empty or {@literal null}.
     * @return the {@link CommandSegment}.
     */
    public static CommandSegment constant(String content) {
        return new Constant(content);
    }

    /**
     * Create a named parameter reference {@link CommandSegment}.
     * 
     * @param name must not be empty or {@literal null}.
     * @return
     */
    public static CommandSegment namedParameter(String name) {
        return new NamedParameter(name);
    }

    public static CommandSegment indexedParameter(int index) {
        return new IndexedParameter(index);
    }

    /**
     *
     * @return the command segment in its {@link String representation}
     */
    public abstract String asString();

    /**
     *
     * @param parametersAccessor
     * @return
     */
    public abstract ArgumentContribution contribute(MethodParametersAccessor parametersAccessor);

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" ").append(asString());
        return sb.toString();
    }

    private static class Constant extends CommandSegment {

        private final String content;

        public Constant(String content) {

            LettuceAssert.notEmpty(content, "Constant must not be empty");

            this.content = content;
        }

        @Override
        public String asString() {
            return content;
        }

        @Override
        public ArgumentContribution contribute(MethodParametersAccessor parametersAccessor) {
            return new ArgumentContribution(-1, asString());
        }
    }

    private static class NamedParameter extends CommandSegment {

        private final String name;

        public NamedParameter(String name) {

            LettuceAssert.notEmpty(name, "Parameter name must not be empty");

            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }

        @Override
        public ArgumentContribution contribute(MethodParametersAccessor parametersAccessor) {

            int index = parametersAccessor.resolveParameterIndex(name);
            return new ArgumentContribution(index, parametersAccessor.getBindableValue(index));
        }
    }

    private static class IndexedParameter extends CommandSegment {

        private final int index;

        public IndexedParameter(int index) {

            LettuceAssert.isTrue(index >= 0, "Parameter index must be non-negative starting at 0");
            this.index = index;
        }

        @Override
        public String asString() {
            return Integer.toString(index);
        }

        @Override
        public ArgumentContribution contribute(MethodParametersAccessor parametersAccessor) {
            return new ArgumentContribution(index, parametersAccessor.getBindableValue(index));
        }
    }

    public static class ArgumentContribution {

        private final int parameterIndex;
        private final Object value;

        ArgumentContribution(int parameterIndex, Object value) {
            this.parameterIndex = parameterIndex;
            this.value = value;
        }

        public int getParameterIndex() {
            return parameterIndex;
        }

        public Object getValue() {
            return value;
        }
    }
}
