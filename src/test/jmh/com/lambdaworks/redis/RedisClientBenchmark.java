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
package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import rx.Observable;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.ByteArrayCodec;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class RedisClientBenchmark {

    private final static int BATCH_SIZE = 20;
    private final static byte[] KEY = "benchmark".getBytes();
    private final static byte[] FOO = "foo".getBytes();

    private RedisClient redisClient;
    private StatefulRedisConnection<byte[], byte[]> connection;
    private RedisFuture commands[];
    private Observable observables[];

    @Setup
    public void setup() {

        redisClient = RedisClient.create(RedisURI.create(TestSettings.host(), TestSettings.port()));
        connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        commands = new RedisFuture[BATCH_SIZE];
        observables = new Observable[BATCH_SIZE];
    }

    @TearDown
    public void tearDown() {

        connection.close();
        redisClient.shutdown(0, 0, TimeUnit.SECONDS);
    }

    @Benchmark
    public void asyncSet() {
        connection.async().set(KEY, KEY).toCompletableFuture().join();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void asyncSetBatch() throws Exception {

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i] = connection.async().set(KEY, KEY);
        }

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i].get();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void asyncSetBatchFlush() throws Exception {

        connection.setAutoFlushCommands(false);

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i] = connection.async().set(KEY, KEY);
        }

        connection.flushCommands();
        connection.setAutoFlushCommands(true);

        for (int i = 0; i < BATCH_SIZE; i++) {
            commands[i].get();
        }
    }

    @Benchmark
    public void syncSet() {
        connection.sync().set(KEY, KEY);
    }

    @Benchmark
    public void syncList() {
        connection.async().del(FOO);
        connection.sync().lpush(FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO, FOO,
                FOO);
        connection.sync().lrange(FOO, 0, -1);
    }

    @Benchmark
    public void reactiveSet() {
        connection.reactive().set(KEY, KEY).toBlocking().single();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void reactiveSetBatch() throws Exception {

        for (int i = 0; i < BATCH_SIZE; i++) {
            observables[i] = connection.reactive().set(KEY, KEY);
        }

        Observable.merge(observables).toBlocking().last();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void reactiveSetBatchFlush() throws Exception {

        connection.setAutoFlushCommands(false);

        for (int i = 0; i < BATCH_SIZE; i++) {
            observables[i] = connection.reactive().set(KEY, KEY);
        }

        Observable.merge(observables).doOnSubscribe(() -> {

            connection.flushCommands();
            connection.setAutoFlushCommands(true);

        }).toBlocking().last();
    }

    public static void main(String[] args) throws InterruptedException {

        RedisClientBenchmark b = new RedisClientBenchmark();
        b.setup();

        Thread.sleep(10000);
        while (true) {
            b.syncList();
        }
    }
}
