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
package io.lettuce.core.api.reactive;

import reactor.core.publisher.Mono;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;

/**
 * A complete reactive and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
public interface RedisReactiveCommands<K, V> extends BaseRedisReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V>,
        RedisGeoReactiveCommands<K, V>, RedisHashReactiveCommands<K, V>, RedisHLLReactiveCommands<K, V>,
        RedisKeyReactiveCommands<K, V>, RedisListReactiveCommands<K, V>, RedisScriptingReactiveCommands<K, V>,
        RedisServerReactiveCommands<K, V>, RedisSetReactiveCommands<K, V>, RedisSortedSetReactiveCommands<K, V>,
        RedisStreamReactiveCommands<K, V>, RedisStringReactiveCommands<K, V>, RedisTransactionalReactiveCommands<K, V> {

    /**
     * Authenticate to the server.
     *
     * @param password the password.
     * @return String simple-string-reply.
     */
    Mono<String> auth(String password);

    /**
     * Change the selected database for the current connection.
     *
     * @param db the database number.
     * @return String simple-string-reply.
     */
    Mono<String> select(int db);

    /**
     * Swap two Redis databases, so that immediately all the clients connected to a given DB will see the data of the other DB,
     * and the other way around.
     *
     * @param db1 the first database number.
     * @param db2 the second database number.
     * @return String simple-string-reply.
     */
    Mono<String> swapdb(int db1, int db2);

    /**
     * @return the underlying connection.
     */
    StatefulRedisConnection<K, V> getStatefulConnection();

}
