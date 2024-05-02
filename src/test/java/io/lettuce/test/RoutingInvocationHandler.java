package io.lettuce.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author Mark Paluch
 */
public class RoutingInvocationHandler extends ConnectionDecoratingInvocationHandler {

    private final InvocationHandler delegate;

    public RoutingInvocationHandler(Object target, InvocationHandler delegate) {
        super(target);
        this.delegate = delegate;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        if (method.getName().equals("getStatefulConnection")) {
            return super.handleInvocation(proxy, method, args);
        }

        return delegate.invoke(proxy, method, args);
    }

}
