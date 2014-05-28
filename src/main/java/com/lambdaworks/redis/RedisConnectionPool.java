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
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 21:58
 */
public class RedisConnectionPool<T> implements Closeable {

    private RedisConnectionProvider<T> redisConnectionProvider;
    private GenericObjectPool<T> objectPool;
    private CloseEvents closeEvents = new CloseEvents();

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
                return Connections.isValid(p.getObject());
            }

            @Override
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

    public T allocateConnection() {
        try {
            return objectPool.borrowObject();
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisException(e.getMessage(), e);
        }
    }

    public void freeConnection(T t) {
        objectPool.returnObject(t);
    }

    public int getNumIdle() throws UnsupportedOperationException {
        return objectPool.getNumIdle();
    }

    public int getNumActive() throws UnsupportedOperationException {
        return objectPool.getNumActive();
    }

    public void close() {
        objectPool.close();
        objectPool = null;

        closeEvents.fireEventClosed(this);
        closeEvents = null;
    }

    public Class<?> getComponentType() {
        return redisConnectionProvider.getComponentType();
    }

    public void addListener(CloseEvents.CloseListener listener) {
        closeEvents.addListener(listener);
    }

    public void removeListener(CloseEvents.CloseListener listener) {
        closeEvents.removeListener(listener);
    }
}
