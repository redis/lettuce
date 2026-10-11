/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HashImport;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for {@code HIMPORT} over the asynchronous API, where {@link HashImport#close()} runs on the caller's thread
 * while the imports it must not disturb are still on their way to the wire. The synchronous suite in
 * {@link HashImportIntegrationTests} cannot cover this: there, every import has completed before the enclosing block exits.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("HIMPORT")
class HashImportAsyncIntegrationTests {

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    HashImportAsyncIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        connection.sync().flushall();
    }

    /**
     * The idiom from the class documentation, over the asynchronous API: imports are issued inside a try-with-resources block
     * and awaited after it. All of them must succeed — {@code close()} discards the fieldset, it does not cancel work already
     * issued against it.
     */
    @Test
    void closeDoesNotCancelImportsIssuedInsideTheBlock() {

        List<RedisFuture<String>> futures = new ArrayList<>();

        try (HashImport<String> fieldset = HashImport.of("name", "email")) {
            for (int i = 0; i < 3; i++) {
                futures.add(connection.async().himportSet("u:" + i, fieldset, "name-" + i, "mail-" + i));
            }
        }

        for (RedisFuture<String> future : futures) {
            assertThat(TestFutures.getOrTimeout(future)).isEqualTo("OK");
        }

        assertThat(connection.sync().hget("u:2", "name")).isEqualTo("name-2");
    }

    /**
     * The same flow with auto-flush disabled, which removes the timing dependency: nothing has reached the channel by the time
     * the block exits, so this deterministically exercises a {@code close()} that observes no prepared connection at all.
     */
    @Test
    void closeDoesNotCancelImportsStillBufferedByDisabledAutoFlush() {

        List<RedisFuture<String>> futures = new ArrayList<>();

        connection.setAutoFlushCommands(false);
        try {
            try (HashImport<String> fieldset = HashImport.of("sku", "price")) {
                for (int i = 0; i < 3; i++) {
                    futures.add(connection.async().himportSet("p:" + i, fieldset, "sku-" + i, "9.9" + i));
                }
            }

            connection.flushCommands();

            for (RedisFuture<String> future : futures) {
                assertThat(TestFutures.getOrTimeout(future)).isEqualTo("OK");
            }
        } finally {
            connection.setAutoFlushCommands(true);
        }

        assertThat(connection.sync().hget("p:1", "sku")).isEqualTo("sku-1");
    }

}
