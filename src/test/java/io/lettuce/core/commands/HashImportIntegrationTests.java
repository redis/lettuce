/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HashImport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for the {@code HIMPORT} command family. The command-flow coverage lives here once, against the synchronous
 * {@link RedisCommands} API, and is re-run through the cluster / reactive / transactional / RESP2 execution paths by the
 * subclasses in this hierarchy. Connection-session-state scenarios that require raw connection control (reconnect replay, the
 * retry-once backstop, cluster node bouncing) live in dedicated classes instead, as they do not fit the shared facade.
 *
 * @see io.lettuce.core.cluster.commands.HashImportClusterCommandIntegrationTests
 * @see io.lettuce.core.commands.HashImportConnectionStateIntegrationTests
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("HIMPORT")
public class HashImportIntegrationTests {

    protected final RedisCommands<String, String> redis;

    @Inject
    protected HashImportIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    /**
     * Full managed flow: prepare a fieldset once, create several hashes sending only values, then discard.
     */
    @Test
    public void himport() {

        HashImport<String> fieldset = HashImport.of("name", "email", "age");

        assertThat(redis.himportPrepare(fieldset)).isEqualTo("OK");
        assertThat(redis.himportSet("u:1", fieldset, "alice", "a@x.com", "25")).isEqualTo("OK");
        assertThat(redis.himportSet("u:2", fieldset, "bob", "b@x.com", "30")).isEqualTo("OK");

        assertThat(redis.hget("u:1", "name")).isEqualTo("alice");
        assertThat(redis.hget("u:1", "email")).isEqualTo("a@x.com");
        assertThat(redis.hget("u:1", "age")).isEqualTo("25");
        assertThat(redis.hget("u:2", "name")).isEqualTo("bob");

        // The created key is an ordinary hash.
        assertThat(redis.hlen("u:1")).isEqualTo(3);

        assertThat(redis.himportDiscard(fieldset)).isTrue();
        assertThat(redis.himportDiscard(fieldset)).isFalse();
    }

    /**
     * The retry-once backstop transparently establishes a fieldset the executing connection has not prepared: a
     * {@code HIMPORT SET} for a never-prepared fieldset first draws {@code no such fieldset} server-side, then succeeds after
     * an automatic re-prepare. This holds uniformly across execution models (the transactional path overrides it, as
     * {@code HIMPORT SET} is not supported within {@code MULTI}).
     */
    @Test
    public void setRecoversViaRetryOnceWhenFieldsetMissing() {

        HashImport<String> fieldset = HashImport.of("name", "email");

        assertThat(redis.himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
        assertThat(redis.hget("u:1", "name")).isEqualTo("alice");
        assertThat(redis.hget("u:1", "email")).isEqualTo("a@x.com");
    }

}
