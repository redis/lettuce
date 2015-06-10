// Copyright (C) 2012 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.api.sync.RedisCommands;

public class BitCommandPoolTest extends BitCommandTest {
    RedisConnectionPool<RedisCommands<String, String>> pool;
    RedisConnectionPool<RedisCommands<String, String>> bitpool;

    @Override
    protected RedisCommands<String, String> connect() {
        pool = client.pool(new BitStringCodec(), 1, 5);
        bitpool = client.pool(new BitStringCodec(), 1, 5);
        bitstring = bitpool.allocateConnection();
        return pool.allocateConnection();
    }

    @Override
    public void closeConnection() throws Exception {
        pool.freeConnection(redis);
        bitpool.freeConnection(bitstring);

        pool.close();
        bitpool.close();
    }
}
