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
