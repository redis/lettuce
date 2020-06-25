/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
