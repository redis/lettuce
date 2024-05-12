package io.lettuce.core.dynamic.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * Composite {@link ParameterNameDiscoverer} to resolve parameter names using multiple {@link ParameterNameDiscoverer}s.
 *
 * @author Mark Paluch
 */
public class CompositeParameterNameDiscoverer implements ParameterNameDiscoverer {

    private Collection<ParameterNameDiscoverer> parameterNameDiscoverers;

    public CompositeParameterNameDiscoverer(ParameterNameDiscoverer... parameterNameDiscoverers) {
        this(Arrays.asList(parameterNameDiscoverers));
    }

    public CompositeParameterNameDiscoverer(Collection<ParameterNameDiscoverer> parameterNameDiscoverers) {
        this.parameterNameDiscoverers = parameterNameDiscoverers;
    }

    @Override
    public String[] getParameterNames(Method method) {

        for (ParameterNameDiscoverer parameterNameDiscoverer : parameterNameDiscoverers) {
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            if (parameterNames != null) {
                return parameterNames;
            }
        }

        return null;
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {

        for (ParameterNameDiscoverer parameterNameDiscoverer : parameterNameDiscoverers) {
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(ctor);
            if (parameterNames != null) {
                return parameterNames;
            }
        }

        return null;
    }

}
