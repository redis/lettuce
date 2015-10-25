package com.lambdaworks.redis.commands;

import com.lambdaworks.redis.api.sync.RedisCommands;
import org.junit.Test;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ServerCommandPoolTest extends ServerCommandTest {

    RedisConnectionPool<RedisCommands<String, String>> pool;

    @Override
    protected RedisCommands<String, String> connect() {
        pool = client.pool();
        return pool.allocateConnection();
    }

    @Override
    public void closeConnection() throws Exception {
        pool.freeConnection(redis);
        pool.close();
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void flushdb() throws Exception {
        super.flushdb();
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void flushdbAsync() throws Exception {
        super.flushdbAsync();
    }
}
