package com.lambdaworks.redis.support;

import com.lambdaworks.redis.RedisConnectionPool;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 15.05.14 21:08
 */
public abstract class WithConnection<T> {
    private RedisConnectionPool<T> pool;

    public WithConnection(RedisConnectionPool<T> pool) {
        this.pool = pool;
        T connection = pool.allocateConnection();
        try {
            run(connection);
        } finally {
            pool.freeConnection(connection);
        }
    }

    protected abstract void run(T connection);
}
