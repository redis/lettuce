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
package io.lettuce.core.support;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class InjectedClient {

    @Inject
    public RedisClient redisClient;

    @Inject
    public RedisClusterClient redisClusterClient;

    @Inject
    @PersonDB
    public RedisClient qualifiedRedisClient;

    @Inject
    @PersonDB
    public RedisClusterClient qualifiedRedisClusterClient;

    private RedisCommands<String, String> connection;

    @PostConstruct
    public void postConstruct() {
        connection = redisClient.connect().sync();
    }

    public void pingRedis() {
        connection.ping();
    }

    @PreDestroy
    public void preDestroy() {
        if (connection != null) {
            connection.getStatefulConnection().close();
        }
    }

}
