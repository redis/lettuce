/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.lambdaworks.redis.internal.AbstractInvocationHandler;
import com.lambdaworks.redis.support.ConnectionPoolSupport;

/**
 * Connection pool for redis connections.
 * 
 * @author Mark Paluch
 * @param <T> Connection type.
 * @since 3.0
 * @deprecated Will be removed in future versions. Use  {@link ConnectionPoolSupport}.
 */
@Deprecated
public class RedisConnectionPool<T> implements Closeable {

    private final RedisConnectionProvider<T> redisConnectionProvider;
    private GenericObjectPool<T> objectPool;
    private CloseEvents closeEvents = new CloseEvents();

    /**
     * Create a new connection pool
     * 
     * @param redisConnectionProvider the connection provider
     * @param maxActive max active connections
     * @param maxIdle max idle connections
     * @param maxWait max wait time (ms) for a connection
     */
    public RedisConnectionPool(RedisConnectionProvider<T> redisConnectionProvider, int maxActive, int maxIdle, long maxWait) {
        this.redisConnectionProvider = redisConnectionProvider;

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(maxIdle);
        config.setMaxTotal(maxActive);
        config.setMaxWaitMillis(maxWait);
        config.setTestOnBorrow(true);

        objectPool = new GenericObjectPool<>(createFactory(redisConnectionProvider), config);
    }

    private PooledObjectFactory<T> createFactory(final RedisConnectionProvider<T> redisConnectionProvider) {
        return new BasePooledObjectFactory<T>() {

            @SuppressWarnings("unchecked")
            @Override
            public T create() throws Exception {

                T connection = redisConnectionProvider.createConnection();
                PooledConnectionInvocationHandler<T> h = new PooledConnectionInvocationHandler<>(connection,
                        RedisConnectionPool.this);

                Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[] { redisConnectionProvider.getComponentType() }, h);

                return (T) proxy;
            }

            @Override
            public PooledObject<T> wrap(T obj) {
                return new DefaultPooledObject<>(obj);
            }

            @Override
            public boolean validateObject(PooledObject<T> p) {
                return Connections.isOpen(p.getObject());
            }

            @Override
            @SuppressWarnings("unchecked")
            public void destroyObject(PooledObject<T> p) throws Exception {

                T object = p.getObject();
                if (Proxy.isProxyClass(object.getClass())) {
                    PooledConnectionInvocationHandler<T> invocationHandler = (PooledConnectionInvocationHandler<T>) Proxy
                            .getInvocationHandler(object);

                    object = invocationHandler.getConnection();
                }

                Connections.close(object);
            }
        };
    }

    /**
     * Allocate a connection from the pool. It must be returned using freeConnection (or alternatively call {@code close()} on
     * the connection).
     *
     * The connections returned by this method are proxies to the underlying connections.
     *
     * @return a pooled connection.
     */
    public T allocateConnection() {
        try {
            return objectPool.borrowObject();
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisException(e.getMessage(), e);
        }
    }

    /**
     * Return a connection into the pool.
     * 
     * @param t the connection.
     */
    public void freeConnection(T t) {
        objectPool.returnObject(t);
    }

    /**
     * 
     * @return the number of idle connections
     */
    public int getNumIdle() {
        return objectPool.getNumIdle();
    }

    /**
     * 
     * @return the number of active connections.
     */
    public int getNumActive() {
        return objectPool.getNumActive();
    }

    /**
     * Close the pool and close all idle connections. Active connections won't be closed.
     */
    @Override
    public void close() {
        objectPool.close();
        objectPool = null;

        closeEvents.fireEventClosed(this);
        closeEvents = null;
    }

    /**
     * 
     * @return the component type (pool resource type).
     */
    public Class<? extends T> getComponentType() {
        return redisConnectionProvider.getComponentType();
    }

    /**
     * Adds a CloseListener.
     * 
     * @param listener the listener
     */
    void addListener(CloseEvents.CloseListener listener) {
        closeEvents.addListener(listener);
    }

    /**
     * Invocation handler which takes care of connection.close(). Connections are returned to the pool on a close()-call.
     *
     * @author Mark Paluch
     * @param <T> Connection type.
     * @since 3.0
     */
    static class PooledConnectionInvocationHandler<T> extends AbstractInvocationHandler {
        public static final Set<String> DISABLED_METHODS = Collections.singleton("getStatefulConnection");

        private T connection;
        private final RedisConnectionPool<T> pool;

        public PooledConnectionInvocationHandler(T connection, RedisConnectionPool<T> pool) {
            this.connection = connection;
            this.pool = pool;

        }

        @SuppressWarnings("unchecked")
        @Override
        protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

            if (DISABLED_METHODS.contains(method.getName())) {
                throw new UnsupportedOperationException(
                        "Calls to " + method.getName() + " are not supported on pooled connections");
            }

            if (connection == null) {
                throw new RedisException("Connection is deallocated and cannot be used anymore.");
            }

            if (method.getName().equals("close")) {
                if (pool.objectPool == null) {
                    return method.invoke(connection, args);
                }
                pool.freeConnection((T) proxy);
                return null;
            }

            try {
                return method.invoke(connection, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        public T getConnection() {
            return connection;
        }
    }

    /**
     * Connection provider for redis connections.
     *
     * @author Mark Paluch
     * @param <T> Connection type.
     * @since 3.0
     */
    interface RedisConnectionProvider<T> {
        T createConnection();

        Class<? extends T> getComponentType();
    }
}
