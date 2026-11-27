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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HashImport;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for the {@code HIMPORT} command family. The full flow — import, lazy prepare on first use, reconnect,
 * cleanup on close, and validation — is written once here against the synchronous {@link RedisCommands} API and re-run through
 * the cluster / reactive / transactional / RESP2 execution paths by the subclasses in this hierarchy. Subclasses override
 * {@link #keyFor(int)} where needed so the shared flow spans nodes on Redis Cluster.
 * <p>
 * Two tests here cover {@link HashImport#close()} against imports that are still on their way to the wire: {@code close()} runs
 * on the caller's thread, so it must withhold cleanup rather than reject imports the enclosing block already issued. The
 * synchronous flow cannot cover that, because every import has completed before the block exits.
 *
 * @see io.lettuce.core.cluster.commands.HashImportClusterCommandIntegrationTests
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
     * Key for the given index. Distinct indexes yield distinct keys. The cluster suite overrides this to route each index to a
     * different master, so the shared flow spans slots and nodes on Redis Cluster without any cluster-specific test code.
     */
    protected String keyFor(int index) {
        return "himport:" + index;
    }

    /**
     * Full managed lifecycle: create several hashes from a fieldset sending only values (the field names are declared
     * transparently on first use), then close it. After close the fieldset is rejected for reuse, but the connection stays
     * usable for a fresh fieldset — closing releases the fieldset without disturbing the connection.
     */
    @Test
    public void himport() {

        HashImport<String> fieldset = HashImport.of("name", "email", "age");

        // Keys span nodes on cluster (see keyFor), so this flow imports to several masters, each self-preparing the fieldset.
        String k1 = keyFor(0);
        String k2 = keyFor(1);
        String k3 = keyFor(2);

        assertThat(redis.himportSet(k1, fieldset, "alice", "a@x.com", "25")).isEqualTo("OK");
        assertThat(redis.himportSet(k2, fieldset, "bob", "b@x.com", "30")).isEqualTo("OK");

        assertThat(redis.hget(k1, "name")).isEqualTo("alice");
        assertThat(redis.hget(k1, "email")).isEqualTo("a@x.com");
        assertThat(redis.hget(k1, "age")).isEqualTo("25");
        assertThat(redis.hget(k2, "name")).isEqualTo("bob");

        // The created key is an ordinary hash.
        assertThat(redis.hlen(k1)).isEqualTo(3);

        // Cleanup: closing discards the fieldset. Reuse is rejected client-side...
        fieldset.close();
        assertThatThrownBy(() -> redis.himportSet(k3, fieldset, "carol", "c@x.com", "40"))
                .isInstanceOf(IllegalStateException.class);

        for (String importedKey : new String[] { k1, k2 }) {
            redis.exists(importedKey);
            assertThatThrownBy(() -> rawHimportSet(importedKey, fieldset.name(), "carol", "c@x.com", "40"))
                    .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("no such fieldset");
        }

        // ...and the connection remains fully usable for a new fieldset afterwards.
        HashImport<String> other = HashImport.of("sku", "price");
        assertThat(redis.himportSet(k3, other, "sku-1", "9.99")).isEqualTo("OK");
        assertThat(redis.hget(k3, "sku")).isEqualTo("sku-1");
        other.close();
    }

    /**
     * Dispatch a raw {@code HIMPORT SET} that carries the fieldset name directly. Unlike {@code himportSet}, this is not a
     * {@link io.lettuce.core.HashImportSetCommand}, so the outbound handler does not auto-inject a {@code PREPARE} for it —
     * letting a test observe whether the fieldset is still prepared on the server.
     */
    private String rawHimportSet(String key, String fieldsetName, String... values) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandType.SET).addKey(key)
                .addKey(fieldsetName);
        for (String value : values) {
            args.addValue(value);
        }
        return redis.dispatch(CommandType.HIMPORT, new StatusOutput<>(StringCodec.UTF8), args);
    }

    /**
     * Multiple fieldsets keep working across a reconnect. Both are used, the connection is bounced with {@code QUIT}, and
     * further imports on both fieldsets succeed with no explicit prepare — the reconnected connection re-injects each
     * fieldset's {@code PREPARE} lazily ahead of the {@code SET}. The distinct keys land on different slots, so over Redis
     * Cluster this also exercises per-node re-prepare across nodes. In v2 there is no retry backstop, so a broken re-prepare
     * would make the post-reconnect {@code himportSet} fail rather than silently recover — the {@code OK} assertions are the
     * discriminator. (The transactional path overrides this, as {@code HIMPORT SET} is not supported within {@code MULTI}.)
     */
    @Test
    public void reconnectPreservesMultipleFieldsets() {

        HashImport<String> people = HashImport.of("name", "email");
        HashImport<String> products = HashImport.of("sku", "price");

        // Two fieldsets on keys that live on different nodes (see keyFor), so on cluster the reconnect exercises re-prepare
        // across nodes.
        String peopleKey = keyFor(0);
        String productKey = keyFor(1);

        // Prime both fieldsets on the current connection(s).
        assertThat(redis.himportSet(peopleKey, people, "alice", "a@x.com")).isEqualTo("OK");
        assertThat(redis.himportSet(productKey, products, "sku-1", "9.99")).isEqualTo("OK");

        // Bounce the connection; auto-reconnect buffers and replays the following imports.
        redis.quit();

        // No re-prepare: the reconnected connection re-injects each fieldset's PREPARE lazily ahead of these imports.
        assertThat(redis.himportSet(peopleKey, people, "bob", "b@x.com")).isEqualTo("OK");
        assertThat(redis.himportSet(productKey, products, "sku-2", "19.99")).isEqualTo("OK");
        assertThat(redis.hget(peopleKey, "name")).isEqualTo("bob");
        assertThat(redis.hget(productKey, "sku")).isEqualTo("sku-2");
    }

    /**
     * The number of values must match the fieldset size; a mismatch is rejected client-side before dispatch. This validation is
     * eager across execution models (it runs before the {@code MULTI} check), so it holds on the transactional path too.
     */
    @Test
    public void himportSetRejectsWrongValueCount() {

        HashImport<String> fieldset = HashImport.of("name", "email");

        assertThatThrownBy(() -> redis.himportSet("u:1", fieldset, "only-one-value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
