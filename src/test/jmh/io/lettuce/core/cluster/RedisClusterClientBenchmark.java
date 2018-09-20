/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.cluster;

import org.openjdk.jmh.annotations.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.test.settings.TestSettings;

/**
 * Benchmark for {@link RedisClusterClient}.
 * <ul>
 * <li>synchronous command execution</li>
 * <li>asynchronous command execution</li>
 * <li>asynchronous command execution with batching</li>
 * <li>asynchronous command execution with delayed flushing</li>
 * <li>reactive command execution</li>
 * <li>reactive command execution with batching</li>
 * <li>reactive command execution with delayed flushing</li>
 * </ul>
 *
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class RedisClusterClientBenchmark {

    private static final int BATCH_SIZE = 20;
    private static final byte[] KEY = "benchmark".getBytes();

    private RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<byte[], byte[]> connection;
    private RedisFuture commands[];
    private Mono monos[];

    @Setup
    public void setup() {

        redisClusterClient = RedisClusterClient.create(RedisURI.create(TestSettings.host(), TestSettings.port(900)));
        connection = redisClusterClient.connect(ByteArrayCodec.INSTANCE);
        commands = new RedisFuture[BATCH_SIZE];
        monos = new Mono[BATCH_SIZE];
    }

    @TearDown
    public void tearDown() {

        connection.close();
        redisClusterClient.shutdown();
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
    public void reactiveSet() {
        connection.reactive().set(KEY, KEY).block();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void reactiveSetBatch() {

        for (int i = 0; i < BATCH_SIZE; i++) {
            monos[i] = connection.reactive().set(KEY, KEY);
        }

        Flux.merge(monos).blockLast();
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void reactiveSetBatchFlush() {

        connection.setAutoFlushCommands(false);

        for (int i = 0; i < BATCH_SIZE; i++) {
            monos[i] = connection.reactive().set(KEY, KEY);
        }

        Flux.merge(monos).doOnSubscribe(subscription -> {

            connection.flushCommands();
            connection.setAutoFlushCommands(true);

        }).blockLast();
    }
}
