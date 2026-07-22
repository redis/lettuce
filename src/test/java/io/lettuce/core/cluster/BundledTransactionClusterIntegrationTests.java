/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.LettuceExtension;

/**
 * Integration tests for bundled transactions in Redis Cluster.
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
public class BundledTransactionClusterIntegrationTests {

    private final RedisClusterClient clusterClient;

    private final StatefulRedisClusterConnection<String, String> connection;

    private final RedisAdvancedClusterCommands<String, String> sync;

    @Inject
    protected BundledTransactionClusterIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {
        this.clusterClient = clusterClient;
        this.connection = connection;
        this.sync = connection.sync();
    }

    @BeforeEach
    void setUp() {
        sync.flushall();
    }

    @Test
    void sameSlotTransactionSucceeds() {
        // Keys with same hash tag map to same slot
        String key1 = "{user1}:name";
        String key2 = "{user1}:email";
        String key3 = "{user1}:age";

        TransactionBuilder<String, String> builder = connection.transaction();
        builder.queue().set(key1, "John");
        builder.queue().set(key2, "john@example.com");
        builder.queue().set(key3, "30");
        builder.queue().mget(key1, key2, key3);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(4);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((String) result.get(1)).isEqualTo("OK");
        assertThat((String) result.get(2)).isEqualTo("OK");
    }

    @Test
    void crossSlotTransactionFailsFast() {
        // Keys without hash tags likely map to different slots
        String key1 = "user:1:name";
        String key2 = "product:1:name";

        TransactionBuilder<String, String> builder = connection.transaction();
        builder.queue().set(key1, "John");

        // Adding a key from different slot should throw
        assertThatThrownBy(() -> builder.queue().set(key2, "Widget")).isInstanceOf(RedisException.class)
                .hasMessageContaining("CROSSSLOT");
    }

    @Test
    void watchWithSameSlotKeys() {
        String key1 = "{txn}:counter";
        String key2 = "{txn}:status";

        sync.set(key1, "0");
        sync.set(key2, "pending");

        // WATCH keys must be in same slot as transaction keys
        TransactionBuilder<String, String> builder = connection.transaction(key1);
        builder.queue().incr(key1);
        builder.queue().set(key2, "completed");

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(2);
        assertThat((Long) result.get(0)).isEqualTo(1L);
    }

    @Test
    void asyncTransactionInCluster() throws Exception {
        String key = "{async}:test";

        TransactionBuilder<String, String> builder = connection.transaction();
        builder.queue().set(key, "value");
        builder.queue().get(key);

        RedisFuture<TransactionResult> future = builder.executeAsync();
        TransactionResult result = future.get(5, TimeUnit.SECONDS);

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(2);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((String) result.get(1)).isEqualTo("value");
    }

    @Test
    void transactionRoutesToCorrectNode() {
        // Use hash tags to ensure predictable slot
        String key1 = "{node1}:a";
        String key2 = "{node1}:b";

        sync.set(key1, "initial");

        TransactionBuilder<String, String> builder = connection.transaction();
        builder.queue().set(key1, "updated");
        builder.queue().set(key2, "new");
        builder.queue().get(key1);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(3);

        // Verify data persisted correctly
        assertThat(sync.get(key1)).isEqualTo("updated");
        assertThat(sync.get(key2)).isEqualTo("new");
    }

    @Test
    void transactionWithWatchSucceedsWhenUnmodified() {
        // In bundled transactions, WATCH+MULTI+commands+EXEC are sent atomically.
        // Therefore, we can only test that WATCH works when no modification occurs.
        String key = "{watch}:key";
        sync.set(key, "original");

        // Create transaction with WATCH - should succeed since no modification between WATCH and EXEC
        TransactionBuilder<String, String> builder = connection.transaction(key);
        builder.queue().set(key, "updated");

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(1);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat(sync.get(key)).isEqualTo("updated");
    }

    @Test
    void transactionWithAddCommand() {
        // Test using addCommand for raw/custom commands
        String key = "{raw}:key";
        StringCodec codec = StringCodec.UTF8;

        TransactionBuilder<String, String> builder = connection.transaction();

        // Use addCommand for SET command
        CommandArgs<String, String> setArgs = new CommandArgs<>(codec).addKey(key).addValue("rawValue");
        RawCommand<String, String> setCommand = RawCommand.of(CommandType.SET, new StatusOutput<>(codec), setArgs);
        builder.addCommand(setCommand);

        // Use commands() API for GET to verify mixing works
        builder.queue().get(key);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(2);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((String) result.get(1)).isEqualTo("rawValue");

        // Verify the value persisted
        assertThat(sync.get(key)).isEqualTo("rawValue");
    }

    @Test
    void transactionWithMultipleAddCommands() {
        // Test using multiple addCommand calls
        String key1 = "{rawmulti}:counter";
        String key2 = "{rawmulti}:name";
        StringCodec codec = StringCodec.UTF8;

        sync.set(key1, "0");

        TransactionBuilder<String, String> builder = connection.transaction();

        // INCR command via addCommand
        CommandArgs<String, String> incrArgs = new CommandArgs<>(codec).addKey(key1);
        builder.addCommand(RawCommand.of(CommandType.INCR, new io.lettuce.core.output.IntegerOutput<>(codec), incrArgs));

        // SET command via addCommand
        CommandArgs<String, String> setArgs = new CommandArgs<>(codec).addKey(key2).addValue("John");
        builder.addCommand(RawCommand.of(CommandType.SET, new StatusOutput<>(codec), setArgs));

        // GET command via addCommand
        CommandArgs<String, String> getArgs = new CommandArgs<>(codec).addKey(key1);
        builder.addCommand(RawCommand.of(CommandType.GET, new ValueOutput<>(codec), getArgs));

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(3);
        assertThat((Long) result.get(0)).isEqualTo(1L); // INCR result
        assertThat((String) result.get(1)).isEqualTo("OK"); // SET result
        assertThat((String) result.get(2)).isEqualTo("1"); // GET result (counter value)
    }

    @Test // WI-12/D7: raw commands carrying a key are slot-validated on the same fail-fast path as typed commands
    void crossSlotRawCommandFailsFast() {
        StringCodec codec = StringCodec.UTF8;
        String key1 = "user:1:name"; // no hash tag -> likely a different slot than key2
        String key2 = "product:1:name";

        TransactionBuilder<String, String> builder = connection.transaction();

        CommandArgs<String, String> args1 = new CommandArgs<>(codec).addKey(key1).addValue("John");
        builder.addCommand(RawCommand.of(CommandType.SET, new StatusOutput<>(codec), args1));

        // A raw command whose first key hashes to a different slot must fail fast, just like the typed path.
        CommandArgs<String, String> args2 = new CommandArgs<>(codec).addKey(key2).addValue("Widget");
        RawCommand<String, String> crossSlot = RawCommand.of(CommandType.SET, new StatusOutput<>(codec), args2);
        assertThatThrownBy(() -> builder.addCommand(crossSlot)).isInstanceOf(RedisException.class)
                .hasMessageContaining("CROSSSLOT");
    }

}
