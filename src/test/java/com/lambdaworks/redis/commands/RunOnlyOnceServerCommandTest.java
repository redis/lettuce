/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.port;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.lambdaworks.CanConnect;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.*;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunOnlyOnceServerCommandTest extends AbstractRedisClientTest {

    /**
     * Executed in order: 1 this test causes a stop of the redis. This means, you cannot repeat the test without restarting your
     * redis.
     */
    @Test
    public void debugSegfault() {

        assumeTrue(CanConnect.to(host(), port(1)));

        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(1))
                .build());
        try {
            connection.debugSegfault();

            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
            assertThat(connection.isOpen()).isFalse();
        } finally {
            connection.close();
        }
    }

    /**
     * Executed in order: 2
     */
    @Test
    public void migrate() {

        assumeTrue(CanConnect.to(host(), port(2)));

        redis.set(key, value);

        String result = redis.migrate("localhost", TestSettings.port(2), key, 0, 10);
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Executed in order: 3
     */
    @Test
    public void migrateCopyReplace() {

        assumeTrue(CanConnect.to(host(), port(2)));

        redis.set(key, value);
        redis.set("key1", value);
        redis.set("key2", value);

        String result = redis.migrate("localhost", TestSettings.port(2), 0, 10, MigrateArgs.Builder.keys(key).copy().replace());
        assertThat(result).isEqualTo("OK");

        result = redis.migrate("localhost", TestSettings.port(2), 0, 10, MigrateArgs.Builder
                .keys(Arrays.asList("key1", "key2")).replace());
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Executed in order: 4 this test causes a stop of the redis. This means, you cannot repeat the test without restarting your
     * redis.
     */
    @Test
    public void shutdown() {

        assumeTrue(CanConnect.to(host(), port(2)));

        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(2))
                .build());
        try {

            connection.shutdown(true);
            connection.shutdown(false);
            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            assertThat(connection.isOpen()).isFalse();

        } finally {
            connection.close();
        }
    }
}
