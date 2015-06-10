package com.lambdaworks.redis.commands;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class HashCommandPoolTest extends HashCommandTest {

    RedisConnectionPool<RedisConnection<String, String>> pool;

    @Override
    protected RedisConnection<String, String> connect() {
        pool = client.pool();
        return pool.allocateConnection();
    }

    @Override
    public void closeConnection() throws Exception {
        pool.freeConnection(redis);
        pool.close();
    }
}
