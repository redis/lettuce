package com.lambdaworks.redis;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 14.05.14 21:58
 */
public class RedisConnectionPool<T> {
    private RedisConnectionProvider<T> redisConnectionProvider;
    private ObjectPool<T> objectPool;

    public RedisConnectionPool(RedisConnectionProvider<T> redisConnectionProvider, int maxActive, int maxIdle, long maxWait) {
        this.redisConnectionProvider = redisConnectionProvider;

        objectPool = new GenericObjectPool<T>(createFactory(redisConnectionProvider), maxActive,
                GenericObjectPool.WHEN_EXHAUSTED_FAIL, maxWait, maxIdle, true, false);
    }

    private PoolableObjectFactory<T> createFactory(final RedisConnectionProvider<T> redisConnectionProvider) {
        return new BasePoolableObjectFactory() {
            @Override
            public Object makeObject() throws Exception {
                return redisConnectionProvider.createConnection();
            }

            @Override
            public boolean validateObject(Object obj) {
                return Connections.isValid(obj);
            }

            @Override
            public void destroyObject(Object obj) throws Exception {
                Connections.close(obj);
            }

            @Override
            public void passivateObject(Object obj) throws Exception {
                super.passivateObject(obj);
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

    public void freeConnection(T t) throws Exception {
        objectPool.returnObject(t);
    }

    public int getNumIdle() throws UnsupportedOperationException {
        return objectPool.getNumIdle();
    }

    public int getNumActive() throws UnsupportedOperationException {
        return objectPool.getNumActive();
    }

    public void close() throws Exception {
        objectPool.clear();
        objectPool.close();
    }
}
