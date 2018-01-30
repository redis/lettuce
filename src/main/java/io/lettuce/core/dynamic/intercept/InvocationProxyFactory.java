/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic.intercept;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.internal.LettuceAssert;

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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T createProxy(ClassLoader classLoader) {

        LettuceAssert.notNull(classLoader, "ClassLoader must not be null");

        Class<?>[] interfaces = this.interfaces.toArray(new Class[0]);

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
    static class InterceptorChainInvocationHandler extends AbstractInvocationHandler {

        private final MethodInterceptorChain.Head context;

        InterceptorChainInvocationHandler(List<MethodInterceptor> interceptors) {
            this.context = MethodInterceptorChain.from(interceptors);
        }

        @Override
        protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
            return context.invoke(proxy, method, args);
        }
    }
}
