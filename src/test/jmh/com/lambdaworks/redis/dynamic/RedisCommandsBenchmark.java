/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.dynamic;

import java.util.concurrent.CompletableFuture;

import org.openjdk.jmh.annotations.*;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.dynamic.batch.BatchSize;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class RedisCommandsBenchmark {

    private final static int BATCH_SIZE = 20;

    private RedisClient redisClient;
    private StatefulRedisConnection<byte[], byte[]> connection;
    private CompletableFuture commands[];
    private RegularCommands regularCommands;
    private BatchCommands batchCommands;

    @Setup
    public void setup() {

        redisClient = RedisClient.create(RedisURI.create(TestSettings.host(), TestSettings.port()));
        connection = redisClient.connect(ByteArrayCodec.INSTANCE);

        RedisCommandFactory redisCommandFactory = new RedisCommandFactory(connection);
        regularCommands = redisCommandFactory.getCommands(RegularCommands.class);
        batchCommands = redisCommandFactory.getCommands(BatchCommands.class);
        commands = new CompletableFuture[BATCH_SIZE];
    }

    @TearDown
    public void tearDown() {

        connection.close();
        redisClient.shutdown();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void asyncSet() {

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i] = regularCommands.set("key", "value").toCompletableFuture();
        }

        CompletableFuture.allOf(commands).join();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void batchSet() {

        for (int i = 0; i < BATCH_SIZE; i++) {
            batchCommands.set("key", "value");
        }
    }

    interface RegularCommands extends Commands {

        RedisFuture<String> set(String key, String value);
    }

    @BatchSize(BATCH_SIZE)
    interface BatchCommands extends Commands {

        void set(String key, String value);
    }

    public static void main(String[] args) {
        RedisCommandsBenchmark b = new RedisCommandsBenchmark();
        b.setup();
        b.asyncSet();
    }
}
