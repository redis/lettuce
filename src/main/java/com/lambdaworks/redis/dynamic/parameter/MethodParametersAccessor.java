package com.lambdaworks.redis.dynamic.parameter;

import com.lambdaworks.redis.dynamic.domain.FlushMode;
import com.lambdaworks.redis.dynamic.domain.Timeout;

import java.util.Iterator;

/**
 * Accessor interface to method parameters during the actual invocation.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public interface MethodParametersAccessor {

    /**
     * @return number of parameters.
     */
    int getParameterCount();

    /**
     * Returns the bindable value with the given index. Bindable means, that {@link FlushMode} and {@link Timeout} values are
     * skipped without noticed in the index. For a method signature taking {@link String}, {@link Timeout} , {@link String},
     * {@code #getBindableParameter(1)} would return the second {@link String} value.
     *
     * @param index
     * @return the bindable value.
     */
    Object getBindableValue(int index);

    /**
     *
     * @param index
     * @return {@literal true} if the parameter at {@code index} is a key.
     */
    boolean isKey(int index);

    /**
     *
     * @param index
     * @return {@literal true} if the parameter at {@code index} is a value.
     */
    boolean isValue(int index);

    /**
     * Returns an iterator over all <em>bindable</em> parameters. This means parameters implementing {@link Timeout} or
     * {@link FlushMode} will not be included in this {@link Iterator}.
     *
     * @return
     */
    Iterator<Object> iterator();

    /**
     * Resolve a parameter name to its index.
     * 
     * @param name the name.
     * @return
     */
    int resolveParameterIndex(String name);
}
