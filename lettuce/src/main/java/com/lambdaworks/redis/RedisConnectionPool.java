package com.lambdaworks.redis;

import java.io.Closeable;
import java.lang.reflect.Proxy;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Connection pool for redis connections.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <T> Connection type.
 * @since 14.05.14 21:58
 */
public class RedisConnectionPool<T> implements Closeable {

    private final RedisConnectionProvider<T> redisConnectionProvider;
    private GenericObjectPool<T> objectPool;
    private CloseEvents closeEvents = new CloseEvents();

    /**
     * Create a new connection pool
     * 
     * @param redisConnectionProvider
     * @param maxActive
     * @param maxIdle
     * @param maxWait
     */
    public RedisConnectionPool(RedisConnectionProvider<T> redisConnectionProvider, int maxActive, int maxIdle, long maxWait) {
        this.redisConnectionProvider = redisConnectionProvider;

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(maxIdle);
        config.setMaxTotal(maxActive);
        config.setMaxWaitMillis(maxWait);
        config.setTestOnBorrow(true);

        objectPool = new GenericObjectPool<T>(createFactory(redisConnectionProvider), config);
    }

    private PooledObjectFactory<T> createFactory(final RedisConnectionProvider<T> redisConnectionProvider) {
        return new BasePooledObjectFactory<T>() {

            @SuppressWarnings("unchecked")
            @Override
            public T create() throws Exception {

                T connection = redisConnectionProvider.createConnection();
                PooledConnectionInvocationHandler<T> h = new PooledConnectionInvocationHandler<T>(connection,
                        RedisConnectionPool.this);

                Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[] { redisConnectionProvider.getComponentType() }, h);

                return (T) proxy;
            }

            @Override
            public PooledObject<T> wrap(T obj) {
                return new DefaultPooledObject<T>(obj);
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
     * Allocate a connection from the pool. It must be returned using freeConnection (or alternatively call <code>close()</code>
     * on the connection).
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
     * @param listener
     */
    public void addListener(CloseEvents.CloseListener listener) {
        closeEvents.addListener(listener);
    }

    /**
     * Removes a CloseListener.
     * 
     * @param listener
     */
    public void removeListener(CloseEvents.CloseListener listener) {
        closeEvents.removeListener(listener);
    }
}
