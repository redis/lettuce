// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.port;
import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.*;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;

import java.util.Arrays;

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
        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(1))
                .build());
        connection.debugSegfault();

        WaitFor.waitOrTimeout(() -> !connection.isOpen(), timeout(seconds(5)));
        assertThat(connection.isOpen()).isFalse();
    }

    /**
     * Executed in order: 2
     * 
     * @throws Exception
     */
    @Test
    public void migrate() throws Exception {
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
    @Ignore
    public void shutdown() throws Exception {

        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(2))
                .build());
        try {

            connection.shutdown(true);
            connection.shutdown(false);
            WaitFor.waitOrTimeout(() -> !connection.isOpen(), timeout(seconds(5)));

            assertThat(connection.isOpen()).isFalse();

        } finally {
            connection.close();
        }

    }

}
