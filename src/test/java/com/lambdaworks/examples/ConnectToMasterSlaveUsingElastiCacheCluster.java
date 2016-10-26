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
package com.lambdaworks.examples;

import java.util.Arrays;
import java.util.List;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.masterslave.MasterSlave;
import com.lambdaworks.redis.masterslave.StatefulRedisMasterSlaveConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToMasterSlaveUsingElastiCacheCluster {

    public static void main(String[] args) {

        // Syntax: redis://[password@]host[:port][/databaseNumber]
        RedisClient redisClient = RedisClient.create();

        List<RedisURI> nodes = Arrays.asList(RedisURI.create("redis://host1"),
                RedisURI.create("redis://host2"),
                RedisURI.create("redis://host3"));

        StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave
                .connect(redisClient, new Utf8StringCodec(), nodes);
        connection.setReadFrom(ReadFrom.MASTER_PREFERRED);

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
