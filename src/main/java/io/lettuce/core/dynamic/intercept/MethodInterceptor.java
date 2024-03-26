package io.lettuce.core.dynamic.intercept;

/**
 * Intercepts calls on an interface on its way to the target. These are nested "on top" of the target.
 *
 * <p>
 * Implementing classes are required to implement the {@link #invoke(MethodInvocation)} method to modify the original behavior.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface MethodInterceptor {

    /**
     * Implement this method to perform extra treatments before and after the invocation. Polite implementations would certainly
     * like to invoke {@link MethodInvocation#proceed()}.
     *
     * @param invocation the method invocation
     * @return the result of the call to {@link MethodInvocation#proceed()}, might be intercepted by the interceptor.
     * @throws Throwable if the interceptors or the target-object throws an exception.
     */
    Object invoke(MethodInvocation invocation) throws Throwable;

}
