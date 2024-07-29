/*
 * Copyright 2024, Redis Ltd. and Contributors
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

package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;

public class RedisContainerIntegrationTests {

    // TODO use https://java.testcontainers.org/supported_docker_environment/
    protected static RedisAsyncCommands<String, String> redis;

    protected static RedisClient redisClient;

    @BeforeAll
    static void setup() {
        RedisURI redisURI = RedisURI.Builder.redis("redis-19897.c55.eu-central-1-1.ec2.redns.redis-cloud.com").withPort(19897)
                .withPassword("9CH6niJKjHFzAiPtp9jvoI9OvErZ7urh").withTimeout(Duration.ofSeconds(30)).build();
        redisClient = RedisClient.create(redisURI);
        try (StatefulRedisConnection<String, String> connect = redisClient.connect()) {
            redis = connect.async();

        }

    }

    @AfterAll
    static void teardown() {
        redis.getStatefulConnection().close();
        redisClient.shutdown();
    }

}
