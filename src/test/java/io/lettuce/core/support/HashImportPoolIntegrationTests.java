/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.support;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.HashImport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * Pooled-connection coverage for {@code HIMPORT}. Fieldsets are per-physical-connection session state, so within a pool a
 * fieldset used on a borrowed connection stays prepared across a borrow/return cycle on that same connection, and each pooled
 * connection self-prepares independently on first use. A command listener records {@code no such fieldset} failures: a zero
 * count proves the outbound handler always injected the {@code PREPARE} ahead of the {@code SET} on each connection.
 */
@Tag(INTEGRATION_TEST)
@EnabledOnCommand("HIMPORT")
class HashImportPoolIntegrationTests extends TestSupport {

    private static RedisClient client;

    @BeforeAll
    static void setupClient() {
        client = RedisClient.create(TestClientResources.create(), RedisURI.Builder.redis(host, port).build());
    }

    @AfterAll
    static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    /**
     * A fieldset first used on a borrowed connection stays prepared when that connection is returned and re-borrowed: pooling
     * does not reset the {@code HIMPORT} session state, so no re-prepare is needed on the second use.
     */
    @Test
    void fieldsetStaysPreparedAcrossPoolReturnAndReborrow() throws Exception {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(1);
        // wrapConnections=false so borrowObject() returns the raw connection (not a close-intercepting proxy), letting us
        // assert the same physical connection is reused.
        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), config, false);

        try {
            HashImport<String> fieldset = HashImport.of("name", "email");

            StatefulRedisConnection<String, String> borrowed = pool.borrowObject();
            borrowed.sync().flushall();
            // First use auto-prepares the fieldset on this connection.
            assertThat(borrowed.sync().himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            pool.returnObject(borrowed);

            // Re-borrow the same physical connection (max total 1): its session state persisted across the borrow cycle.
            StatefulRedisConnection<String, String> reborrowed = pool.borrowObject();
            assertThat(reborrowed).isSameAs(borrowed);
            assertThat(reborrowed.sync().himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(reborrowed.sync().hget("u:2", "name")).isEqualTo("bob");
            pool.returnObject(reborrowed);

            // Zero failures: the pooled connection kept the fieldset prepared across the borrow cycle.
            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            pool.close();
            client.removeListener(listener);
        }
    }

    /**
     * Each pooled connection self-prepares independently: a fieldset first used on one borrowed connection works, and using the
     * same fieldset on a different borrowed connection also works with no {@code no such fieldset} error, because that
     * connection's own outbound handler injects the {@code PREPARE} ahead of its first {@code SET}.
     */
    @Test
    void eachPooledConnectionSelfPrepares() throws Exception {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(2);
        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), config, false);

        try {
            StatefulRedisConnection<String, String> c1 = pool.borrowObject();
            StatefulRedisConnection<String, String> c2 = pool.borrowObject();
            assertThat(c1).isNotSameAs(c2);
            c1.sync().flushall();

            HashImport<String> fieldset = HashImport.of("name", "email");

            // c1 self-prepares on first use.
            assertThat(c1.sync().himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");

            // c2 never used this fieldset, yet its own handler injects PREPARE ahead of the SET, so it succeeds with no error.
            assertThat(c2.sync().himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(c2.sync().hget("u:2", "name")).isEqualTo("bob");

            assertThat(noSuchFieldsetFailures).hasValue(0);

            pool.returnObject(c1);
            pool.returnObject(c2);
        } finally {
            pool.close();
            client.removeListener(listener);
        }
    }

    private static CommandListener countingNoSuchFieldset(AtomicInteger counter) {
        return new CommandListener() {

            @Override
            public void commandFailed(CommandFailedEvent event) {
                Throwable cause = event.getCause();
                if (cause != null && cause.getMessage() != null
                        && cause.getMessage().toLowerCase().contains("no such fieldset")) {
                    counter.incrementAndGet();
                }
            }

        };
    }

}
