/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

package io.lettuce.core.failover;

import java.util.Collection;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.BaseRedisClient;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public interface MultiDbClient extends BaseRedisClient {

    public static MultiDbClient create(Collection<DatabaseConfig> databaseConfigs) {
        if (databaseConfigs == null || databaseConfigs.isEmpty()) {
            throw new IllegalArgumentException("Database configs must not be empty");
        }
        return new MultiDbClientImpl(databaseConfigs);
    }

    public static MultiDbClient create(ClientResources resources, Collection<DatabaseConfig> databaseConfigs) {
        if (resources == null) {
            throw new IllegalArgumentException("Client resources must not be null");
        }
        if (databaseConfigs == null || databaseConfigs.isEmpty()) {
            throw new IllegalArgumentException("Database configs must not be empty");
        }
        return new MultiDbClientImpl(resources, databaseConfigs);
    }

    /**
     * Get the configured Redis URIs.
     *
     * @return the configured Redis URIs
     */
    Collection<RedisURI> getRedisURIs();

    /**
     * Open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection
     */
    <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec);

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    StatefulRedisMultiDbConnection<String, String> connect();

    /**
     * Open a new pub/sub connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful pub/sub connection
     */
    <K, V> StatefulRedisMultiDbPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec);

    /**
     * Open a new pub/sub connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful pub/sub connection
     */
    StatefulRedisMultiDbPubSubConnection<String, String> connectPubSub();

}
