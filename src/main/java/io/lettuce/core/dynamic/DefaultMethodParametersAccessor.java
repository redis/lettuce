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
package io.lettuce.core.dynamic;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.lettuce.core.*;
import io.lettuce.core.dynamic.annotation.Key;
import io.lettuce.core.dynamic.annotation.Value;
import io.lettuce.core.dynamic.parameter.MethodParametersAccessor;
import io.lettuce.core.dynamic.parameter.Parameter;
import io.lettuce.core.dynamic.parameter.Parameters;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Default {@link MethodParametersAccessor} implementation.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class DefaultMethodParametersAccessor implements MethodParametersAccessor {

    private final Parameters<? extends Parameter> parameters;

    private final List<Object> values;

    DefaultMethodParametersAccessor(Parameters<? extends Parameter> parameters, Object... values) {

        LettuceAssert.notNull(parameters, "Parameters must not be null");
        LettuceAssert.notNull(values, "Values must not be null");

        this.parameters = parameters;
        this.values = Arrays.asList(values);
    }

    public int getParameterCount() {
        return parameters.getBindableParameters().size();
    }

    @Override
    public Object getBindableValue(int index) {
        return values.get(parameters.getBindableParameter(index).getParameterIndex());
    }

    @Override
    public boolean isKey(int index) {
        return parameters.getBindableParameter(index).findAnnotation(Key.class) != null;
    }

    @Override
    public boolean isValue(int index) {
        return parameters.getBindableParameter(index).findAnnotation(Value.class) != null;
    }

    @Override
    public Iterator<Object> iterator() {
        return new BindableParameterIterator(this);
    }

    @Override
    public int resolveParameterIndex(String name) {

        List<? extends Parameter> bindableParameters = parameters.getBindableParameters();

        for (int i = 0; i < bindableParameters.size(); i++) {

            if (name.equals(bindableParameters.get(i).getName())) {
                return i;
            }
        }

        throw new IllegalArgumentException(String.format("Cannot resolve named parameter %s", name));
    }

    public Parameters<? extends Parameter> getParameters() {
        return parameters;
    }

    @Override
    public boolean isBindableNullValue(int index) {

        Parameter bindableParameter = parameters.getBindableParameter(index);

        if (bindableParameter.isAssignableTo(Limit.class) || bindableParameter.isAssignableTo(io.lettuce.core.Value.class)
                || bindableParameter.isAssignableTo(KeyValue.class) || bindableParameter.isAssignableTo(ScoredValue.class)
                || bindableParameter.isAssignableTo(GeoCoordinates.class) || bindableParameter.isAssignableTo(Range.class)) {
            return false;
        }

        return true;
    }

    /**
     * Iterator class to allow traversing all bindable parameters inside the accessor.
     */
    static class BindableParameterIterator implements Iterator<Object> {

        private final int bindableParameterCount;

        private final DefaultMethodParametersAccessor accessor;

        private int currentIndex = 0;

        /**
         * Creates a new {@link BindableParameterIterator}.
         *
         * @param accessor must not be {@code null}.
         */
        BindableParameterIterator(DefaultMethodParametersAccessor accessor) {

            LettuceAssert.notNull(accessor, "ParametersParameterAccessor must not be null!");

            this.accessor = accessor;
            this.bindableParameterCount = accessor.getParameters().getBindableParameters().size();
        }

        /**
         * Return the next bindable parameter.
         *
         * @return
         */
        public Object next() {
            return accessor.getBindableValue(currentIndex++);
        }

        public boolean hasNext() {
            return bindableParameterCount > currentIndex;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
