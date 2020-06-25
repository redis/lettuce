/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.masterslave;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link NodeConnectionFactory} implementation that on {@link RedisClient}.
 *
 * @author Mark Paluch
 */
class RedisClientNodeConnectionFactory implements NodeConnectionFactory {

    private final RedisClient client;

    RedisClientNodeConnectionFactory(RedisClient client) {
        this.client = client;
    }

    @Override
    public <K, V> CompletableFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {
        return client.connectAsync(codec, redisURI).toCompletableFuture();
    }

}
