package com.lambdaworks.redis;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 14.05.14 21:58
 */
public class RedisConnectionPool<T> {
    private RedisConnectionProvider<T> redisConnectionProvider;
    private ObjectPool<T> objectPool;

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
                return redisConnectionProvider.createConnection();
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
                Connections.close(p.getObject());
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
        objectPool.close();
    }
}
