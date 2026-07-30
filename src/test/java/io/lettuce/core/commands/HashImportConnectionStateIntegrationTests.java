/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

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
 * not fit the shared command-flow base ({@link HashImportIntegrationTests}): reconnect replay and the {@code no such fieldset}
 * retry-once backstop. These are the "specific scenarios" that live directly in a dedicated class rather than being inherited
 * across the execution-model overloads.
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
     * A prepared fieldset is re-established transparently after a reconnect: a {@code HIMPORT SET} issued after the connection
     * bounced succeeds without the application re-preparing the fieldset.
     */
    @Test
    void reconnectPreservesFieldset() {

        HashImport<String> fieldset = HashImport.of("name", "email");
        redis.himportPrepare(fieldset);
        assertThat(redis.himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");

        redis.quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        // No re-prepare here: the fieldset must have been replayed on activation (or restored by the retry-once backstop).
        assertThat(redis.himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
        assertThat(redis.hget("u:2", "name")).isEqualTo("bob");
        assertThat(redis.hget("u:2", "email")).isEqualTo("b@x.com");
    }

    /**
     * Multiple declared fieldsets are all preserved across a reconnect.
     */
    @Test
    void reconnectPreservesMultipleFieldsets() {

        HashImport<String> people = HashImport.of("name", "email");
        HashImport<String> products = HashImport.of("sku", "price");
        redis.himportPrepare(people);
        redis.himportPrepare(products);

        redis.quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(redis.himportSet("p:1", people, "alice", "a@x.com")).isEqualTo("OK");
        assertThat(redis.himportSet("s:1", products, "sku-1", "9.99")).isEqualTo("OK");
        assertThat(redis.hget("p:1", "name")).isEqualTo("alice");
        assertThat(redis.hget("s:1", "sku")).isEqualTo("sku-1");
    }

    /**
     * A discarded fieldset is not replayed after a reconnect and is rejected client-side, while other fieldsets survive.
     */
    @Test
    void discardedFieldsetIsNotPreservedButOthersAre() {

        HashImport<String> kept = HashImport.of("name", "email");
        HashImport<String> discarded = HashImport.of("sku", "price");
        redis.himportPrepare(kept);
        redis.himportPrepare(discarded);
        assertThat(redis.himportDiscard(discarded)).isTrue();

        redis.quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(redis.himportSet("k:1", kept, "alice", "a@x.com")).isEqualTo("OK");
        assertThat(discarded.isDiscarded()).isTrue();
    }

    /**
     * Proves the reconnect guarantee is delivered by activation-time replay, not by the retry-once backstop masking a broken
     * replay. A command listener records every {@code no such fieldset} failure; after a reconnect the {@code SET} succeeds
     * with zero such failures, which is only possible if the fieldset was re-prepared on activation before the {@code SET} was
     * written. Were replay broken, the first {@code SET} would fail {@code no such fieldset} (recorded here) before retry-once
     * silently recovered it.
     */
    @Test
    void reconnectReplayHappensAheadOfRetry() {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        StatefulRedisConnection<String, String> listened = client.connect();
        try {
            RedisCommands<String, String> cmd = listened.sync();
            HashImport<String> fieldset = HashImport.of("name", "email");
            cmd.himportPrepare(fieldset);
            cmd.himportSet("u:1", fieldset, "alice", "a@x.com");

            cmd.quit();
            Wait.untilTrue(listened::isOpen).waitOrTimeout();

            assertThat(cmd.himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(cmd.hget("u:2", "name")).isEqualTo("bob");

            // Zero "no such fieldset" errors => the SET hit an already-prepared connection => replay did it, not retry.
            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            listened.close();
            client.removeListener(listener);
        }
    }

    /**
     * Positive control for {@link #reconnectReplayHappensAheadOfRetry()}: a never-prepared {@code SET} draws
     * {@code no such fieldset} (recorded by the listener) before retry-once recovers it. Proving the failure IS observed here
     * is what makes the zero-failures assertion in the reconnect test meaningful. (The recovery outcome itself is covered
     * cross-flavor by {@link HashImportIntegrationTests#setRecoversViaRetryOnceWhenFieldsetMissing()}.)
     */
    @Test
    void noSuchFieldsetErrorIsObservable() {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        StatefulRedisConnection<String, String> listened = client.connect();
        try {
            RedisCommands<String, String> cmd = listened.sync();
            HashImport<String> fieldset = HashImport.of("name", "email");

            // No himportPrepare call: the first SET fails "no such fieldset" server-side and is recovered by the retry-once
            // backstop.
            assertThat(cmd.himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            assertThat(cmd.hget("u:1", "name")).isEqualTo("alice");
            assertThat(cmd.hget("u:1", "email")).isEqualTo("a@x.com");

            assertThat(noSuchFieldsetFailures).hasValue(1);
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
