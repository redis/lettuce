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
