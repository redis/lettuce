/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.HashImport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HashImportIntegrationTests;
import io.lettuce.test.condition.EnabledOnCommand;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the {@code HIMPORT} command-flow tests through the transactional API. {@code HIMPORT SET} is per-connection session
 * state and is not supported inside {@code MULTI}; the base flow is therefore overridden to assert the client-side rejection
 * instead.
 */
@Tag(INTEGRATION_TEST)
class HashImportTxCommandIntegrationTests extends HashImportIntegrationTests {

    @Inject
    HashImportTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

    @Test
    @Override
    @EnabledOnCommand("HIMPORT")
    public void himport() {

        HashImport<String> fieldset = HashImport.of("name", "email", "age");

        assertThatThrownBy(() -> redis.himportSet("u:1", fieldset, "alice", "a@x.com", "25"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @Override
    @EnabledOnCommand("HIMPORT")
    public void reconnectPreservesMultipleFieldsets() {

        HashImport<String> fieldset = HashImport.of("name", "email");

        assertThatThrownBy(() -> redis.himportSet("person:1", fieldset, "alice", "a@x.com"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
