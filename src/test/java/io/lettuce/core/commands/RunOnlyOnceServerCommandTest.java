/*
 * Copyright 2011-2016 the original author or authors.
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
package io.lettuce.core.commands;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static io.lettuce.core.TestSettings.host;
import static io.lettuce.core.TestSettings.port;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.code.tempusfugit.temporal.WaitFor;
import io.lettuce.CanConnect;
import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.MigrateArgs;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSettings;
import io.lettuce.core.api.async.RedisAsyncCommands;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunOnlyOnceServerCommandTest extends AbstractRedisClientTest {

    /**
     * Executed in order: 1 this test causes a stop of the redis. This means, you cannot repeat the test without restarting your
     * redis.
     *
     * @throws Exception
     */
    @Test
    public void debugSegfault() throws Exception {

        assumeTrue(CanConnect.to(host(), port(1)));

        final RedisAsyncCommands<String, String> commands = client.connect(RedisURI.Builder.redis(host(), port(1))
                .build()).async();
        try {
            commands.debugSegfault();

            WaitFor.waitOrTimeout(() -> !commands.getStatefulConnection().isOpen(), timeout(seconds(5)));
            assertThat(commands.getStatefulConnection().isOpen()).isFalse();
        } finally {
            commands.getStatefulConnection().close();
        }
    }

    /**
     * Executed in order: 2
     *
     * @throws Exception
     */
    @Test
    public void migrate() throws Exception {

        assumeTrue(CanConnect.to(host(), port(2)));

        redis.set(key, value);

        String result = redis.migrate("localhost", TestSettings.port(2), key, 0, 10);
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Executed in order: 3
     *
     * @throws Exception
     */
    @Test
    public void migrateCopyReplace() throws Exception {

        assumeTrue(CanConnect.to(host(), port(2)));

        redis.set(key, value);
        redis.set("key1", value);
        redis.set("key2", value);

        String result = redis.migrate("localhost", TestSettings.port(2), 0, 10, MigrateArgs.Builder.keys(key).copy().replace());
        assertThat(result).isEqualTo("OK");

        result = redis.migrate("localhost", TestSettings.port(2), 0, 10, MigrateArgs.Builder.keys(Arrays.asList("key1", "key2")).replace());
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Executed in order: 4 this test causes a stop of the redis. This means, you cannot repeat the test without restarting your
     * redis.
     *
     * @throws Exception
     */
    @Test
    public void shutdown() throws Exception {

        assumeTrue(CanConnect.to(host(), port(2)));

        final RedisAsyncCommands<String, String> commands = client.connect(RedisURI.Builder.redis(host(), port(2))
                .build()).async();
        try {

            commands.shutdown(true);
            commands.shutdown(false);
            WaitFor.waitOrTimeout(() -> !commands.getStatefulConnection().isOpen(), timeout(seconds(5)));

            assertThat(commands.getStatefulConnection().isOpen()).isFalse();

        } finally {
            commands.getStatefulConnection().close();
        }
    }
}
