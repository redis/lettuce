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
package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.resource.DefaultRedisClient;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public abstract class AbstractRedisClientTest extends TestSupport {

    protected static RedisClient client;

    protected RedisCommands<String, String> redis;

    protected StatefulRedisConnection<String, String> statefulRedisConnection;

    @BeforeAll
    public static void setupClient() {
        client = DefaultRedisClient.get();
        client.setOptions(ClientOptions.create());
    }

    protected RedisCommands<String, String> connect() {
        statefulRedisConnection = client.connect();
        return statefulRedisConnection.sync();
    }

    @BeforeEach
    public void openConnection() {
        client.setOptions(ClientOptions.builder().build());
        redis = connect();
        boolean scriptRunning;
        do {

            scriptRunning = false;

            try {
                redis.flushall();
                redis.flushdb();
            } catch (RedisBusyException e) {
                scriptRunning = true;
                try {
                    redis.scriptKill();
                } catch (RedisException e1) {
                    // I know, it sounds crazy, but there is a possibility where one of the commands above raises BUSY.
                    // Meanwhile the script ends and a call to SCRIPT KILL says NOTBUSY.
                }
            }
        } while (scriptRunning);
    }

    @AfterEach
    public void closeConnection() {
        if (statefulRedisConnection != null) {
            statefulRedisConnection.close();
        }
    }

}
