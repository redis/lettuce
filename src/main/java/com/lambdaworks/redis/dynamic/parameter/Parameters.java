package com.lambdaworks.redis.dynamic.parameter;

import com.lambdaworks.redis.internal.LettuceAssert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
            parameters.add(createParameter(method, i));
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
