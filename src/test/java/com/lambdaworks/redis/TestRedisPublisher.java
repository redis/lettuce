package com.lambdaworks.redis;

import java.util.function.Supplier;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class TestRedisPublisher<K, V, T> extends RedisPublisher<K, V, T> {

    public TestRedisPublisher(RedisCommand<K, V, T> staticCommand, StatefulConnection<K, V> connection, boolean dissolve) {
        super(staticCommand, connection, dissolve);
    }

    public TestRedisPublisher(Supplier<RedisCommand<K, V, T>> redisCommandSupplier, StatefulConnection<K, V> connection,
            boolean dissolve) {
        super(redisCommandSupplier, connection, dissolve);
    }
}
