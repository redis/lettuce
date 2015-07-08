package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class PipeliningTest extends AbstractCommandTest {

    @Test
    public void basic() throws Exception {

        RedisAsyncConnection<String, String> connection = client.connectAsync();
        connection.setAutoFlushCommands(false);

        int iterations = 1000;
        List<RedisFuture<?>> futures = triggerSet(connection, iterations);

        verifyNotExecuted(iterations);

        connection.flushCommands();

        LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));

        verifyExecuted(iterations);

        connection.close();
    }

    protected void verifyExecuted(int iterations) {
        for (int i = 0; i < iterations; i++) {
            assertThat(redis.get(key(i))).as("Key " + key(i) + " must be " + value(i)).isEqualTo(value(i));
        }
    }

    @Test
    public void setAutoFlushTrueDoesNotFlush() throws Exception {

        RedisAsyncConnection<String, String> connection = client.connectAsync();
        connection.setAutoFlushCommands(false);

        int iterations = 1000;
        List<RedisFuture<?>> futures = triggerSet(connection, iterations);

        verifyNotExecuted(iterations);

        connection.setAutoFlushCommands(true);

        verifyNotExecuted(iterations);

        connection.flushCommands();
        boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));
        assertThat(result).isTrue();

        connection.close();
    }

    protected void verifyNotExecuted(int iterations) {
        for (int i = 0; i < iterations; i++) {
            assertThat(redis.get(key(i))).as("Key " + key(i) + " must be null").isNull();
        }
    }

    protected List<RedisFuture<?>> triggerSet(RedisAsyncConnection<String, String> connection, int iterations) {
        List<RedisFuture<?>> futures = Lists.newArrayList();
        for (int i = 0; i < iterations; i++) {
            futures.add(connection.set(key(i), value(i)));
        }
        return futures;
    }

    protected String value(int i) {
        return value + "-" + i;
    }

    protected String key(int i) {
        return key + "-" + i;
    }
}
