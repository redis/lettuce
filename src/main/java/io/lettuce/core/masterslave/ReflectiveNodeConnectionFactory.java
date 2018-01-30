/*
 * Copyright 2016-2018 the original author or authors.
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
package io.lettuce.core.masterslave;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link NodeConnectionFactory} implementation that operates via reflection on {@link RedisClient}. Using reflection does not
 * require utility methods to leak on the API.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
class ReflectiveNodeConnectionFactory implements NodeConnectionFactory {

    private final Method connectToNodeAsync;
    private final RedisClient client;

    protected ReflectiveNodeConnectionFactory(RedisClient client) {

        this.client = client;

        try {
            connectToNodeAsync = RedisClient.class.getDeclaredMethod("connectStandaloneAsync", RedisCodec.class,
                    RedisURI.class);
            connectToNodeAsync.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <K, V> CompletableFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {
        try {
            return (CompletableFuture<StatefulRedisConnection<K, V>>) connectToNodeAsync.invoke(client, codec, redisURI);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {

            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new IllegalStateException(e.getCause());
        }
    }
}
