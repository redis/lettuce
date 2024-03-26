package io.lettuce.core.dynamic.intercept;

/**
 * Provides an invocation target object.
 *
 * @see MethodInterceptor
 * @author Mark Paluch
 * @since 5.0
 */
public interface InvocationTargetProvider {

    /**
     * @return the invocation target.
     */
    Object getInvocationTarget();

}
