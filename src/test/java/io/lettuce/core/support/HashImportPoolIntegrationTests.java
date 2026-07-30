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
 * Pooled-connection coverage for {@code HIMPORT}. Fieldsets are per-physical-connection session state, so within a pool they
 * survive a borrow/return cycle on the same connection, and preparing on one borrowed connection does not prepare another. A
 * command listener records {@code no such fieldset} failures to distinguish "the pooled connection kept its session" (zero
 * failures) from "the retry-once backstop re-established it" (one failure).
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
     * A fieldset prepared on a borrowed connection is still prepared when that connection is returned and re-borrowed: pooling
     * does not reset the {@code HIMPORT} session state.
     */
    @Test
    void preparedFieldsetSurvivesPoolReturnAndReborrow() throws Exception {

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
            assertThat(borrowed.sync().himportPrepare(fieldset)).isEqualTo("OK");
            assertThat(borrowed.sync().himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            pool.returnObject(borrowed);

            // Re-borrow the same physical connection (max total 1): its session state persisted across the borrow cycle.
            StatefulRedisConnection<String, String> reborrowed = pool.borrowObject();
            assertThat(reborrowed).isSameAs(borrowed);
            assertThat(reborrowed.sync().himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(reborrowed.sync().hget("u:2", "name")).isEqualTo("bob");
            pool.returnObject(reborrowed);

            // Zero failures: no re-prepare was needed, the pooled connection kept the fieldset prepared.
            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            pool.close();
            client.removeListener(listener);
        }
    }

    /**
     * Fieldsets are scoped to the physical connection, not the pool: preparing on one borrowed connection does not prepare
     * another. A {@code SET} on a connection that never prepared the fieldset first draws {@code no such fieldset} and is then
     * recovered by retry-once, keeping the pool usage transparent to the application.
     */
    @Test
    void fieldsetsArePerPooledConnection() throws Exception {

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

            // Prepare on c1 only; the SET on c1 hits an already-prepared connection.
            c1.sync().himportPrepare(fieldset);
            assertThat(c1.sync().himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            assertThat(noSuchFieldsetFailures).hasValue(0);

            // c2 never prepared this fieldset: the SET draws "no such fieldset" once (per-connection state), then retry-once
            // re-prepares on c2 and succeeds.
            assertThat(c2.sync().himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(noSuchFieldsetFailures).hasValue(1);
            assertThat(c2.sync().hget("u:2", "name")).isEqualTo("bob");

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
