package com.lambdaworks.redis.dynamic.intercept;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lambdaworks.redis.internal.AbstractInvocationHandler;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceLists;

/**
 * Factory to create invocation proxies.
 * <p>
 * Method calls to invocation proxies can be intercepted and modified by a chain of {@link MethodInterceptor}s. Each
 * {@link MethodInterceptor} can continue the call chain, terminate prematurely or modify all aspects of a {@link Method}
 * invocation.
 * <p>
 * {@link InvocationProxyFactory} produces invocation proxies which can implement multiple interface type. Any non-interface
 * types are rejected.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see MethodInterceptor
 * @see MethodInvocation
 */
public class InvocationProxyFactory {

    private final List<MethodInterceptor> interceptors = new ArrayList<>();
    private final List<Class<?>> interfaces = new ArrayList<>();

    /**
     * Create a proxy instance give a {@link ClassLoader}.
     * 
     * @param classLoader must not be {@literal null}.
     * @param <T> inferred result type.
     * @return the invocation proxy instance.
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(ClassLoader classLoader) {

        LettuceAssert.notNull(classLoader, "ClassLoader must not be null");

        Class<?>[] interfaces = this.interfaces.toArray(new Class[this.interfaces.size()]);

        return (T) Proxy.newProxyInstance(classLoader, interfaces, new InterceptorChainInvocationHandler(interceptors));
    }

    /**
     * Add a interface type that should be implemented by the resulting invocation proxy.
     * 
     * @param ifc must not be {@literal null} and must be an interface type.
     */
    public void addInterface(Class<?> ifc) {

        LettuceAssert.notNull(ifc, "Interface type must not be null");
        LettuceAssert.isTrue(ifc.isInterface(), "Type must be an interface");

        this.interfaces.add(ifc);
    }

    /**
     * Add a {@link MethodInterceptor} to the interceptor chain.
     * 
     * @param interceptor notNull
     */
    public void addInterceptor(MethodInterceptor interceptor) {

        LettuceAssert.notNull(interceptor, "MethodInterceptor must not be null");

        this.interceptors.add(interceptor);
    }

    /**
     * {@link MethodInterceptor}-based {@link InterceptorChainInvocationHandler}.
     */
    private static class InterceptorChainInvocationHandler extends AbstractInvocationHandler {

        private final List<MethodInterceptor> interceptors;

        InterceptorChainInvocationHandler(List<MethodInterceptor> interceptors) {
            this.interceptors = LettuceLists.newList(interceptors);
        }

        @Override
        protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

            Iterator<MethodInterceptor> iterator = interceptors.iterator();

            if (iterator.hasNext()) {
                return iterator.next().invoke(getInvocation(method, args, iterator));
            }

            return null;
        }

        private DefaultMethodInvocation getInvocation(final Method method, final Object[] args,
                final Iterator<MethodInterceptor> iterator) {
            return new DefaultMethodInvocation(method, args) {

                @Override
                public Object proceed() throws Throwable {

                    if (iterator.hasNext()) {
                        return iterator.next().invoke(getInvocation(method, args, iterator));
                    }
                    return null;
                }
            };
        }
    }
}
