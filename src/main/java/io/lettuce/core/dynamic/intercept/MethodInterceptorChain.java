/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic.intercept;

import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Invocation context with a static call chain of {@link MethodInterceptor}s to handle method invocations.
 * <p>
 * {@link MethodInterceptorChain} is created from one or more {@link MethodInterceptor}s and compiled to a forward-only call
 * chain. A chain of {@link MethodInterceptorChain} has a head and tail context. The tail context is no-op and simply returns
 * {@code null}.
 * <p>
 * Invocations are represented as {@link PooledMethodInvocation} using thread-local pooling. An invocation lives within the
 * boundaries of a thread therefore it's safe to use thread-local object pooling.
 *
 * @author Mark Paluch
 * @since 5.0
 */
abstract class MethodInterceptorChain {

    private final ThreadLocal<PooledMethodInvocation> pool = ThreadLocal.withInitial(PooledMethodInvocation::new);

    final MethodInterceptorChain next;

    MethodInterceptorChain(MethodInterceptorChain next) {
        this.next = next;
    }

    /**
     * Create a {@link MethodInterceptorChain} from {@link MethodInterceptor}s. Chain elements are created eagerly by
     * stack-walking {@code interceptors}. Make sure the {@link Iterable} does not exhaust the stack size.
     *
     * @param interceptors must not be {@code null}.
     * @return the {@link MethodInterceptorChain} that is an entry point for method invocations.
     */
    public static Head from(Iterable<? extends MethodInterceptor> interceptors) {
        return new Head(next(interceptors.iterator()));
    }

    private static MethodInterceptorChain next(Iterator<? extends MethodInterceptor> iterator) {
        return iterator.hasNext() ? createContext(iterator, iterator.next()) : Tail.INSTANCE;
    }

    private static MethodInterceptorChain createContext(Iterator<? extends MethodInterceptor> iterator,
            MethodInterceptor interceptor) {
        return new MethodInterceptorContext(next(iterator), interceptor);
    }

    /**
     * Invoke a {@link Method} with its {@code args}.
     *
     * @param target must not be {@code null}.
     * @param method must not be {@code null}.
     * @param args must not be {@code null}.
     * @return
     * @throws Throwable
     */
    public Object invoke(Object target, Method method, Object[] args) throws Throwable {

        PooledMethodInvocation invocation = getInvocation(target, method, args, next);

        try {
            // JIT hint
            if (next instanceof MethodInterceptorContext) {
                return next.proceed(invocation);
            }
            return next.proceed(invocation);
        } finally {
            invocation.clear();
        }
    }

    private PooledMethodInvocation getInvocation(Object target, Method method, Object[] args, MethodInterceptorChain next) {

        PooledMethodInvocation pooledMethodInvocation = pool.get();
        pooledMethodInvocation.initialize(target, method, args, next);
        return pooledMethodInvocation;
    }

    /**
     * Proceed to the next {@link MethodInterceptorChain}.
     *
     * @param invocation must not be {@code null}.
     * @return
     * @throws Throwable
     */
    abstract Object proceed(MethodInvocation invocation) throws Throwable;

    /**
     * {@link MethodInterceptorChain} using {@link MethodInterceptor} to handle invocations.
     */
    static class MethodInterceptorContext extends MethodInterceptorChain {

        private final MethodInterceptor interceptor;

        MethodInterceptorContext(MethodInterceptorChain next, MethodInterceptor interceptor) {
            super(next);
            this.interceptor = interceptor;
        }

        @Override
        Object proceed(MethodInvocation invocation) throws Throwable {
            return interceptor.invoke(invocation);
        }

    }

    /**
     * Head {@link MethodInterceptorChain} to delegate to the next {@link MethodInterceptorChain}.
     */
    static class Head extends MethodInterceptorChain {

        protected Head(MethodInterceptorChain next) {
            super(next);
        }

        @Override
        Object proceed(MethodInvocation invocation) throws Throwable {
            return next.proceed(invocation);
        }

    }

    /**
     * Tail {@link MethodInterceptorChain}, no-op.
     */
    static class Tail extends MethodInterceptorChain {

        public static Tail INSTANCE = new Tail();

        private Tail() {
            super(null);
        }

        @Override
        Object proceed(MethodInvocation invocation) throws Throwable {
            return null;
        }

    }

    /**
     * Stateful {@link MethodInvocation} using {@link MethodInterceptorChain}. The state is only valid throughout a call.
     */
    static class PooledMethodInvocation implements MethodInvocation, InvocationTargetProvider {

        private Object target;

        private Method method;

        private Object args[];

        private MethodInterceptorChain current;

        PooledMethodInvocation() {
        }

        /**
         * Initialize state from the method call.
         *
         * @param target
         * @param method
         * @param args
         * @param head
         */
        public void initialize(Object target, Method method, Object[] args, MethodInterceptorChain head) {
            this.target = target;
            this.method = method;
            this.args = args;
            this.current = head;
        }

        /**
         * Clear invocation state.
         */
        public void clear() {
            this.target = null;
            this.method = null;
            this.args = null;
            this.current = null;
        }

        @Override
        public Object proceed() throws Throwable {
            current = current.next;
            return current.proceed(this);
        }

        @Override
        public Object getInvocationTarget() {
            return target;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return args;
        }

    }

}
