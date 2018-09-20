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
package io.lettuce.core.dynamic;

import org.mockito.Mockito;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.batch.BatchSize;

/**
 * Benchmark for commands executed through {@link RedisCommandFactory}.
 *
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class RedisCommandFactoryBenchmark {

    private RedisCommandFactory redisCommandFactory;
    private RegularCommands regularCommands;
    private RedisAsyncCommandsImpl<String, String> asyncCommands;

    @Setup
    public void setup() {

        redisCommandFactory = new RedisCommandFactory(new MockStatefulConnection(EmptyRedisChannelWriter.INSTANCE));
        regularCommands = redisCommandFactory.getCommands(RegularCommands.class);

        asyncCommands = new RedisAsyncCommandsImpl<>(EmptyStatefulRedisConnection.INSTANCE, StringCodec.UTF8);
    }

    @Benchmark
    public void createRegularCommands() {
        redisCommandFactory.getCommands(RegularCommands.class);
    }

    @Benchmark
    public void createBatchCommands() {
        redisCommandFactory.getCommands(BatchCommands.class);
    }

    @Benchmark
    public void executeCommandInterfaceCommand() {
        regularCommands.set("key", "value");
    }

    @Benchmark
    public void executeAsyncCommand() {
        asyncCommands.set("key", "value");
    }

    interface RegularCommands extends Commands {

        RedisFuture<String> set(String key, String value);
    }

    @BatchSize(10)
    private
    interface BatchCommands extends Commands {

        void set(String key, String value);
    }

    static class MockStatefulConnection extends EmptyStatefulRedisConnection {

        RedisCommands sync;
        RedisReactiveCommands reactive;

        MockStatefulConnection(RedisChannelWriter writer) {
            super(writer);

            sync = Mockito.mock(RedisCommands.class);
            reactive = (RedisReactiveCommands) Mockito.mock(AbstractRedisReactiveCommands.class, Mockito.withSettings()
                    .extraInterfaces(RedisReactiveCommands.class));
        }

        @Override
        public RedisCommands sync() {
            return sync;
        }

        @Override
        public RedisReactiveCommands reactive() {
            return reactive;
        }
    }
}
