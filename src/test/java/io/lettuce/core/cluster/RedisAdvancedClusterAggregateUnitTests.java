/*
 * Copyright 2025
 */
package io.lettuce.core.cluster;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for cluster-specific Aggregation cursor behavior: stamping nodeId and routing cursor calls.
 */
class RedisAdvancedClusterAggregateUnitTests {

    private StatefulRedisClusterConnection<String, String> clusterConn;

    private StatefulRedisConnection<String, String> nodeConn;

    private RedisAdvancedClusterAsyncCommandsImpl<String, String> async;

    @BeforeEach
    void setup() {
        clusterConn = mock(StatefulRedisClusterConnection.class);
        nodeConn = mock(StatefulRedisConnection.class);

        // Partitions with a single node that owns all slots
        Partitions partitions = new Partitions();
        RedisClusterNode node = new RedisClusterNode();
        node.setNodeId("node-1");
        node.setUri(io.lettuce.core.RedisURI.Builder.redis("127.0.0.1").withPort(6379).build());
        List<Integer> allSlots = new ArrayList<>();
        for (int i = 0; i < io.lettuce.core.cluster.SlotHash.SLOT_COUNT; i++)
            allSlots.add(i);
        node.setSlots(allSlots);
        node.setFlags(java.util.EnumSet.of(RedisClusterNode.NodeFlag.UPSTREAM));

        partitions.addPartition(node);
        partitions.updateCache();
        // Mock channel writer and async connection provider to satisfy getConnectionAsync(host,port)
        ClusterDistributionChannelWriter writer = mock(ClusterDistributionChannelWriter.class);
        when(clusterConn.getChannelWriter()).thenReturn(writer);
        // Single Mockito mock that implements BOTH interfaces
        ClusterConnectionProvider provider =
                mock(ClusterConnectionProvider.class, withSettings().extraInterfaces(AsyncClusterConnectionProvider.class));
        AsyncClusterConnectionProvider asyncProvider = (AsyncClusterConnectionProvider) provider;
        when(writer.getClusterConnectionProvider()).thenReturn(provider);
        when(asyncProvider.getConnectionAsync(eq(ConnectionIntent.WRITE), anyString(), anyInt()))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(nodeConn));

        when(clusterConn.getPartitions()).thenReturn(partitions);
        when(clusterConn.getConnection(eq("node-1"), eq(ConnectionIntent.WRITE))).thenReturn(nodeConn);

        async = new RedisAdvancedClusterAsyncCommandsImpl<>(clusterConn, StringCodec.UTF8);
    }

    @Test
    void ftAggregate_stampsNodeId_whenCursorCreated() {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .withCursor(AggregateArgs.WithCursor.of(1L)).build();

        AggregationReply<String, String> replyWithCursor = new AggregationReply<>();
        replyWithCursor.setCursor(AggregationReply.Cursor.of(42L, null));

        CompletableFuture<AggregationReply<String, String>> cf = new CompletableFuture<>();
        cf.complete(replyWithCursor);
        io.lettuce.core.RedisFuture<AggregationReply<String, String>> nodeFuture = new io.lettuce.core.cluster.PipelinedRedisFuture<>(
                cf);

        io.lettuce.core.api.async.RedisAsyncCommands<String, String> nodeAsync = mock(
                io.lettuce.core.api.async.RedisAsyncCommands.class);
        when(nodeConn.async()).thenReturn(nodeAsync);
        when(nodeAsync.ftAggregate(anyString(), anyString(), any())).thenReturn(nodeFuture);

        AggregationReply<String, String> out = async.ftAggregate("idx", "*", args).toCompletableFuture().join();

        assertThat(out.getCursor()).isPresent();
        assertThat(out.getCursor().get().getCursorId()).isEqualTo(42L);
        assertThat(out.getCursor().get().getNodeId()).contains("node-1");
    }

    @Test
    void ftCursordel_throwsWhenMissingNodeId() {
        AggregationReply.Cursor cursor = AggregationReply.Cursor.of(5L, null);

        CompletableFuture<String> cf = async.ftCursordel("idx", cursor).toCompletableFuture();

        assertThatThrownBy(cf::join).hasCauseInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing nodeId");
    }

}
