/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.inject.New;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HashImport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Standalone connection-session-state scenarios for {@code HIMPORT} that require raw connection and client control and so do
 * not fit the shared command-flow base ({@link HashImportIntegrationTests}): lazy re-prepare across reconnects. These are the
 * "specific scenarios" that live directly in a dedicated class rather than being inherited across the execution-model
 * overloads.
 * <p>
 * A {@link CommandListener} records every {@code no such fieldset} failure. Because v2 has no retry-once backstop, a broken
 * lazy prepare would make {@code himportSet} itself fail loudly; the zero-failures assertion is an additional guarantee that no
 * un-prepared {@code SET} ever reached the wire.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("HIMPORT")
class HashImportConnectionStateIntegrationTests {

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;

    private final RedisCommands<String, String> redis;

    @Inject
    HashImportConnectionStateIntegrationTests(@New RedisClient client,
            @New StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
        this.redis = connection.sync();
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    /**
     * A fieldset is re-established transparently after a reconnect: a {@code HIMPORT SET} issued after the connection bounced
     * succeeds without any explicit prepare, and no {@code no such fieldset} error ever reaches the wire — the reconnected
     * connection re-injects the {@code PREPARE} lazily ahead of the {@code SET}.
     */
    @Test
    void reconnectPreservesFieldset() {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);
        try {
            HashImport<String> fieldset = HashImport.of("name", "email");
            assertThat(redis.himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");

            redis.quit();
            Wait.untilTrue(connection::isOpen).waitOrTimeout();

            // No re-prepare here: the reconnected connection re-injects PREPARE lazily ahead of this SET.
            assertThat(redis.himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(redis.hget("u:2", "name")).isEqualTo("bob");
            assertThat(redis.hget("u:2", "email")).isEqualTo("b@x.com");

            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            client.removeListener(listener);
        }
    }

    /**
     * Multiple fieldsets are all re-established across a reconnect, each lazily re-prepared on first use afterwards.
     */
    @Test
    void reconnectPreservesMultipleFieldsets() {

        HashImport<String> people = HashImport.of("name", "email");
        HashImport<String> products = HashImport.of("sku", "price");
        assertThat(redis.himportSet("p:0", people, "seed", "s@x.com")).isEqualTo("OK");
        assertThat(redis.himportSet("s:0", products, "sku-0", "1.00")).isEqualTo("OK");

        redis.quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(redis.himportSet("p:1", people, "alice", "a@x.com")).isEqualTo("OK");
        assertThat(redis.himportSet("s:1", products, "sku-1", "9.99")).isEqualTo("OK");
        assertThat(redis.hget("p:1", "name")).isEqualTo("alice");
        assertThat(redis.hget("s:1", "sku")).isEqualTo("sku-1");
    }

    /**
     * A closed fieldset is rejected client-side and can no longer be used for imports, while other fieldsets keep working
     * across a reconnect.
     */
    @Test
    void closedFieldsetIsRejectedButOthersWork() {

        HashImport<String> kept = HashImport.of("name", "email");
        HashImport<String> closed = HashImport.of("sku", "price");
        assertThat(redis.himportSet("k:0", kept, "seed", "s@x.com")).isEqualTo("OK");
        assertThat(redis.himportSet("s:0", closed, "sku-0", "1.00")).isEqualTo("OK");
        closed.close();

        redis.quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(redis.himportSet("k:1", kept, "alice", "a@x.com")).isEqualTo("OK");
        assertThat(closed.isDiscarded()).isTrue();
        assertThatThrownBy(() -> redis.himportSet("s:1", closed, "sku-1", "9.99")).isInstanceOf(IllegalStateException.class);
    }

    /**
     * The very first {@code HIMPORT SET} on a fresh connection carries no explicit prepare, yet no {@code no such fieldset}
     * error is ever observed — proving the outbound handler injects the {@code PREPARE} ahead of the first {@code SET}.
     */
    @Test
    void firstSetOnFreshConnectionInjectsPrepare() {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        StatefulRedisConnection<String, String> listened = client.connect();
        try {
            RedisCommands<String, String> cmd = listened.sync();
            HashImport<String> fieldset = HashImport.of("name", "email");

            // No explicit prepare: the handler injects HIMPORT PREPARE ahead of this first SET.
            assertThat(cmd.himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            assertThat(cmd.hget("u:1", "name")).isEqualTo("alice");
            assertThat(cmd.hget("u:1", "email")).isEqualTo("a@x.com");

            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            listened.close();
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
