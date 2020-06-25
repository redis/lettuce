/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.dynamic.segment;

import io.lettuce.core.dynamic.parameter.MethodParametersAccessor;
import io.lettuce.core.dynamic.parameter.Parameter;
import io.lettuce.core.internal.LettuceAssert;

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
     * @param content must not be empty or {@code null}.
     * @return the {@link CommandSegment}.
     */
    public static CommandSegment constant(String content) {
        return new Constant(content);
    }

    /**
     * Create a named parameter reference {@link CommandSegment}.
     *
     * @param name must not be empty or {@code null}.
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
     * Check whether this segment can consume the {@link Parameter} by applying parameter substitution.
     *
     * @param parameter
     * @return
     * @since 5.1.3
     */
    public abstract boolean canConsume(Parameter parameter);

    /**
     * @param parametersAccessor
     * @return
     */
    public abstract ArgumentContribution contribute(MethodParametersAccessor parametersAccessor);

    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();
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
        public boolean canConsume(Parameter parameter) {
            return false;
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
        public boolean canConsume(Parameter parameter) {
            return parameter.getName() != null && parameter.getName().equals(name);
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
        public boolean canConsume(Parameter parameter) {
            return parameter.getParameterIndex() == index;
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
