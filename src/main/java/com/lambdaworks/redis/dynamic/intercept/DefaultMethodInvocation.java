package com.lambdaworks.redis.dynamic.intercept;

import java.lang.reflect.Method;

/**
 * Default implementation of {@link MethodInvocation}
 * 
 * @author Mark Paluch
 */
abstract class DefaultMethodInvocation implements MethodInvocation {

    private final Method method;
    private final Object[] arguments;

    DefaultMethodInvocation(Method method, Object[] arguments) {

        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }
}
