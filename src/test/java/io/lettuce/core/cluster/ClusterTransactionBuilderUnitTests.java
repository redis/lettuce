/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link ClusterTransactionBuilder}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class ClusterTransactionBuilderUnitTests {

    private static final StringCodec codec = StringCodec.UTF8;

    @Mock
    private StatefulRedisClusterConnection<String, String> connection;

    @Mock
    private Partitions partitions;

    @Mock
    private RedisClusterNode master;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(connection.getCodec()).thenReturn(codec);
        when(connection.getOptions()).thenReturn(ClientOptions.create());
    }

    @Test // WI-9/P1: executeAsync() must not block the caller thread on cluster node connection acquisition
    void executeAsyncDoesNotBlockOnNodeResolution() {
        when(connection.getPartitions()).thenReturn(partitions);
        when(partitions.getMasterBySlot(anyInt())).thenReturn(master);
        when(master.getNodeId()).thenReturn("node-1");

        // A never-completing connection acquisition: the old .join() would block here forever.
        CompletableFuture<StatefulRedisConnection<String, String>> pending = new CompletableFuture<>();
        when(connection.getConnectionAsync("node-1")).thenReturn(pending);

        ClusterTransactionBuilder<String, String> builder = new ClusterTransactionBuilder<>(connection, codec);
        builder.queue().set("key", "value"); // sets the target slot and collects the command

        RedisFuture<TransactionResult> future = builder.executeAsync();

        // Returned without blocking: the node connection is still unresolved, so the transaction is not yet done.
        assertThat(future.isDone()).isFalse();
        assertThat(pending).isNotDone();
    }

}
