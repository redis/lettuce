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
package io.lettuce.core.api.sync;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.json.JsonParser;

/**
 *
 * A complete synchronous and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Tihomir Mateev
 * @since 3.0
 */
public interface RedisCommands<K, V> extends BaseRedisCommands<K, V>, RedisAclCommands<K, V>, RedisClusterCommands<K, V>,
        RedisFunctionCommands<K, V>, RedisGeoCommands<K, V>, RedisHashCommands<K, V>, RedisHLLCommands<K, V>,
        RedisKeyCommands<K, V>, RedisListCommands<K, V>, RedisScriptingCommands<K, V>, RedisServerCommands<K, V>,
        RedisSetCommands<K, V>, RedisSortedSetCommands<K, V>, RedisStreamCommands<K, V>, RedisStringCommands<K, V>,
        RedisTransactionalCommands<K, V>, RedisJsonCommands<K, V>, RedisVectorSetCommands<K, V> {

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    String auth(CharSequence password);

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    String auth(String username, CharSequence password);

    /**
     * Change the selected database for the current Commands.
     *
     * @param db the database number
     * @return String simple-string-reply
     */
    String select(int db);

    /**
     * Swap two Redis databases, so that immediately all the clients connected to a given DB will see the data of the other DB,
     * and the other way around
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    String swapdb(int db1, int db2);

    /**
     * @return the underlying connection.
     * @since 6.2, will be removed with Lettuce 7 to avoid exposing the underlying connection.
     */
    @Deprecated
    StatefulRedisConnection<K, V> getStatefulConnection();

    /**
     * @return the currently configured instance of the {@link JsonParser}
     * @since 6.5
     */
    JsonParser getJsonParser();

}
