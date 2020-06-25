/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.pubsub.api.sync;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * Synchronous and thread-safe Redis PubSub API.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisPubSubCommands<K, V> extends RedisCommands<K, V> {

    /**
     * Listen for messages published to channels matching the given patterns.
     *
     * @param patterns the patterns
     */
    void psubscribe(K... patterns);

    /**
     * Stop listening for messages posted to channels matching the given patterns.
     *
     * @param patterns the patterns
     */
    void punsubscribe(K... patterns);

    /**
     * Listen for messages published to the given channels.
     *
     * @param channels the channels
     */
    void subscribe(K... channels);

    /**
     * Stop listening for messages posted to the given channels.
     *
     * @param channels the channels
     */
    void unsubscribe(K... channels);

    /**
     * @return the underlying connection.
     */
    StatefulRedisPubSubConnection<K, V> getStatefulConnection();

}
