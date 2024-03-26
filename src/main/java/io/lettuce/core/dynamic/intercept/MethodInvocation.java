package io.lettuce.core.dynamic.intercept;

import java.lang.reflect.Method;

/**
 * Description of an invocation to a method, given to an interceptor upon method-call.
 *
 * <p>
 * A method invocation is a joinpoint and can be intercepted by a method interceptor.
 *
 * @see MethodInterceptor
 * @author Mark Paluch
 * @since 5.0
 */
public interface MethodInvocation {

    /**
     * Proceed to the next interceptor in the chain.
     * <p>
     * The implementation and the semantics of this method depends on the actual joinpoint type (see the children interfaces).
     *
     * @return see the children interfaces' proceed definition
     * @throws Throwable if the invocation throws an exception
     */
    Object proceed() throws Throwable;

    /**
     * @return the originally called {@link Method}.
     */
    Method getMethod();

    /**
     * @return method call arguments.
     */
    Object[] getArguments();

}
