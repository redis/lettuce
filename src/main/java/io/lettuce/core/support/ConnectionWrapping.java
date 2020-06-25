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
package io.lettuce.core.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.internal.AsyncCloseable;

/**
 * Utility to wrap pooled connections for return-on-close.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class ConnectionWrapping {

    /**
     * Unwrap a potentially {@link Wrapper} object. Recurses across {@link Wrapper wrappers}
     *
     * @param object the potentially wrapped object.
     * @return the {@code object} if it is not wrapped or the {@link Wrapper#unwrap() unwrapped} object.
     */
    public static Object unwrap(Object object) {

        while (object instanceof Wrapper<?>) {
            object = ((Wrapper<?>) object).unwrap();
        }

        return object;
    }

    /**
     * Wrap a connection along its {@link Origin} reference.
     *
     * @param connection
     * @param pool
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <T> T wrapConnection(T connection, Origin<T> pool) {

        ReturnObjectOnCloseInvocationHandler<T> handler = new ReturnObjectOnCloseInvocationHandler<T>(connection, pool);

        Class<?>[] implementedInterfaces = connection.getClass().getInterfaces();
        Class[] interfaces = new Class[implementedInterfaces.length + 1];
        interfaces[0] = HasTargetConnection.class;
        System.arraycopy(implementedInterfaces, 0, interfaces, 1, implementedInterfaces.length);

        T proxiedConnection = (T) Proxy.newProxyInstance(connection.getClass().getClassLoader(), interfaces, handler);
        handler.setProxiedConnection(proxiedConnection);

        return proxiedConnection;
    }

    /**
     * Invocation handler that takes care of connection.close(). Connections are returned to the pool on a close()-call.
     *
     * @author Mark Paluch
     * @param <T> Connection type.
     * @since 4.3
     */
    static class ReturnObjectOnCloseInvocationHandler<T> extends AbstractInvocationHandler implements Wrapper<T> {

        private T connection;

        private T proxiedConnection;

        private Map<Method, Object> connectionProxies = new ConcurrentHashMap<>(5, 1);

        private final Origin<T> pool;

        ReturnObjectOnCloseInvocationHandler(T connection, Origin<T> pool) {
            this.connection = connection;
            this.pool = pool;
        }

        void setProxiedConnection(T proxiedConnection) {
            this.proxiedConnection = proxiedConnection;
        }

        @Override
        protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.getName().equals("getStatefulConnection")) {
                return proxiedConnection;
            }

            if (method.getName().equals("getTargetConnection")) {
                return connection;
            }

            if (connection == null) {
                throw new RedisException("Connection is deallocated and cannot be used anymore.");
            }

            if (method.getName().equals("close")) {
                pool.returnObject(proxiedConnection);
                connection = null;
                proxiedConnection = null;
                connectionProxies.clear();
                return null;
            }

            if (method.getName().equals("closeAsync")) {
                CompletableFuture<Void> future = pool.returnObjectAsync(proxiedConnection);
                connection = null;
                proxiedConnection = null;
                connectionProxies.clear();
                return future;
            }

            try {

                if (method.getName().equals("sync") || method.getName().equals("async")
                        || method.getName().equals("reactive")) {
                    return connectionProxies.computeIfAbsent(method, m -> getInnerProxy(method, args));
                }

                return method.invoke(connection, args);

            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private Object getInnerProxy(Method method, Object[] args) {

            try {
                Object result = method.invoke(connection, args);

                result = Proxy.newProxyInstance(getClass().getClassLoader(), result.getClass().getInterfaces(),
                        new DelegateCloseToConnectionInvocationHandler((AsyncCloseable) proxiedConnection, result));

                return result;
            } catch (IllegalAccessException e) {
                throw new RedisException(e);
            } catch (InvocationTargetException e) {
                throw new RedisException(e.getTargetException());
            }
        }

        public T getConnection() {
            return connection;
        }

        @Override
        public T unwrap() {
            return getConnection();
        }

    }

    /**
     * Invocation handler that takes care of connection.close(). Connections are returned to the pool on a close()-call.
     *
     * @author Mark Paluch
     * @param <T> Connection type.
     * @since 4.3
     */
    @SuppressWarnings("try")
    static class DelegateCloseToConnectionInvocationHandler<T extends AsyncCloseable & AutoCloseable>
            extends AbstractInvocationHandler implements Wrapper<Object> {

        private final T proxiedConnection;

        private final Object api;

        DelegateCloseToConnectionInvocationHandler(T proxiedConnection, Object api) {

            this.proxiedConnection = proxiedConnection;
            this.api = api;
        }

        @Override
        protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.getName().equals("getStatefulConnection")) {
                return proxiedConnection;
            }

            try {

                if (method.getName().equals("close")) {
                    proxiedConnection.close();
                    return null;
                }

                if (method.getName().equals("closeAsync")) {
                    return proxiedConnection.closeAsync();
                }

                return method.invoke(api, args);

            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        @Override
        public Object unwrap() {
            return api;
        }

    }

    /**
     * Interface to retrieve an underlying target connection from a proxy.
     */
    interface HasTargetConnection {

        StatefulConnection<?, ?> getTargetConnection();

    }

    /**
     * Interface to return objects to their origin.
     */
    interface Origin<T> {

        /**
         * Synchronously return the object.
         */
        void returnObject(T o) throws Exception;

        /**
         * Return the object asynchronously.
         */
        CompletableFuture<Void> returnObjectAsync(T o) throws Exception;

    }

    /**
     * Marker interface to indicate a wrapper.
     *
     * @param <T> Type of the wrapped object.
     * @since 5.2
     */
    interface Wrapper<T> {

        T unwrap();

    }

}
