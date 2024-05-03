package io.lettuce.core.dynamic.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Interface to discover parameter names for methods and constructors.
 *
 * <p>
 * Parameter name discovery is not always possible, but various strategies are available to try, such as looking for debug
 * information that may have been emitted at compile time, and looking for argname annotation values.
 */
public interface ParameterNameDiscoverer {

    /**
     * Return parameter names for this method, or {@code null} if they cannot be determined.
     *
     * @param method method to find parameter names for
     * @return an array of parameter names if the names can be resolved, or {@code null} if they cannot
     */
    String[] getParameterNames(Method method);

    /**
     * Return parameter names for this constructor, or {@code null} if they cannot be determined.
     *
     * @param ctor constructor to find parameter names for
     * @return an array of parameter names if the names can be resolved, or {@code null} if they cannot
     */
    String[] getParameterNames(Constructor<?> ctor);

}
