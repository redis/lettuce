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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Base class to abstract method {@link Parameter}s.
 *
 * @author Mark Paluch
 */
public abstract class Parameters<P extends Parameter> implements Iterable<P> {

    private final List<P> parameters;
    private final List<P> bindableParameters;

    /**
     * Create new {@link Parameters} given a {@link Method}.
     *
     * @param method must not be {@literal null}.
     */
    public Parameters(Method method) {

        LettuceAssert.notNull(method, "Method must not be null");

        this.parameters = new ArrayList<>(method.getParameterCount());

        for (int i = 0; i < method.getParameterCount(); i++) {

            P parameter = createParameter(method, i);

            parameters.add(parameter);
        }

        this.bindableParameters = createBindableParameters();
    }

    /**
     * Create a new {@link Parameters} for given a {@link Method} at {@code parameterIndex}.
     *
     * @param method must not be {@literal null}.
     * @param parameterIndex the parameter index.
     * @return the {@link Parameter}.
     */
    protected abstract P createParameter(Method method, int parameterIndex);

    /**
     * Returns {@link Parameter} instances with effectively all special parameters removed.
     *
     * @return
     */
    private List<P> createBindableParameters() {

        List<P> bindables = new ArrayList<>(parameters.size());

        for (P parameter : parameters) {
            if (parameter.isBindable()) {
                bindables.add(parameter);
            }
        }

        return bindables;
    }

    /**
     * @return
     */
    public List<P> getParameters() {
        return parameters;
    }

    /**
     * Get the bindable parameter according it's logical position in the command. Declarative position may differ because of
     * special parameters interleaved.
     *
     * @param index
     * @return the {@link Parameter}.
     */
    public Parameter getBindableParameter(int index) {
        return getBindableParameters().get(index);
    }

    /**
     * Returns {@link Parameter} instances with effectively all special parameters removed.
     *
     * @return
     */
    public List<P> getBindableParameters() {
        return bindableParameters;
    }

    @Override
    public Iterator<P> iterator() {
        return getBindableParameters().iterator();
    }

}
