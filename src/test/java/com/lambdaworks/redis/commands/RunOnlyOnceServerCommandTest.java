// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.TestSettings.host;
import static com.lambdaworks.redis.TestSettings.port;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisURI;

public class RunOnlyOnceServerCommandTest extends AbstractRedisClientTest {

    /**
     * this test causes a stop of the redis. This means, you cannot repeat the test without restarting your redis.
     * 
     * @throws Exception
     */
    @Test
    public void shutdown() throws Exception {

        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(4))
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

    @Test
    public void migrate() throws Exception {
        redis.set(key, value);

        String result = redis.migrate("localhost", port + 1, key, 0, 10);
        assertThat(result).isEqualTo("OK");
    }

    /**
     * this test causes a stop of the redis. This means, you cannot repeat the test without restarting your redis.
     *
     * @throws Exception
     */
    @Test
    public void debugSegfault() throws Exception {
        final RedisAsyncConnection<String, String> connection = client.connectAsync(RedisURI.Builder.redis(host(), port(3))
                .build());
        connection.debugSegfault();
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return !connection.isOpen();
            }
        }, timeout(seconds(5)));
        assertThat(connection.isOpen()).isFalse();
    }
}
