/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.sentinel;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.resource.FastShutdown;

/**
 * @author Mark Paluch
 */
public abstract class AbstractSentinelTest extends TestSupport {

    protected static final String MASTER_ID = "mymaster";

    protected static RedisClient sentinelClient;
    protected RedisSentinelCommands<String, String> sentinel;

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(sentinelClient);
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinel().sync();
    }

    @AfterEach
    public void closeConnection() {
        if (sentinel != null) {
            sentinel.getStatefulConnection().close();
        }
    }
}
