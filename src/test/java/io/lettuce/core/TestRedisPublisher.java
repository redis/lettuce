package io.lettuce.core;

import java.util.function.Supplier;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * @author Mark Paluch
 */
public class TestRedisPublisher<K, V, T> extends RedisPublisher<K, V, T> {

    public TestRedisPublisher(RedisCommand<K, V, T> staticCommand, StatefulConnection<K, V> connection, boolean dissolve) {
        super(staticCommand, connection, dissolve, ImmediateEventExecutor.INSTANCE);
    }

    public TestRedisPublisher(Supplier<RedisCommand<K, V, T>> redisCommandSupplier, StatefulConnection<K, V> connection,
            boolean dissolve) {
        super(redisCommandSupplier, connection, dissolve, ImmediateEventExecutor.INSTANCE);
    }
}
